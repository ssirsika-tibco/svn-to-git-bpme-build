package com.tibco.bpm.cdm.core.queue;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * A performer for 'autoPurgeApplication' jobs.
 * Creates 'autoPurgeCaseType' jobs for each case type within a given application.
 * 
 * @author smorgan
 * @since 2019
 */
public class AutoPurgeApplicationJobPerformer implements JobPerformer
{
	static CLFClassContext		logCtx						= CloudLoggingFramework
			.init(AutoPurgeApplicationJobPerformer.class, CDMLoggingInfo.instance);

	// Correlation id format for autoPurgeCaseType jobs
	private static final String	APCT_CORRELATION_ID_FORMAT	= "APCT-%s-%s";

	private static final int	CHILD_JOB_DELAY_DEFAULT		= 10;

	private JobQueue			jobQueue;

	private int					childJobDelay				= CHILD_JOB_DELAY_DEFAULT;

	private TypeDAO				typeDAO;

    DAOFactory daoFactory;

	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		typeDAO = daoFactory.getTypeDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	// Called by Spring
	public AutoPurgeApplicationJobPerformer()
	{
	}

	// Called by Spring
	public void setJobQueue(JobQueue jobQueue)
	{
		this.jobQueue = jobQueue;
	}

	@Override
	public void perform(Job job) throws InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("perform");
		clf.local.trace("enter");
		clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_APPLICATION).debug("Perform Job: %s", job);

		BigInteger applicationId = job.getApplicationId();

		// Get the type ids for all the case types in the app.
		List<BigInteger> caseTypeIds = typeDAO.getCaseTypeIdsByApplication(applicationId);

		// Create a job for each case type.   
		for (BigInteger caseTypeId : caseTypeIds)
		{
			Job childJob = new Job();
			childJob.setMethod(Job.METHOD_AUTO_PURGE_CASE_TYPE);
			childJob.setApplicationId(applicationId);
			childJob.setTypeId(caseTypeId);
			// Post job on queue
			try
			{
				String correlationId = String.format(APCT_CORRELATION_ID_FORMAT, applicationId, caseTypeId);
				jobQueue.enqueueJob(childJob, correlationId, childJobDelay);
			}
			catch (QueueException e)
			{
				throw InternalException.newInternalException(e, "Failed to enqueue job message");
			}
		}
	}
}
