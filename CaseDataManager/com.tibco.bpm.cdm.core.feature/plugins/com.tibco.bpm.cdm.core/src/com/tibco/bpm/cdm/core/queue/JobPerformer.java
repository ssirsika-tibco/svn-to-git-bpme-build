package com.tibco.bpm.cdm.core.queue;

import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.core.model.Job;

/**
 * A JobPerformer class is responsible for processing a jobs from CDM's internal job queue.
 * Treated as singletons, so much be thread-safe and stateless.
 * @author smorgan
 * @since 2019
 */
public interface JobPerformer
{
	public void perform(Job job) throws CDMException;
}
