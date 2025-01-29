package com.tibco.bpm.cdm.api.exception;

import java.sql.SQLRecoverableException;

/**
 * Indicates that a problem occurred when reading or writing a data store.
 *
 * @author smorgan
 * @since 2019
 */
public class PersistenceException extends InternalException
{
	private static final long serialVersionUID = 1L;

	protected PersistenceException(CDMErrorData errorData)
	{
		super(errorData);
	}

	protected PersistenceException(CDMErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	protected PersistenceException(CDMErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	protected PersistenceException(CDMErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	/**
	 * SQLRecoverableExceptions become TransientPersistenceException (retryable) and
	 * any other causes become a plain PersistenceException (not retryable)
	 *
	 * @param cause
	 * @return
	 */
	public static PersistenceException newRepositoryProblem(Throwable cause)
	{
		PersistenceException e;
		if (cause instanceof SQLRecoverableException)
		{
			e = new TransientPersistenceException(CDMErrorData.CDM_PERSISTENCE_TRANSIENT_REPOSITORY_ERROR, cause);
		}
		else
		{
			e = new PersistenceException(CDMErrorData.CDM_PERSISTENCE_REPOSITORY_ERROR, cause);
		}
		return e;
	}
}
