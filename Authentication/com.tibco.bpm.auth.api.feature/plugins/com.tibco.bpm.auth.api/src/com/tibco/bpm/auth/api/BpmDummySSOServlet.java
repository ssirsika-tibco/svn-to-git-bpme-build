/*
* Copyright (c) 2004-2024. Cloud Software Group, Inc. All Rights Reserved.
*/

package com.tibco.bpm.auth.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ACE-8799: 
 * Dummy servlet registered with Pax-Web to handle SSO-related URLs such as the 
 * SAML Entry Point URL, Assertion Consumer URL, and OpenID Redirect URL.
 * 
 * <p>This servlet was introduced as part of the upgrade from Pax Web 7.x to 8.x. 
 * In Pax Web 8.x, servlets and filters must be explicitly registered, and 
 * filters are only invoked if a servlet is mapped to the target URL. By 
 * registering instances of this empty servlet with the relevant URLs, we 
 * ensure that Pax Web initializes the filter chain correctly for SSO requests.</p>
 * 
 * <p><b>Note:</b> This servlet does not process any logic itself; it serves 
 * only as a placeholder to trigger filter processing in Pax Web.</p>
 * 
 * @author ssirsika
 * @since 15 Nov 2024
 */
public class BpmDummySSOServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		 // No implementation required.
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		 // No implementation required.
	}
}