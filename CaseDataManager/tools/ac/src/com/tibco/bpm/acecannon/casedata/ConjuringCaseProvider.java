/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.casedata.ValueConjurer.Option;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.StructuredType;

public class ConjuringCaseProvider implements CaseProvider
{
	private static final ObjectMapper om = new ObjectMapper();
	static
	{
		om.enable(Feature.WRITE_BIGDECIMAL_AS_PLAIN);
	}

	private ConjuringModel	model;

	private StructuredType	type;

	public ConjuringCaseProvider(ConjuringModel model, StructuredType type)
	{
		this.model = model;
		this.type = type;
	}

	private ValueConjurer< ? > constructConjurer(ConjuringNode node) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException
	{

		String conjurerName = node.getConjurer();
		ValueConjurer< ? > conjurer = null;
		if (conjurerName != null)
		{
			Class< ? extends ValueConjurer< ? >> conjurerClass = (Class< ? extends ValueConjurer< ? >>) Class
					.forName(conjurerName);
			try
			{
				// Attempt constructor that takes a type
				Constructor< ? extends ValueConjurer< ? >> constructor = conjurerClass
						.getConstructor(StructuredType.class);
				conjurer = constructor.newInstance(type);
			}
			catch (Exception e)
			{
				try
				{
					// Attempt constructor that takes an attribute
					Constructor< ? extends ValueConjurer< ? >> constructor = conjurerClass
							.getConstructor(Attribute.class);
					conjurer = constructor.newInstance(node.getTargetAttribute());
				}
				catch (Exception e1)
				{
					// Fall back to default constructor
					conjurer = conjurerClass.newInstance();
				}
			}

			Map<Option, Object> optionValuesMap = conjurer.getOptionValues();
			for (Entry<String, Object> entry : node.getOptions().entrySet())
			{
				Option option = conjurer.getOptions().stream().filter(o -> o.getName().equals(entry.getKey()))
						.findFirst().orElse(null);
				if (option != null)
				{
					switch (option.getType())
					{
						case BOOLEAN:
							optionValuesMap.put(option, Boolean.valueOf(entry.getValue().toString()));
							break;
						case INTEGER:
							optionValuesMap.put(option, Integer.valueOf(entry.getValue().toString()));
							break;
						case BIG_DECIMAL:
							optionValuesMap.put(option, new BigDecimal(entry.getValue().toString()));
							break;
						case TEXT_LIST:
							optionValuesMap.put(option, entry.getValue());
							break;
						default:
							optionValuesMap.put(option, entry.getValue());
					}

				}
			}
		}
		return conjurer;
	}

	private JsonNode constructValueNode(Object value)
	{
		JsonNode valueNode = null;
		if (value != null)
		{
			valueNode = value instanceof String ? JsonNodeFactory.instance.textNode(value.toString())
					: (value instanceof Boolean ? JsonNodeFactory.instance.booleanNode((boolean) value)
							: JsonNodeFactory.instance.numberNode(new BigDecimal(value.toString())));
		}
		else
		{
			valueNode = JsonNodeFactory.instance.nullNode();
		}
		return valueNode;
	}

