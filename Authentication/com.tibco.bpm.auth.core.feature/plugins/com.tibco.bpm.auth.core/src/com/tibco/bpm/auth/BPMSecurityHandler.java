package com.tibco.bpm.auth;

import java.io.IOException;

import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class BPMSecurityHandler extends SecurityHandler{

    private static CLFClassContext logCtx = CloudLoggingFramework.init(BPMSecurityHandler.class, AuthLoggingInfo.instance);
    
	@Override
	protected RoleInfo prepareConstraintInfo(String pathInContext, Request request) {
	    CLFMethodContext clf = logCtx.getMethodContext("prepareConstraintInfo"); //$NON-NLS-1$
	    clf.local.trace("prepareConstraintInfo"); //$NON-NLS-1$
		return null;
	}

	@Override
	protected boolean checkUserDataPermissions(String pathInContext, Request request, Response response,
			RoleInfo constraintInfo) throws IOException {
	    CLFMethodContext clf = logCtx.getMethodContext("checkUserDataPermissions"); //$NON-NLS-1$
	    clf.local.trace("checkUserDataPermissions"); //$NON-NLS-1$
		return false;
	}

	@Override
	protected boolean isAuthMandatory(Request baseRequest, Response base_response, Object constraintInfo) {
	    CLFMethodContext clf = logCtx.getMethodContext("isAuthMandatory"); //$NON-NLS-1$
	    clf.local.trace("isAuthMandatory"); //$NON-NLS-1$
		return true;
	}

	@Override
	protected boolean checkWebResourcePermissions(String pathInContext, Request request, Response response,
			Object constraintInfo, UserIdentity userIdentity) throws IOException {
	    CLFMethodContext clf = logCtx.getMethodContext("checkWebResourcePermissions"); //$NON-NLS-1$
	    clf.local.trace("checkWebResourcePermissions"); //$NON-NLS-1$
		return false;
	}

}
