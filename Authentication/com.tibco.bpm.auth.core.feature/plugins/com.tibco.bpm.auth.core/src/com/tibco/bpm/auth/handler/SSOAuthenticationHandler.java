package com.tibco.bpm.auth.handler;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tibco.bpm.auth.AuthenticationHandlerFactory;
import com.tibco.bpm.auth.api.AuthLoggerMessageId;
import com.tibco.bpm.auth.api.BPMAuthenticationHandler;
import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.bpm.auth.exception.UnauthorizedUserException;
import com.tibco.bpm.auth.ldap.NameCallbackHandler;
import com.tibco.bpm.auth.ldap.SSOLoginModule;
import com.tibco.bpm.auth.logging.AuthAuditMessages;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class SSOAuthenticationHandler extends AbstractAuthHandler implements BPMAuthenticationHandler {

	static CLFClassContext logCtx = CloudLoggingFramework.init(SSOAuthenticationHandler.class,
			AuthLoggingInfo.instance);

	public SSOAuthenticationHandler() {
		AuthenticationHandlerFactory.getInstance().setSSOAuthenticationHandler(this);
	}

	public synchronized boolean handleAuthentication(HttpServletRequest req, HttpServletResponse res, String userName)
			throws UnauthorizedUserException {
		CLFMethodContext clf = logCtx.getMethodContext("handleAuthentication");

		boolean login = false;
		try {
			Map<String, String> ldapOptions = getDeDelegate().lookupUser(userName);
			if (ldapOptions.size() <= 0) {
				// user is not recognized by system, return false
				clf.local.debug("User " + userName + "  is not recognised, please contact administrator");
				throw new LoginException("Authentication failed , please check credentials");
			}
			subject = new Subject();
			SSOLoginModule loginModule = new SSOLoginModule();
			clf.local.trace("initilaizing the login module");
			loginModule.initialize(subject, new NameCallbackHandler(userName), null, ldapOptions);
			clf.local.trace("login into  the login module");
			login = loginModule.login(getDeDelegate());
			clf.local.trace("commiting the login module");
			loginModule.commit();
			clf.local.debug("Returning the LDAP check result '%b' for SSO auth", login);
			return login;

		} catch (Throwable exception) {
			clf.local.debug(exception, "Exception in user lookup");
			clf.local.messageId(AuthLoggerMessageId.AUTH_CHECK_FAIL_00001).error("Authentication failed.");
			throw new UnauthorizedUserException(ErrorCode.AUTH_INVALID_AUTHENTICATION,null,null);
		}
	}
}
