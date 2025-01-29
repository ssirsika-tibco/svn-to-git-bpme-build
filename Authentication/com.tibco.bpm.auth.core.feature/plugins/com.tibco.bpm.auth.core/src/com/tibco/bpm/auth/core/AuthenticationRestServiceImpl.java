package com.tibco.bpm.auth.core;

import javax.ws.rs.core.Response;

import com.tibco.bpm.auth.api.AuthLogger;
import com.tibco.bpm.auth.api.AuthenticationRestService;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.security.CurrentUser;
import com.tibco.n2.common.security.RequestContext;

public class AuthenticationRestServiceImpl implements AuthenticationRestService {

	static CLFClassContext logCtx = CloudLoggingFramework.init(AuthenticationRestServiceImpl.class,
			AuthLoggingInfo.instance);

	@Override
	public Response authenticate() {
		CLFMethodContext clf = logCtx.getMethodContext("authenticate");
		CurrentUser currentUser = RequestContext.getCurrent().getCurrentUser();
		UserInfo user = new UserInfo(currentUser.getName(), currentUser.getGuid());
		AuthLogger.debug("Setting the current user " + currentUser.getName() + ":" + currentUser.getGuid());
		clf.local.debug("Authenticated with User ID = '%s' and User name = '%s'", currentUser.getGuid(), currentUser.getName());
		return Response.ok(user).build();
	}
}
