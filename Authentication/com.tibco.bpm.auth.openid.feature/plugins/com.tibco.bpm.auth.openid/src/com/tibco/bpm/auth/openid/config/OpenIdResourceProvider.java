/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.openid.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.OpenIdAuthentication;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * @author ssirsika
 *
 */
public class OpenIdResourceProvider {

	private CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdResourceProvider.class, AuthLoggingInfo.instance);
	
	public final static OpenIdResourceProvider INSTANCE = new OpenIdResourceProvider();

	// following two are for the localhost specific application
	/*
	 * private String clientId =
	 * "361313120177-0je5uvjektplq2o5psc2ql3nmr5sggcc.apps.googleusercontent.com";
	 * private String clientSecret = "t6zIZTR7CCc1RzyJomk59AmP";
	 */
	// following two are for the http://ace-nightly-test.emea.tibco.com/ specific
	// config

	private String clientId = "361313120177-anq3fuhlfc4joscm1kage1dboap06ihl.apps.googleusercontent.com";
	private String clientSecret = "Eni0qigROcIC8g0DdV-rsluO";

	private String accessTokenUri = "https://www.googleapis.com/oauth2/v3/token";
	private String userAuthorizationUri = "https://accounts.google.com/o/oauth2/auth";
//	private String redirectUri = "http://ace-nightly-test.emea.tibco.com/google-login";
	private String redirectUri = "http://localhost/google-login";
	private String relativeRedirectUri = "/google-login";
	// TODO : How to get the issuer from RI or do we want it???
	private String issuer = "accounts.google.com";
	private String jwkUrl = "https://www.googleapis.com/oauth2/v2/certs";

	private String userKey = "email";

	OpenIdResourceProvider() {
	}

	/**
	 * Initialize the configuration from resource template and return the
	 * <code>true</code> if initialization is done otherwise return false.
	 * 
	 * @return
	 */
	public boolean init(AdminConfigurationService adminConfigService) {
		CLFMethodContext clf = logCtx.getMethodContext("init");
		clf.local.debug("Initializing from Resource Instance");
		try {
			List<String> openIdAuthRINames = adminConfigService.getOpenIdAuthenticationNames();
			String newRiName = openIdAuthRINames.isEmpty() ? null : openIdAuthRINames.get(0);
			// if new RI is available then only set the configuration.
			if (newRiName != null) {
				OpenIdAuthentication openIdAuthRI = adminConfigService.getOpenIdAuthenticationByName(newRiName);
				setClientId(openIdAuthRI.getClientId());
				setClientSecret(openIdAuthRI.getClientSecret());
				setAccessTokenUri(openIdAuthRI.getAccessTokenUri());
				setUserAuthorizationUri(openIdAuthRI.getAuthorizationUri());
				setRedirectUri(openIdAuthRI.getRedirectUri());
				setRelativeRedirectUri(calculateRelativeUri(this.redirectUri));
				setJwkUrl(openIdAuthRI.getJsonWebKeySetUri());
				setUserKey(openIdAuthRI.getUserKey());
				clf.local.trace("Configuration details : '%s'", toString());
				return true;
			}
		} catch (Exception e) {
			clf.local.error(e, "Error while reading the Resource Instance details from admin");
			return false;
		}
		clf.local.debug("Could not found valid resource instance for SAML");
		return false;
	}

	/**
	 * Return the last part of passed redirect url. Redirect url entered in the IDP
	 * should be of the format 'http://host:port/appPath'. This method will return
	 * the last '/appPath'. If the redirect url is not in the supported format then
	 * {@link RuntimeException} is thrown.
	 * 
	 * @param aRedirectUri
	 * @return
	 * @throws RuntimeException
	 */
	private String calculateRelativeUri(String aRedirectUri) {
		CLFMethodContext clf = logCtx.getMethodContext("calculateRelativeUri");
		clf.local.debug("Calculation relative URL for '%s'", aRedirectUri);

		if(aRedirectUri == null || aRedirectUri.isEmpty()) {
			return "/";
		}
		URI uri = null;
		try {
			uri = new URI(aRedirectUri);
		} catch (URISyntaxException e) {
			clf.local.error(e, "Format of redirected uri must be 'http://host:port/appPath'");
			throw new RuntimeException("Format of redirected uri must be 'http://host:port/appPath'", e);
		}
		String[] segments = uri.getPath().split("/");
		if (segments.length != 2) {
			clf.local.error("Format of redirected uri must be 'http://host:port/appPath'");
			throw new RuntimeException("Format of redirected uri must be 'http://host:port/appPath'");
		}
		return "/" + segments[segments.length - 1];
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the clientSecret
	 */
	public String getClientSecret() {
		return clientSecret;
	}

	/**
	 * @param clientSecret the clientSecret to set
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * @return the accessTokenUri
	 */
	public String getAccessTokenUri() {
		return accessTokenUri;
	}

	/**
	 * @param accessTokenUri the accessTokenUri to set
	 */
	public void setAccessTokenUri(String accessTokenUri) {
		this.accessTokenUri = accessTokenUri;
	}

	/**
	 * @return the userAuthorizationUri
	 */
	public String getUserAuthorizationUri() {
		return userAuthorizationUri;
	}

	/**
	 * @param userAuthorizationUri the userAuthorizationUri to set
	 */
	public void setUserAuthorizationUri(String userAuthorizationUri) {
		this.userAuthorizationUri = userAuthorizationUri;
	}

	/**
	 * @return the redirectUri
	 */
	public String getRedirectUri() {
		return redirectUri;
	}

	/**
	 * @param redirectUri the redirectUri to set
	 */
	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	/**
	 * 
	 * @return relative redirect uri
	 */
	public String getRelativeRedirectUri() {
		return relativeRedirectUri;
	}

	/**
	 * 
	 * @param relativeRedirectUri the relative redirect uri
	 */
	public void setRelativeRedirectUri(String relativeRedirectUri) {
		this.relativeRedirectUri = relativeRedirectUri;
	}

	/**
	 * @return the issuer
	 */
	public String getIssuer() {
		return issuer;
	}

	/**
	 * @param issuer the issuer to set
	 */
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	/**
	 * @return the jwkUrl
	 */
	public String getJwkUrl() {
		return jwkUrl;
	}

	/**
	 * @param jwkUrl the jwkUrl to set
	 */
	public void setJwkUrl(String jwkUrl) {
		this.jwkUrl = jwkUrl;
	}

	/**
	 * @return the userKey
	 */
	public String getUserKey() {
		return userKey;
	}

	/**
	 * @param userKey the userKey to set
	 */
	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	@Override
	public String toString() {
		return "OpenIdResourceProvider [clientId=" + clientId + ", clientSecret=" + clientSecret + ", accessTokenUri="
				+ accessTokenUri + ", userAuthorizationUri=" + userAuthorizationUri + ", redirectUri=" + redirectUri
				+ ", relativeRedirectUri=" + relativeRedirectUri + ", issuer=" + issuer + ", jwkUrl=" + jwkUrl
				+ ", userKey=" + userKey + "]";
	}

}
