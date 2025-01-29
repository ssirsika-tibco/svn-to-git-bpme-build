/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.saml;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.auth.api.BPMAuthenticationListener;
import com.tibco.bpm.auth.saml.config.DynamicWebSecurityConfig;
import com.tibco.bpm.auth.saml.config.SAMLWebProfileConfiguration;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Context Helper is used to store the context data which need to be used in
 * other classes of this module. This class is instantiated from
 * 'SAML-Auth-Blueprint.xml' and marked as a 'singleton'.
 * 
 * @author ssirsika
 * 
 */
public class SAMLContextHelper {
	
	private static CLFClassContext logCtx = CloudLoggingFramework.init(SAMLContextHelper.class,
			AuthLoggingInfo.instance);

	private BPMAuthenticationListener authListener;

	private static SAMLContextHelper INSTANCE;
	
	private AdminConfigurationService adminConfigService;
	
	/**
	 * Default constructor which will be called from 'SAML-Auth-Blueprint.xml'
	 * only one time.
	 */
	public SAMLContextHelper() {
		INSTANCE = this;
		CLFMethodContext clf = logCtx.getMethodContext("SAMLContextHelper");
		clf.local.trace("Creating instance of SAMLContextHelper");
	}
	
	/**
	 * Return the instance of {@link SAMLContextHelper}
	 * 
	 * @return
	 */
	public static SAMLContextHelper getINSTANCE() {
		return INSTANCE;
	}
	
	/**
	 * Return the {@link AdminConfigurationService}
	 * 
	 * @return
	 */
	public AdminConfigurationService getAdminConfigService() {
		return adminConfigService;
	}

	/**
	 * Set the {@link AdminConfigurationService}
	 * 
	 * @param adminConfigService
	 */
	public void setAdminConfigService(AdminConfigurationService adminConfigService) {
		this.adminConfigService = adminConfigService;
	}

	/**
	 * The reference element can notify reference listeners of the service selection
	 * changes. This method is callback method and will be called when service is
	 * injected/changed.
	 * 
	 * @param service the types implemented by the service object proxy
	 */
	public void setReference(AdminConfigurationService service) {
		CLFMethodContext clf = logCtx.getMethodContext("setReference");
		clf.local.debug("Setting the AdminConfigurationService reference");

		setAdminConfigService(service);
		
		clf.local.trace("About the initialize SAML configuration from Resource Instance");
		// Set the configuration properties from adminService by reading the resource instance.
		// Configure the security config only if required resource instance is present and enabled.
		if (SAMLWebProfileConfiguration.INSTANCE.init(service)) {
			clf.local.trace("Configuring the SAML Security");
			AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
			applicationContext.register(DynamicWebSecurityConfig.class);
			applicationContext.setClassLoader(SAMLSecurityBundleActivator.class.getClassLoader());
			applicationContext.refresh();

			DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("springSecurityFilterChain",
					applicationContext);
			clf.local.trace("Registering 'springSecurityFilterChain' as a service");
			SAMLSecurityBundleActivator.INSTANCE.registerFilterAsService(filterProxy,
					new String[] { SAMLWebProfileConfiguration.INSTANCE.getIdpLoginURL(),
							SAMLWebProfileConfiguration.INSTANCE.getIdpSSOURL() });
		}
		
		if (getAuthListener() != null) {
			clf.local.debug("Notify listeners about AdminConfigService registration");
			getAuthListener().onAdminServiceRegister(service);
		}
	}

	/**
	 * The reference element can notify reference listeners of the service selection
	 * changes. This method is callback method and will be called when service is
	 * injected/changed.
	 * 
	 * @param service the types implemented by the service object proxy
	 */
	public void setReference(BPMAuthenticationListener aListener) {
		CLFMethodContext clf = logCtx.getMethodContext("setReference");
		clf.local.debug("Setting the BPMAuthenticationListener reference");

		setAuthListener(aListener);
		if (getAdminConfigService() != null) {
			clf.local.debug("Notify listeners about AdminConfigService registration");
			getAuthListener().onAdminServiceRegister(getAdminConfigService());
		}
	}
	
	/**
	 * @return the authListener
	 */
	public BPMAuthenticationListener getAuthListener() {
		return authListener;
	}

	/**
	 * @param authListener the authListener to set
	 */
	public void setAuthListener(BPMAuthenticationListener authListener) {
		this.authListener = authListener;
	}

}
