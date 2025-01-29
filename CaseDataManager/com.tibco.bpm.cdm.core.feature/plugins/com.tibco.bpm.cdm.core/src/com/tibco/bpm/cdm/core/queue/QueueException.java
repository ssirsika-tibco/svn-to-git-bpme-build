package com.tibco.bpm.cdm.core.queue;

/**
 * Represents a problem relating to CDM's internal job queue
 * @author smorgan
 * @since 2019
 */
public class QueueException extends Exception
{
	private static final long serialVersionUID = 1L;

	public QueueException(Exception cause)
	{
		super("Exception manipulating queue", cause);
	}
}
