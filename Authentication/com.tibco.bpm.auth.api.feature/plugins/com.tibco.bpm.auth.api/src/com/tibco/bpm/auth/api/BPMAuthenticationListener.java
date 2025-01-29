/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;

/**
 * Interface defines the contract for callback listener which will be notified
 * for authentication success and failure.
 * 
 * @author ssirsika
 * 
 */
public interface BPMAuthenticationListener {

	/**
	 * Called when a user has been successfully authenticated.
	 *
	 * @param request  the request which caused the successful authentication
	 * @param response the response
	 * @param userName user name / Principal name
	 */
	void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, String userName);
	
	/**
	 * Called when a user has been failed to authenticate.
	 * @param request the request which caused the successful authentication
	 * @param response the response
	 * @param message authentication exception message
	 */
	void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String message);

	/**
	 * Notify when {@link AdminConfigurationService} is injected.
	 * 
	 * @param adminConfigurationService injected {@link AdminConfigurationService}
	 */
	void onAdminServiceRegister(AdminConfigurationService adminConfigurationService);

}
