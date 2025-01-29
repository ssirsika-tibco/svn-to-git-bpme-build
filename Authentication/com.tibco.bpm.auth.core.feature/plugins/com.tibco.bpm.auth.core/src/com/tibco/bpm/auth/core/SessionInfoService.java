/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.core;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Authentication engine service to handle client session.
 *
 * @author sajain
 * @since Mar 31, 2020
 */
@Path("/clientSession")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface SessionInfoService {
    
    /**
     * REST service to get the current session object.
     * 
     * @return the current session object.
     * 
     * @throws Exception
     */
    @GET
    public Response getCurrentSessionObject() throws Exception;
}
