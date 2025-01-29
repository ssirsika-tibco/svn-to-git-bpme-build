package com.tibco.bpm.cdm.api.exception;

/**
 * A special kind of PersistenceException relating to failures resulting from operations
 * that may succeed if attempted again.
 *
 * <p/>&copy;2016 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class TransientPersistenceException extends PersistenceException
{
	private static final long serialVersionUID = 1L;

	private TransientPersistenceException(CDMErrorData errorData)
	{
		super(errorData);
	}

	TransientPersistenceException(CDMErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	private TransientPersistenceException(CDMErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	private TransientPersistenceException(CDMErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	@Override
	public boolean isRetryable()
	{
		return true;
	}

	public static TransientPersistenceException newNotConnected()
	{
		TransientPersistenceException e = new TransientPersistenceException(
				CDMErrorData.CDM_PERSISTENCE_NOT_CONNECTED);
		return e;
	}

	public static TransientPersistenceException newNotConnected(Throwable cause)
	{
		TransientPersistenceException e = new TransientPersistenceException(CDMErrorData.CDM_PERSISTENCE_NOT_CONNECTED,
				cause);
		return e;
	}
}
