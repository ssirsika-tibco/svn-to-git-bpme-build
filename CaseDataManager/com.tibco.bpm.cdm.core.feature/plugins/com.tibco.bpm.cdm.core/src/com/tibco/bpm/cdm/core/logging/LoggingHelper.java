package com.tibco.bpm.cdm.core.logging;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.CDMException.MetadataEntry;

public class LoggingHelper
{
	private static final ObjectMapper om = new ObjectMapper();

	private static String buildMetadataEntriesIntoJsonObject(List<MetadataEntry> metadataEntries)
	{
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		for (MetadataEntry metadataEntry : metadataEntries)
		{
			objectNode.put(metadataEntry.getName(), metadataEntry.getValue());
		}
		String json;
		try
		{
			json = om.writeValueAsString(objectNode);
		}
		catch (JsonProcessingException e)
		{
			json = "Failed to convert metadata entries to JSON: " + e.getMessage();
		}
		return json;
	}

	public static com.tibco.bpm.logging.cloud.context.CLFMethodContext.Audit addLoggingAttributesFromExceptionMetadata(
			com.tibco.bpm.logging.cloud.context.CLFMethodContext.Audit log, Exception e)
	{
		if (e instanceof CDMException)
		{
			CDMException cdme = (CDMException) e;
			List<MetadataEntry> sensitiveMetadataEntries = cdme.getSensitiveMetadataEntries();
			List<MetadataEntry> nonSensitiveMetadataEntries = cdme.getNonSensitiveMetadataEntries();
			if (!sensitiveMetadataEntries.isEmpty())
			{
				log = log.attribute(CDMLoggingInfo.CDM_METADATA_SENSITIVE,
						buildMetadataEntriesIntoJsonObject(sensitiveMetadataEntries));
				nonSensitiveMetadataEntries.add(new MetadataEntry("sensitiveMetadataCount",
						String.valueOf(sensitiveMetadataEntries.size()), false));
			}
			//			if (!nonSensitiveMetadataEntries.isEmpty())
			//			{
			//				techLog = techLog.attribute(SharedLoggingInfo.CDM_SHARED_METADATA,
			//						buildMetadataEntriesIntoJsonObject(nonSensitiveMetadataEntries));
			//			}
		}
		return log;
	}
}
