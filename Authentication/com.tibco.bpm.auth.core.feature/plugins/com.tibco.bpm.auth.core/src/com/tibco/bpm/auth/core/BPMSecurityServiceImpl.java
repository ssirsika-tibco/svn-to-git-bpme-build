/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.core;

import java.net.URLDecoder;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.ws.rs.core.Context;

import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.session.Session;

import com.tibco.bpm.ace.admin.model.HttpClient;
import com.tibco.bpm.ace.admin.model.OpenIdAuthentication;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.auth.AuthLogger;
import com.tibco.bpm.auth.AuthenticationHandlerFactory;
import com.tibco.bpm.auth.DEDelegate;
import com.tibco.bpm.auth.UserGuidPrincipal;
import com.tibco.bpm.auth.UserNamePrinciple;
import com.tibco.bpm.auth.admin.AuthPropertyConfig;
import com.tibco.bpm.auth.api.AuthLoggerMessageId;
import com.tibco.bpm.auth.api.BPMAuthVerifier;
import com.tibco.bpm.auth.api.BPMAuthenticationHandler;
import com.tibco.bpm.auth.api.BPMSecurityService;
import com.tibco.bpm.auth.api.HttpClientSSOBinding;
import com.tibco.bpm.auth.api.PathExclusions;
import com.tibco.bpm.auth.api.SSOSecuritySupport;
import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.bpm.auth.exception.UnauthorizedUserException;
import com.tibco.bpm.auth.handler.SSOAuthenticationHandler;
import com.tibco.bpm.auth.ldap.BasicAuthenticationHandler;
import com.tibco.bpm.auth.logging.AuthAuditMessages;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.security.CurrentUser;
import com.tibco.n2.common.security.RequestContext;
import com.tibco.n2.de.api.services.SecurityService;
import com.tibco.n2.logging.metadata.common.CommonMetaData;
import org.apache.http.impl.client.HttpClientBuilder;
import com.tibco.bpm.ace.admin.model.HttpClient;

public class BPMSecurityServiceImpl implements BPMSecurityService, SSOSecuritySupport {

	private static final String X_BPM_AUTH_MODE = "X-BPM-AUTH-MODE";
	
	private static final String CURRENT_USER_KEY = "current_user_key";
	
	private static final String AUTHORIZATION = "Authorization";

	private static final String AUTH_TOKEN_EXPIRY_ATTR_KEY = "auth_token_expiry";

	private static final String AUTH_TOKEN_ATTR_KEY = "auth_token";
	
