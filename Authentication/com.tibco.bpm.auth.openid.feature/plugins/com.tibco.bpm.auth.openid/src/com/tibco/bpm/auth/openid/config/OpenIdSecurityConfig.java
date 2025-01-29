/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.openid.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.tibco.bpm.auth.openid.OpenIdSecurityBundleActivator;
import com.tibco.bpm.auth.openid.handler.BPMOpenIDAuthenticationFailureHandler;
import com.tibco.bpm.auth.openid.handler.BPMOpenIDAuthenticationSuccessHandler;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * <p>
 * Allows customization to the {@link WebSecurity}. By extending
 * {@link WebSecurityConfigurerAdapter}, Configuration provided in this class is
 * used by {@link WebSecurity} to build the 'springSecurityFilterChain'.
 * 'springSecurityFilterChain' bean is created via
 * {@link WebSecurityConfiguration.springSecurityFilterChain()} method.
 * {@link WebSecurity}.performBuild() method creates a {@link FilterChainProxy}
 * with passed 'springSecurityFilterChain'.
 * </p>
 * <p>
 * This {@link FilterChainProxy} proxy intercepts all the request and passed
 * them to 'springSecurityFilterChain' to authenticate the request.
 * </p>
 * 
 * @author ssirsika
 *
 */
@Configuration
@EnableWebSecurity(debug = false)
public class OpenIdSecurityConfig extends WebSecurityConfigurerAdapter {

	private CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdSecurityConfig.class, AuthLoggingInfo.instance);

	/**
	 * Create filter which process the incoming HTTP request and retrieve the
	 * 'access token' from IDP if not already authenticated.
	 * 
	 * @return {@link OpenIdConnectFilter}
	 */
	@Bean
	public OpenIdConnectFilter openIdConnectFilter() {
		CLFMethodContext clf = logCtx.getMethodContext("openIdConnectFilter");
		clf.local.debug("OpenID Bootstrap: Creating openIdConnectFilter bean");
		final OpenIdConnectFilter filter = new OpenIdConnectFilter(
				OpenIdResourceProvider.INSTANCE.getRelativeRedirectUri(), successHandler());
		filter.setRestTemplate(restTemplate());
		return filter;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("configure");
		clf.local.debug("OpenID Bootstrap: Configuring security filters");
		OpenIdSecurityBundleActivator.setApplicationContext(getApplicationContext());
		http.addFilterAfter(new OAuth2ClientContextFilter(), AbstractPreAuthenticatedProcessingFilter.class)
				.addFilterAfter(openIdConnectFilter(), OAuth2ClientContextFilter.class).httpBasic()
				.authenticationEntryPoint(
						new LoginUrlAuthenticationEntryPoint(OpenIdResourceProvider.INSTANCE.getRelativeRedirectUri()))
				.and().authorizeRequests().antMatchers("/bpm/auth/v1/sso").permitAll().antMatchers("/apps/**")
				.authenticated();
	}

	@Bean
	public BPMOpenIDAuthenticationSuccessHandler successHandler() {
		CLFMethodContext clf = logCtx.getMethodContext("successHandler");
		clf.local.debug("OpenID Bootstrap: Creating successHandler bean");
		return new BPMOpenIDAuthenticationSuccessHandler();
	}

	@Bean
	public AuthenticationFailureHandler failureHandler() {
		CLFMethodContext clf = logCtx.getMethodContext("failureHandler");
		clf.local.debug("OpenID Bootstrap: Creating failureHandler bean");
		return new BPMOpenIDAuthenticationFailureHandler();
	}

	/**
	 * Create a resource specific for the 'Authorization Code' Flow. In this flow,
	 * the client (usually a web server) only gets an authorization code after the
	 * Resource Owner (the end-user) grants access. With that authorization code,
	 * the client then makes another call to the API, passing client_id and
	 * client_secret , together with the authorization code, to obtain the ID Token.
	 * 
	 * @return
	 */
	@Bean
	protected OAuth2ProtectedResourceDetails resource() {
		CLFMethodContext clf = logCtx.getMethodContext("resource");
		clf.local.debug("OpenID Bootstrap: Creating resource bean");
		AuthorizationCodeResourceDetails resource = new AuthorizationCodeResourceDetails();
		resource.setAccessTokenUri(OpenIdResourceProvider.INSTANCE.getAccessTokenUri());
		clf.local.trace("OpenID Bootstrap: Access token URI : '%s'",
				OpenIdResourceProvider.INSTANCE.getAccessTokenUri());
		resource.setClientId(OpenIdResourceProvider.INSTANCE.getClientId());
		clf.local.trace("OpenID Bootstrap: Client Id : '%s'", OpenIdResourceProvider.INSTANCE.getClientId());
		resource.setClientSecret(OpenIdResourceProvider.INSTANCE.getClientSecret());
		clf.local.trace("OpenID Bootstrap: Client Secret : '%s'", OpenIdResourceProvider.INSTANCE.getClientSecret());
		resource.setPreEstablishedRedirectUri(OpenIdResourceProvider.INSTANCE.getRedirectUri());
		clf.local.trace("OpenID Bootstrap: PreEstablishedRedirectUri : '%s'",
				OpenIdResourceProvider.INSTANCE.getRedirectUri());
		// 'access_type' is added to get the refresh token. Refresh token can be used to
		// retrieve the access token once it is due to expire or expired.
		resource.setUserAuthorizationUri(
				OpenIdResourceProvider.INSTANCE.getUserAuthorizationUri() + "?access_type=offline");
		clf.local.trace("OpenID Bootstrap: UserAuthorizationUri : '%s'",
				OpenIdResourceProvider.INSTANCE.getUserAuthorizationUri() + "?access_type=offline");
		resource.setUseCurrentUri(false);
		/*
		 * For Google and ADFS , both the following scopes are mandatory. When 'openid'
		 * is included in the scope request parameter, an ID token is issued from the
		 * token endpoint in addition to an access token.
		 */
		resource.setScope(Arrays.asList("openid", "email"));
		return resource;
	}

	/**
	 * Create a template which will be used to communicate with the IDP over REST
	 * protocol.
	 * 
	 * @return {@link OAuth2RestTemplate}
	 */
	@Bean
	public OAuth2RestTemplate restTemplate() {
		CLFMethodContext clf = logCtx.getMethodContext("restTemplate");
		clf.local.debug("OpenID Bootstrap: Creating restTemplate bean");
		AccessTokenRequest atr = new DefaultAccessTokenRequest();
		return new OAuth2RestTemplate(resource(), new DefaultOAuth2ClientContext(atr));
	}
}
