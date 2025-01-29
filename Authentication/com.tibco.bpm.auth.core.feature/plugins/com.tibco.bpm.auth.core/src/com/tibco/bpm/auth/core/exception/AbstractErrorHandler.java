/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2015 TIBCO Software Inc
*/
package com.tibco.bpm.auth.core.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.bpm.auth.rest.v1.model.ContextAttribute;
import com.tibco.bpm.auth.rest.v1.model.Error;
import com.tibco.bpm.auth.exception.AuthBaseException;
import com.tibco.bpm.auth.exception.RESTException;

/**
 * Helper class that deals with the correct error message format
 * 
 */
public abstract class AbstractErrorHandler
{
	/**
	 * Create the REST response object for an error
	 *
	 * @param status		HTTP Status to return
	 * @param code			Error code from "ErrorCodes" to highlight the given error
	 * @param textMsg		Free text version of what the error is
	 * @param attributes	Any appropriate information to do with the error
	 * @return
	 */
	protected Response restError(int status, String code, String textMsg, Map<String, String> attributes)
	{
		Error error = new Error();

		error.setErrorMsg(textMsg);
		error.setErrorCode(code);
		error.setStackTrace(null);

		if (attributes != null)
		{
			error.setContextAttributes(convert(attributes));
		}

		return Response.status(status).entity(error).type("application/json").build();
	}

	/**
	 * Create the REST response object for an error
	 *
	 * @param status		HTTP Status to return
	 * @param code			Error code from "ErrorCodes" to highlight the given error
	 * @param textMsg		Free text version of what the error is
	 * @param attributes	Any appropriate information to do with the error
	 * @return
	 */
	protected Response restError(int status, int code, String textMsg, Map<String, String> attributes)
	{
		return restError(status, Long.toString(code), textMsg, attributes);
	}

	/**
	 * Create the REST response object for an error
	 *
	 * @param status		HTTP Status to return
	 * @param code			Error code from WRPMessages to highlight the given error
	 * @param throwable		Exception error to get the details from
	 * @param attributes	Any appropriate information to do with the error
	 * @return
	 */
	protected Response restError(int status, ErrorCode code, String textMsg, Throwable throwable,
			Map<String, String> attributes)
	{
		Error error = new Error();

		error.setErrorCode(code.name());
		error.setErrorMsg(textMsg);

		if (throwable != null)
		{
			if ((error.getErrorMsg() == null) || error.getErrorMsg().isEmpty())
			{
				String exceptionMsg = throwable.getMessage();

				// If we do not have an error message, use the first line of the stack trace,
				// not ideal, but better than nothing as the stack trace may not be set to
				// return over the interface
				if (exceptionMsg == null || exceptionMsg.isEmpty())
				{
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					throwable.printStackTrace(pw);
					String exceptionMessage = sw.toString();
					int endOfFirstLine = exceptionMessage.indexOf('\n');
					// Handle windows end of line characters as well
					int windowsEndIfLine = exceptionMessage.indexOf('\r');
					if ((windowsEndIfLine > 0) && (windowsEndIfLine < endOfFirstLine))
					{
						endOfFirstLine = windowsEndIfLine;
					}
					if ((endOfFirstLine > 0) && (exceptionMessage.length() > endOfFirstLine))
					{
						exceptionMsg = exceptionMessage.substring(0, endOfFirstLine);
					}
				}
				error.setErrorMsg(exceptionMsg);
			}
		}

		if (attributes != null)
		{
			error.setContextAttributes(convert(attributes));
		}

		return Response.status(status).entity(error).type("application/json").build();
	}

	/**
	 * Converts an exception into a rest response
	 *
	 * @param throwable
	 * @return
	 */
	protected Response restError(Throwable throwable)
	{
		RESTException restException = restException(throwable);
		return restError(restException.getStatus(), restException.getCode(), restException.getMessage(),
				restException.getThrowable(), restException.getAttributes());
	}

	/**
	 * Wraps a normal exception in a RESTException
	 *
	 * @param throwable		Any exception
	 * @return
	 */
	protected RESTException restException(Throwable throwable)
	{
		// Check if this is already a RESTException, if so we can just return that, no need to wrap it up
		if ((throwable != null) && (throwable instanceof RESTException))
		{
			return (RESTException) (throwable);
		}
		else if ((throwable != null) && (throwable instanceof AuthBaseException))
		{
		    AuthBaseException de = (AuthBaseException) throwable;

			return new RESTException(de.getCode().getHttpStatus(), de.getCode(), de.getMessage(), throwable,
					de.getAttributes());
		}
		else
		{
			return new RESTException(Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorCode.AUTH_UNKNOWN_ERROR,
					throwable, null);
		}
	}

	/**
	 * Takes a map of name, value pairs and converts them to a list of error attributes
	 * 
	 * @param attributes	Name Value Pairs
	 * @return	Error attributes
	 */
	protected List<ContextAttribute> convert(Map<String, String> attributes)
	{
		List<ContextAttribute> errAttribs = new ArrayList<ContextAttribute>();
		for (Entry<String, String> attrib : attributes.entrySet())
		{
			ContextAttribute errAttrib = new ContextAttribute();
			errAttrib.setName(attrib.getKey());
			errAttrib.setValue(attrib.getValue());
			errAttribs.add(errAttrib);
		}
		return errAttribs;
	}

	/**
	 * Looks at a given class to generate the value returned by each getter
	 * 
	 * @param obj	The class to check the getters for
	 * @return	Map of names to 
	 */
	protected Map<String, String> getAttributeMap(Object obj)
	{
		Map<String, String> result = new HashMap<String, String>();
		for (Method m : obj.getClass().getMethods())
		{
			if (m.getName().startsWith("get") && !m.getName().startsWith("getClass"))
			{
				try
				{
					String name = m.getName().substring(3);
					if (!name.isEmpty())
					{
						String lowerName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
						Object value = m.invoke(obj);
						if (value != null)
						{
							// Make sure we don't have any return characters in the value
							String strVal = value.toString();
							if (value instanceof String)
							{
								strVal = (String) value;
							}
							else
							{
								// For objects created by the REST interface it will have "class ClassName" littered
								// all over it, so remove those
								strVal = getAttributeDetails(value);
							}
							result.put(lowerName, strVal.trim());
						}
					}
				}
				catch (Exception e)
				{
					// Skip any types that can't be retrieved
				}
			}
		}

		return result;
	}

	/**
	 * Converts an object is to display string for returning in an error message
	 * 
	 * @param value		Object to convert to a string
	 * @return
	 */
	protected String getAttributeDetails(Object value)
	{
		String strVal = value.toString().replaceAll("\n", "");

		// For objects created by the REST interface it will have "class ClassName" littered
		// all over it, so remove those
		Object baseObject = value;
		if (value instanceof List)
		{
			baseObject = null;
			// For lists, we need the class name inside the list rather than the actual list type
			if (!((List< ? >) value).isEmpty())
			{
				baseObject = ((List< ? >) value).get(0);
			}
		}
		if (baseObject != null)
		{
			// Get the name of the class that has been added as a string to the text
			String className = baseObject.getClass().getName();
			int lastPart = className.lastIndexOf(".") + 1;
			if (lastPart < className.length())
			{
				className = className.substring(lastPart);
				strVal = strVal.replaceAll("class " + className, "");
			}
		}

		return strVal;
	}
}
