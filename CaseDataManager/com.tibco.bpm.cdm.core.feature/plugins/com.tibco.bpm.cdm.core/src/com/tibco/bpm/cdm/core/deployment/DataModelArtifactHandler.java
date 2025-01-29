package com.tibco.bpm.cdm.core.deployment;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.core.deployment.DeploymentContext.DataModelArtifact;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.dt.rasc.RuntimeContent;

/**
 * Processes a DataModel artifact from the RASC, adding content to the DeploymentContext.
 * @author smorgan
 *
 */
public class DataModelArtifactHandler extends AbstractArtifactHandler implements ArtifactHandler
{
	@Override
	public void handleArtifact(RuntimeContent content, DeploymentContext context)
			throws InternalException, DeploymentException
	{
		try
		{
			String json = getContentsAsUTF8String(content);
			DataModel dm = DataModel.deserialize(json);
			String name = content.getName();
			String key = name.substring(0, name.lastIndexOf(".dm"));
			DataModelArtifact dataModelArtifact = context.mapOfDataModelArtifacts.get(key);
			if (dataModelArtifact != null)
			{
				dataModelArtifact.setJson(json);
				dataModelArtifact.setDataModel(dm);
			}
			else
			{
				dataModelArtifact = new DataModelArtifact(dm, json, null);
				context.mapOfDataModelArtifacts.put(key, dataModelArtifact);
			}
		}
		catch (DataModelSerializationException e)
		{
			throw DeploymentException.newDataModelDeserializationFailed(e);
		}
	}
}
