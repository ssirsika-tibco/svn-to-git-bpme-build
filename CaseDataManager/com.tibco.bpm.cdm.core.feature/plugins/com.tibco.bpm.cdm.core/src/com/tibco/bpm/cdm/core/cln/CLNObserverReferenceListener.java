package com.tibco.bpm.cdm.core.cln;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.CaseLifecycleNotificationObserver;
import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification.Event;
import com.tibco.bpm.cdm.core.cln.CLNObserverList.CLNObserverDetails;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Handles requests to add/remove observers for CaseLifecycleNotifications (CLNs).
 * Called from Blueprint when implementors of the CaseLifecycleNotificationObserver interface arrive/depart.
 * Maintains the list of observers (and the events they're interested in) in the CLNObserverList, 
 * which is shared with code that dispatches events. 
 * @author smorgan
 * @since 2019
 */
public class CLNObserverReferenceListener
{
	private static final String	PROP_KEY_EVENTS	= "events";

	static CLFClassContext		logCtx			= CloudLoggingFramework.init(CLNObserverReferenceListener.class,
			CDMLoggingInfo.instance);

	private CLNObserverList		list;

	// Called by Blueprint
	public void setClnObserverList(CLNObserverList list)
	{
		this.list = list;
	}

	public void bind(CaseLifecycleNotificationObserver observer, Map<String, String> properties)
	{
		CLFMethodContext clf = logCtx.getMethodContext("bind");
		if (observer != null)
		{
			synchronized (list)
			{
				// Only add if not already bound
				if (!(list.stream().anyMatch(o -> o.getObserver() == observer)))
				{
					// Work out which events the observer would like (their not specifying events
					// means that ALL events are of interest)
					List<Event> observedEvents = null;
					if (properties != null && properties.containsKey(PROP_KEY_EVENTS))
					{
						String eventsExpression = properties.get(PROP_KEY_EVENTS);
						if (eventsExpression != null)
						{
							// Expression is a comma-separate list of enum values
							String[] fragments = eventsExpression.split(",");
							for (String fragment : fragments)
							{
								String eventName = fragment.trim();
								Event event = Event.valueOf(eventName);
								if (event != null)
								{
									if (observedEvents == null)
									{
										observedEvents = new ArrayList<>();
									}
									observedEvents.add(event);
								}
							}
						}
						clf.local.debug(
								String.format("Binding CLN observer for events %s: %s", observedEvents, observer));
					}
					else
					{
						clf.local.debug(String.format("Binding CLN observer for all events: %s", observer));
					}
					list.add(new CLNObserverDetails(observer, observedEvents));
				}
			}
		}
	}

	public void unbind(CaseLifecycleNotificationObserver observer, Map<String, String> properties)
	{
		CLFMethodContext clf = logCtx.getMethodContext("unbind");
		if (observer != null)
		{
			synchronized (list)
			{
				if (list.stream().anyMatch(o -> o.getObserver() == observer))
				{
					clf.local.debug(String.format("Unbinding CLN observer: %s", observer));
					list.removeIf(o -> o.getObserver() == observer);
				}
			}
		}
	}
}
