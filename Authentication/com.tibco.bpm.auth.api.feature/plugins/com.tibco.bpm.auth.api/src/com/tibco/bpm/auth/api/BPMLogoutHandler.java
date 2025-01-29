/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.api;

import javax.servlet.http.HttpServletRequest;

/**
 * Logout handler provide required cleanups functionality which need to be done to perform clear the user informations. 
 * @author ssirsika
 */
public interface BPMLogoutHandler {

	/**
	 * Implementors should clear the user informations as per the specific authentication method.(Basic/SAML/OAuth)
	 * @param httpServletRequest servlet request coming from caller of the handler. 
	 */
	public void logout(HttpServletRequest httpServletRequest); 
}
