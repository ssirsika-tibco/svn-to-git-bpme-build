package com.tibco.bpm.auth.ldap;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import com.tibco.bpm.auth.AuthLogger;
import com.tibco.bpm.auth.BPMAbstractLognModule;
import com.tibco.bpm.auth.DEDelegate;
import com.tibco.bpm.auth.UserGuidPrincipal;
import com.tibco.bpm.auth.UserNamePrinciple;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.de.services.InvalidServiceRequestFault;
import com.tibco.n2.de.services.SecurityFault;

/**
 * 
 * @author GVS
 *
 */

public class BPMLDAPLoginModule extends BPMAbstractLognModule {

	private DEDelegate deDelegate;
	
	private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMLDAPLoginModule.class, AuthLoggingInfo.instance);

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
	    CLFMethodContext clf = logCtx.getMethodContext("initialize"); //$NON-NLS-1$
		super.initialize(subject, callbackHandler, options);
		clf.local.trace(" Inside the LDAP logon Module  initialize"); //$NON-NLS-1$
	}

	@Override
	public boolean login() throws LoginException {
		boolean authenticate = false;
		CLFMethodContext clf = logCtx.getMethodContext("login"); //$NON-NLS-1$
		clf.local.trace(" Inside the LDAP logon Module  LOGIN"); //$NON-NLS-1$
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("Username: ");
		callbacks[1] = new PasswordCallback("Password: ", false);

		try {
			callbackHandler.handle(callbacks);
			String ldapAlias = (String) this.options.get(DEDelegate.LDAP_ALIAS);
			String userDn = (String) this.options.get(DEDelegate.LDAP_DN);
			String userName = (String) this.options.get(DEDelegate.USER_NAME);
			String userGuid = (String) this.options.get(DEDelegate.GUID);
			char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
			if ((tmpPassword == null || tmpPassword.length == 0)) {
				throw new LoginException("Empty passwords not allowed");
			}

			String password = new String(tmpPassword);
			//here LDAP bind happens and its tested if this works
			clf.local.debug("calling DE authenticate method");
			authenticate = deDelegate.authenticate(ldapAlias, userDn, password);
			clf.local.debug("returned from DE authenticate method "+authenticate);
			//add the principals to the subject
			principals.add(new UserNamePrinciple(userName));
			principals.add(new UserGuidPrincipal(userGuid));
		} catch (IOException ioException) {
			clf.local.error(" failed with IO exception",ioException);
			throw new LoginException(ioException.getMessage());
		} catch (Throwable unsupportedCallbackException) {
			clf.local.error(" failed with unsupportedCallbackException",unsupportedCallbackException);
			throw new LoginException(
					unsupportedCallbackException.getMessage() + " not available to obtain information from user.");
		}
		authenticated=authenticate;
		return authenticate;
	}

	@Override
	public boolean commit() throws LoginException {
		AuthLogger.debug("commit " + System.identityHashCode(this));
		if(authenticated){
			//add the principals to the subject if the user is authenticated
			subject.getPrincipals().addAll(principals);
		}
		
		return authenticated;
	}

	@Override
	public boolean abort() throws LoginException {
		user = null;
		principals.clear();
		user = null;
		AuthLogger.debug("abort " + System.identityHashCode(this));
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		user = null;
		subject.getPrincipals().removeAll(principals);
		principals.clear();
		AuthLogger.debug("logout " + System.identityHashCode(this));
		return true;
	}

	public boolean login(DEDelegate deDelegate) throws LoginException {
		this.deDelegate = deDelegate;
		return login();
	}

}
