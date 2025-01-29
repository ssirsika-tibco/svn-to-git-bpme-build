package com.tibco.bpm.cdm.core.rest.v1;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.CDMErrorData;
import com.tibco.bpm.cdm.api.rest.v1.model.ContextAttribute;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Intercepts Exceptions occurring during or prior to invocation of API implementations and
 * ensures that they are converted to appropriate Responses with a body containing
 * an Error object.
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception>
{
	static CLFClassContext logCtx = CloudLoggingFramework.init(ExceptionMapper.class, CDMLoggingInfo.instance);

	@Override
	public Response toResponse(Exception e)
	{
		CLFMethodContext clf = logCtx.getMethodContext("toResponse");
		Response response = null;

		com.tibco.bpm.cdm.api.rest.v1.model.Error err = new com.tibco.bpm.cdm.api.rest.v1.model.Error();
		int status = 500;
		if (e instanceof JsonProcessingException)
		{
			// Jackson error occurred due to bad JSON in request.
			// We don't create an exception here, but still make use of ErrorData in
			// order to centralise _all_ error messages in that class.
			CDMErrorData ed = CDMErrorData.CDM_REST_BADJSON;
			err.setErrorCode(ed.getCode());
			err.setErrorMsg(ed.getMessageTemplate());
			status = ed.getHTTPStatus();
		}
		else if (e instanceof CDMException)
		{
			// CDMBaseException is the base of CDM exceptions contains the required ingredients to construct an Error object
			CDMException be = (CDMException) e;
			err.setErrorCode(be.getErrorData().getCode());
			err.setErrorMsg(be.getMessage());
			for (Entry<String, String> entry : be.getAttributes().entrySet())
			{
				ContextAttribute ca = new ContextAttribute();
				ca.setName(entry.getKey());
				ca.setValue(entry.getValue());
				err.getContextAttributes().add(ca);
			}

			status = be.getErrorData().getHTTPStatus();
		}
		else if (e instanceof java.lang.IllegalArgumentException)
		{
			CDMErrorData ed = CDMErrorData.CDM_REST_ILLEGAL_ARGUMENT_EXCEPTION;
			err.setErrorCode(ed.getCode());
			err.setErrorMsg(ed.getMessageTemplate());
			status = ed.getHTTPStatus();
		}
		else if (e instanceof java.lang.RuntimeException && null != e.getMessage()
				&& e.getMessage().contains("Invalid URL encoding"))
		{
			CDMErrorData ed = CDMErrorData.CDM_REST_INVALID_URL_ENCODING;
			err.setErrorCode(ed.getCode());
			err.setErrorMsg(ed.getMessageTemplate());
			status = ed.getHTTPStatus();
		}
		else
		{
			// If this ever happens, it suggests a bug as something has failed to be mapped
			CDMErrorData ed = CDMErrorData.CDM_REST_INTERNAL;
			err.setErrorCode(ed.getCode());
			err.setErrorMsg(ed.getMessageTemplate());
			status = ed.getHTTPStatus();
			// Put a String representation of the Exception as a context
			// attribute of the Error response.
			ContextAttribute ca = new ContextAttribute();
			ca.setName("message");
			String exceptionString = e.toString();
			ca.setValue(exceptionString);
			err.getContextAttributes().add(ca);
			// As well a returning the error to the caller, debug log it too
			clf.local.debug(e, "Internal error: %s", exceptionString);
		}
		
		err.setStackTrace(writeStackTraceToString(e));
		
		// Generate a response with the appropriate status code with the error payload
		response = Response.status(status).entity(err).type(MediaType.APPLICATION_JSON).build();
		return response;
	}

	private String writeStackTraceToString(Throwable t)
	{
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
