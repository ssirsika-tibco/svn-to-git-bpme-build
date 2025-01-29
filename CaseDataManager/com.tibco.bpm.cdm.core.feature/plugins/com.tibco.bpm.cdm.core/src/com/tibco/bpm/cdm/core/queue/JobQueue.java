package com.tibco.bpm.cdm.core.queue;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.msg.pq.PQMessage;

/**
 * CDM's internal job queue
 * @author smorgan
 * @since 2019
 */
public interface JobQueue
{
	public void enqueueJob(Job job, String correlationId, int delay) throws PersistenceException, QueueException;

	public PQMessage getNextJob() throws PersistenceException, QueueException;

	public void purgeJobsWithCorrelationId(String correlationId) throws PersistenceException, QueueException;

	public void blockForSeconds(int seconds);
}