/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.tibco.bpm.cdm.api.rest.v1.model.CasesPostRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRefRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.LinksPostRequestBody;


/**
 * @GENERATED this is generated code; do not edit.
 */
@Path("/cases")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface CasesService
{
	/**
     * Gets one or more cases
     * <p>
     * Gets matching cases. \n$filter must be included (see $filter for usage scenarios).\n$search and $dql must not be used together.\nCases are returned in reverse modificationTimestamp order (most recently modified first).\n
     * @param filter There are two usage scenarios:\n\n1. To obtain a specific set of cases by reference:\n  * caseReference in(ref1, ref2, ...)\n  \n2. To obtain cases of a given type:\n  * caseType eq '{namespace}.{case type name}' and applicationMajorVersion eq {number} and any combination of the following:\n    * cid eq '{value}' to filter by case identifier\n    * caseState eq '{value}' to filter by a given state attribute value\n    * modificationTimestamp le '{value}' to filter to cases last modified at or before the given timestamp, an ISO-8601 expression (which may be partially specified from the left, but must include time-zone)\n
     * @param search A search term to look for in cases' searchable attributes.\n
     * @param dql A Data Query Language query.\n\nOne or more expressions (separated by ' and ') of the form:\n  * {attributeName} = {value}\n  \nValue should be enclosed in single quotes when it is of Text type (escape single quotes with backslash).\n\ne.g.\n  * name = 'Bob' and height = 1.8 and dateOfBirth = 2019-03-18 and timeOfBirth = 14:30\n  * name = 'Henry' and isAdult = true and alias = null\n
     * @param select Determines which aspect(s) of case(s) are returned in the response.\n\nOne or more comma-separated values from the following list:\n  * casedata (or c)\n  * summary (or s)\n  * caseReference (or cr)\n  * metadata (or m)\n
     * @param skip The starting position in the results list from which to return items. \n
     * @param top Limits the number of items returned.\n
     * @param count If specified with value 'true', this API will return a count of all cases in the system. $count may not be combined with any other query parameters (in other words, it will not be possible to obtain a count of cases that match given criteria).\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@GET
	
	
	
	public Response casesGet(@QueryParam("$filter") String filter,
	@QueryParam("$search") String search,
	@QueryParam("$dql") String dql,
	@QueryParam("$select") String select,
	@QueryParam("$skip") Integer skip,
	@QueryParam("$top") Integer top,
	@QueryParam("$count") Boolean count) throws Exception;

	/**
     * Updates one or more cases,
     * <p>
     * Updates the given case(s) with new casedata. All cases must be of the same type. Optimistic locking means that an update will fail if the version number portion of the case reference does not match the currently stored version.  Update of multiple cases will be atomic (i.e. if one fails, all will fail).\n\nCan fail, returning a 400 BAD REQUEST with the follow error code:\n * CDM_REFERENCE_VERSION_MISMATCH - If a case exists, but the version part of the case reference doesn&#39;t match that in the database (implying the case has been changed by another caller)\n \nReturns updated case references. Note :\n  * The version number portion of each case reference will be incremented.\n  * The order of the array will match that in the request.\n
     * @param body 
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@PUT
	
	
	
	public Response casesPut(CasesPutRequestBody body) throws Exception;

	/**
     * Creates one or more cases
     * <p>
     * Creates one or more cases of a given case type\n
     * @param body 
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@POST
	
	
	
	public Response casesPost(CasesPostRequestBody body) throws Exception;

	/**
     * Deletes one or more cases
     * <p>
     * Deletes one or more cases.  Links between the deleted cases and other cases are implicitly removed.\n\nCases are identified by a mandatory set of constraints specified via $filter:\n  - caseType eq &#39;{namespace}.{case type name}&#39; - to identify the case type.\n  - applicationMajorVersion eq {number} - to identify the major version of the application containing the case type.\n  - caseState eq &#39;{value}&#39; - to specify the state that cases must be in\n  - modificationTimestamp le &#39;{timestamp}&#39; - where timestamp is an ISO-8601 expression (which may be partially specified from the left, but must include time-zone) to affect only cases that were last modified on or before the given timestamp.\n  \nIn addition, if the desire is to exclude cases that are referenced by a process, the filter can include:\n  - isReferencedByProcess eq FALSE (the opposite, isReferencedByProcess eq TRUE, will _not_ be implemented as _only_ wanting to affect such cases is unlikely)\n  \nCan fail, returning a 400 BAD REQUEST with the follow error code:\n * CDM_PERMISSIONS_NO_SYSTEM_ACTION - The deletion is prohibited due to the caller not having the appropriate system action permission.            \n
     * @param filter Must include all of:\n  * caseType eq '{namespace}.{case type name}'\n  * applicationMajorVersion eq {number}\n  * caseState eq '{value}'\n  * modificationTimestamp le '{timestamp}'\n  \nCan optionally include:\n  * isReferencedByProcess eq {FALSE}\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@DELETE
	
	
	
	public Response casesDelete(@QueryParam("$filter") String filter) throws Exception;

	/**
     * Gets a single case
     * <p>

     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     * @param select Determines which aspect(s) of case(s) are returned in the response.\n\nOne or more comma-separated values from the following list:\n  * casedata (or c)\n  * summary (or s)\n  * caseReference (or cr)\n  * metadata (or m)\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@GET
	@Path("/{caseReference}")
	
	
	public Response casesCaseReferenceGet(@PathParam("caseReference") String caseReference,
	@QueryParam("$select") String select) throws Exception;

	/**
     * Updates a single case
     * <p>
     * Updates a single case.\n\nCan fail, returning a 400 BAD REQUEST with the follow error code:\n * CDM_CASE_VERSION_MISMATCH - If the case exists, but the version part of the case reference doesn&#39;t match that in the database (implying the case has been changed by another caller)\n \nReturns an updated case reference. Note :\n  * The version number portion of the case reference will be incremented.\n
     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     * @param body Updated casedata\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@PUT
	@Path("/{caseReference}")
	
	
	public Response casesCaseReferencePut(@PathParam("caseReference") String caseReference,
	CasesPutRefRequestBody body) throws Exception;

	/**
     * Deletes a single case
     * <p>
     * Deletes a single case.\n\nCan fail, returning a 400 BAD REQUEST with the follow error code:\n * CDM_CASE_VERSION_MISMATCH - If a case exists, but the version part of the case reference doesn&#39;t match that in the database (implying the case has been changed by another caller)\n * CDM_PERMISSIONS_NO_SYSTEM_ACTION - The deletion is prohibited due to the caller not having the appropriate system action permission.\n
     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@DELETE
	@Path("/{caseReference}")
	
	
	public Response casesCaseReferenceDelete(@PathParam("caseReference") String caseReference) throws Exception;

	/**
     * Gets links from the given case\n
     * <p>
     * Returns all links, unless $filter is specified to limit to a given link name.\nResults can be limited to just those target cases that match given criteria, by specifying a $dql parameter (this is only allowed when $filter contains a name constraint, such that returned cases will be of the same type).
     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     * @param filter May include:\n  * name eq '{name}', to filter to just links for the given link name. e.g. name eq 'addresses'.\n
     * @param dql A Data Query Language query.\n\nOne or more expressions (separated by ' and ') of the form:\n  * {attributeName} = {value}\n  \nValue should be enclosed in single quotes when it is of Text type (escape single quotes with backslash).\n\ne.g.\n  * name = 'Bob' and height = 1.8 and dateOfBirth = 2019-03-18 and timeOfBirth = 14:30\n  * name = 'Henry' and isAdult = true and alias = null\n
     * @param skip The starting position in the results list from which to return items. \n
     * @param top Limits the number of items returned.\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@GET
	@Path("/{caseReference}/links")
	
	
	public Response casesCaseReferenceLinksGet(@PathParam("caseReference") String caseReference,
	@QueryParam("$filter") String filter,
	@QueryParam("$dql") String dql,
	@QueryParam("$skip") Integer skip,
	@QueryParam("$top") Integer top) throws Exception;

	/**
     * Creates new links from the given case\n
     * <p>
     * The body contains an array of new links, each including the link name and target case reference.\nNote that this API is not version-aware, so the version portion of case references is ignored.\n
     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     * @param body 
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@POST
	@Path("/{caseReference}/links")
	
	
	public Response casesCaseReferenceLinksPost(@PathParam("caseReference") String caseReference,
	LinksPostRequestBody body) throws Exception;

	/**
     * Deletes links\n
     * <p>

     * @param caseReference In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel.Order-1-0\n
     * @param filter May include (separated by ' and '):\n  * name eq '{name}', to filter to just links for the given link name. e.g. name eq 'addresses'.\n  * targetCaseReference eq '{targetCaseReference}'\n  * targetCaseReference in('{targetCaseReference1}', '{targetCaseReference2}, ...')\n\nNote that targetCaseReference is only allowed when a name constraint is specified.\n
     *
     * @GENERATED this is generated code; do not edit.
     */
 	@DELETE
	@Path("/{caseReference}/links")
	
	
	public Response casesCaseReferenceLinksDelete(@PathParam("caseReference") String caseReference,
	@QueryParam("$filter") String filter) throws Exception;

}

