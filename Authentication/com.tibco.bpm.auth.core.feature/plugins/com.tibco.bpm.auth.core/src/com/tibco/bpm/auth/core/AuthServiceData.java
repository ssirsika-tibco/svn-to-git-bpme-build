/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.core;

import java.util.List;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.OpenIdAuthentication;
import com.tibco.bpm.ace.admin.model.SamlWebProfileAuthentication;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.auth.admin.AuthPropertyConfig;

/**
 * This singleton class is used for storing the data related to REST service.
 * 
 * @author ssirsika
 *
 */
public class AuthServiceData {

	/**
	 * Enum representation of the supported auth modes
	 * 
	 * @author ssirsika
	 *
	 */
	public enum AUTH_MODE {
		BASIC(1), OPEN_ID(2), BASIC_OPEN_ID(3), SAML(4), BASIC_SAML(5);

		public final int value;

		private AUTH_MODE(int aValue) {
			value = aValue;
		}
	}

	public static final AuthServiceData INSTANCE = new AuthServiceData();

	private AdminConfigurationService adminConfigurationService;

	AuthServiceData() {
		// package level constructor
	}

	/**
	 * @return the adminConfigurationService
	 */
	public AdminConfigurationService getAdminConfigurationService() {
		return adminConfigurationService;
	}

	/**
	 * @param adminConfigurationService the adminConfigurationService to set
	 */
	public void setAdminConfigurationService(AdminConfigurationService adminConfigurationService) {
		this.adminConfigurationService = adminConfigurationService;
	}

	/**
	 * Return the configured {@link OpenIdAuthentication} If nothing is configured
	 * then return null.
	 * 
	 * @return {@link OpenIdAuthentication}
	 */
	public OpenIdAuthentication getOpenIdAuthRI() throws ServiceException {
		if (adminConfigurationService != null) {
			AuthPropertyConfig authPropertyConfig = AuthPropertyConfig.INSTANCE;
			String resourceName = (authPropertyConfig != null) ? authPropertyConfig.getOpenIdAuthSharedResourceName()
					: null;
			if (resourceName != null) {
				OpenIdAuthentication openIdAuthRI = adminConfigurationService
						.getOpenIdAuthenticationByName(resourceName);
				return (openIdAuthRI != null && openIdAuthRI.getEnabled()) ? openIdAuthRI : null;
			} else {
				List<String> openIdAuthRINames = adminConfigurationService.getOpenIdAuthenticationNames();
				for (String riName : openIdAuthRINames) {
					OpenIdAuthentication openIdAuthRI = adminConfigurationService.getOpenIdAuthenticationByName(riName);
					if (openIdAuthRI != null && openIdAuthRI.getEnabled()) {
						return openIdAuthRI;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return the configured {@link SamlWebProfileAuthentication} If nothing is configured
	 * then return null.
	 * 
	 * @return {@link SamlWebProfileAuthentication}
	 */
	public SamlWebProfileAuthentication getSAMLAuthRI() throws ServiceException {
		if (adminConfigurationService != null) {
			List<String> samlAuthRINames = adminConfigurationService.getSamlWebProfileAuthenticationNames();
			for (String riName : samlAuthRINames) {
				SamlWebProfileAuthentication samlAuthRI = adminConfigurationService.getSamlWebProfileAuthenticationByName(riName);
				if (samlAuthRI != null && samlAuthRI.getEnabled()) {
					return samlAuthRI;
				}
			}
		}
		return null;
	}
	
	/**
	 * Return the configured {@link AUTH_MODE} using {@link AuthPropertyConfig} and
	 * {@link AdminConfigurationService}
	 * 
	 * @return {@link AUTH_MODE} enum
	 */
	public AUTH_MODE getConfiguredAuthModeEnum() throws ServiceException{
		OpenIdAuthentication openIdAuthRI = getOpenIdAuthRI();
		boolean basicAuthEnabled = AuthPropertyConfig.INSTANCE.isBasicAuthEnabled();
		if (openIdAuthRI != null) {
			boolean openIdAuthEnabled = (openIdAuthRI != null && openIdAuthRI.getEnabled()) ? true : false;
			if (openIdAuthEnabled) {
				return basicAuthEnabled ? AUTH_MODE.BASIC_OPEN_ID : AUTH_MODE.OPEN_ID;
			}
		}
		SamlWebProfileAuthentication samlAuthRI = getSAMLAuthRI();
		if(samlAuthRI != null) {
			boolean samlAuthEnabled = (samlAuthRI != null && samlAuthRI.getEnabled()) ? true : false;
			if (samlAuthEnabled) {
				return basicAuthEnabled ? AUTH_MODE.BASIC_SAML : AUTH_MODE.SAML;
			}
		}
		return AUTH_MODE.BASIC;
	}

	/**
	 * Return the supported authorization mode value using
	 * {@link AuthPropertyConfig} and {@link AdminConfigurationService}.
	 * 
	 * @return int supported/configured auth mode value
	 */
	public int getConfiguredAuthMode() throws ServiceException{
		return getConfiguredAuthModeEnum().value;
	}
}
