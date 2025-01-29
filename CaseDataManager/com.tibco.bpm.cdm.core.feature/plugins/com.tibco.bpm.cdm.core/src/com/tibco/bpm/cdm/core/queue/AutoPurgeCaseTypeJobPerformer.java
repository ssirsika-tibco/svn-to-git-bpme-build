package com.tibco.bpm.cdm.core.queue;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConstants;
import com.tibco.bpm.cdm.core.dao.CaseDAO;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.container.engine.api.ContainerEngineInstances;
import com.tibco.bpm.container.engine.model.InstanceInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * A performer for 'autoPurgeCaseType' jobs.
 * Determines which cases of a given type qualify for purging (i.e. have been in a terminal state
 * for longer than the configured time period and are not referenced by a process), marks
 * them for purge by setting the marked_for_purge flag in the database (making them invisible to all APIs)
 * and then schedules 'deleteCases' jobs to perform the action deletion in batches. 
 * @author smorgan
 * @since 2019
 */
public class AutoPurgeCaseTypeJobPerformer extends AbstractAutoPurgeJobPerformer implements JobPerformer
{
	// Correlation id format for autoPurgeApplication jobs
	private static final String							DC_CORRELATION_ID_FORMAT	= "DC-%s";

	private static final int							CHILD_JOB_DELAY_DEFAULT		= 10;

	// The number of case references per deleteCases job
	private static final int							PURGE_BATCH_SIZE			= 50;

	static CLFClassContext								logCtx						= CloudLoggingFramework
			.init(AutoPurgeCaseTypeJobPerformer.class, CDMLoggingInfo.instance);

	private JobQueue									jobQueue;

	private int											childJobDelay				= CHILD_JOB_DELAY_DEFAULT;

	private ContainerEngineInstances					processInstancesAPI;

	private CaseDAO										caseDAO;

	// Formatter for rendering creation/modification stamps to String.
	// SimpleDateFormat is not thread-safe, but we don't want to create a new one
	// each time we use it, so this essentially gives us a per-thread singleton.
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
		caseDAO = daoFactory.getCaseDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	
	
	// Called by Spring
	public AutoPurgeCaseTypeJobPerformer()
	{
	}

	// Called by Spring
	public void setProcessInstancesAPI(ContainerEngineInstances processInstancesAPI)
	{
		this.processInstancesAPI = processInstancesAPI;
	}

	// Called by Spring
	public void setCaseDAO(CaseDAO caseDAO)
	{
		this.caseDAO = caseDAO;
	}

	// Called by Spring
	public void setJobQueue(JobQueue jobQueue)
	{
		this.jobQueue = jobQueue;
	}

	@Override
	public void perform(Job job) throws ReferenceException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("perform");
		clf.local.trace("enter");
		clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE).debug("Perform Job: %s", job);

		// Call the Admin service to get our config parameter
		Integer purgeTimeMinutes = getPurgeTimeInMinutes();
		if (purgeTimeMinutes == null)
		{
			purgeTimeMinutes = AutoPurgeConstants.DEFAULT_PURGE_TIME;
			clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE).debug(
					"Assuming default time after which a terminal-state case can be purged as the %s configuration "
							+ "property is not set. Minutes: %d",
					AutoPurgeConstants.PROPERTY_PURGE_TIME, purgeTimeMinutes);
		}

		if (processInstancesAPI == null)
		{
			clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE).warn(
					"ContainerEngineInstances service is not available, so it is not possible to determine if cases are safe to purge.");
		}
		else
		{
			Calendar maxModificationTimestamp = Calendar.getInstance();
			maxModificationTimestamp.add(Calendar.MINUTE, -purgeTimeMinutes);
			BigInteger typeId = job.getTypeId();
			clf.local.debug("Checking for terminal state cases last modified at or before %s",
					FORMATTER.get().format(maxModificationTimestamp.getTime()));
			List<CaseReference> caseRefs = caseDAO.primeForPurge(typeId, maxModificationTimestamp);

			clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE)
					.debug("Primed cases (deleting subject to no process references): %s", caseRefs);

			// Work out which cases are safe to delete (i.e. those that are _not_ referenced by processes)
			List<CaseReference> unreferencedCaseRefs = new ArrayList<>();
			for (CaseReference caseRef : caseRefs)
			{
				try
				{
					// TODO Use Mark's new method instead: long countInstancesByGoRefs(List<String> goRefs) throws Exception;
					// when it exists (as of 18 Sep 19, this method does not exist).
					List<InstanceInfo> instances = processInstancesAPI
							.findInstancesByGoRefs(Collections.singletonList(caseRef.toString()));
					if (instances != null && !instances.isEmpty())
					{
						// Instance(s) found for case, so can't delete it
						if (clf.local.isTraceEnabled())
						{
							clf.local.trace("Unable to delete case as referenced by process instance(s): %s", caseRef);
						}
					}
					else
					{
						// Not referenced by processes, so safe to delete
						unreferencedCaseRefs.add(caseRef);
						clf.local.messageId(CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE).debug("Safe to delete: %s",
								caseRef);
					}
				}
				catch (Exception e)
				{
					throw InternalException.newInternalException(e,
							"Call to process instances API failed: " + e.getMessage());
				}
			}

			// Batch up cases and create jobs
			int mfpCount = caseRefs.size();
			for (int base = 0; base < mfpCount; base += PURGE_BATCH_SIZE)
			{
				Job deleteJob = new Job();
				deleteJob.setMethod(Job.METHOD_DELETE_CASES);
				deleteJob.setApplicationId(job.getApplicationId());
				deleteJob.setTypeId(job.getTypeId());

				// Take a sub-list of case references for this batch
				int lastIndex = Math.min(base + PURGE_BATCH_SIZE, mfpCount) - 1;
				List<CaseReference> batchOfRefs = caseRefs.subList(base, lastIndex + 1);

				caseDAO.markForPurge(batchOfRefs);

				deleteJob.getCaseReferences()
						.addAll(batchOfRefs.stream().map(r -> r.toString()).collect(Collectors.toList()));

				// Create a Job to delete a batch of cases
				clf.local.debug(String.format("Creating job for cases at indexes %d-%d. Total case count is %s.", base,
						lastIndex, mfpCount));

				// Post job on queue
				try
				{
					String correlationId = String.format(DC_CORRELATION_ID_FORMAT, UUID.randomUUID().toString());
					jobQueue.enqueueJob(deleteJob, correlationId, childJobDelay);
				}
				catch (QueueException e)
				{
					throw InternalException.newInternalException(e, "Failed to enqueue job message");
				}
			}
		}
	}
}
