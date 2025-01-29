/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.saml;

import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.tibco.bpm.auth.api.BpmDummySSOServlet;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * @author ssirsika
 *
 */
public class SAMLSecurityBundleActivator implements BundleActivator {

	private static ApplicationContext context;

	private BundleContext bundleContext;

	public static SAMLSecurityBundleActivator INSTANCE;

	private static CLFClassContext logCtx;

	@Override
	public void start(BundleContext bc) throws Exception {
		this.bundleContext = bc;
		INSTANCE = this;
	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
	}

	public static ApplicationContext getApplicationContext() {
		return context;
	}

	public static void setApplicationContext(ApplicationContext context) {
		SAMLSecurityBundleActivator.context = context;
	}

	/**
     * Registers the provided filter as an OSGi service, making it available as a servlet
     * filter within the Jetty (Web server) environment.
     * 
     * <p>This method leverages the OSGi HTTP whiteboard service pattern to dynamically
     * register the filter, allowing Pax Web to detect and apply it to specified URL patterns.
     * Registering the filter this way is necessary to ensure it is automatically applied to 
     * requests matching these patterns without manual configuration in Pax Web.</p>
     * 
     * @param filterProxy The filter instance to be registered.
     * @param urls The URL patterns to associate with this filter on the whiteboard.
     */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void registerFilterAsService(DelegatingFilterProxy filterProxy, String[] urls) {
		if (urls.length > 0) {
			CLFMethodContext clf = getLoggingContext().getMethodContext("registerFilterAsService");
			clf.local.trace("Registering 'springSecurityFilterChain' as a service in bundleContext");
			
			Hashtable props = new Hashtable();
			/* ACE-8799: Prior to the Pax-Web upgrade from version 7.x to 8.x, filters were applied globally on '/*'.
			After the upgrade, filters must be applied individually to specific SAML SSO-related URLs.
			Therefore, we now set each SAML URL pattern explicitly. */
			props.put("osgi.http.whiteboard.filter.pattern", urls);
			props.put("osgi.http.whiteboard.filter.name", "springSecurityFilterChain");

			// Register the filter with the OSGi context
			bundleContext.registerService(Filter.class.getName(), filterProxy, props);
			
			// To ensure Pax Web processes the filter, we must register a servlet against these URLs.
			registerServlet(urls);
		}
	}

	
    /**
     * ACE-8799: Registers a dummy servlet for the provided URLs to ensure Pax Web applies filters
     * on these endpoints. After upgrading Pax Web from 7.x to 8.x, simply registering 
     * filters with the whiteboard service alone no longer triggers them. To address this,
     * an empty servlet is registered at each URL, ensuring that Pax Web processes the
     * 'springSecurityFilterChain' filter for Single Sign-On (SSO) functionality.
     * 
     * <p>This change was introduced to support Pax Web’s updated behavior in version 8.x,
     * which requires an active servlet mapped to a URL for any registered filter to execute.
     * Without this dummy servlet registration, Pax Web will not invoke the SSO filters.</p>
     * 
     * @param urls The URL patterns for which dummy servlets should be registered.
     */
	private void registerServlet(String[] urls) {
		for (int i = 0; i < urls.length; i++) {
			Hashtable<String, String> properties = new Hashtable<>();
			properties.put("osgi.http.whiteboard.servlet.pattern", urls[i]);
			properties.put("osgi.http.whiteboard.servlet.name", "bpmSamlSsoServlet" + (i + 1));

            // Register a dummy servlet with the OSGi context to activate the filter on the given URL.
			bundleContext.registerService(Servlet.class, new BpmDummySSOServlet(), properties);
		}
	}
	
	/**
	 * Returns the bean object for passed bean 'name'. If not found then logs the
	 * error and returns null;
	 * 
	 * @param name name of the bean to be lookup
	 * @return the bean object otherwise null if not found
	 */
	public static Object getBean(String name) {
		CLFMethodContext clf = getLoggingContext().getMethodContext("getBean");
		try {
			return getApplicationContext().getBean(name);
		} catch (Exception e) {
			clf.local.error(e, "Error while accessing the beans");
			return null;
		}
	}
	
	/**
	 * Return the {@link CLFClassContext} for current class.
	 * @return
	 */
	private static CLFClassContext getLoggingContext() {
		if(logCtx == null) {
			logCtx = CloudLoggingFramework.init(SAMLSecurityBundleActivator.class,
					AuthLoggingInfo.instance);
		}
		return logCtx;
	}
}
