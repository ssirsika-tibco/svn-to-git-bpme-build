/**
 * 
 */
package com.tibco.bpm.auth.saml.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.tibco.bpm.auth.api.BPMAuthenticationListener;
import com.tibco.bpm.auth.saml.SAMLContextHelper;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Handler triggers in case of failure in SAML authentication process.
 * @author ssirsika
 *
 */
public class BPMSAMLAuthenticationFailureHandler implements AuthenticationFailureHandler {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMSAMLAuthenticationFailureHandler.class,
			AuthLoggingInfo.instance);

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		CLFMethodContext clf = logCtx.getMethodContext("onAuthenticationFailure");
		clf.local.debug(exception, "Authentication failed");
		storeErrorInCookie(response);
		BPMAuthenticationListener listener = SAMLContextHelper.getINSTANCE().getAuthListener();
		if (listener != null) {
			clf.local.trace("Calling authentication failure listener");
			listener.onAuthenticationFailure(request, response, exception.getMessage());
		}
	}

	/**
	 * Store the code in cookie to notify the exception
	 * 
	 * @param response
	 * @param username
	 */
	private void storeErrorInCookie(HttpServletResponse response) {
		CLFMethodContext clf = logCtx.getMethodContext("storeErrorInCookie");
		clf.local.debug("Storing the error in the cookie");

		Cookie samlCookie = new Cookie("SSO_AUTH_ERROR", "ERROR");
		samlCookie.setPath("/");
		samlCookie.setHttpOnly(true);
		response.addCookie(samlCookie);
	}
}
