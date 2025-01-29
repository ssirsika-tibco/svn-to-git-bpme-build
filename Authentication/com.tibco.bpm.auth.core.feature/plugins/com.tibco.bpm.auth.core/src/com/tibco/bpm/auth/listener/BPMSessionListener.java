/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.tibco.bpm.auth.logging.AuthAuditMessages;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.security.CurrentUser;
import com.tibco.n2.common.security.RequestContext;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * BPMSessionListener will get the notification when new session is created or
 * destroyed. Currently this class's sole responsibility is to log the audit event
 * when session is destroyed because of timeout.
 * 
 * @author ssirsika
 *
 */
public class BPMSessionListener implements HttpSessionListener {

	private static CLFClassContext logCtx;

	public void sessionCreated(HttpSessionEvent se) {
		// Do nothing
	}

	/**
	 * Called when session is destroyed
	 */
	public void sessionDestroyed(HttpSessionEvent se) {
		CLFMethodContext clf = getLogginContext().getMethodContext("sessionDestroyed");

		RequestContext requestContext = RequestContext.getCurrent();
		// when request context is null that means session was destroyed as timeout happened.
		CurrentUser currentUser=null;
		if(null!=requestContext) {
			currentUser = RequestContext.getCurrent().getCurrentUser();
		}
		if (null != currentUser) {
			clf.audit.audit(AuthAuditMessages.USER_LOGGED_OUT, clf.param(CloudMetaData.USER_ID, currentUser.getGuid()),
					clf.param(CloudMetaData.USERNAME, currentUser.getName()),
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, currentUser.getGuid()));
		} else {
			clf.audit.audit(AuthAuditMessages.USER_LOGGED_OUT);
		}
	}

	private static CLFClassContext getLogginContext() {
		if (logCtx == null) {
			synchronized (BPMSessionListener.class) {
				if (logCtx == null) {
					logCtx = CloudLoggingFramework.init(BPMSessionListener.class, AuthLoggingInfo.instance);
				}
			}
		}
		return logCtx;
	}
}