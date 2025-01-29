package com.tibco.bpm.auth;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

import org.osgi.framework.BundleContext;

public abstract class BPMAbstractLognModule implements LoginModule {
	protected Set<Principal> principals = new HashSet<Principal>();
	protected Subject subject;
	protected String user;
	protected CallbackHandler callbackHandler;
	protected boolean debug;
	protected Map<String, ?> options;
	protected boolean authenticated = false;
	
	 /**
     * the bundle context is required to use the encryption service
     */
    protected BundleContext bundleContext;
    
    public void initialize(Subject sub, CallbackHandler handler, Map<String, ?> options) {
        this.subject = sub;
        this.callbackHandler = handler;
        this.options = options;
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
    }
}
