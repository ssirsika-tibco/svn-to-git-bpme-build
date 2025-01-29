package com.tibco.bpm.cdm.core.logging;

import com.tibco.bpm.logging.cloud.context.CLFIMessageId;

/**
 * Message ids for debug messages.
 * i.e. messages that are recorded to aid debugging, not necessarily literally of 'debug' severity.
 * Typically used in conjunction with messages that log performance metrics.
 *
 * @author smorgan
 * @since 2019
 */
public enum CDMDebugMessages implements CLFIMessageId
{
	//@formatter:off
	CDM_METRIC_PROCESS_JOB,
	CDM_JOB_AUTO_PURGE,
	CDM_JOB_AUTO_PURGE_CASE_TYPE,
	CDM_JOB_AUTO_PURGE_APPLICATION,
	CDM_JOB_AUTO_PURGE_APPLICATION_MFP,
	CDM_JOB_DELETE_CASES,
	CDM_JOBQUEUE_THREAD_START,
	CDM_JOBQUEUE_THREAD_STOP,
	CDM_JOBQUEUE_ENQUEUE_JOB,
	CDM_JOBQUEUE_ENQUEUE_FAILED,
	CDM_JOBQUEUE_PROCESSING_FAILED,
	CDM_CASE_DELETE_EXCEPTION,
	CDM_CASES_PURGED,
	//@formatter:on
}
