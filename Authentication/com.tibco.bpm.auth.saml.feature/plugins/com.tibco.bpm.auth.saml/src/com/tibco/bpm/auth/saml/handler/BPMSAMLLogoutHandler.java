/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.saml.handler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tibco.bpm.auth.api.BPMLogoutHandler;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Logout handler specific to SAML.
 * 
 * @author ssirsika
 *
 */
public class BPMSAMLLogoutHandler implements BPMLogoutHandler {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMSAMLLogoutHandler.class,
			AuthLoggingInfo.instance);

	public void logout(HttpServletRequest httpServletRequest) {
		CLFMethodContext clf = logCtx.getMethodContext("logout");
		clf.local.debug("Perofrming SAML logout");
		SecurityContext context = SecurityContextHolder.getContext();
		if (context != null) {
			context.setAuthentication(null);
			SecurityContextHolder.clearContext();
			clf.local.trace("Cleared security context");
		}

		if (httpServletRequest != null) {
			for (Cookie cookie : httpServletRequest.getCookies()) {
				if (cookie.getName().equals("SAML_TOKEN") && cookie.getValue() != null) {
					cookie.setValue(null);
					cookie.setPath("/");
					cookie.setHttpOnly(true);
					cookie.setMaxAge(0);
					clf.local.trace("Cleared SAML token from cookie");
				}
			}
		}
	}
}
