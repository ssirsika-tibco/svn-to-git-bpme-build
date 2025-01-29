/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.handler;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.tibco.bpm.auth.api.BPMLogoutHandler;

/**
 * Composite handler which will store the logout handler implementing {@link BPMLogoutHandler} and 
 * perform logout through all of them.
 * @author ssirsika
 */
public class BPMCompositeLogoutHandler implements BPMLogoutHandler {

	public BPMCompositeLogoutHandler() {
		// Add logout handler for basic authentication.
		addLogoutHandler(new BasicAuthLogoutHandler());
	}

	private final Set<BPMLogoutHandler> logoutHandlers = new HashSet<BPMLogoutHandler>();

	public void logout(HttpServletRequest httpServletRequest) {
		for (BPMLogoutHandler handler : this.logoutHandlers) {
			handler.logout(httpServletRequest);
		}
	}

	/**
	 * This method is callback method and will be called when service is
	 * injected/changed. This method is configured in the blueprint file.
	 * @param handler
	 */
	public void addLogoutHandler(BPMLogoutHandler handler) {
		logoutHandlers.add(handler);
	}
}
