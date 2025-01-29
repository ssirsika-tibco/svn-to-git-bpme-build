package com.tibco.bpm.auth.api;

import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class AuthLogger {
   
    private static CLFClassContext logCtx = CloudLoggingFramework.init(AuthLogger.class, AuthLoggingInfo.instance);
     
	public static void debug(String message) {
	    CLFMethodContext clf = logCtx.getMethodContext("debug"); //$NON-NLS-1$
        clf.local.trace(message);
	}
	
	public static void error(String message,Throwable t) {
		t.printStackTrace();
		CLFMethodContext clf = logCtx.getMethodContext("error"); //$NON-NLS-1$
        clf.local.trace(message);
	}
}
