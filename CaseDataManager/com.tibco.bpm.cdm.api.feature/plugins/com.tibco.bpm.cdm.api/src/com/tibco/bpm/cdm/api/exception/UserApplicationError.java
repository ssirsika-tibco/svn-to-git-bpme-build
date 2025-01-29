package com.tibco.bpm.cdm.api.exception;

/**
 * Parent class for ArgumentException and ValidationException (but
 * not InternalException and others)
 * 20200514: Reinstating the class name UserApplicationError. See ACE-3559 for more details.
 * @author smorgan
 * @since 2019
 */
public class UserApplicationError extends CDMException
{
	private static final long serialVersionUID = 3099025454826042176L;

	public UserApplicationError(ErrorData errorData)
	{
		super(errorData);
	}

	public UserApplicationError(ErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	public UserApplicationError(ErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	public UserApplicationError(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}
}
