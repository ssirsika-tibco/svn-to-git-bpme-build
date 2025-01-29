package com.tibco.bpm.auth.saml;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.Filter;

import org.springframework.context.ApplicationContext;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;


/**
 * 
 * @author GVS
 * Remove if not required.
 */

@Deprecated
public class BPMSAMLLoginModule implements LoginModule {

	private Filter springSecurityFilterChain;
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean login() throws LoginException {
	
		ApplicationContext context = SAMLSecurityBundleActivator.getApplicationContext();
		SavedRequestAwareAuthenticationSuccessHandler handler = (SavedRequestAwareAuthenticationSuccessHandler) context.getBean("successRedirectHandler");
		handler.setDefaultTargetUrl("http://localhost/apps/work-manager/index.html-111");
		return false;
	}

	@Override
	public boolean commit() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abort() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean logout() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	public Filter getSpringSecurityFilterChain() {
		return springSecurityFilterChain;
	}

	public void setSpringSecurityFilterChain(Filter springSecurityFilterChain) {
		this.springSecurityFilterChain = springSecurityFilterChain;
	}

}
