package com.tibco.bpm.cdm.api.message;

import java.util.List;

/**
 * A message notifying the update or deletion of case(s)
 * (and potentially other case-related events in the future)
 *
 * @author smorgan
 * @since 2019
 */
public interface CaseLifecycleNotification
{
	public static enum Event
	{
		UPDATED,
		DELETED
	};

	public Event getEvent();

	public List<String> getCaseReferences();
}
