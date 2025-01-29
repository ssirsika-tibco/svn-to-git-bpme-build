package com.tibco.bpm.auth.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/authenticate")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface AuthenticationRestService {

	@GET
	public Response authenticate();
}
