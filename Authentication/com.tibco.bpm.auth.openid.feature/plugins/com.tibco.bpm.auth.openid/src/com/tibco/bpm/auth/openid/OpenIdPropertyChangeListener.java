package com.tibco.bpm.auth.openid;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.api.PropertyChangeService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.model.PropertyChange;
import com.tibco.bpm.auth.openid.config.OpenIdResourceProvider;
import com.tibco.bpm.auth.openid.config.OpenIdSecurityConfig;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class OpenIdPropertyChangeListener implements PropertyChangeService {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdPropertyChangeListener.class,
			AuthLoggingInfo.instance);

	@Override
	public void processPropertyChange(PropertyChange propertyChange) {

		CLFMethodContext clf = logCtx.getMethodContext("processPropertyChange");
		Property property = propertyChange.getProperty();

		if ((property != null) && (property.getGroupId().compareTo(GroupId.auth) == 0)
				&& property.getName().equalsIgnoreCase("ssoMode") && property.getValue().equalsIgnoreCase("openId")) {

			clf.local.trace("Enable the OpenID authentication");
			// Set the configuration properties from adminService by reading the resource
			// instance.
			// Configure the security config only if required resource instance is present
			// and enabled.
			AdminConfigurationService service = OpenIdContextHelper.getINSTANCE().getAdminConfigService();
			if (service != null) {
				clf.local.trace("About the initialize OpenID conf from Resource Instance");
				if (OpenIdResourceProvider.INSTANCE.init(service)) {
					clf.local.trace("Configuring the OpenId Security");
					AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
					applicationContext.register(OpenIdSecurityConfig.class);
					applicationContext.setClassLoader(OpenIdSecurityBundleActivator.class.getClassLoader());
					applicationContext.refresh();

					DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("springSecurityFilterChain",
							applicationContext);
					clf.local.trace("Registering 'springSecurityFilterChain' as a service");
					OpenIdSecurityBundleActivator.INSTANCE.registerFilterAsService(filterProxy, OpenIdResourceProvider.INSTANCE.getRelativeRedirectUri());
				}
			}
		}
	}
}
