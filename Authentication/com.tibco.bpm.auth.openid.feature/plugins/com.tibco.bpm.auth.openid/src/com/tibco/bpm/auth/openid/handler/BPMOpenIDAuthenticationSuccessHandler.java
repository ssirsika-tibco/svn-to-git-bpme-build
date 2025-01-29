/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.openid.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.tibco.bpm.auth.api.BPMAuthenticationListener;
import com.tibco.bpm.auth.openid.OpenIdContextHelper;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Handler for successful login. 
 * 
 * @author ssirsika
 *
 */
public class BPMOpenIDAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private CLFClassContext logCtx = CloudLoggingFramework.init(BPMOpenIDAuthenticationSuccessHandler.class,
			AuthLoggingInfo.instance);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CLFMethodContext clf = logCtx.getMethodContext("onAuthenticationSuccess");
		clf.local.debug("Authentication successful");

		BPMAuthenticationListener listener = OpenIdContextHelper.getINSTANCE().getAuthListener();
		if (listener != null) {
			clf.local.trace("Calling authentication success listener");
			listener.onAuthenticationSuccess(request, response, authentication.getName());
		}
	}
}
