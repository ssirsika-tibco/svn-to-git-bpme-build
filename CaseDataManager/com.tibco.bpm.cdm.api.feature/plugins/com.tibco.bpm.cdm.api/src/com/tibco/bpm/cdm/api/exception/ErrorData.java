package com.tibco.bpm.cdm.api.exception;

/**
 *
 * Encapsulates data for a particular type of error, comprising error code, default message and a hint of which
 * status code the error would map to if occurring in an HTTP context.  Combined with an exception and (where appropriate)
 * context attributes, this gives all the ingredients required for later construction of an error payload, as per the requirements
 * described at http://confluence.tibco.com/display/BPM/Ariel+REST+Service+Guidelines#ArielRESTServiceGuidelines-Errorpayloaddefinition,
 * but has no dependency on the Swagger-generated Error class, so is not bound to a particular version of the CM API (v1, v2 etc),
 * or indeed invocation via REST at all.   
 * 
 * The message may contain {tokens}, where
 * 'tokens' corresponds to the name of a context attribute, allowing context attribute values to be baked into messages,
 * as per the description at http://confluence.tibco.com/display/BPM/Ariel+Client+Error+Reporting
 * ("The errorMsg property should be specified in English, with all parameterised content already embedded in the message.")
 *
 * @author smorgan
 * @since 2019
 */
public class ErrorData implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID 			= 1L;

	public static final int	OK_200						= 200;

	public static final int	BAD_REQUEST_400				= 400;

	public static final int	FORBIDDEN_403				= 403;

	public static final int	NOT_FOUND_404				= 404;

	public static final int	INTERNAL_SERVER_ERROR_500	= 500;

	private String			code;

	private String			messageTemplate;

	// Consider this a hint of what status this would map to IF it were to be returned in response
	// to an HTTP request. Although that may appear to make this class HTTP-specific, it was a choice
	// of that or having a mapping of error code to http status elsewhere, which would be harder to maintain.
	// In a non-HTTP context, this would simply be ignored.
	private int				httpStatus;

	public ErrorData(String code, String messageTemplate, int httpStatus)
	{
		this.code = code;
		this.messageTemplate = messageTemplate;
		this.httpStatus = httpStatus;
	}

	public String getCode()
	{
		return code;
	}

	public String getMessageTemplate()
	{
		return messageTemplate;
	}

	public int getHTTPStatus()
	{
		return httpStatus;
	}
}
