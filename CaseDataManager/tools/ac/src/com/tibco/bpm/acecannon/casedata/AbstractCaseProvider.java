/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractCaseProvider implements CaseProvider
{
	private static final ObjectMapper om = new ObjectMapper();

	protected String bumpNumericProperty(String oldCasedata, String propertyName)
			throws JsonProcessingException, IOException
	{
		JsonNode tree = om.readTree(oldCasedata);
		if (tree.has(propertyName))
		{
			JsonNode versionNode = tree.at("/" + propertyName);
			if (versionNode instanceof DoubleNode)
			{
				double currentValue = new Double(versionNode.asText());
				double newValue = currentValue + 1;
				((ObjectNode) tree).put(propertyName, newValue);
			}
			else
			{
				int currentValue = new Integer(versionNode.asText());
				int newValue = currentValue + 1;
				((ObjectNode) tree).put(propertyName, newValue);
			}
		}
		return om.writeValueAsString(tree);
	}

	protected static String setLength(String original, int length)
	{
		StringBuilder buf = new StringBuilder();
		while (buf.length() < length)
		{
			buf.append(original);
		}
		if (buf.length() > length)
		{
			buf.setLength(length);
		}
		return buf.toString();
	}
}
