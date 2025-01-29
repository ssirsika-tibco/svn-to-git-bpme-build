/*
\ * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tibco.bpm.auth;


import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.NamespaceException;

import com.tibco.bpm.auth.api.SecurityFilter;
import com.tibco.bpm.auth.filters.AuthenticateServlet;
import com.tibco.bpm.auth.filters.LogoutServlet;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator implements BundleActivator {

	private ServiceReference<?> reference;
	private WebContainer web;
	private ServiceRegistration<org.eclipse.jetty.server.Handler> handlers;
	
	private static CLFClassContext logCtx; 

	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		//if(isEnableAuth())
			//addContextHandler(bc);
		//registerWebContainer(bc);
	}

	@SuppressWarnings("restriction")
	private void registerWebContainer(BundleContext bc) throws ServletException, NamespaceException, InterruptedException {
		
		int counter = 0;
	    boolean started = false;
	    while (!started) {

	    	reference = bc.getServiceReference(WebContainer.class);
	        started = reference != null;
	        if (started) {
	            final WebContainer web = (WebContainer) bc
	                    .getService(reference);
	            if (web != null) {
					//web.registerFilter(new SecurityFilter(),new String[]{"*","/bpm/*","/apps/*"},null,null, null);
					web.registerServlet("/bpm/authenticate", new AuthenticateServlet(), null,
							null);
					web.registerServlet("/bpm/logout", new LogoutServlet(), null,
							null);
	            }else {
	                // wait, throw exception after 5 retries.
	                if (counter > 10) {
	                    throw new ServletException(
	                            "Could not start the helloworld-wc service, WebContainer service not started or not available.");
	                } else {
	                    counter++;
	                    Thread.sleep(counter * 1000);
	                }
	            }
	        }
	    }
		
	}

	private void addContextHandler(BundleContext bc) {
		ServletContextHandler rootContext=new ServletContextHandler(ServletContextHandler.SESSIONS);
		rootContext.addFilter(new FilterHolder(new SecurityFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
		rootContext.setDefaultSecurityHandlerClass(BPMSecurityHandler.class);
		
		ServletHolder authHolder= new ServletHolder("authServlet", new AuthenticateServlet());
		rootContext.addServlet(authHolder, "/bpm/authenticate");
		ServletHolder logoutHolder= new ServletHolder("logoutServlet", new LogoutServlet());
		rootContext.addServlet(logoutHolder, "/bpm/logout");
		CLFMethodContext clf = getLoggingContext().getMethodContext("addContextHandler"); //$NON-NLS-1$
		clf.local.trace("registering the service"); //$NON-NLS-1$
		ServletContextListener contextListener=new ServletContextListener() {
			
			@Override
			public void contextInitialized(ServletContextEvent event) {
				AuthLogger.debug("context created "+event.getServletContext().getContextPath());
			}
			
			@Override
			public void contextDestroyed(ServletContextEvent event) {
				AuthLogger.debug("context destroyed "+event.getServletContext().getContextPath());				
			}
		};
		handlers = bc.registerService(org.eclipse.jetty.server.Handler.class,rootContext,null);
		
	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if(null!=reference){
			bc.ungetService(reference);
			reference=null;
			web=null;
		}
		if(null!=handlers){
			handlers.unregister();
			handlers=null;
		}
	}
	
	private boolean isEnableAuth(){
		String isAuthenitcate = System.getProperty("com.tibco.authenticate");
		if(null!=isAuthenitcate && isAuthenitcate.equalsIgnoreCase("false")){
			return false;
		}
		return true;
	}
	
	/**
	 * Return the {@link CLFClassContext} for current class.
	 * @return
	 */
	private CLFClassContext getLoggingContext() {
		if(logCtx == null) {
			logCtx = CloudLoggingFramework.init(Activator.class, AuthLoggingInfo.instance);
		}
		return logCtx;
	}
}
