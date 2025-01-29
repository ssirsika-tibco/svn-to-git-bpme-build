package com.tibco.bpm.cdm.core.autopurge;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.PropertyDAO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.cdm.core.queue.JobQueue;
import com.tibco.bpm.cdm.core.queue.QueueException;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.msg.pq.CloudPQ;

/**
 * Handles changes made to auto-purge related config properties.
 * 
 * @author smorgan
 * @since 2019
 */
public class AutoPurgeConfigChangeHandler implements AutoPurgeConstants
{
	static CLFClassContext								logCtx		= CloudLoggingFramework
			.init(AutoPurgeConfigChangeHandler.class, CDMLoggingInfo.instance);

	private static final ThreadLocal<SimpleDateFormat>	FORMATTER	= ThreadLocal.withInitial(() -> {
																		SimpleDateFormat result = new SimpleDateFormat(
																				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
																		result.setTimeZone(TimeZone.getTimeZone("UTC"));
																		return (result);
																	});

	private PropertyDAO									propertyDAO;

	private JobQueue									jobQueue;

    DAOFactory daoFactory;

	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		propertyDAO = daoFactory.getPropertyDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	
	// Called by Spring
	public void setPropertyDAO(PropertyDAO propertyDAO)
	{
		this.propertyDAO = propertyDAO;
	}
	// Called by Spring

	public void setJobQueue(JobQueue jobQueue)
	{
		this.jobQueue = jobQueue;
	}

	public void handleIntervalChange(String value)
	{
		CLFMethodContext clf = logCtx.getMethodContext("handleIntervalChange");
		clf.local.debug("Handling auto-purge interval change to: %s", value);

		Integer interval = null;
		if (value != null)
		{
			try
			{
				interval = Integer.parseInt(value);
				if (interval < MIN_INTERVAL_MINUTES || interval > MAX_INTERVAL_MINUTES)
				{
					clf.local.error(String.format("Config property must have a value between %d and %d, not %d",
							MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES, interval));
					interval = null;
				}
			}
			catch (NumberFormatException e)
			{
				clf.local.error(String.format("Config property has a non-integer value: %s", value));
			}
		}

		if (interval == null)
		{
			interval = DEFAULT_INTERVAL_MINUTES;
			clf.local.info("Assuming default interval: %d", interval);
		}

		try
		{
			String lastAutoPurgeTime = propertyDAO.get(PropertyDAO.NAME_LAST_AUTO_PURGE_TIME);
			if (lastAutoPurgeTime != null)
			{
				Date parse = FORMATTER.get().parse(lastAutoPurgeTime);
				Date now = new Date();
				long minutesSinceRun = (now.getTime() - parse.getTime()) / 60000;
				clf.local.info("Last run: %s, Now: %s, Minutes since run: %d", parse, now, minutesSinceRun);
				Job job = new Job();
				job.setMethod(Job.METHOD_AUTO_PURGE);
				try
				{
					// Cancel existing AP job and then queue another one, either to
					// happen immediately (if the configured interval has already elapsed
					// since the last run) or appropriately delayed.
					jobQueue.purgeJobsWithCorrelationId("AP");
					if (minutesSinceRun >= interval)
					{
						clf.local.info("Run is due now.");
						jobQueue.enqueueJob(job, Job.AP_JOB_CORRELATION_ID, 0);
					}
					else
					{
						int dueInMinutes = (int) (interval - minutesSinceRun);
						clf.local.info("Run is due in minutes: " + dueInMinutes);
						int dueInSeconds = dueInMinutes * 60;
						jobQueue.enqueueJob(job, Job.AP_JOB_CORRELATION_ID, dueInSeconds);
					}
				}
				catch (QueueException e)
				{
					clf.local.error(e, e.toString());
				}
			}
			else
			{
				// Job has NEVER run before, so queue the initial run
				Job job = new Job();
				job.setMethod(Job.METHOD_AUTO_PURGE);
				try
				{
					jobQueue.enqueueJob(job, Job.AP_JOB_CORRELATION_ID, CloudPQ.DELAY_NONE);
				}
				catch (QueueException e)
				{
					clf.local.error(e, e.toString());
				}
			}
		}
		catch (PersistenceException e)
		{
			clf.local.error(e, e.toString());
		}
		catch (ParseException e)
		{
			clf.local.error(e, e.toString());
		}
	}
}
