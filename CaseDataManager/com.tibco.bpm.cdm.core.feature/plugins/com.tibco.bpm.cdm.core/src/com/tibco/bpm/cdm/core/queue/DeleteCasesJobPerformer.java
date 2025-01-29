package com.tibco.bpm.cdm.core.queue;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.NotAuthorisedException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.CaseManager;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * A JobPeformer that deletes cases based on a list of case references
 * @author smorgan
 *
 */
public class DeleteCasesJobPerformer implements JobPerformer
{
	static CLFClassContext	logCtx	= CloudLoggingFramework.init(DeleteCasesJobPerformer.class,
			CDMLoggingInfo.instance);

	private CaseManager		caseManager;

	public DeleteCasesJobPerformer()
	{
	}

	// Called by Spring
	public void setCaseManager(CaseManager caseManager)
	{
		this.caseManager = caseManager;
	}

	@Override
	public void perform(Job job) throws PersistenceException, InternalException, ReferenceException,
			NotAuthorisedException, ArgumentException
	{
		CLFMethodContext clf = logCtx.getMethodContext("perform");
		clf.local.trace("enter");
		clf.local.messageId(CDMDebugMessages.CDM_JOB_DELETE_CASES).debug("Perform Job: %s", job);
		clf.local.messageId(CDMDebugMessages.CDM_JOB_DELETE_CASES).debug("Cases to delete: " + job.getCaseReferences());
		for (String ref : job.getCaseReferences())
		{
			try
			{
				// true flag indicates that system action check should not be performed
				caseManager.deleteCase(new CaseReference(ref), true);
			}
			catch (ReferenceException e)
			{
				// Rather than failing the job and repeatedly retrying (and failing),
				// log a warning.
				clf.local.messageId(CDMDebugMessages.CDM_JOB_DELETE_CASES).warn(e, "Failed to delete case: %s",
						e.toString());
			}
		}
	}
}
