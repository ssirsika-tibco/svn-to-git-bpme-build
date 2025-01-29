/**
 * 
 */
package com.tibco.bpm.auth.saml.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.tibco.bpm.auth.api.BPMAuthenticationListener;
import com.tibco.bpm.auth.saml.SAMLContextHelper;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Handler triggers in case of successful authentication process.
 * @author ssirsika
 *
 */
public class BPMSamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMSamlAuthenticationSuccessHandler.class,
			AuthLoggingInfo.instance);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CLFMethodContext clf = logCtx.getMethodContext("onAuthenticationSuccess");
		clf.local.debug("Authentication successful");
		storeUserNameInCookie(response, authentication.getName());
		BPMAuthenticationListener listener = SAMLContextHelper.getINSTANCE().getAuthListener();
		if (listener != null) {
			clf.local.trace("Calling authentication success listener");
			listener.onAuthenticationSuccess(request, response, authentication.getName());
		}
	}

	/**
	 * Store the 'username' in the cookie.
	 * @param response
	 * @param username
	 * @throws UnsupportedEncodingException 
	 */
	private void storeUserNameInCookie(HttpServletResponse response, String username) throws UnsupportedEncodingException {
		if (username != null) {
			CLFMethodContext clf = logCtx.getMethodContext("storeUserNameInCookie");
			clf.local.debug("Storing the '%s' in the cookie", username);

			username = URLEncoder.encode(username, "UTF-8");
			Cookie samlCookie = new Cookie("SAML_TOKEN", username);
			samlCookie.setPath("/");
			samlCookie.setHttpOnly(true);
			response.addCookie(samlCookie);
		}
	}
}
