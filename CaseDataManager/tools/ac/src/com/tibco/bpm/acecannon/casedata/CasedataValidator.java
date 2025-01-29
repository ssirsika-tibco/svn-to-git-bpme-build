/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.StateModel;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 *
 * A simple tree validator that expects any attribute values to be of the appropriate
 * data type.  Attributes are allowed to be missing entirely, or explicitly set null. 
 * Extra attributes are ignored. Constraints beyond the basic type (length, mix/max etc) are not enforced.
 *
 * <p/>&copy;2016 TIBCO Software Inc.
 * @author smorgan
 * @since 2016
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

	public static void validate(StructuredType type, String casedata) throws Exception
	{
		// Read JSON
		JsonNode root = null;
		root = om.readTree(casedata);
		List<Error> errors = new ArrayList<Error>();
		if (!(root instanceof ObjectNode))
		{
			errors.add(new Error(null, "Casedata must be a JSON object"));
		}
		else
		{
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
						for (String state : stateModel.getStates().stream().map(s -> s.getValue())
								.collect(Collectors.toList()))
						{
							if (state.equals(stateValue))
							{
								// We've found a state in the model that matches the provided value.
								foundState = true;
								break;
							}
						}

						if (!foundState)
						{
							errors.add(
									new Error(null, String.format("%s -> State attribute not set to a valid value: %s",
											stateAttribute.getName(), stateValue)));
						}
					}
				}
			}
			validateBranch(type, root, "", errors);
		}

		if (!errors.isEmpty())
		{
			throw new Exception(errors.toString());
		}
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

	private static boolean validateSimpleValue(JsonNode valueNode, Attribute attr, String path, List<Error> errors)
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
			if (!(valueNode instanceof NumericNode))
			{
				errors.add(new Error(path, makeNodeTypeError("NumericNode", valueNode)));
				ok = false;
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
		else if (type == BaseType.USER)
		{
			if (!(valueNode instanceof ObjectNode))
			{
				errors.add(new Error(path, makeNodeTypeError("ObjectNode", valueNode)));
				ok = false;
			}
		}
		else if (type == BaseType.WEBLINK)
		{
			if (!(valueNode instanceof ObjectNode))
			{
				errors.add(new Error(path, makeNodeTypeError("ObjectNode", valueNode)));
				ok = false;
			}
		}
		return ok;
	}

	private static void validateBranch(StructuredType type, JsonNode node, String path, List<Error> errors)
	{
		for (Attribute attr : type.getAttributes())
		{
			StringBuilder newPath = new StringBuilder(path);
			if (newPath.length() != 0)
			{
				newPath.append("/");
			}
			newPath.append(attr.getName());

			AbstractType attrType = attr.getTypeObject();
			JsonNode valueNode = node.at("/" + attr.getName());
			boolean valid = true;
			// If no node is specified, or appears null, do nothing; That's valid.
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
							for (int i = 0; i < arrayNode.size(); i++)
							{
								StringBuilder entryPath = new StringBuilder(newPath);
								entryPath.append("[").append(i).append("]");
								JsonNode arrayEntry = arrayNode.get(i);

								if (arrayEntry instanceof ObjectNode)
								{
									// MergeAPI: All arrays are now arrays of tagged Objects rather than BaseTypes or StructuredTypes
									// This means that there will be _id and value where the value is the original BaseType
									// or StructuredType that has been encapsulated

									JsonNode id = ((ObjectNode) arrayEntry).at("/_id");

									if (!(id instanceof NumericNode))
									{
										errors.add(new Error(entryPath.toString(),
												String.format("Expecting _id properties in array element %d for %s", i,
														attrType.getName())));
									}

									// For all base types other than base:User and base:Group,base:WebLink the actual value
									// (whether that be a string or number) will be held in 
									// a '_value' property.  For base:User and base:Group, there will be no _value
									// property; Instead, the 'id', 'name' and 'email' that make
									// up the user will be at this level, as siblings of _id.

									if (attrType == BaseType.USER || attrType == BaseType.GROUP
											|| attrType == BaseType.WEBLINK)
									{
										if (!attr.isValueValid(arrayEntry))
										{
											errors.add(new Error(entryPath.toString(),
													String.format("Not a valid %s value", attrType.getName())));

										}
									}
									else
									{
										JsonNode value = ((ObjectNode) arrayEntry).at("/_value");

										if (value != null)
										{
											if (validateSimpleValue(value, attr, entryPath.toString(), errors))
											{
												valid = attr.isValueValid(value);
												if (!valid)
												{
													errors.add(new Error(entryPath.toString(), String.format(
															"Not a %s value: %s", attrType.getName(), value.asText())));
												}
											}
										}
										else
										{
											errors.add(new Error(entryPath.toString(),
													String.format(
															"Expecting _value property in array element %d for %s", i,
															attrType.getName())));
										}
									}
								}
								else
								{
									errors.add(new Error(entryPath.toString(),
											"Expected an object in the array encapsulating the base type value with _id associated"));
								}
							}
						}
						else
						{
							// Non-array. Just a single base type value to validate
							if (validateSimpleValue(valueNode, attr, newPath.toString(), errors))
							{
								valid = attr.isValueValid(valueNode);
								if (!valid)
								{
									errors.add(new Error(newPath.toString(), String.format("Not a %s value: %s",
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
							for (int i = 0; i < arrayNode.size(); i++)
							{
								JsonNode arrayEntry = arrayNode.get(i);
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
											newPath.toString() + "[" + i + "]", errors);
								}
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
							validateBranch((StructuredType) attrType, valueNode, newPath.toString(), errors);
						}
					}
				}
			}
		}
	}
}
