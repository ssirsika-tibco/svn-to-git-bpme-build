/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.core;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Authentication engine service to check ping (i.e., to check if the server is online or not).
 *
 * @author sajain
 * @since Mar 31, 2020
 */

@Path("/ping")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface PingService
{
    
    /**
     * REST service to check ping (i.e., to check if server is online or not).
     * 
     * @param artifactVersionId
     * @return
     * @throws Exception
     */
    @GET
    public Response getPingStatus() throws Exception;
}
