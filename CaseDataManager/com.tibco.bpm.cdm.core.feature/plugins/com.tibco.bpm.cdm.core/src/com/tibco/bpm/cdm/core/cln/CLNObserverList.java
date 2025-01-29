package com.tibco.bpm.cdm.core.cln;

import java.util.ArrayList;
import java.util.List;

import com.tibco.bpm.cdm.api.CaseLifecycleNotificationObserver;
import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification.Event;
import com.tibco.bpm.cdm.core.cln.CLNObserverList.CLNObserverDetails;

public class CLNObserverList extends ArrayList<CLNObserverDetails>
{
	public static class CLNObserverDetails
	{
		CaseLifecycleNotificationObserver	observer;

		List<Event>							observedEvents;

		public CLNObserverDetails(CaseLifecycleNotificationObserver observer, List<Event> observedEvents)
		{
			this.observer = observer;
			this.observedEvents = observedEvents;
		}

		public boolean includesEvent(Event event)
		{
			return observedEvents == null || observedEvents.contains(event);
		}

		public CaseLifecycleNotificationObserver getObserver()
		{
			return observer;
		}
	}

	private static final long serialVersionUID = 1L;

}
