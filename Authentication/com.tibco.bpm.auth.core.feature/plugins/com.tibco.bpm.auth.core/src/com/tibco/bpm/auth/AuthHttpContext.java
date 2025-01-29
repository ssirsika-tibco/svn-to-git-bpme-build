package com.tibco.bpm.auth;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

public class AuthHttpContext implements HttpContext {

	public boolean handleSecurity(HttpServletRequest req,
								  HttpServletResponse res) throws IOException {

		if (req.getHeader("Authorization") == null) {
			res.addHeader("WWW-Authenticate", "Basic realm=\"Test Realm\"");
			res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		if (authenticated(req)) {
			return true;
		} else {
			res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

	}

	protected boolean authenticated(HttpServletRequest request) {
		request.setAttribute(AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);

		String authzHeader = request.getHeader("Authorization");
		String usernameAndPassword = new String(Base64.getDecoder().decode(authzHeader.substring(6).getBytes()));

		int userNameIndex = usernameAndPassword.indexOf(":");
		String username = usernameAndPassword.substring(0, userNameIndex);
		String password = usernameAndPassword.substring(userNameIndex + 1);

		// Here I will do lame hard coded credential check. HIGHLY NOT RECOMMENDED!
		boolean success = ((username.equals("admin") && password
				.equals("admin")));
		if (success) {
			request.setAttribute(REMOTE_USER, "admin");
		}
		return success;
	}

	public URL getResource(String s) {
		return null; // To change body of implemented methods use File |
		// Settings | File Templates.
	}

	public String getMimeType(String s) {
		return null; // To change body of implemented methods use File |
		// Settings | File Templates.
	}
}
