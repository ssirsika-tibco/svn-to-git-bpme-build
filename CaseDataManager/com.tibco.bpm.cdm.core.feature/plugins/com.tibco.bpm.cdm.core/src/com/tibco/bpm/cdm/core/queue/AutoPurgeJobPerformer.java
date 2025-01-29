package com.tibco.bpm.cdm.core.queue;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConstants;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.PropertyDAO;
import com.tibco.bpm.cdm.core.deployment.DataModelManager;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * A perform for 'autoPurge' jobs.
 * Creates an 'autoPurgeSandbox' job for each sandbox that contains applications and then re-enqueues
 * to happen again at a configurable interval later.
 * @author smorgan
 */
public class AutoPurgeJobPerformer extends AbstractAutoPurgeJobPerformer implements JobPerformer
{
	// Correlation id format for autoPurgeApplication jobs
	private static final String							APA_CORRELATION_ID_FORMAT	= "APA-%s";

	static CLFClassContext								logCtx						= CloudLoggingFramework
			.init(AutoPurgeJobPerformer.class, CDMLoggingInfo.instance);

	private JobQueue									jobQueue;

	private DataModelManager							dataModelManager;

	private PropertyDAO									propertyDAO;

	private static final int							AUTO_PURGE_INTERVAL_DEFAULT	= 15;

	private static final int							CHILD_JOB_DELAY_DEFAULT		= 10;

	private int											childJobDelay				= CHILD_JOB_DELAY_DEFAULT;

	private static final ThreadLocal<SimpleDateFormat>	FORMATTER					= ThreadLocal.withInitial(() -> {
																						SimpleDateFormat result = new SimpleDateFormat(
																								"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
																						result.setTimeZone(TimeZone
																								.getTimeZone("UTC"));
																						return (result);
																					});

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
	public AutoPurgeJobPerformer()
	{
	}

	// Called by Spring
	public void setPropertyDAO(PropertyDAO propertyDAO)
	{
		this.propertyDAO = propertyDAO;
	}

	// Called by Spring
	public void setDataModelManager(DataModelManager dataModelManager)
	{
		this.dataModelManager = dataModelManager;
	}

	// Called by Spring
	public void setAdminConfigurationService(AdminConfigurationService adminConfigurationService)
	{
		this.adminConfigurationService = adminConfigurationService;
	}

	// Called by Spring
	public void setJobQueue(JobQueue jobQueue)
	{
		this.jobQueue = jobQueue;
	}

	// Called by Spring
	public void init()
	{
	}

	@Override
	public void perform(Job job) throws PersistenceException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("perform");
		clf.local.trace("enter");
		clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE).debug("Perform Job: %s", job);

		List<BigInteger> applicationIds = dataModelManager.getApplicationIds();

		// Queue a child job for each application
		for (BigInteger applicationId : applicationIds)
		{
			String correlationId = String.format(APA_CORRELATION_ID_FORMAT, applicationId.toString());
			Job childJob = new Job();
			childJob.setMethod(Job.METHOD_AUTO_PURGE_APPLICATION);
			childJob.setApplicationId(applicationId);
			try
			{
				jobQueue.enqueueJob(childJob, correlationId, childJobDelay);
			}
			catch (QueueException e)
			{
				throw InternalException.newInternalException(e,
						"Unable to queue autoPurgeSandbox job: " + e.toString());
			}
		}

		// Record the last run time
		propertyDAO.set(PropertyDAO.NAME_LAST_AUTO_PURGE_TIME, FORMATTER.get().format(new Date()));

		// Repost the job, so the process starts again after autoPurgeInterval seconds
		try
		{
			Integer intervalInMinutes = getIntervalInMinutes();
			if (intervalInMinutes == null)
			{
				intervalInMinutes = AUTO_PURGE_INTERVAL_DEFAULT;
				clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE).debug(
						"Assuming default auto-purge cycle interval as %s configuration property is not set. Minutes: %d",
						AutoPurgeConstants.PROPERTY_INTERVAL, intervalInMinutes);
			}
			int intervalInSeconds = intervalInMinutes * 60;
			jobQueue.enqueueJob(job, Job.AP_JOB_CORRELATION_ID, intervalInSeconds);
		}
		catch (QueueException e)
		{
			throw InternalException.newInternalException(e, "Unable to queue autoPurge job: " + e.toString());
		}

	}
}
