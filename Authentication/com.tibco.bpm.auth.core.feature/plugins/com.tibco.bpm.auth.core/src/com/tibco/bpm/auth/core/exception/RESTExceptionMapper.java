package com.tibco.bpm.auth.core.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Class to be invoked when an exception is about to be thrown over the REST interface.
 * 
 * This class will automatically be called at the point when an exception would be thrown from
 * within the code to the boundary to the REST layer.  This makes sure that the Java exceptions
 * are all converted to REST messages 
 * 
 */
public class RESTExceptionMapper extends AbstractErrorHandler implements ExceptionMapper<Exception>
{
	/**
	 * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
	 *
	 * @param exception
	 * @return
	 */
	public Response toResponse(Exception exception)
	{
		return restError(exception);
	}
}