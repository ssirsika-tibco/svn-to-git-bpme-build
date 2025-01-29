package com.tibco.bpm.auth;

import com.tibco.bpm.auth.handler.SSOAuthenticationHandler;
import com.tibco.bpm.auth.ldap.BasicAuthenticationHandler;

public class AuthenticationHandlerFactory {
	
	private static AuthenticationHandlerFactory instance =new AuthenticationHandlerFactory();
	private BasicAuthenticationHandler basicAuthenticationHandler;

	private SSOAuthenticationHandler sooAuthenticationHandler;
	
	public BasicAuthenticationHandler getBasicAuthenticationHandler(){
		return this.basicAuthenticationHandler;
	}
	
	public  void setAuthenticationHandler(BasicAuthenticationHandler basicAuthenticationHandler){
		instance.basicAuthenticationHandler= basicAuthenticationHandler;
	}
	
	public static AuthenticationHandlerFactory getInstance(){
		return instance;
	}

	public void setSSOAuthenticationHandler(SSOAuthenticationHandler ssoAuthenticationHandler) {
		this.sooAuthenticationHandler = ssoAuthenticationHandler;
	}
	
	public SSOAuthenticationHandler getSOOAuthenticationHandler() {
		return sooAuthenticationHandler;
	}
}
