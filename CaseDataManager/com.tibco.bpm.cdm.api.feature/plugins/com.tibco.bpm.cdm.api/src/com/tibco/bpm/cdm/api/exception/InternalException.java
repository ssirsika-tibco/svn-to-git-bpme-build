package com.tibco.bpm.cdm.api.exception;

/**
 * An InternalException can be used to wrap any unexpected error (including the type "that should never happen").
 * Such errors will subsequently be considered status 500 in REST.
 *
 * @author smorgan
 * @since 2019
 */
public class InternalException extends CDMException
{
	private static final long	serialVersionUID	= -1942260801385724811L;

	private static final String	DEFAULT_MESSAGE		= "Internal Exception";

	protected InternalException(CDMErrorData errorData)
	{
		super(errorData);
	}

	protected InternalException(CDMErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	protected InternalException(CDMErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	protected InternalException(CDMErrorData errorData, String message)
	{
		super(errorData, new String[]{"message", message});
	}

	protected InternalException(CDMErrorData errorData, Throwable cause, String message)
	{
		super(errorData, cause, new String[]{"message", message});
	}

	protected InternalException(CDMErrorData errorData, Throwable cause, String[] params)
	{

		super(errorData, cause, params);

	}

	public static InternalException newInternalException(Throwable cause)
	{
		InternalException e = new InternalException(CDMErrorData.CDM_INTERNAL, cause, DEFAULT_MESSAGE);
		return e;
	}

	public static InternalException newInternalException(Throwable cause, String message)
	{
		InternalException e = new InternalException(CDMErrorData.CDM_INTERNAL, cause, message);
		return e;
	}

	public static InternalException newInternalException(String message)
	{
		InternalException e = new InternalException(CDMErrorData.CDM_INTERNAL, message);
		return e;
	}
}
