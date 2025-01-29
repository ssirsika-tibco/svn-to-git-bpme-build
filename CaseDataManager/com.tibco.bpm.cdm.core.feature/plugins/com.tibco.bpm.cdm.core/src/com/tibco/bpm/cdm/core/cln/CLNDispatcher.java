package com.tibco.bpm.cdm.core.cln;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.CaseLifecycleNotificationObserver;
import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification;
import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification.Event;
import com.tibco.bpm.cdm.core.cln.CLNObserverList.CLNObserverDetails;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.message.CaseLifecycleNotificationImpl;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Dispatches Case Lifecycle Notifications (CLNs) to observer(s).
 * The clnObserverList is populated by {@link CLNObserverReferenceListener} in 
 * response to requests from Blueprint.
 * @author smorgan
 * @since 2019
 */
public class CLNDispatcher
{
	static CLFClassContext	logCtx	= CloudLoggingFramework.init(CLNDispatcher.class, CDMLoggingInfo.instance);

	private CLNObserverList	clnObserverList;

	// Called by Spring
	public void setClnObserverList(CLNObserverList clnObserverList)
	{
		this.clnObserverList = clnObserverList;
	}

	public boolean hasObservers()
	{
		return clnObserverList != null && !clnObserverList.isEmpty();
	}

	public void dispatch(Event event, List<CaseReference> caseReferences)
	{
		CaseLifecycleNotificationImpl cln = new CaseLifecycleNotificationImpl();
		cln.setEvent(event);
		cln.getCaseReferences()
				.addAll(caseReferences.stream().map(CaseReference::toString).collect(Collectors.toList()));
		dispatch(cln);
	}

	public void dispatch(Event event, CaseReference caseReference)
	{
		CaseLifecycleNotificationImpl cln = new CaseLifecycleNotificationImpl();
		cln.setEvent(event);
		cln.getCaseReferences().add(caseReference.toString());
		dispatch(cln);
	}

	protected void dispatch(CaseLifecycleNotification cln)
	{
		CLFMethodContext clf = logCtx.getMethodContext("dispatch");
		if (clnObserverList != null)
		{
			synchronized (clnObserverList)
			{
				// Filter the observer list to just those that are interested
				// in this event (or ALL events).
				List<CLNObserverDetails> targets = new ArrayList<>();
				for (int i = 0; i < clnObserverList.size(); i++)
				{
					CLNObserverDetails clnObserverDetails = clnObserverList.get(i);
					if (clnObserverDetails.includesEvent(cln.getEvent()))
					{
						targets.add(clnObserverDetails);
					}
				}

				// Dispatch CLN to appropriate targets
				clf.local.debug(String.format("Dispatching CLN to %d observer(s): %s", targets.size(), cln));
				for (int i = 0; i < targets.size(); i++)
				{
					CLNObserverDetails clnObserverDetails = targets.get(i);
					CaseLifecycleNotificationObserver observer = clnObserverDetails.getObserver();
					clf.local.debug(
							String.format("Dispatching CLN to observer %d/%d (%s)", i + 1, targets.size(), observer));
					observer.process(cln);
				}
			}
		}
		else
		{
			clf.local.debug("clnObserverList is not set");
		}
	}
}
