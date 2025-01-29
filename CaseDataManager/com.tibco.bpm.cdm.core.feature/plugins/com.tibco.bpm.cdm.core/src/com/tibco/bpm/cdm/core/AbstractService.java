package com.tibco.bpm.cdm.core;

import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.de.rest.model.AuthorizationRequest;
import com.tibco.bpm.de.rest.model.AuthorizationResponse;
import com.tibco.bpm.de.rest.model.SystemActionRequest;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.auth.SystemActionId;
import com.tibco.n2.de.api.services.SecurityService;

/**
 * Common base class for service implementation, both private and REST
 * @author smorgan
 * @since 2019
 */
public abstract class AbstractService
{
	static CLFClassContext		logCtx			= CloudLoggingFramework.init(AbstractService.class,
			CDMLoggingInfo.instance);
	
	private SecurityService		securityService	= null;

	protected void logException(CLFMethodContext clf, Exception e)
	{
		// Note that we use the concept of a status code (4XX vs. 500) even in non-REST scenarios.
		// Exception to log will either be a CDMException, or another exception
		// (i.e. one of the private API exceptions) with a CDMException as its cause.
		int httpStatus = 500;
		if (!(e instanceof CDMException))
		{
			Throwable cause = e.getCause();
			if (cause instanceof CDMException)
			{
				httpStatus = ((CDMException) cause).getErrorData().getHTTPStatus();
			}
		}
		else
		{
			httpStatus = ((CDMException) e).getErrorData().getHTTPStatus();
		}

		if (httpStatus < 500)
		{
			// User errors are logged as warnings
			clf.local.warn(e, e.getMessage());
		}
		else
		{
			// System errors are logged as errors
			clf.local.error(e, e.getMessage());
		}
	}
	
	protected boolean isActionAuthorised(SystemActionId eSystemAction)
	{
		CLFMethodContext clf = logCtx.getMethodContext("isActionAuthorised");

		boolean authorised = false;

		String component = String.valueOf(eSystemAction.getComponent());
		String name = String.valueOf(eSystemAction.getName());

		if (null != securityService)
		{
			AuthorizationRequest actions = new AuthorizationRequest();

			SystemActionRequest action = new SystemActionRequest();
			action.setComponent(component);
			action.setAction(name);
			actions.getActions().add(action);

			AuthorizationResponse response;
			try
			{
				response = securityService.isActionAuthorized(actions);

				authorised = response.getOverall().booleanValue();
			}
			catch (Exception e)
			{
				clf.local.info(e, "Exception checking authorisation for %s %s", component, name);
			}
		}

		if (!authorised)
		{
			clf.local.debug("Caller is not authorized for this action %s %s", component, name);
		}

		return authorised;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}
}
