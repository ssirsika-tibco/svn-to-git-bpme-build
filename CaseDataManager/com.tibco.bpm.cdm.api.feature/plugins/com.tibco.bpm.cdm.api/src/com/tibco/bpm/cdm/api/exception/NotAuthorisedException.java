package com.tibco.bpm.cdm.api.exception;

public class NotAuthorisedException extends ArgumentException
{
	private static final long serialVersionUID = 7517804806965433394L;

	public NotAuthorisedException(ErrorData errorData)
	{
		super(errorData);
	}

	public static NotAuthorisedException newNotAuthorisedException(String method) throws NotAuthorisedException
	{

		return new NotAuthorisedException(CDMErrorData.CDM_API_FORBIDDEN);
	}
}
