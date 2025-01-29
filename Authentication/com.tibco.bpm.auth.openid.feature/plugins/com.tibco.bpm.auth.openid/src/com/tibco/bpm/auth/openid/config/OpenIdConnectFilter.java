/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.openid.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.tibco.bpm.auth.openid.OpenIdSecurityBundleActivator;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * <p>
 * Extends the {@link AbstractAuthenticationProcessingFilter} which is used for
 * the processing HTTP based authentication request. This filter has
 * {@link #attemptAuthentication(HttpServletRequest, HttpServletResponse)
 * attemptAuthentication} method which is used for getting the accessToken from
 * external IDP. This filter also update 'http session' after successful
 * authentication.
 * </p>
 * 
 * @author ssirsika
 *
 */
public class OpenIdConnectFilter extends AbstractAuthenticationProcessingFilter {

	private static final String AUTH_TOKEN_EXPIRY_ATTR_KEY = "auth_token_expiry";

	private static final String AUTH_TOKEN_ATTR_KEY = "auth_token";

	// Used for storing the refresh token which can be used to retrieve the access
	// token which are expired or due to expired.
	private static final String AUTH_REFRESH_TOKEN_ATTR_KEY = "auth_refresh_token";

	public OAuth2RestTemplate restTemplate;

	private CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdConnectFilter.class, AuthLoggingInfo.instance);

	/**
	 * Override the default constructor to provide
	 * <tt>defaultFilterProcessesUrl</tt> and provide 'No Operation' authentication
	 * manager.
	 * 
	 * @param defaultFilterProcessesUrl URL that determines if authentication is
	 *                                  required
	 * @param successHandler            Authentication success handler
	 */
	public OpenIdConnectFilter(String defaultFilterProcessesUrl, AuthenticationSuccessHandler successHandler) {
		super(defaultFilterProcessesUrl);
		setAuthenticationManager(new NoopAuthenticationManager());
		setAuthenticationSuccessHandler(successHandler);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		OAuth2AccessToken accessToken;
		// create a new session if it's already not present
		HttpSession session = request.getSession();
		CLFMethodContext clf = logCtx.getMethodContext("attemptAuthentication");
		clf.local.debug("Attempting authentication");
		try {
			/*
			 * Create a new AccessTokenRequest object to copy the request parameters. This
			 * was done to copy the 'authorization Code' received after response received
			 * from IDP in the initial call. Create a a new OAuth2ClientContext and set the
			 * access token from 'session' object. This will make sure that, if accessToken
			 * is already present in the 'session' then request for new access token is not
			 * made. New template object is generated from old resource information and
			 * newly generated client context.
			 */
			AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest(request.getParameterMap());
			OAuth2ClientContext auth2ClientContext = new DefaultOAuth2ClientContext(accessTokenRequest);
			auth2ClientContext.setAccessToken((OAuth2AccessToken) session.getAttribute(AUTH_TOKEN_ATTR_KEY));
			auth2ClientContext.setPreservedState(accessTokenRequest.getStateKey(),
					OpenIdResourceProvider.INSTANCE.getRedirectUri());
			OAuth2ProtectedResourceDetails resourceDetails = (OAuth2ProtectedResourceDetails) OpenIdSecurityBundleActivator
					.getBean("resource");
			restTemplate = new OAuth2RestTemplate(resourceDetails, auth2ClientContext);
			clf.local.trace("Acquiring access token");
			accessToken = restTemplate.getAccessToken();
		} catch (final OAuth2Exception e) {
			clf.local.error(e, "Could not obtain access token");
			throw new BadCredentialsException("Could not obtain access token", e);
		}
		try {
			// Verify if received access token is correct and valid.
			final String idToken = accessToken.getAdditionalInformation().get("id_token").toString();
			clf.local.trace("Acquired access token having id_token : %s", idToken);
			Map<String, Object> authInfo = OpenIdAuthVerifier.INSTANCE.getVerifiedClaims(idToken, false);
			if (authInfo == null) {
				clf.local.debug("Could not verify id_token '%s' with IDP", idToken);
				throw new BadCredentialsException("Could not obtain user details from token");
			}

			final OpenIdConnectUserDetails user = new OpenIdConnectUserDetails(authInfo, accessToken,
					OpenIdResourceProvider.INSTANCE.getUserKey());
			storeAccessTokenInSession(accessToken, session);
			storeAccessTokenInCookie(response, idToken, user.getUsername());
			return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		} catch (final Exception e) {
			clf.local.error(e, "Could not obtain user details from token");
			throw new BadCredentialsException("Could not obtain user details from token", e);
		}

	}

	/**
	 * Store the access token information in the session.
	 * 
	 * @param accessToken access token
	 * @param session     session in which attributes need to be stored.
	 */
	private void storeAccessTokenInSession(OAuth2AccessToken accessToken, HttpSession session) {
		CLFMethodContext clf = logCtx.getMethodContext("storeAccessTokenInSession");
		clf.local.debug("Storing  access token in session");

		session.setAttribute(AUTH_TOKEN_ATTR_KEY, accessToken);
		OAuth2RefreshToken refreshToken = accessToken.getRefreshToken();
		if (refreshToken != null) {
			session.setAttribute(AUTH_REFRESH_TOKEN_ATTR_KEY, refreshToken);
			clf.local.trace("Storing refresh token : %s", refreshToken.getValue());
		}
		int expiryTime = (int) accessToken.getExpiration().getTime();
		session.setAttribute(AUTH_TOKEN_EXPIRY_ATTR_KEY, expiryTime);
		clf.local.trace("Setting session's  MaxInactiveInterval : %d", expiryTime);
		session.setMaxInactiveInterval(expiryTime);
	}

	/**
	 * Store the 'username' and JWT token in the cookie.
	 * @param response
	 * @param idToken
	 * @param username
	 * @throws UnsupportedEncodingException 
	 */
	private void storeAccessTokenInCookie(HttpServletResponse response, String idToken, String username) throws UnsupportedEncodingException {
		CLFMethodContext clf = logCtx.getMethodContext("storeAccessTokenInCookie");

		if (idToken != null && username != null) {
			clf.local.debug("Storing  userName :'%s' and idToken : '%s' in cookie", username, idToken);
			String result = String.join("$BPM_CLAIM$", username, idToken);
			result = URLEncoder.encode(result, "UTF-8");
			Cookie openIDCookie = new Cookie("OPENID_TOKEN", result);
			openIDCookie.setPath("/");
			openIDCookie.setHttpOnly(true);
			response.addCookie(openIDCookie);
			clf.local.trace("Added '%s' to cookie", result);
		}
	}

	/**
	 * Set the initial rest template to make REST calls to obtaining OpenID connect
	 * related informations from IDP.
	 * 
	 * @param aRestTemplate {@link OAuth2RestTemplate}
	 */
	public void setRestTemplate(OAuth2RestTemplate aRestTemplate) {
		restTemplate = aRestTemplate;
	}

	/**
	 * No operation authentication manager which really does not do anything.
	 * @author ssirsika
	 *
	 */
	private static class NoopAuthenticationManager implements AuthenticationManager {

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			throw new UnsupportedOperationException("No authentication should be done with this AuthenticationManager");
		}

	}
}
