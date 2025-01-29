/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2015 TIBCO Software Inc
*/
package com.tibco.bpm.auth.exception;

import java.util.Map;
import java.util.Map.Entry;

import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;


/**
 * Exception used to handle REST errors
 *
 */
public class RESTException extends Exception
{
	private static final long	serialVersionUID	= 1L;

	// Free text version of what the error is
	private String				message;

	// HTTP Status to return the error as
	private int					status;

	// Error code from WRPMessages to highlight the given error
	private ErrorCode			code;

	// Exception error to get the details from
	private Throwable			throwable;

	// Any appropriate information to do with the error
	private Map<String, String>	attributes;

	/**
	 * Creates an error exception
	 *
	 * @param status		HTTP Status to return
	 * @param code			Error code from "ErrorCodes" to highlight the given error
	 * @param message		Free text version of what the error is
	 * @param attributes	Any appropriate information to do with the error
	 */
	public RESTException(int status, ErrorCode code, String message, Map<String, String> attributes)
	{
		setStatus(status);
		setCode(code);
		setMessage(message);
		setAttributes(attributes);
	}

	/**
	 * Creates an error exception
	 *
	 * @param status		HTTP Status to return
	 * @param code			Error code from SSMessages to highlight the given error
	 * @param throwable		Exception error to get the details from
	 * @param attributes	Any appropriate information to do with the error
	 */
	public RESTException(int status, ErrorCode code, Throwable throwable, Map<String, String> attributes)
	{
		setStatus(status);
		setCode(code);
		setMessage(throwable.getMessage());
		setThrowable(throwable);
		setAttributes(attributes);
	}

	/**
	 * Creates an error exception
	 * 
	 * @param status		HTTP Status to return
	 * @param code			Error code from SSMessages to highlight the given error
	 * @param message		Free text version of what the error is
	 * @param throwable		Exception error to get the details from
	 * @param attributes	Any appropriate information to do with the error
	 */
	public RESTException(int status, ErrorCode code, String message, Throwable throwable,
			Map<String, String> attributes)
	{
		setStatus(status);
		setCode(code);
		setMessage(message);
		setThrowable(throwable);
		setAttributes(attributes);
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public ErrorCode getCode()
	{
		return code;
	}

	public void setCode(ErrorCode code)
	{
		this.code = code;
	}

	public Throwable getThrowable()
	{
		return throwable;
	}

	public void setThrowable(Throwable throwable)
	{
		this.throwable = throwable;
	}

	public Map<String, String> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes)
	{
		this.attributes = attributes;
	}

	public String getAttributesAsString()
	{
		StringBuilder str = new StringBuilder();

		for (Entry<String, String> attrib : attributes.entrySet())
		{
			if (str.length() > 0)
			{
				str.append(", ");
			}
			str.append(attrib.getKey());
			str.append('=');
			str.append(attrib.getValue());
		}
		return str.toString();
	}

	public String toString()
	{
		return "RESTException [status=" + status + ", code=" + code + ", throwable=" + throwable + ", attributes="
				+ getAttributesAsString() + "]";
	}
}
