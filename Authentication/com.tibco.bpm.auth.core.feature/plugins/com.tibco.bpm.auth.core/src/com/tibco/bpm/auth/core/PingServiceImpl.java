/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.core;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.n2.common.security.CurrentUser;
import com.tibco.n2.common.security.RequestContext;
import com.tibco.bpm.auth.exception.UnauthorizedUserException;

/**
 * Service implementation of <code>PingService</code>
 *
 * @author sajain
 * @since Mar 31, 2020
 */
public class PingServiceImpl implements PingService{

    /**
     * @see sample.v5.PingService#getPingStatus()
     *
     * @return
     * @throws Exception
     */
    @Override
    public Response getPingStatus() throws Exception {
        try {
        	CurrentUser currentUser = RequestContext.getCurrent().getCurrentUser();
    		UserInfo user = new UserInfo(currentUser.getName(), currentUser.getGuid());
            return Response.ok(user).header("X-BPM-AUTH-MODE", AuthServiceData.INSTANCE.getConfiguredAuthMode()).build();
        } catch (Exception e) {
            throw new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, null);
        }
    }

}
