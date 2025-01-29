package com.tibco.bpm.cdm.core.deployment;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.core.deployment.DeploymentContext.DataModelArtifact;
import com.tibco.bpm.dt.rasc.RuntimeContent;

/**
 * Script Artifacts Handler
 *
 *
 * @author jaugusti
 * @since 16 Apr 2019
 */
public class ScriptArtifactHandler extends AbstractArtifactHandler implements ArtifactHandler
{
	@Override
	public void handleArtifact(RuntimeContent content, DeploymentContext context)
			throws InternalException, DeploymentException
	{
		String name = content.getName();
		String script = getContentsAsUTF8String(content);
		String key = name.substring(0, name.lastIndexOf(".js"));
		DataModelArtifact dataModelArtifact = context.mapOfDataModelArtifacts.get(key);
		if (dataModelArtifact != null)
		{
			dataModelArtifact.setScript(script);
		}
		else
		{
			dataModelArtifact = new DataModelArtifact(null, null, script);
			context.mapOfDataModelArtifacts.put(key, dataModelArtifact);
		}
	}
}
