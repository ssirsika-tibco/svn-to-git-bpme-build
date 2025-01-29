/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.tibco.bpm.auth.BPMSession;
import com.tibco.bpm.auth.exception.UnauthorizedUserException;
import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;

/**
 * Implementation for <code>SessionInfoService</code>.
 *
 * @author sajain
 * @since Mar 31, 2020
 */
public class SessionInfoServiceImpl implements SessionInfoService{

    /**
     * Java object to represent the current BPM session.
     */
    private BPMSession bpmSession;

    /**
     * Original request to get BPM session attributes from.
     */
    @Context
    private HttpServletRequest originalRequest;
   
    /**
     * @see sample.v5.SessionInfoService#getCurrentSessionObject()
     *
     * @return
     * @throws Exception
     */
    @Override
    public Response getCurrentSessionObject() throws Exception {
        try {
            if (null != originalRequest) {
                /*
                 * Get the HTTP session object from the original request.
                 */
                HttpSession httpSessionInstance = originalRequest.getSession();
                if (null != httpSessionInstance) {
                    /*
                     * Update the BPM session instance with the latest state of
                     * HTTP session instance.
                     */
                    this.bpmSession = new BPMSession(httpSessionInstance.getLastAccessedTime(),
                            Long.valueOf(httpSessionInstance.getMaxInactiveInterval()).longValue());

                    if (null != this.bpmSession) {
                        return Response.ok(this.bpmSession).build();
                    } else {
                        throw new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, null);
                    }
                } else {
                    throw new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, null);
                }
            }
        } catch (Exception e) {
            throw new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, null);
        }

        throw new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, null);
    }

    /**
     * @return the bpmSession
     */
    public BPMSession getBpmSession() {
        return bpmSession;
    }

    /**
     * @param bpmSession the bpmSession to set
     */
    public void setBpmSession(BPMSession bpmSession) {
        this.bpmSession = bpmSession;
    }

}