	private void conjuringNodeToChildNode(ConjuringNode node, ObjectNode jsonParent) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if (node.getIsExcluded())
		{
			// Branch excluded, so nothing to do.
			return;
		}
		if (node.getConjurer() != null)
		{
			// Leaf node - conjure value(s)
			ValueConjurer< ? > conjurer = constructConjurer(node);
			AbstractType targetAttributeType = node.getTargetAttribute().getTypeObject();
			if (node.getIsArray())
			{
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				int minSize = node.getArrayMinSize();
				int maxSize = node.getArrayMaxSize();
				int size = (int) (minSize + (Math.random() * (1 + maxSize - minSize)));
				boolean valueIsNull = false;
				for (int i = 0; i < size; i++)
				{
					Object value = conjurer.conjure();
					if (value != null && (targetAttributeType == BaseType.USER || targetAttributeType == BaseType.GROUP
							|| targetAttributeType == BaseType.WEBLINK))
					{
						try
						{
							ObjectNode valueObject = (ObjectNode) om.readTree(value.toString());
							arrayNode.add(valueObject);
						}
						catch (IOException e)
						{
							JsonNode valueNode = constructValueNode(value);
							valueIsNull = value == null;
							arrayNode.add(valueNode);
						}
					}
					else
					{
						JsonNode valueNode = constructValueNode(value);
						valueIsNull = value == null;
						arrayNode.add(valueNode);
					}
				}
				if (!valueIsNull)
				{
					jsonParent.set(node.getName(), arrayNode);
				}
			}
			else
			{

				Object value = conjurer.conjure();
				JsonNode valueNode = constructValueNode(value);
				if (valueNode != null)
				{
					if (targetAttributeType == BaseType.USER || targetAttributeType == BaseType.GROUP
							|| targetAttributeType == BaseType.WEBLINK)
					{
						try
						{
							ObjectNode valueObject = (ObjectNode) om.readTree(value.toString());
							jsonParent.set(node.getName(), valueObject);
						}
						catch (IOException e)
						{
							jsonParent.set(node.getName(), valueNode);
						}
					}
					else
					{
						jsonParent.set(node.getName(), valueNode);
					}
				}
			}
		}
		else
		{
			// Object
			if (node.getIsArray())
			{
				// Array of objects
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				int minSize = node.getArrayMinSize();
				int maxSize = node.getArrayMaxSize();
				int size = (int) (minSize + (Math.random() * (1 + maxSize - minSize)));
				for (int i = 0; i < size; i++)
				{
					ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
					for (ConjuringNode child : node.getAttributes())
					{
						conjuringNodeToChildNode(child, objectNode);
					}
					arrayNode.add(objectNode);
				}
				jsonParent.set(node.getName(), arrayNode);
			}
			else
			{
				// Single object
				ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
				jsonParent.set(node.getName(), objectNode);
				for (ConjuringNode child : node.getAttributes())
				{
					conjuringNodeToChildNode(child, objectNode);
				}
			}
		}
	}

	@Override
	public Gubbins buildCase(BigInteger uniqueId) throws Exception
	{
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		List<ConjuringNode> attributes = new ArrayList<>(model.getAttributes());
		//		Collections.reverse(attributes);
		for (ConjuringNode node : attributes)
		{
			conjuringNodeToChildNode(node, objectNode);
		}

		String json = om.writeValueAsString(objectNode);
		return new Gubbins(json);
	}

	private ConjuringNode getRandomTopLevelNode()
	{
		// Just doing top-level for now
		List<ConjuringNode> attributes = model.getAttributes();
		ConjuringNode result = null;
		do
		{
			int index = (int) (Math.random() * attributes.size());
			if (!attributes.get(index).getName().equals("state"))
			{
				result = attributes.get(index);
			}
			// Keep going until we hit a leaf node that isn't state.
		}
		while (result == null || result.getConjurer() == null);
		return result;
	}

	public String updateCase(String existingCasedata)
			throws JsonProcessingException, IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, ClassNotFoundException
	{
		// Deserialize existing casedata
		ObjectNode caseRoot = (ObjectNode) om.readTree(existingCasedata);

		// Replace a random top-level attribute with a new value
		ConjuringNode conjuringNode = getRandomTopLevelNode();
		ValueConjurer< ? > conjurer = constructConjurer(conjuringNode);
		Object newValue = conjurer.conjure();
		JsonNode valueNode = constructValueNode(newValue);
		caseRoot.set(conjuringNode.getName(), valueNode);

		// Convert back to JSON
		om.enable(SerializationFeature.INDENT_OUTPUT);
		String json = om.writeValueAsString(caseRoot);
		return json;
	}

	@Override
	public String setVersion(String oldCasedata, int version)
	{
		// TODO Actually change the content!
		return oldCasedata;
	}

	@Override
	public BigInteger getAppId()
	{
		return new BigInteger(model.getApplicationId());
	}

	@Override
	public String incrementVersion(String oldCasedata) throws JsonProcessingException, IOException
	{
		// TODO Actually change the content!
		return oldCasedata;
	}

}
