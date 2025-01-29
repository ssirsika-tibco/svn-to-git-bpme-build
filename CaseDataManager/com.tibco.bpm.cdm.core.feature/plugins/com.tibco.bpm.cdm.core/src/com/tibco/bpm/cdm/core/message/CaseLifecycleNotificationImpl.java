package com.tibco.bpm.cdm.core.message;

import java.util.ArrayList;
import java.util.List;

import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification;

/**
 * A message notifying the deletion of case(s)
 * (and potentially other case-related events in the future)
 *
 * @author smorgan
 * @since 2019
 */
public class CaseLifecycleNotificationImpl implements CaseLifecycleNotification
{
	private Event			event;

	private List<String>	caseReferences	= new ArrayList<String>();

	public Event getEvent()
	{
		return event;
	}

	public void setEvent(Event event)
	{
		this.event = event;
	}

	public List<String> getCaseReferences()
	{
		return caseReferences;
	}

	public String toString()
	{
		return String.format("%s %s", event, caseReferences);
	}
}
