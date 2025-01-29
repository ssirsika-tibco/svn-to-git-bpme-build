package com.tibco.bpm.auth;

import java.util.HashMap;
import java.util.Map;

import com.tibco.n2.de.api.services.SecurityService;
import com.tibco.n2.de.api.services.UserDetails;
import com.tibco.n2.de.services.InternalServiceFault;
import com.tibco.n2.de.services.InvalidServiceRequestFault;
import com.tibco.n2.de.services.SecurityFault;

public class DEDelegate {
	public static final String GUID = "com.tibco.bpm.guid";
	public static final String LDAP_ALIAS = "com.tibco.bpm.ldap-alias";
	public static final String LDAP_DN = "com.tibco.bpm.ldap-dn";
	public static final String USER_NAME = "com.tibco.bpm.name";

	private SecurityService securityService;

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public Map<String,String> lookupUser(String userName) throws InternalServiceFault, InvalidServiceRequestFault{
		 Map<String,String> ldapOptions= new HashMap<String, String>();
		//make the call to Directory Engine
		AuthLogger.debug("Making the call to Directory engine for lookup "+userName);
		UserDetails userDetails = getSecurityService().lookupUser(userName);
		if(null!=userDetails){
			ldapOptions.put(USER_NAME,userDetails.getName());
			ldapOptions.put(GUID,userDetails.getGuid());
			ldapOptions.put(LDAP_DN,userDetails.getLdapDn());
			ldapOptions.put(LDAP_ALIAS,userDetails.getLdapAlias());
		}
		return ldapOptions;
	}

	public boolean authenticate(String ldapAlias,String userDn,String password) throws SecurityFault, InvalidServiceRequestFault{
		boolean authenticate=false;
		authenticate = getSecurityService().authenticate(ldapAlias, userDn, password);
		return authenticate;
	}
}
