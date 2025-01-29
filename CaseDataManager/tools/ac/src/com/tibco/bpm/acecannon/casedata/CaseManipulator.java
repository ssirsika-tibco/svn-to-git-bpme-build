/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StructuredType;

public class CaseManipulator
{
	private static final ObjectMapper	om				= new ObjectMapper();

	private static final String			NOW_CONSTANT	= "now";

	private static final String			STATE_ATTR_NAME	= "state";

	private static final String			TIME_FORMAT		= "HH:mm";

	private static final String			DATE_FORMAT		= "yyyy-MM-dd";

	private static String createDate(String value)
	{
		// If 'now', calculate value, else use as-is from model
		String result = value;
		if (NOW_CONSTANT.equals(value))
		{
			result = new SimpleDateFormat(DATE_FORMAT).format(System.currentTimeMillis());
		}
		return result;
	}

	private static String createTime(String value)
	{
		// If 'now', calculate value, else use as-is from model
		String result = value;
		if (NOW_CONSTANT.equals(value))
		{
			result = new SimpleDateFormat(TIME_FORMAT).format(System.currentTimeMillis());
		}
		return result;
	}

	public static String applyNonArrayInitialValuesAndSetState(StructuredType type, String casedata, BigInteger stateId)
			throws JsonProcessingException, IOException
	{
		// Deserialize the existing casedata
		ObjectNode root = (ObjectNode) om.readTree(casedata);
		ObjectNode node = applyNonArrayInitialValuesAndSetState(type, root, stateId);
		String json = node != null ? om.writeValueAsString(node) : null;
		return json;
	}

	private static ObjectNode applyNonArrayInitialValuesAndSetState(StructuredType type, ObjectNode node,
			BigInteger stateId) throws JsonProcessingException, IOException
	{
		JsonNodeFactory factory = JsonNodeFactory.instance;

		// For each attribute in the model...
		for (Attribute attr : type.getAttributes())
		{
			// We ignore array attributes
			if (!attr.getIsArray())
			{
				AbstractType attrType = attr.getTypeObject();
				String attrName = attr.getName();

				if (attrType instanceof BaseType)
				{
					// It's a base type attribute, so if it has no existing value, but an initial value
					// is defined, apply it now.
					String initialValue = attr.getInitialValue();

					if (initialValue != null)
					{
						// An initial value is defined.  If the containing node doesn't
						// exist yet, we create it now (and will definitely end up applying the initial
						// value as it can't possible have an existing value).
						if (node == null)
						{
							node = factory.objectNode();
						}

						// Unless the containing node already contains a value for this attribute,
						// go ahead and set it.
						if (!node.has(attrName))
						{
							JsonNode valueNode = null;
							if (attrType == BaseType.TEXT)
							{
								// Text becomes text in JSON ("myAttr": "hello")
								valueNode = factory.textNode(initialValue);
							}
							else if (attrType == BaseType.NUMBER)
							{
								// Number becomes unquoted in JSON ("myAttr": 123)
								valueNode = factory.numberNode(new BigDecimal(initialValue));
							}
							else if (attrType == BaseType.DATE)
							{
								// Date become a quoted string, honouring the special 'now' constant
								valueNode = factory.textNode(createDate(initialValue));
							}
							else if (attrType == BaseType.TIME)
							{
								// ...as does Time
								valueNode = factory.textNode(createTime(initialValue));
							}

							// Set it in the tree
							node.set(attrName, valueNode);
						}
					}
				}
				else if (attrType instanceof StructuredType)
				{
					// It's a structured type, so recurse into it
					// (passing null as state argument; state is only of interest at the top level)

					ObjectNode childNode = null;
					if (node != null && node.has(attrName))
					{
						if (node.get(attrName) instanceof ObjectNode)
						{
							childNode = (ObjectNode) node.get(attrName);
						}
					}

					// If no existing child object node was found, we pass null.  We don't yet know
					// whether we'll encounter any initial values further down the tree.  If we don't,
					// and we don't already have a childNode, then we don't want to add one (as it will be empty).
					boolean childNodeAlreadyExisted = childNode != null;
					childNode = applyNonArrayInitialValuesAndSetState((StructuredType) attrType, childNode, null);

					if (childNode != null && !childNodeAlreadyExisted)
					{
						// The child node has just been created as a result of initial values being applied.
						// If the container doesn't yet exist, create it now.
						if (node == null)
						{
							node = factory.objectNode();
						}

						// Add the child to the container.
						node.set(attrName, childNode);
					}
				}
			}
		}

		if (stateId != null)
		{
			// A stateId was supplied.  
			// Look up the value corresponding to the supplied id.
			State newState = type.getStateModel().getStateById(stateId);
			if (newState != null)
			{
				// Found it.  Set the value in the casedata (which will replace any existing
				// value).
				if (node == null)
				{
					node = factory.objectNode();
				}
				node.set(STATE_ATTR_NAME, factory.textNode(newState.getValue()));
			}
			else
			{
				throw new IllegalArgumentException("No state in model for id " + stateId);
			}
		}

		return node;
	}

}