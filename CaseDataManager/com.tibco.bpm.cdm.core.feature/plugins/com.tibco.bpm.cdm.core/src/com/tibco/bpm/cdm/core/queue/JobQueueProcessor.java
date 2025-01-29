package com.tibco.bpm.cdm.core.queue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.msg.aq.DqdMessage;
import com.tibco.bpm.msg.pq.PQMessage;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * Processes jobs taken from CDM's internal job queue.
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class JobQueueProcessor
{
	static CLFClassContext						logCtx	= CloudLoggingFramework.init(JobQueueProcessor.class,
			CDMLoggingInfo.instance);

	private static final ObjectMapper			om		= new ObjectMapper();

	// The queue from which we get jobs to process
	private JobQueue							jobQueue;

	private AutoPurgeJobPerformer				autoPurgeJobPerformer;

	private AutoPurgeApplicationJobPerformer	autoPurgeApplicationJobPerformer;

	private AutoPurgeCaseTypeJobPerformer		autoPurgeCaseTypeJobPerformer;

	private DeleteCasesJobPerformer				deleteCasesJobPerformer;

	// Called by Spring
	public void setJobQueue(JobQueue jobQueue)
	{
		this.jobQueue = jobQueue;
	}

	// Called by Spring
	public void setAutoPurgeJobPerformer(AutoPurgeJobPerformer autoPurgeJobPerformer)
	{
		this.autoPurgeJobPerformer = autoPurgeJobPerformer;
	}

	// Called by Spring
	public void setAutoPurgeSandboxJobPerformer(AutoPurgeCaseTypeJobPerformer autoPurgeCaseTypeJobPerformer)
	{
		this.autoPurgeCaseTypeJobPerformer = autoPurgeCaseTypeJobPerformer;
	}

	// Called by Spring
	public void setAutoPurgeApplicationJobPerformer(AutoPurgeApplicationJobPerformer autoPurgeApplicationJobPerformer)
	{
		this.autoPurgeApplicationJobPerformer = autoPurgeApplicationJobPerformer;
	}

	// Called by Spring
	public void setAutoPurgeCaseTypeJobPerformer(AutoPurgeCaseTypeJobPerformer autoPurgeCaseTypeJobPerformer)
	{
		this.autoPurgeCaseTypeJobPerformer = autoPurgeCaseTypeJobPerformer;
	}

	// Called by Spring
	public void setDeleteCasesJobPerformer(DeleteCasesJobPerformer deleteCasesJobPerformer)
	{
		this.deleteCasesJobPerformer = deleteCasesJobPerformer;
	}

	/**
	 * Called by {@link JobQueueProcessingThread}
	 *
	 * @throws PersistenceException
	 * @throws QueueException
	 * @throws JobProcessingException
	 */
	public int getNextJobAndProcessIt() throws PersistenceException, QueueException, JobProcessingException
	{
		// Get next available message (if there is one), process it and return wait time
		int waitPeriod = 0;
		DqdMessage msg = null;

		PQMessage jobPQMessage = jobQueue.getNextJob();

		msg = jobPQMessage.getMessage();

		if (null == msg)
		{
			waitPeriod = jobPQMessage.getWaitPeriod();
		}
		else
		{
			String correlationId = msg.getCorrelationData();
			purgeJobsWithCorrelationIdTX(correlationId);

			String jobJSON = new String(msg.getMsg(), StandardCharsets.UTF_8);
			Job job;
			try
			{
				job = om.readValue(jobJSON, Job.class);
			}
			catch (IOException e)
			{
				throw new QueueException(e);
			}

			// Process the job
			processJob(job);
		}

		return (waitPeriod);
	}

	/**
	 * Called by {@link JobQueueProcessingThread}
	 */
	public void blockForSeconds(int waitPeriod)
	{
		jobQueue.blockForSeconds(waitPeriod); // typically up to 180 seconds
	}

	public void purgeJobsWithCorrelationIdTX(String correlationId) throws PersistenceException, QueueException
	{
		jobQueue.purgeJobsWithCorrelationId(correlationId);
	}

	/**
	 * Throws an exception if the job is syntactically invalid
	 *
	 * @param job
	 * @throws JobProcessingException
	 */
	private void validateJob(Job job) throws JobProcessingException
	{
		// Check the method is recognised
		if (!job.isMethodValid())
		{
			throw JobProcessingException.newUnknownMethod(job.getMethod());
		}

		String method = job.getMethod();

		// Perform method-specific validation
		if (method.equals(Job.METHOD_DELETE_CASES))
		{
			// Nothing to check
		}
		else if (method.equals(Job.METHOD_AUTO_PURGE))
		{
			// Nothing to check
		}
		else if (method.equals(Job.METHOD_AUTO_PURGE_CASE_TYPE))
		{
			// Nothing to check
		}
		else if (method.equals(Job.METHOD_AUTO_PURGE_APPLICATION))
		{
			// Nothing to check
		}
	}

	public void processJob(Job job) throws JobProcessingException
	{
		CLFMethodContext clf = logCtx.getMethodContext("processJob");
		clf.local.debug("%s", "Processing job: " + job);
		long startTime = System.nanoTime();
		try
		{
			// After this call, we can assume the job and its content are valid
			validateJob(job);

			String method = job.getMethod();

			// Handle the method appropriately, based on the method
			JobPerformer performer = null;
			if (method.equals(Job.METHOD_DELETE_CASES))
			{
				performer = deleteCasesJobPerformer;
			}
			else if (method.equals(Job.METHOD_AUTO_PURGE))
			{
				performer = autoPurgeJobPerformer;
			}
			else if (method.equals(Job.METHOD_AUTO_PURGE_CASE_TYPE))
			{
				performer = autoPurgeCaseTypeJobPerformer;
			}
			else if (method.equals(Job.METHOD_AUTO_PURGE_APPLICATION))
			{
				performer = autoPurgeApplicationJobPerformer;
			}
			else
			{
				// Theoretically unreachable, as job has been validated above
				throw InternalException.newInternalException("Unrecognised method: " + method);
			}
			performer.perform(job);
			long finishTime = System.nanoTime();
			
			// Note that some attributes will be null, depending on the job's method
			clf.local.messageId(CDMDebugMessages.CDM_METRIC_PROCESS_JOB)
					.attribute(CDMLoggingInfo.CDM_TIME_TAKEN, finishTime - startTime)
					.attribute(CommonMetaData.APPLICATION_ID, job.getApplicationId())
					.attribute(CDMLoggingInfo.CDM_METHOD, job.getMethod()).debug("Job processing complete");

		}
		catch (CDMException e)
		{
			throw JobProcessingException.newFailure(e);
		}
	}
}
