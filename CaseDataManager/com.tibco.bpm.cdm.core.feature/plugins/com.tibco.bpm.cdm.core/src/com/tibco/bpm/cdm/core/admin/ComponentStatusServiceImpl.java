package com.tibco.bpm.cdm.core.admin;

import com.tibco.bpm.ace.admin.api.ComponentStatusService;
import com.tibco.bpm.ace.admin.model.ComponentHealthyStatus;
import com.tibco.bpm.ace.admin.model.ComponentReadyStatus;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Implementation of ACE Admin's ComponentStatusService interface.
 * At the time of writing, the mere fact this bean can be reached indicates
 * that CDM is healthy and ready.
 * 
 * @author smorgan
 * @since 2019
 */
public class ComponentStatusServiceImpl implements ComponentStatusService
{
	static CLFClassContext		logCtx			= CloudLoggingFramework.init(ComponentStatusServiceImpl.class,
			CDMLoggingInfo.instance);

	private static final String	COMPONENT_NAME	= "CDM";

	public ComponentStatusServiceImpl()
	{
		CLFMethodContext clf = logCtx.getMethodContext("ComponentStatusServiceImpl");
		clf.local.debug("Constructing instance of CDM's ComponentStatusService implementation");
	}

	/**
	 * Returns the ready status of the CDM component.
	 *
	 * @return the ready status of the CDM component
	 */
	public ComponentReadyStatus getComponentReadyStatus()
	{
		// Always ready.
		CLFMethodContext clf = logCtx.getMethodContext("getComponentReadyStatus");
		clf.local.debug("getComponentReadyStatus returning true");
		return new ComponentReadyStatus(COMPONENT_NAME, true);
	}

	/**
	 * Returns the healthy status of the CDM component.
	 *
	 * @return the healthy status of the CDM component
	 */
	public ComponentHealthyStatus getComponentHealthyStatus()
	{
		// Always healthy.
		CLFMethodContext clf = logCtx.getMethodContext("getComponentHealthyStatus");
		clf.local.debug("getComponentHealthyStatus returning true");
		return new ComponentHealthyStatus(COMPONENT_NAME, true);
	}
}
