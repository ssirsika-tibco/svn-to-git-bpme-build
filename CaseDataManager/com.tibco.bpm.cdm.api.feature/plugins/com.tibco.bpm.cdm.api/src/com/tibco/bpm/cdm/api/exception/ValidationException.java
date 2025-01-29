package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates a validation error (applies to both case and non-case data)
 * 
 * @author smorgan
 * @since 2019
 */
public class ValidationException extends UserApplicationError
{
	private static final long serialVersionUID = 4864304185899893815L;

	public ValidationException(ErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	public ValidationException(ErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	public ValidationException(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	public ValidationException(ErrorData errorData)
	{
		super(errorData);
	}

	public static ValidationException newNotJSON(Throwable cause)
	{
		return new ValidationException(CDMErrorData.CDM_DATA_NOT_JSON, cause,
				new String[]{"message", cause.getMessage()});
	}

	public static ValidationException newNotJSONObject()
	{
		return new ValidationException(CDMErrorData.CDM_DATA_NOT_JSON_OBJECT);//V
	}

	public static ValidationException newInvalid(String details)
	{
		return new ValidationException(CDMErrorData.CDM_DATA_INVALID, new String[]{"details", details});//V
	}
}
