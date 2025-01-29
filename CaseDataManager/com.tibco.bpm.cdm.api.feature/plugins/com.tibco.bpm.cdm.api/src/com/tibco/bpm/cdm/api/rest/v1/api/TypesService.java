/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


/**
 * @GENERATED this is generated code; do not edit.
 */
@Path("/types")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface TypesService
{
	/**
     * Gets type information\n
     * <p>
     * Types are sorted by applicationId, then applicationMajorVersion, then namespace, then type name.\n
     * @param filter Limits the response to types that match given constraints.\n\nAny combination of:\n  * isCase eq {TRUE or FALSE}\n  * namespace eq {namespace>}\n  * applicationId eq '{applicationId}'\n  * applicationMajorVersion eq '{number}'\n  \n
     * @param select Determines which aspect(s) of types are returned in the response.\n\nOptions are...\n  * basic (or b) - basic properties\n  * attributes (or a)\n  * summaryAttributes (or sa)\n  * states (or s)\n  * links (or l)\n  * dependencies (or d)\n
     * @param skip The starting position in the results list from which to return items. \n
     * @param top Limits the number of items returned.\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@GET
	
	
	
	public Response typesGet(@QueryParam("$filter") String filter,
	@QueryParam("$select") String select,
	@QueryParam("$skip") Integer skip,
	@QueryParam("$top") Integer top) throws Exception;

}