	private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMSecurityServiceImpl.class, AuthLoggingInfo.instance);

	private SecurityService securityService;
	
	private HttpClientSSOBinding httpClientSSOBinding;

	private DEDelegate deDelegate;

	private HttpSessionListener bpmSessionListener;

	@Context
	private HttpServletRequest orginialRequest;
	
	private BPMAuthVerifier openIdTokenVerifier;

	private AtomicBoolean listenerAdded = new AtomicBoolean(false);

	public BPMSecurityServiceImpl() {
		AuthLogger.debug("BPMSecurityServiceImpl created");
	}

	@Override
	public  boolean authenticate(HttpServletRequest req, HttpServletResponse res, PathExclusions exclusions)
			throws Exception {
		//printRequest(req);
			
		String authHeader = req.getHeader(AUTHORIZATION);
		CLFMethodContext clf = logCtx.getMethodContext("authenticate"); //$NON-NLS-1$
        
		clf.local.trace(" Entered filter " + req.getRequestURL());
		/** if the header is not present check if the session has the security
		subject, if yes, we can go ahead and follow the chain. Also if the request contains
	    /apps/login/ we can ignore intercepting */
		boolean excludePath = exclusions.isExcludePath(req);
		if(excludePath) {
			return true;
		}
		
		if (isAlreadySessionEstablished(req)) {
			clf.local.debug("There is already existing session");
			res.setHeader( "X-XSS-Protection", "1; mode=block");
			populateRequestContext(req, null, false);
			return true;
		}
		
		clf.local.debug(X_BPM_AUTH_MODE+" is: [%d]", AuthServiceData.INSTANCE.getConfiguredAuthMode());
		req.setAttribute(X_BPM_AUTH_MODE, AuthServiceData.INSTANCE.getConfiguredAuthMode());
		
		checkForSSOLoginErrors(req, res);
		/**
		 * Check for OpenId cookie to test already authenticated user
		 */
		Cookie openIdCookie = getOpenIdCookie(req);
		if (openIdCookie != null) {
			clf.local.debug("Open id cookie is present");
			String value = URLDecoder.decode(openIdCookie.getValue(), "UTF-8");
			String[] split = value.split("\\$BPM_CLAIM\\$");
			clearCookie(res, openIdCookie);
			if (split.length == 2) {
				String userName = split[0];
				String id_token = split[1];

				boolean tokenVerified = openIdTokenVerifier.verify(id_token, false);
				if (tokenVerified) {
					return handleSSOAuthRequest(req, res, userName);
				}
			}
			return false;
		}
		/**
		 * Check for SAML cookie to test already authenticated user
		 */
		Cookie samlCookie = getSAMLCookie(req);
		if (samlCookie != null) {
			clf.local.debug("SAML  cookie is present");
			String userName = URLDecoder.decode(samlCookie.getValue(), "UTF-8");
			clearCookie(res, samlCookie);
			if (userName != null) {
				return handleSSOAuthRequest(req, res, userName);
			}
			return false;
		}
		
		/*
		 * ACE-5239: Check for the authentication header prefix for the OpenID 'ID
		 * TOKEN' to assert authentication of the request.
		 */
		if (authHeader != null) {
			String headerPrefixProperty = AuthPropertyConfig.INSTANCE.getOpenIdAuthHeaderPrefixProverty();
			if (authHeader.startsWith(headerPrefixProperty)) {
				clf.local.debug("Autherization header prefix '%s' is present for OpenId", headerPrefixProperty);
				String[] split = authHeader.split("\\s");
				if (split.length == 2) {
					String userName = openIdTokenVerifier.getVerifiedUserKeyValue(split[1], false);
					if (userName != null) {
						return handleSSOAuthRequest(req, res, userName);
					}
				}
			}
		}
		
		/**
		 * Check for Basic authentication
		 */
		if (AuthPropertyConfig.INSTANCE.isBasicAuthEnabled()) {
			clf.local.debug("Basic auth enabled so try authenticating using basic");
			BasicAuthenticationHandler authenticationHandler=new BasicAuthenticationHandler();
			authenticationHandler.setDeDelegate(deDelegate);
			clf.local.debug("Calling basic authenticate handler authenticate");
			boolean isAuthenticated = authenticationHandler.handleAuthentication(req, res, authHeader);
			clf.local.debug("Is basic auth successful ? ["+isAuthenticated+"]");
			if (isAuthenticated) {
				populateRequestContext(req, authenticationHandler, authHeader!=null);
				clf.local.debug("Logged in using basic authentication.");
				return true;
			}
		}
		return false;
	}

	
	public synchronized boolean authenticate(HttpServletRequest req, PathExclusions exclusions) throws Exception {

		String authHeader = req.getHeader(AUTHORIZATION);
		boolean isAuthenticated = false;
		CLFMethodContext clf = logCtx.getMethodContext("authenticate"); //$NON-NLS-1$

		/**
		 * TODO: here we are assuming that it is a basic authentication request, we need
		 * to have another property on shared resource to configure if it is a basic
		 * authentication and client also should pass special request header
		 */
		isAuthenticated = handleBasicAuthRequest(req, exclusions, authHeader);
		if (!isAuthenticated) {
			if (isSSOAuthenticated(orginialRequest)) {
				req.setAttribute("SSO_ENABLED", true);
				isAuthenticated = true;
			}
		}

		return isAuthenticated;

	}

	private boolean isSSOAuthenticated(HttpServletRequest request) {
		if (null != request) {
			HttpSession session = request.getSession();
			Object authToken = session.getAttribute(AUTH_TOKEN_ATTR_KEY);
			if (null != authToken) {
				// here we need to check if the access token expiry time has exceeded the max
				// idle time
				Object oBjTokenExpiry = session.getAttribute(AUTH_TOKEN_EXPIRY_ATTR_KEY);
				long tokenExpiry = 0;
				if (oBjTokenExpiry instanceof Long) {
					int maxInactiveInterval = session.getMaxInactiveInterval();
					tokenExpiry = ((Long) oBjTokenExpiry).longValue();
					// find the last accessed time and then calculate when the session expires
					// compare that with the expiry time
				}
				return true;
			}
		}
		return false;
	}

	private boolean isSSORequest(HttpServletRequest request) {

		return false;
	}

	private boolean handleBasicAuthRequest(HttpServletRequest req, PathExclusions exclusions, String authHeader)
			throws Exception {
		AuthLogger.debug(" Entered filter " + req.getRequestURL());
		// if the header is not present check if the session has the security
		// subject, if yes
		// we can go ahead and follow the chain. Also if the request contains
		// /apps/login/ we can ignore
		// intercepting
		if (exclusions.isExcludePath(req) || isAlreadySessionEstablished(req)) {
			// login successfull populateRequest Context;
			populateRequestContext(req, null, false);
			return true;
		} else {
			BasicAuthenticationHandler authenticationHandler = AuthenticationHandlerFactory.getInstance()
					.getBasicAuthenticationHandler();
			boolean isAuthenticated = authenticationHandler.handleAuthentication(req,null,authHeader);
			if (!isAuthenticated) {
				return false;
			} else {
				populateRequestContext(req, authenticationHandler, true);
				return true;
			}
		}
	}
	
	
	private boolean handleSSOAuthRequest(HttpServletRequest req, HttpServletResponse res,String userName)
			throws Exception {
		AuthLogger.debug(" Entered filter " + req.getRequestURL());
		
		
			 //SSOAuthenticationHandler authenticationHandler = AuthenticationHandlerFactory.getInstance().getSOOAuthenticationHandler();
			 SSOAuthenticationHandler authenticationHandler=new SSOAuthenticationHandler();
			 authenticationHandler.setDeDelegate(deDelegate);
			 boolean isAuthenticated = authenticationHandler.handleAuthentication(req,res,userName);
			if (!isAuthenticated) {
				return false;
			} else {
				populateRequestContext(req, authenticationHandler, true);
				return true;
			}
	}

	private synchronized void populateRequestContext(HttpServletRequest req, BPMAuthenticationHandler authenticationHandler, boolean setTimeout)
			throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("populateRequestContext"); //$NON-NLS-1$
		
		clf.local.debug("populateRequestContext called with auth handler" +authenticationHandler);
		HttpSession session = req.getSession();
		if (setTimeout) {
			int bpmSessionTimeout = AuthPropertyConfig.INSTANCE.getBpmSessionTimeout();
			//get already set session timeout to reset the value
			AuthPropertyConfig.INSTANCE.setDefaultSessionTimeout(session.getMaxInactiveInterval());
			session.setMaxInactiveInterval(bpmSessionTimeout);
			clf.local.debug("Setting setMaxInactiveInterval to '%d'", bpmSessionTimeout);
			if (listenerAdded.compareAndSet(false, true) && session instanceof Session) {
				// HttpSessionListener should be added only once during the bootstrap.
				((Session) session).getSessionHandler().addEventListener(getBpmSessionListener());
				clf.local.debug("Added HttpSessionListener to session handler.");
			}
		}
		// check if the user has already a secure session
		// populate the session with the current user for Subsequent requests
		Object objectUser = session.getAttribute(CURRENT_USER_KEY);
		clf.local.debug("current user key in the session "+objectUser);
		CurrentUser currentUser = null;
		if (null != objectUser) {
			currentUser = (CurrentUser) objectUser;
			// populate the request context
			RequestContext context = new RequestContext(req, null, currentUser);
			RequestContext.setCurrent(context);
		} else {
			Subject subject = null;

			if (null == authenticationHandler) {
				throw new Exception("No security context established, please check with Administrstor");
			} else {
				subject = authenticationHandler.getSubject();
				clf.local.debug("Subject from the handler is "+subject);
				if (null == subject || subject.getPrincipals().size() <= 0) {
					throw new Exception("No security context established, please check with Administrstor");
				}
			}
			// there is no existing session so create the current user
			clf.local.debug("no existing session so create the current user");
			Set<Principal> principals = subject.getPrincipals();
			String userName = null;
			String userGuid = null;
			for (Principal principal : principals) {
				if (principal instanceof UserNamePrinciple) {
					UserNamePrinciple userPrincipal = (UserNamePrinciple) principal;
					userName = userPrincipal.getName();
				} else if (principal instanceof UserGuidPrincipal) {
					UserGuidPrincipal guidPrincipal = (UserGuidPrincipal) principal;
					userGuid = guidPrincipal.getName();
				}
			}
			// create the current user
			currentUser = new CurrentUser(userGuid, userName, subject);
			session.setAttribute(CURRENT_USER_KEY, currentUser);
			// populate the request context
			RequestContext context = new RequestContext(req, null, currentUser);
			RequestContext.setCurrent(context);
			if(setTimeout) {
				clf.audit.audit(AuthAuditMessages.USER_LOGGED_IN, clf.param(CloudMetaData.USER_ID, currentUser.getGuid()),
						clf.param(CloudMetaData.USERNAME, currentUser.getName()),
						clf.param(CommonMetaData.MANAGED_OBJECT_ID, currentUser.getGuid()));
			}
			

		}
			
	

	}

	private boolean isAlreadySessionEstablished(HttpServletRequest req) throws ServiceException {

		String authHeader = req.getHeader(AUTHORIZATION);
		CLFMethodContext clf = logCtx.getMethodContext("isAlreadySessionEstablished");
		//if auth header is sent as part of login although there might be an existing session.
		//If the session is present then invlalidate it
		if(null!=authHeader) {
			if(AuthPropertyConfig.INSTANCE.isInvalidateSessionOnLogin()) {
				clf.local.debug("isInvalidateSessionOnLogin is true, so invalidate the session...");	
				HttpSession session = req.getSession();
				if(null!=session) {
					session.invalidate();
				}
				return false;
			}
		}
		// this needs to be replaced with the request context
		// Here we should not create a new session
		HttpSession session = req.getSession(false);
		if (session != null) {
			Object context = session.getAttribute(CURRENT_USER_KEY);
			if (null != context) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Return non-empty-value cookies related to OpenId authentication
	 * @param req {@link HttpServletRequest} from which cookie need to be find
	 * @return {@link Cookie}
	 */
	private Cookie getOpenIdCookie(HttpServletRequest req) {
		if (req != null) {
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie != null && cookie.getName() != null && cookie.getName().equals("OPENID_TOKEN")
							&& cookie.getValue() != null) {
						return cookie;
					}
				}
			}
		}
		return null;
	}

	/**
	 *  Return non-empty-value cookies related to SAML authentication
	 * @param req {@link HttpServletRequest} from which cookie need to be find
	 * @return {@link Cookie}
	 */
	private Cookie getSAMLCookie(HttpServletRequest req) {
		if (req != null) {
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie != null && cookie.getName() != null && cookie.getName().equals("SAML_TOKEN")
							&& cookie.getValue() != null) {
						return cookie;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Check for any SSO login error from reading the cookie. If there is an error then throw exception
	 * @param req Request
	 * @param res Response
	 * @throws UnauthorizedUserException thrown if there is an SSO authentication error
	 */
	private void checkForSSOLoginErrors(HttpServletRequest req, HttpServletResponse res) throws UnauthorizedUserException {
		if (req != null) {
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie != null && cookie.getName() != null && cookie.getName().equals("SSO_AUTH_ERROR")
							&& cookie.getValue() != null) {
						CLFMethodContext clf = logCtx.getMethodContext("checkForSSOLoginErrors"); //$NON-NLS-1$
						clf.local.messageId(AuthLoggerMessageId.AUTH_CHECK_FAIL_00002).error("Authentication failed.");
						clearCookie(res, cookie);
						throw new UnauthorizedUserException(ErrorCode.AUTH_INVALID_AUTHENTICATION,null,null);
					}
				}
			}
		}
	}
	
	/**
	 *  Clear cookie
	 * @param res Response which clear the cookie
	 * @param cookie to be cleared
	 */
	private void clearCookie(HttpServletResponse res, Cookie cookie) {
		cookie.setValue(null);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		res.addCookie(cookie);
	}

	@Override
	public void setOpenIdTokenVerifier(BPMAuthVerifier verifier) {
		openIdTokenVerifier = verifier;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
		deDelegate=new DEDelegate();
		deDelegate.setSecurityService(securityService);
	}

	/**
	 * @return the bpmSessionListener
	 */
	public HttpSessionListener getBpmSessionListener() {
		return bpmSessionListener;
	}

	/**
	 * @param bpmSessionListener the bpmSessionListener to set
	 */
	public void setBpmSessionListener(HttpSessionListener bpmSessionListener) {
		this.bpmSessionListener = bpmSessionListener;
	}
	
	public HttpClientSSOBinding getHttpClientSSOBinding() {
		return httpClientSSOBinding;
	}

	public void setHttpClientSSOBinding(HttpClientSSOBinding httpClientSSOBinding) {
		this.httpClientSSOBinding = httpClientSSOBinding;
	}

	@Override
	public void updateSSOBindingDetails(HttpClientBuilder builder, HttpClient client) throws Exception {
		getHttpClientSSOBinding().updateSSOBindingDetails(builder, client);
	}

	@Override
	public boolean isOpenIdTokenValid(String httpClientSharedResourceName) throws Exception {
		return getHttpClientSSOBinding().isOpenIdTokenValid(httpClientSharedResourceName);
	}

}
