package com.tibco.bpm.auth.core;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/logout")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface LogoutRestService {
	@GET
	public Response logout() throws Exception;
}
