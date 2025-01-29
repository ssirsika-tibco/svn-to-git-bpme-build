package com.tibco.bpm.cdm.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 *
 * Utility methods to discover information about supplied casedata with reference to 
 * its corresponding case model.
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class CasedataIntrospector
{
	static CLFClassContext				logCtx					= CloudLoggingFramework.init(CasedataIntrospector.class,
			CDMLoggingInfo.instance);

	private static final String			EMPTY_JSON_OBJECT		= "{}";

	private static final ObjectMapper	om						= new ObjectMapper();
	static
	{
		om.enable(Feature.WRITE_BIGDECIMAL_AS_PLAIN);
	}

	/**
	 * Returns a case summary (JSON ObjectNode) of the supplied case data, created by correlating
	 * the case data with the supplied case type model.  If the case data JSON is bad, or doesn't
	 * match the model, therefore meaning a summary can't be created, this logs a warning
	 * and returns an empty JSON object (this is an edge case and would not ordinarily occur).
	 *
	 * @param casedata
	 * @param caseType
	 * @return
	 */
	public static ObjectNode buildSummary(ObjectNode casedata, StructuredType caseType)
	{
		CLFMethodContext clf = logCtx.getMethodContext("buildSummary");
		clf.local.trace("enter");
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode resultTree = factory.objectNode();
		List<Attribute> summaryAttributes = caseType.getSummaryAttributes();
		for (Attribute attr : summaryAttributes)
		{
			// For the given attribute, see if the casedata contains
			// a value for it.  We always treat represent the value as quoted text
			// in the result, regardless of type (and this is what it _should_ be in the original payload).
			JsonNode value = casedata.at("/" + attr.getName());
			if ((value instanceof ValueNode && !(value instanceof MissingNode)) || value instanceof ObjectNode)
			{
				JsonNode newNode = value.deepCopy();
				resultTree.set(attr.getName(), newNode);
			}
		}
		clf.local.trace("exit");
		return resultTree;
	}

	/**
	 * Returns a case summary (serialized JSON) of the supplied case data, created by correlating
	 * the case data with the supplied case type model.  If the case data JSON is bad, or doesn't
	 * match the model, therefore meaning a summary can't be created, this logs a warning
	 * and returns an empty JSON object (this is an edge case and would not ordinarily occur).
	 *
	 * @param casedata
	 * @param caseType
	 * @return
	 */
	public static String buildSummary(String casedata, StructuredType caseType)
	{
		CLFMethodContext clf = logCtx.getMethodContext("buildSummary");

		try
		{
			clf.local.trace("enter");

			// Attempt to build a JSON object tree from the raw text
			ObjectNode caseTree = null;

			Exception parseFailure = null;
			try
			{
				JsonNode caseTreeNode = om.readTree(casedata.getBytes(StandardCharsets.UTF_8));
				if (caseTreeNode instanceof ObjectNode)
				{
					caseTree = (ObjectNode) caseTreeNode;
				}
				else
				{
					parseFailure = new IllegalArgumentException("Casedata is a "
							+ caseTreeNode.getClass().getSimpleName() + ", but expected an ObjectNode");
				}
			}
			catch (JsonProcessingException e)
			{
				parseFailure = e;
			}
			catch (IOException e)
			{
				parseFailure = e;
			}

			String result = null;
			if (parseFailure != null)
			{
				clf.local.warn(parseFailure, "Casedata not parsable as JSON object node, so returning empty summary");
				result = EMPTY_JSON_OBJECT;
			}
			else
			{
				ObjectNode resultTree = buildSummary(caseTree, caseType);
				try
				{
					result = om.writeValueAsString(resultTree);
				}
				catch (JsonProcessingException e)
				{
					// Serialization shouldn't be able to fail, as we've just created the object
					result = EMPTY_JSON_OBJECT;
				}
			}
			return result;
		}
		finally
		{
			clf.local.trace("exit");
		}
	}

}
