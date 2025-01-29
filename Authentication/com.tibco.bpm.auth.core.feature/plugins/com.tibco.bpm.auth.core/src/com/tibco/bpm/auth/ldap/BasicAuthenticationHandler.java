package com.tibco.bpm.auth.ldap;

import java.util.Base64;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tibco.bpm.auth.AuthLogger;
import com.tibco.bpm.auth.AuthenticationHandlerFactory;
import com.tibco.bpm.auth.api.BPMAuthenticationHandler;
import com.tibco.bpm.auth.handler.AbstractAuthHandler;
import com.tibco.bpm.auth.logging.AuthAuditMessages;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class BasicAuthenticationHandler extends AbstractAuthHandler implements BPMAuthenticationHandler{
	
	
	private static CLFClassContext logCtx = CloudLoggingFramework.init(BasicAuthenticationHandler.class, AuthLoggingInfo.instance);
	
	public BasicAuthenticationHandler() {
		AuthenticationHandlerFactory.getInstance().setAuthenticationHandler(this);
	}

	public synchronized boolean handleAuthentication(HttpServletRequest req, HttpServletResponse res,String authHeader) {
		boolean isAuthenticated = false;
		CLFMethodContext clf = logCtx.getMethodContext("handleAuthentication"); //$NON-NLS-1$
		if (authHeader != null) {

			String[] authHeaderSplit = authHeader.split("\\s");

			for (int i = 0; i < authHeaderSplit.length; i++) {
				String token = authHeaderSplit[i];
				if (token.equalsIgnoreCase("Basic")) {

					String credentials = new String(Base64.getDecoder().decode(authHeaderSplit[i + 1]));
					int index = credentials.indexOf(":");
					if (index != -1) {

						try {
							String username = credentials.substring(0, index).trim();
							String password = credentials.substring(index + 1).trim();
							clf.local.debug("calling handle ldap login");
							isAuthenticated = handleLDAPLogin(username, password);
							clf.local.debug("ldap login returned "+isAuthenticated);
							break;
						} catch (LoginException e) {
							// we need to consume this error because we need to
							// just send false
							// for now i am printing the stack trace but will
							// remove this and hook into the logs
							clf.local.debug("Failed with login exception in handleAuthentication");
							e.printStackTrace();
							AuthLogger.debug(e.getMessage());
							isAuthenticated = false;
						}
					}
				}
			}
		}
		clf.local.debug("handleAuthentication returning "+isAuthenticated);
		return isAuthenticated;

	}

	protected boolean handleLDAPLogin(String userName, String password) throws LoginException {
		boolean login = false;
		CLFMethodContext clf = logCtx.getMethodContext("handleLDAPLogin"); //$NON-NLS-1$
		try {
			clf.local.debug("calling securityservice lookup user for user: [%s]",userName);
			Map<String, String> ldapOptions =getDeDelegate().lookupUser(userName);
			clf.local.debug("Fetched ldap options  from security service "+ldapOptions.size());
			if(ldapOptions.size()<=0){
				//user is not recognised by system, return false
				AuthLogger.debug("User "+ userName+"  is not recognised, please contact administrator" );
				throw new LoginException("Authentication failed , please check credentials");
			} 
			subject = new Subject();
			clf.local.debug("created the subject "+subject);
			BPMLDAPLoginModule loginModule = new BPMLDAPLoginModule();
			AuthLogger.debug("initilaizing the login module");
			loginModule.initialize(subject, new NamePasswordCallbackHandler(userName, password), null, ldapOptions);
			AuthLogger.debug("login into  the login module");
			login = loginModule.login(getDeDelegate());
			AuthLogger.debug("commiting the login module" +login);
			loginModule.commit();
			clf.local.debug("Subject after commiting the login module"+subject);
			//clf.audit.audit(AuthAuditMessages.USER_LOGGED_IN, clf.param(CloudMetaData.USERNAME, userName));
		} catch (Throwable exception) {
			clf.local.error("Exception occurred in handleLDAPLogin",exception);
			throw new LoginException(exception.getMessage());
		}
		return login;

	}



}
