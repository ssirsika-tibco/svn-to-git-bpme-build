/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.tibco.bpm.auth.api.BPMLogoutHandler;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class BasicAuthLogoutHandler implements BPMLogoutHandler {

	static CLFClassContext logCtx = CloudLoggingFramework.init(BasicAuthLogoutHandler.class, AuthLoggingInfo.instance);

	public void logout(HttpServletRequest httpServletRequest) {
		CLFMethodContext clf = logCtx.getMethodContext("logout");

		if (null != httpServletRequest) {
			HttpSession session = httpServletRequest.getSession();
			if (session != null) {
				session.invalidate();
			}
		}
	}
}
