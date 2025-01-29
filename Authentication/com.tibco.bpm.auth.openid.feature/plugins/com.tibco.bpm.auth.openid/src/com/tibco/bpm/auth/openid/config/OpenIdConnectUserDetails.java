/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.openid.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;


/**
 * Stores the User information to be encapsulated into {@link Authentication} 
 * @author ssirsika
 *
 */
public class OpenIdConnectUserDetails implements UserDetails {

	private CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdConnectUserDetails.class, AuthLoggingInfo.instance);
	
    private static final long serialVersionUID = 2L;

    private String userId;
    private OAuth2AccessToken token;
    private static final String defaultUserKey = "email";
    private static final String subKey = "sub";
    private Map<String, Object> userInfo;
    private String userKey;

    public OpenIdConnectUserDetails(Map<String, Object> userInfo, OAuth2AccessToken token, String userKey) {
        this.userId = (String) userInfo.get(subKey);
        this.userInfo = userInfo;
        this.token = token;
        this.userKey = userKey;
		CLFMethodContext clf = logCtx.getMethodContext("OpenIdConnectUserDetails");
		clf.local.trace("Creating OpenId User details having '%s'", toString());
    }

    @Override
    public String getUsername() {
		CLFMethodContext clf = logCtx.getMethodContext("getUsername");
    	if (userInfo != null) {
			if (userKey != null && userInfo.containsKey(userKey)) {
				clf.local.trace("Returning user name for key '%s': '%s'", userKey, userInfo.get(userKey));
				return (String) userInfo.get(userKey);
			}

			if (userInfo.containsKey(defaultUserKey)) {
				clf.local.trace("Returning user name for default key '%s': '%s'", defaultUserKey, userInfo.get(defaultUserKey));
				return (String) userInfo.get(defaultUserKey);
			} else {
				clf.local.trace("Returning user name for subKey '%s': '%s'", subKey, userInfo.get(subKey));
				return (String) userInfo.get(subKey);
			}
		}
		return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OAuth2AccessToken getToken() {
        return token;
    }

    public void setToken(OAuth2AccessToken token) {
        this.token = token;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

	@Override
	public String toString() {
		return "OpenIdConnectUserDetails [userId=" + userId + ", token=" + token + ", userInfo=" + userInfo
				+ ", userKey=" + userKey + "]";
	}

}
