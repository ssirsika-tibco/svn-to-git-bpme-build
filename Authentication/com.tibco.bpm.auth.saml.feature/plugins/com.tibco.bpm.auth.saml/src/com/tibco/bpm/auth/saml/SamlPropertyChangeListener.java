package com.tibco.bpm.auth.saml;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.api.PropertyChangeService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.model.PropertyChange;
import com.tibco.bpm.auth.saml.config.DynamicWebSecurityConfig;
import com.tibco.bpm.auth.saml.config.SAMLWebProfileConfiguration;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class SamlPropertyChangeListener implements PropertyChangeService {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(SamlPropertyChangeListener.class,
			AuthLoggingInfo.instance);

	@Override
	public void processPropertyChange(PropertyChange propertyChange) {

		CLFMethodContext clf = logCtx.getMethodContext("processPropertyChange");
		Property property = propertyChange.getProperty();

		if ((property != null) && (property.getGroupId().compareTo(GroupId.auth) == 0)
				&& property.getName().equalsIgnoreCase("ssoMode") && property.getValue().equalsIgnoreCase("saml")) {
			
			clf.local.trace("Enable the SAML authentication");
			

			// Set the configuration properties from adminService by reading the resource
			// instance.
			// Configure the security config only if required resource instance is present
			// and enabled.
			AdminConfigurationService service = SAMLContextHelper.getINSTANCE().getAdminConfigService();
			if (service != null) {
				clf.local.trace("About the initialize SAML conf from Resource Instance");
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
			}
		}
	}
}
