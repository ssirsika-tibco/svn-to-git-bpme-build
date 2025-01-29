package com.tibco.bpm.auth.api;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tibco.bpm.auth.exception.UnauthorizedUserException;

/**
 * Specify the contract for the handler which will handle the different type of
 * authentication.
 * 
 * @author ssirsika
 */
public interface BPMAuthenticationHandler {

	public boolean handleAuthentication(HttpServletRequest req, HttpServletResponse res, String userName) throws UnauthorizedUserException;

	public Subject getSubject();
}
