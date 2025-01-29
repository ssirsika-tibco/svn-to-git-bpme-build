package com.tibco.bpm.cdm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.tibco.bpm.cdm.api.exception.ValidationException;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.AllowedValue;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.IdentifierInitialisationInfo;
import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StateModel;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * A simple tree validator that expects any attribute values to be of the appropriate
 * data type.  Attributes are allowed to be missing entirely, or explicitly set null, unless they are mandatory. 
 * 
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class CasedataValidator
{
	private static final ObjectMapper om = new ObjectMapper();

	public static class Error
	{
		public String	path;

		public String	message;

		public Error(String path, String message)
		{
			this.path = path;
			this.message = message;
		}

        public String toString()
		{
			return path == null ? message : String.format("%s -> %s", path, message);
		}
	}

	/**
     * Validate the given casedata against the given type.
     * 
	 * Can optionally:
	 * - Apply default values.  If no value is set for the attribute, but a default value is configured
	 *   in the model, setting applyDefaultValues=true will cause this value to be applied.
	 * - Ignore missing mandatory attributes when a default value is configured.  When validating casedata, but
	 *   not intending to persist it (so not wishing for it to be altered by this method), set applyDefaultValues=false,
	 *   but allowMissingMandatoriesWhenDefaultValueConfigured=true to prevent missing mandatory attributes being
	 *   considered invalid if there is a default value configured (i.e. the default value will ultimately be applied
	 *   when the case is persisted).
	 * - Remove superfluous content.  If removeSuperfluousProperties=true, then any properties in the casedata that are
	 *   not recognised are removed.  The returned map details what was removed.
	 * - Remove properties that are explicitly set null (e.g. "myProp":null). It is important that this flag is used when
	 *   intending to subsequently persist casedata in the database.  This is because queries that process DQL null checks
	 *   (e.g. "myProp = null") will detect absent properties, but NOT those explicitly set null.  Although the latter is
	 *   possible, a method for doing this that makes use of the attribute-specific table index has not been found; The problem
	 *   is avoided by ensuring explicit nulls don't appear in the casedata. As a bonus, this makes the casedata that bit smaller.
     * 
     * Returns a map of any superfluous content removed from the casedata.
     * 
     * @param type
     * @param root
     * @param applyDefaultValues
     * @param allowMissingMandatoriesWhenDefaultValueConfigured
     * @param removeNulls
     * @param strictTypeValidation if true, perform strict type check
     * @return Map from JSON path to removed content
     * @throws ValidationException
     */
	public static Map<String, String> validate(StructuredType type, JsonNode root, boolean applyDefaultValues,
			boolean allowMissingMandatoriesWhenDefaultValueConfigured, boolean allowSuperfluousProperties,
            boolean removeSuperfluousProperties, boolean removeNulls,
            boolean strictTypeValidation) throws ValidationException
	{
		Map<String, String> removals = new HashMap<>(); 
		List<Error> errors = new ArrayList<Error>();
		if (!(root instanceof ObjectNode))
		{
			errors.add(new Error(null, "Casedata must be a JSON object"));
		}
		else
		{
			validateBranch(type, root, "", errors, removals, applyDefaultValues,
					allowMissingMandatoriesWhenDefaultValueConfigured, allowSuperfluousProperties,
                    removeSuperfluousProperties, removeNulls, strictTypeValidation);

			// Check state attribute value
			Attribute stateAttribute = type.getStateAttribute();
			// If type _has_ as state attribute... (it will, because it would have been rejected
			// at deployment-time otherwise)
			if (stateAttribute != null)
			{
				// Get the provided value for the state attribute
				JsonNode stateNode = root.at("/" + stateAttribute.getName());

				if (stateNode instanceof MissingNode || stateNode instanceof NullNode)
				{
					if (!(allowMissingMandatoriesWhenDefaultValueConfigured
                            && stateAttribute.getDefaultValue() != null)
                            && strictTypeValidation)
						errors.add(new Error(null, "State attribute not set"));
				}
				else
				{
					String stateValue = stateNode.asText();
					StateModel stateModel = type.getStateModel();

					// Compare value to the statemodel (Again, there _will_ be a state model
					// as that's validated on deployment).
					if (stateModel != null)
					{
						boolean foundState = false;
						for (String state : stateModel.getStates().stream().map(State::getValue)
								.collect(Collectors.toList()))
						{
							if (state.equals(stateValue))
							{
								// We've found a state in the model that matches the provided value.
								foundState = true;
								break;
							}
						}

                        if (!foundState && strictTypeValidation)
						{
							errors.add(
									new Error(null, String.format("%s -> State attribute not set to a valid value: %s",
											stateAttribute.getName(), stateValue)));
						}
					}
				}
			}
		}

		if (!errors.isEmpty())
		{
			throw ValidationException.newInvalid(errors.toString());
		}
		return removals;
	}

    /**
     * Default API to perform the strict type check.
     */
    public static Map<String, String> validate(StructuredType type,
            JsonNode root, boolean applyDefaultValues,
            boolean allowMissingMandatoriesWhenDefaultValueConfigured,
            boolean allowSuperfluousProperties,
            boolean removeSuperfluousProperties, boolean removeNulls)
            throws ValidationException {

        return validate(type, root, applyDefaultValues,
                allowMissingMandatoriesWhenDefaultValueConfigured,
                allowSuperfluousProperties, removeSuperfluousProperties, removeNulls, true);
    }

	private static String makeNodeTypeError(String expectedNodeTypeName, JsonNode actualNode)
	{
		StringBuilder buf = new StringBuilder("Expected ");
		buf.append(expectedNodeTypeName);
		buf.append(" but found ");
		buf.append(actualNode.getClass().getSimpleName());
		buf.append(": ");
		buf.append(actualNode);
		return buf.toString();
	}

	private static boolean validateSimpleValue(JsonNode valueNode, Attribute attr, String path, List<Error> errors,boolean isArray)
	{
		boolean ok = true;
		AbstractType type = attr.getTypeObject();
		if (type == BaseType.TEXT)
		{
			if (!(valueNode instanceof TextNode))
			{
				errors.add(new Error(path, makeNodeTypeError("TextNode", valueNode)));
				ok = false;
			}
		}
		else if (type == BaseType.NUMBER)
		{
			//if it is a numeric node we are fine, if not do some checks
			if(!(valueNode instanceof NumericNode)) {
				// we know it is numeric node however we know that for arrays it is passed
				// text array so check for text node
				if(isArray && valueNode instanceof TextNode) {
					TextNode t= (TextNode)valueNode;
					try {
						Double.parseDouble(t.asText());
					} catch(Exception e) {
						//ignore the exception and mark it as error
						errors.add(new Error(path, makeNodeTypeError("NumericNode", valueNode)));
						ok = false;
					}
				} else {//in all other cases it is not valid
					errors.add(new Error(path, makeNodeTypeError("NumericNode", valueNode)));
					ok = false;
				}
			}
		}
		else if (type == BaseType.FIXED_POINT_NUMBER)
		{
			//if it is a numeric node we are fine, if not do some checks
			if(!(valueNode instanceof NumericNode)) {
				// we know it is numeric node however we know that for arrays it is passed
				// text array so check for text node
				if(isArray && valueNode instanceof TextNode) {
					TextNode t= (TextNode)valueNode;
					try {
						Double.parseDouble(t.asText());
					} catch(Exception e) {
						//ignore the exception and mark it as error
						errors.add(new Error(path, makeNodeTypeError("NumericNode", valueNode)));
						ok = false;
					}
				} else {//in all other cases it is not valid
					errors.add(new Error(path, makeNodeTypeError("NumericNode", valueNode)));
					ok = false;
				}
			}
		}
		else if (type == BaseType.DATE)
		{
			if (!(valueNode instanceof TextNode))
			{
				errors.add(new Error(path, makeNodeTypeError("TextNode", valueNode)));
				ok = false;
			}
		}
		else if (type == BaseType.TIME)
		{
			if (!(valueNode instanceof TextNode))
			{
				errors.add(new Error(path, makeNodeTypeError("TextNode", valueNode)));
				ok = false;
			}
		}
		else if (type == BaseType.DATE_TIME_TZ)
		{
			if (!(valueNode instanceof TextNode))
			{
				errors.add(new Error(path, makeNodeTypeError("TextNode", valueNode)));
				ok = false;
			}
		}
		else if (type == BaseType.URI)
		{
			if (!(valueNode instanceof TextNode))
			{
				errors.add(new Error(path, makeNodeTypeError("TextNode", valueNode)));
				ok = false;
			}
		}

		// If there are allowed values, check our value is one of them
		List<AllowedValue> allowedValues = attr.getAllowedValues();
		if (!allowedValues.isEmpty())
		{
			String text = valueNode.asText();
			if (!(allowedValues.stream().anyMatch(av -> av.getValue().equals(text))))
			{
				errors.add(new Error(path, String.format("Value does not match an allowed value: %s", text)));
				ok = true;
			}
		}

		return ok;
	
	}

	private static ValueNode makeDefaultValueNode(Attribute attr)
	{
		ValueNode result = null;
		String defaultValue = attr.getDefaultValue();
		JsonNodeFactory fac = JsonNodeFactory.instance;
		AbstractType type = attr.getTypeObject();
		if (type == BaseType.BOOLEAN)
		{
			if ("true".equals(defaultValue))
			{
				result = fac.booleanNode(true);
			}
			else if ("false".equals(defaultValue))
			{
				result = fac.booleanNode(false);
			}
		}
		else if (type == BaseType.NUMBER || type == BaseType.FIXED_POINT_NUMBER)
		{
			if (defaultValue.indexOf('.') != -1)
			{
				result = fac.numberNode(Double.parseDouble(defaultValue));
			}
			else
			{
				result = fac.numberNode(Long.parseLong(defaultValue));
			}
		}
		else
		{
			result = fac.textNode(defaultValue);
		}
		return result;
	}

	private static void validateBranch(StructuredType type, JsonNode node, String path, List<Error> errors,
			Map<String, String> removals, boolean applyDefaultValues,
			boolean allowMissingMandatoriesWhenDefaultValueConfigured, boolean allowSuperfluousProperties,
            boolean removeSuperfluousProperties, boolean removeNulls,
            boolean strictTypeValidation)
	{
		// is it a auto identifier
		boolean isAutoIdentifier=false;
		IdentifierInitialisationInfo identifierInitialisationInfo = type.getIdentifierInitialisationInfo();
		if(null!=identifierInitialisationInfo) {
			isAutoIdentifier=true;
		}
		if (!(node instanceof ObjectNode))
		{
			errors.add(new Error(path.toString(), "Expected object node"));
		}
		else
		{
			for (Attribute attr : type.getAttributes())
			{
				
				//check if it is a auto identifier and then ignore validation
				if(isAutoIdentifier) {
					if(attr.getIsIdentifier()) continue;
				}
				StringBuilder newPath = new StringBuilder(path);
				if (newPath.length() != 0)
				{
					newPath.append("/");
				}
				newPath.append(attr.getName());

				AbstractType attrType = attr.getTypeObject();

				JsonNode valueNode = ((ObjectNode) node).get(attr.getName());

				if (removeNulls)
				{
					if (valueNode instanceof NullNode)
					{
						// We've been asked to remove properties that are explicitly set null (e.g. "myProp":null),
						// so remove it.
						((ObjectNode) node).remove(attr.getName());
						valueNode = null;
					}
				}

				boolean valid = true;
				// If there is no value, apply the default value, if the model specifies one
				if (valueNode == null || valueNode instanceof NullNode || valueNode instanceof MissingNode)
				{
					// Only applies to non-array attributes
					if (!attr.getIsArray())
					{
						// This is a non-array attribute with no value. If we've been asked to apply
						// default values, apply now, if one is configured.
						if (applyDefaultValues)
						{
							String defaultValue = attr.getDefaultValue();
							if (defaultValue != null)
							{
								defaultValue = defaultValue.trim();
								if (!defaultValue.isEmpty())
								{
									valueNode = makeDefaultValueNode(attr);
									((ObjectNode) node).set(attr.getName(), valueNode);
								}
							}
						}
					}
				}

				if (valueNode != null && !(valueNode instanceof MissingNode) && !(valueNode instanceof NullNode))
				{
					// Validate arrayness
					if (valueNode instanceof ArrayNode)
					{
						if (!attr.getIsArray())
						{
							errors.add(new Error(newPath.toString(), "Got array but didn't expect one"));
							valid = false;
						}
					}
					else
					{
						if (attr.getIsArray())
						{
							errors.add(new Error(newPath.toString(), "Expecting array but got a single value"));
							valid = false;
						}
					}

					// Perform appropriate validation depending on attribute type (base type vs. structured type object)
					if (valid)
					{
						if (attrType instanceof BaseType)
						{
							if (attr.getIsArray())
							{
								// Array of base type, so validate each value
								ArrayNode arrayNode = (ArrayNode) valueNode;

                                if (attr.getIsMandatory() && arrayNode.size() == 0
                                        && strictTypeValidation)
								{
									// Mandatory array attribute contains no elements
									errors.add(new Error(newPath.toString(),
											"Mandatory array attribute contains no values"));
								}

								for (int i = 0; i < arrayNode.size();)
								{
									StringBuilder entryPath = new StringBuilder(newPath);
									entryPath.append("[").append(i).append("]");
									JsonNode arrayEntry = arrayNode.get(i);
									if (arrayEntry instanceof NullNode && removeNulls)
									{
										// The array entry is null. We've been asked to remove nulls, so
										// remove it - and don't do i++ as the remaining elements will shuffle up.
										arrayNode.remove(i);
									}
									else if (validateSimpleValue(arrayEntry, attr, entryPath.toString(), errors,true))
									{
										AbstractType objType = attr.getTypeObject();
										//number arrays are dealt separately so validation needs to be done
										//on the types rather than values
										if(objType == BaseType.NUMBER || objType == BaseType.FIXED_POINT_NUMBER) {
											double asDouble = arrayEntry.asDouble();
											NumericNode numberNode = JsonNodeFactory.instance.numberNode(asDouble);
											attr.isValueValid(numberNode);
										}else {
											valid = attr.isValueValid(arrayEntry);
										}
										
										
                                        if (!valid && strictTypeValidation)
										{
											errors.add(new Error(entryPath.toString(), String.format(
													"Not a %s value: %s", attrType.getName(), arrayEntry.toString())));
										}	
									}
									
									i++;

								}
							}
							else
							{
								// Non-array. Just a single base type value to validate
								if (validateSimpleValue(valueNode, attr, newPath.toString(), errors,false))
								{
									valid = attr.isValueValid(valueNode);
                                    if (!valid & strictTypeValidation)
									{
										errors.add(new Error(newPath.toString(),
												String.format("Not a %s%s value: %s",
														attr.getConstraints().isEmpty() ? "" : "(constrained) ",
														attrType.getName(), valueNode.toString())));
									}
								}
							}
						}
						else if (attrType instanceof StructuredType)
						{
							if (attr.getIsArray())
							{
								// Array of structured type objects, so validate each value
								ArrayNode arrayNode = (ArrayNode) valueNode;
								for (int i = 0; i < arrayNode.size();)
								{
									JsonNode arrayEntry = arrayNode.get(i);
									if (arrayEntry instanceof NullNode && removeNulls)
									{
										// The array entry is null. We've been asked to remove nulls, so
										// remove it - and don't do i++ as the remaining elements will shuffle up.
										arrayNode.remove(i);
									}
									else
									{
										if (!(arrayEntry instanceof ObjectNode))
										{
											errors.add(new Error(newPath.toString() + "[" + i + "]",
													"Expected an object in the array but found "
															+ arrayEntry.getClass().getSimpleName()));
										}
										else
										{
											// Recurse into the nested object
											validateBranch((StructuredType) attrType, arrayEntry,
													newPath.toString() + "[" + i + "]", errors, removals,
													applyDefaultValues,
													allowMissingMandatoriesWhenDefaultValueConfigured,
													allowSuperfluousProperties, removeSuperfluousProperties,
                                                    removeNulls, strictTypeValidation);
										}
										
									}
									i++;
								}
							}
							else
							{
								// Non-array. Just a single structured type object to validate.
								if (!(valueNode instanceof ObjectNode))
								{
									errors.add(new Error(newPath.toString(),
											"Expected an object but found " + valueNode.getClass().getSimpleName()));
								}
								// Recurse into the nested object
								validateBranch((StructuredType) attrType, valueNode, newPath.toString(), errors,
										removals, applyDefaultValues, allowMissingMandatoriesWhenDefaultValueConfigured,
                                        allowSuperfluousProperties, removeSuperfluousProperties, removeNulls,
                                        strictTypeValidation);
							}
						}
					}
				}
				else
				{
					// Node missing. If it's mandatory, that's invalid, unless we've been told to allow
					// missing mandatory attributes when a default value is configured (i.e. because we
					// know it will ultimately be given that value before being persisted.).
                    if (attr.getIsMandatory() && strictTypeValidation
							&& !(attr.getDefaultValue() != null && allowMissingMandatoriesWhenDefaultValueConfigured))
					{
						errors.add(new Error(newPath.toString(), "Mandatory attribute has no value"));
					}
				}
			}
		}

		if (removeSuperfluousProperties || !allowSuperfluousProperties)
		{
			// Check for superfluous properties
			if (node instanceof ObjectNode)
			{
				// As we'll potentially be removing properties and ObjectNode doesn't
				// provide a ListIterator of names, copy the field names into a 
				// temporary list that be be list-iterated over.
				List<String> fieldNames = new ArrayList<>();
				((ObjectNode) node).fieldNames().forEachRemaining(f -> fieldNames.add(f));

				for (ListIterator<String> iter = fieldNames.listIterator(); iter.hasNext();)
				{
					String propertyName = iter.next();
					Attribute attr = type.getAttributeByName(propertyName);
					if (attr == null)
					{
						// Superfluous property found
						String superfluousPropertyPath = (path != null && "".equals(path) ? "" : path + "/")
								+ propertyName;
						if (removeSuperfluousProperties)
						{
							// Record the fact we've removed unrecognised content
							String removedValue = null;
							try
							{
								removedValue = om.writeValueAsString(node.get(propertyName));
							}
							catch (JsonProcessingException e)
							{
							}
							removals.put(superfluousPropertyPath, removedValue);
							((ObjectNode) node).remove(propertyName);
						}
						else if (!allowSuperfluousProperties)
						{
							// We've found a superfluous property, but were not told to remove it and
							// have been told we shouldn't allow it. Therefore, this is an error.
							errors.add(new Error(superfluousPropertyPath,
									"Type has no attribute called '" + propertyName + "'"));
						}
					}
				}
			}
		}
	}
}
