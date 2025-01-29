package com.tibco.bpm.cdm.core.deployment;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.dt.rasc.RuntimeContent;

/**
 * An ArtifaceHandler's job is to take a given type of artifact from a RASC, get its
 * content, and put something in a DeploymentContext that can be later used to perform
 * the deployment.
 * @author smorgan
 * @since 2019
 */
public interface ArtifactHandler
{
	/**
	 * Take the runtimeContent and put appropriate information in the context.
	 * @param runtimeContent
	 * @param context
	 * @throws InternalException
	 * @throws DeploymentException
	 */
	public void handleArtifact(RuntimeContent runtimeContent, DeploymentContext context)
			throws InternalException, DeploymentException;
}
