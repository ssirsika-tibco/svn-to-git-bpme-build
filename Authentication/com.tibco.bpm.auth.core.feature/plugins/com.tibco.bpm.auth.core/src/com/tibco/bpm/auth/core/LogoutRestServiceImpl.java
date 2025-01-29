/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.core;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.tibco.bpm.auth.api.AuthLogger;
import com.tibco.bpm.auth.api.BPMLogoutHandler;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class LogoutRestServiceImpl implements LogoutRestService {

	static CLFClassContext logCtx = CloudLoggingFramework.init(LogoutRestServiceImpl.class, AuthLoggingInfo.instance);

	@Context
	private HttpServletRequest httpServletRequest;

	private BPMLogoutHandler logoutHandler;

	public BPMLogoutHandler getLogoutHandler() {
		return logoutHandler;
	}

	public void setLogoutHandler(BPMLogoutHandler logoutHandler) {
		this.logoutHandler = logoutHandler;
	}

	@Override
	public Response logout() throws Exception{
		CLFMethodContext clf = logCtx.getMethodContext("logout");
		AuthLogger.debug("Logging out of the session");
		logoutFromSession(httpServletRequest);

		return Response.ok().header("X-BPM-AUTH-MODE", AuthServiceData.INSTANCE.getConfiguredAuthMode()).build();
	}

	public void logoutFromSession(HttpServletRequest httpServletRequest) {
		getLogoutHandler().logout(httpServletRequest);
	}
}
