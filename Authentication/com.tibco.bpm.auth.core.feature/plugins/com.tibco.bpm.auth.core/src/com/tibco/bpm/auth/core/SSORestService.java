/**
 * 
 */
package com.tibco.bpm.auth.core;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
/**
 * Common REST service which will redirect to specific SSO (SAML or Open ID) endpoint, depending on what is configured.  
 * @author ssirsika
 *
 */
@Path("/sso")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface SSORestService {

	/**
	 * @return Redirect response depending upon the configured SSO resource instance
	 */
	@GET
	public Response redirect() throws Exception;
}
