package com.tibco.bpm.cdm.core.queue;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConfigChangeHandler;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConstants;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.logging.cloud.context.CLFThreadContext;

/**
 * A thread that processes messages from the internal job queue.  These are messages
 * that were posted in order to perform time-consuming processing asynchronously.
 * At the time of writing, this primarily means the 'auto-purge' process that cleans
 * up old terminal-state cases.  It is also used when cases are explicitly purged by a user.
 * 
 * @author smorgan
 * @since 2019
 */
public class JobQueueProcessingThread extends Thread
{
	static CLFClassContext					logCtx								= CloudLoggingFramework
			.init(JobQueueProcessingThread.class, CDMLoggingInfo.instance);

	private static final String				THREAD_NAME							= "CDM-JobQueueProcessor";

	private boolean							term								= false;

	private JobQueueProcessor				jobQueueProcessor;

	private AutoPurgeConfigChangeHandler	autoPurgeConfigChangeHandler;

	protected AdminConfigurationService		adminConfigurationService;

	// Called by Spring
	public void setAdminConfigurationService(AdminConfigurationService adminConfigurationService)
	{
		this.adminConfigurationService = adminConfigurationService;
	}

	// Called by Spring
	public void setAutoPurgeConfigChangeHandler(AutoPurgeConfigChangeHandler autoPurgeConfigChangeHandler)
	{
		this.autoPurgeConfigChangeHandler = autoPurgeConfigChangeHandler;
	}

	// Called by Spring
	public void setJobQueueProcessor(JobQueueProcessor jobQueueProcessor)
	{
		this.jobQueueProcessor = jobQueueProcessor;
	}

	// Called by Spring
	public void init()
	{
		// Start me (I'm a Thread)
		start();
	}

	// Called by Spring
	public void destroy()
	{
		// Stop me (I'm a Thread)
		term();
		interrupt();
	}

	public JobQueueProcessingThread()
	{
		super(THREAD_NAME);
	}

	public void term()
	{
		this.term = true;
	}

	@Override
	public void run()
	{
		@SuppressWarnings("unused")
		CLFThreadContext parent = CLFThreadContext.beginContext(CLFThreadContext.RECORD_LOG_MESSAGES.OFF);
		CLFMethodContext clf = logCtx.getMethodContext("run");
		clf.local.messageId(CDMDebugMessages.CDM_JOBQUEUE_THREAD_START).debug("JobQueueProcessingThread starting");

		// Simulate the configured AP interval being changed in order to 
		// ensure there's a job queued (this will cancel any existing job)
		try
		{
			Property property = adminConfigurationService.getProperty(GroupId.cdm,
					AutoPurgeConstants.PROPERTY_INTERVAL);
			autoPurgeConfigChangeHandler.handleIntervalChange(property != null ? property.getValue() : null);
		}
		catch (ServiceException e)
		{
			clf.local.error(e, e.toString());
		}

		// Keep processing until asked to stop...
		while (!term)
		{
			try
			{
				// The Spring config is such that any exception thrown within this call will
				// cause a rollback, so the job will remain on the queue and be picked up again.
				int waitPeriod = jobQueueProcessor.getNextJobAndProcessIt();

				if (waitPeriod > 0)
				{
					jobQueueProcessor.blockForSeconds(waitPeriod); // typically up to 180 seconds
				}
			}
			catch (Exception e)
			{
				clf.local.messageId(CDMDebugMessages.CDM_JOBQUEUE_PROCESSING_FAILED).error(e, "%s",
						"Failed to process job: " + e.toString());

			}
		}
		clf.local.messageId(CDMDebugMessages.CDM_JOBQUEUE_THREAD_STOP).debug("JobQueueProcessingThread exiting");
	}
}
