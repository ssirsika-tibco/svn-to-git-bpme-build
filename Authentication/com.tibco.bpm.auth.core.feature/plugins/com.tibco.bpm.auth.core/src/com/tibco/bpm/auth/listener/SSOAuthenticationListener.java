/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.listener;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.auth.api.BPMAuthenticationListener;
import com.tibco.bpm.auth.api.BPMSecurityService;
import com.tibco.bpm.auth.core.AuthServiceData;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Authentication Listener for SSO services.
 * 
 * @author ssirsika
 * 
 */
public class SSOAuthenticationListener implements BPMAuthenticationListener {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(SSOAuthenticationListener.class,
			AuthLoggingInfo.instance);

	// Service object can be used for post processing after authentication is
	// done.
	private BPMSecurityService bpmSecurityService;

	public SSOAuthenticationListener(BPMSecurityService bpmSecurityService) {
		this.bpmSecurityService = bpmSecurityService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, String userName) {
		CLFMethodContext clf = logCtx.getMethodContext("onAuthenticationSuccess"); //$NON-NLS-1$
		try {
			response.sendRedirect(response.encodeRedirectURL("/apps/login"));
		} catch (IOException e) {
			clf.local.error(e, "Failed to redirect");
		}
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String message) {
		CLFMethodContext clf = logCtx.getMethodContext("onAuthenticationFailure"); //$NON-NLS-1$
		try {
			response.sendRedirect(response.encodeRedirectURL("/apps/login"));
		} catch (IOException e) {
			clf.local.error(e, "Failed to redirect");
		}
	}
	
	@Override
	public void onAdminServiceRegister(AdminConfigurationService adminConfigurationService) {
		AuthServiceData.INSTANCE.setAdminConfigurationService(adminConfigurationService);
	}
}
