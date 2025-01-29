package com.tibco.bpm.cdm.core.queue;

import java.math.BigInteger;

/**
 * Represents a problem relating to CDM's internal job queue
 * @author smorgan
 * @since 2019
 */
public class JobProcessingException extends Exception
{
	private static final long serialVersionUID = -5339386434018972073L;

	private JobProcessingException(String message)
	{
		super(message);
	}

	private JobProcessingException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public static JobProcessingException newFailure(Throwable cause)
	{
		return new JobProcessingException("Job processing failed: " + cause.getMessage(), cause);
	}

	public static JobProcessingException newUnknownMethod(String method)
	{
		return new JobProcessingException("Unknown method: " + method);
	}

	public static JobProcessingException newBadApplicationId(String id)
	{
		// TODO major version
		return new JobProcessingException("Bad application id: " + id);
	}

	public static JobProcessingException newBadTypeId(BigInteger id)
	{
		return new JobProcessingException("Bad type id: " + id);
	}
}