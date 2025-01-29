/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.openid;

import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.tibco.bpm.auth.api.BpmDummySSOServlet;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class OpenIdSecurityBundleActivator implements BundleActivator {


	private static CLFClassContext logCtx; 
	
	private static ApplicationContext context;
	public static OpenIdSecurityBundleActivator INSTANCE;
	private BundleContext bundleContext;
	
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
		CLFMethodContext clf = getLoggingContext().getMethodContext("setApplicationContext");
		clf.local.debug("Setting application context");
		OpenIdSecurityBundleActivator.context = context;
	}

	/**
	 * Register the filter as an OSGi service. Jetty (Web server) will be notified about this 
	 * filter (which is registered as a service) and automatically registered as a servlet filter.
	 * @param filterProxy filter which need to be registered.
	 */
	public void registerFilterAsService(DelegatingFilterProxy filterProxy, String redirectURI) {
		CLFMethodContext clf = getLoggingContext().getMethodContext("registerFilterAsService");
		clf.local.debug("Registering 'springSecurityFilterChain' as a service in bundleContext");
		Hashtable<String, String> props = new Hashtable<String, String>();
		//After upgrade to pax web 8.0.x , we no more need filter-pattern to be /* 
		//as the filter priorities have changed. Now we only need the sso redirect URI
		//to go through the filter chain
		props.put("osgi.http.whiteboard.filter.pattern", redirectURI);
		props.put("osgi.http.whiteboard.filter.name", "springSecurityFilterChain");
		bundleContext.registerService(Filter.class.getName(), filterProxy, props);
		registerServlet(redirectURI);
	}
	
	private void registerServlet(String redirectURI) {
		Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("osgi.http.whiteboard.servlet.pattern", redirectURI);
        properties.put("osgi.http.whiteboard.servlet.name", "ssoServlet");

        // Register the servlet with the OSGi context
        bundleContext.registerService(
            Servlet.class,
            new BpmDummySSOServlet(),
            properties
        );		
	}

	/**
	 *  Returns the bean object for passed bean 'name'. If not found then logs the error and returns null; 
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
			logCtx = CloudLoggingFramework.init(OpenIdSecurityBundleActivator.class,
					AuthLoggingInfo.instance);
		}
		return logCtx;
	}
}
