/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2020 TIBCO Software Inc
*/
package com.tibco.bpm.auth.exception;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.text.StringSubstitutor;
import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;

/**
 * Exception used to handle Auth errors. *
 *
 * @author sajain
 * @since Apr 16, 2020
 */
public class AuthBaseException extends Exception
{
	private static final long	serialVersionUID	= 1L;

	private String				message;

	// Error code from WRPMessages to highlight the given error
	private ErrorCode			code;

	// Exception error to get the details from
	private Throwable			throwable;

	// Any appropriate information to do with the error
	private Map<String, String>	attributes;

	private static Properties	prop;

	static
	{
		InputStream is = null;
		try
		{
			prop = new Properties();
			is = AuthBaseException.class.getResourceAsStream("/auth_messages.properties");

			if (is == null)
			{
				is = AuthBaseException.class.getResourceAsStream("/resources/auth_messages.properties");
			}

			prop.load(is);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected String buildMessage(ErrorCode code, Map<String, String> attributes)
	{
		String message = prop.getProperty(code.name());

		if (message != null && attributes != null)
		{
			StringSubstitutor sub = new StringSubstitutor(attributes, "{", "}");
			String resolvedString = sub.replace(message);
			message = resolvedString;
		}

		if (message == null)
		{
			message = "Message not found for code [" + code + "]";
		}

		return (message);
	}

	/**
	 * Creates an error exception
	 *
	 * @param code			Error code from "ErrorCodes" to highlight the given error
	 * @param attributes	Any appropriate information to do with the error
	 */
	public AuthBaseException(ErrorCode code, Map<String, String> attributes)
	{
		setCode(code);
		setMessage(buildMessage(code, attributes));
		setAttributes(attributes);
	}

	/**
	 * Creates an error exception
	 *
	 * @param code			Error code from "WrpMessages" to highlight the given error
	 * @param throwable		Exception error to get the details from
	 * @param attributes	Any appropriate information to do with the error
	 */
	public AuthBaseException(ErrorCode code, Throwable throwable, Map<String, String> attributes)
	{
		setCode(code);
		setMessage(buildMessage(code, attributes) + ((throwable != null && throwable.getMessage() != null)
				? (" " + throwable.getClass().getName() + " " + throwable.getMessage())
				: ""));
		setThrowable(throwable);
		setAttributes(attributes);
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

	protected void setAttributes(Map<String, String> attributes)
	{
		this.attributes = attributes;
	}

	@Override
    public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	@Override
    public String toString()
	{
		return "DeploymentException [code=" + code + ", throwable=" + throwable + "]";
	}
	
	public String toResponseMsg()
    {
        return "{\"errorMsg\": \"" + getMessage() + "\", \"errorCode\": \"" + getCode() + "\"}";
    }
}
