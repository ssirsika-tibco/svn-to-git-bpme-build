package com.tibco.bpm.auth.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.bpm.auth.exception.UnauthorizedUserException;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class BPMContainerFilter extends SecurityFilter implements ContainerRequestFilter, ContainerResponseFilter {

	static CLFClassContext logCtx = CloudLoggingFramework.init(BPMContainerFilter.class, AuthLoggingInfo.instance);
	
	@Context
	private HttpServletRequest httpServletRequest;

	@Context
	private HttpServletResponse httpServletResponse;

	@Override
	public void filter(ContainerRequestContext reqContext, ContainerResponseContext respContext) throws IOException {
		// nothing to do here
	}

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		CLFMethodContext clf = logCtx.getMethodContext("filter");
		clf.local.debug(" intercepted the CXF request context " + context);
		try {
			if (getSecurityService().authenticate(httpServletRequest, httpServletResponse, exclusions)) {
				// authentication sucess so dont do anything
				clf.local.debug("authentication successful ");
			} else {
				sendForbiddenResponse(context, (Integer) httpServletRequest.getAttribute("X-BPM-AUTH-MODE"));
			}

		} catch (UnauthorizedUserException e) {
			// If user fails to login with SSO, then SSO code will throw UnauthorizedUserException.
			sendForbiddenSSOResponse(context, e, (Integer) httpServletRequest.getAttribute("X-BPM-AUTH-MODE"));
		} catch (Throwable t) {
			AuthLogger.error(t.getMessage(), t);
			sendForbiddenResponse(context, t);
		}

	}

	private void sendForbiddenResponse(ContainerRequestContext context, Throwable t) throws IOException {
		sendForbiddenResponse(context, t, null);
	}

	private void sendForbiddenResponse(ContainerRequestContext context, Integer authMode) throws IOException {
		sendForbiddenResponse(context, null, authMode);
	}

	private void sendForbiddenResponse(ContainerRequestContext reqContext, Throwable e, Integer authMode)
			throws IOException {
		UnauthorizedUserException exception = new UnauthorizedUserException(ErrorCode.AUTH_AUTHERROR, e, null);
		ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN).entity(exception.toResponseMsg());
		builder = (authMode != null) ? builder.header("X-BPM-AUTH-MODE", authMode) : builder;
		reqContext.abortWith(builder.build());
	}

	/**
	 *  Send the response with error code specific to SSO authentication 
	 * @param reqContext {@link ContainerRequestContext} 
	 * @param exception {@link UnauthorizedUserException} specific to SSO
	 * @param authMode authentication mode
	 * @throws IOException
	 */
	private void sendForbiddenSSOResponse(ContainerRequestContext reqContext, UnauthorizedUserException exception,
			Integer authMode) throws IOException {
		ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN).entity(exception.toResponseMsg());
		builder = (authMode != null) ? builder.header("X-BPM-AUTH-MODE", authMode) : builder;
		reqContext.abortWith(builder.build());
	}
	
	public boolean isSecurityEnabled() {
		// check if the property is set
		String isAuthenitcate = System.getProperty("com.tibco.authenticate");
		if (null == isAuthenitcate) {
			// if the property is not set by default it is disabled
			return false;
		} else {
			// if the property is set check if the value is false
			if (isAuthenitcate.equalsIgnoreCase("false")) {
				return false;
			} else {
				return true;
			}
		}
	}

}
