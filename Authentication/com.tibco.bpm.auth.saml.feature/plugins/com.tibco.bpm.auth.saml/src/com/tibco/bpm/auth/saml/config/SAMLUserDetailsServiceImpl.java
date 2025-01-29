/**
 * Copyright (c) TIBCO Software Inc 2004 - 2020. All rights reserved.
 */

package com.tibco.bpm.auth.saml.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {
	
	private static CLFClassContext logCtx = CloudLoggingFramework.init(SAMLUserDetailsServiceImpl.class,
			AuthLoggingInfo.instance);

	public Object loadUserBySAML(SAMLCredential credential)
			throws UsernameNotFoundException {
		CLFMethodContext clf = logCtx.getMethodContext("loadUserBySAML");
		String userID = credential.getNameID().getValue();
		clf.local.debug("Loading user with ID : %s", userID);
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
		authorities.add(authority);

		return new User(userID, "", true, true, true, true, authorities);
	}
	
}
