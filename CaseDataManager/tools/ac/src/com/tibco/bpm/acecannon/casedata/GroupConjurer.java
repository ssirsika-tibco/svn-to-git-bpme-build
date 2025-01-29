package com.tibco.bpm.acecannon.casedata;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.common.EntityReference;

public class GroupConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private List<EntityReference>			entityReferences;

	private static final ObjectMapper		om	= new ObjectMapper();

	private static final JsonNodeFactory	fac	= JsonNodeFactory.instance;

	public GroupConjurer(Attribute attribute)
	{
		this.entityReferences = attribute.getEntityReferences();
	}

	@Override
	public String conjure()
	{
		String json;
		if (entityReferences.isEmpty())
		{
			json = "ERROR: No entityReferences";
		}
		else
		{
			int idx = ThreadLocalRandom.current().nextInt(entityReferences.size());
			EntityReference entityReference = entityReferences.get(idx);
			ObjectNode node = fac.objectNode();
			node.set("id", fac.textNode(entityReference.getEntityId()));
			node.set("name", fac.textNode(entityReference.getCachedName()));
			node.set("type", fac.textNode("base:Group"));
			try
			{
				json = om.writeValueAsString(node);
			}
			catch (JsonProcessingException e)
			{
				json = "ERROR: " + e.getMessage();
				e.printStackTrace();
			}
		}
		return json;
	}

	@Override
	public String getDescription()
	{
		return "Makes a JSON object suitable for attributes of the base:Group type.  These have 'id', 'name' and 'type' properties."
				+ "\n\nNote: If this generator is selected for non-object attributes, the value will be written as a string, "
				+ "rather than an object.  The value will be picked from those offered in the attribute's 'entityReferences' list.";
	}

}
