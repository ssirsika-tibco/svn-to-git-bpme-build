package com.tibco.bpm.cdm.api;

import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification;

/**
 * Defines the interface for an observer that wishes to receive CaseLifecycleNotifcations
 * from CDM. At the time of writing, this means case deletions, but may include other events
 * in the future.  The CLN has an event type (from an enumeration) and a list of
 * associated case reference(s).
 * 
 * Assuming the implementation is wrapped in a Spring bean called 'clnObserver', the
 * Blueprint declaration to make it visible to CDM would be something like:
 * 
 * <pre>{@code 
 * <bean id="clnObserver" factory-ref="springBridge" factory-method="lookupSpringBean">
		<argument value="clnObserver" />
	</bean>
	<service ref="clnObserver" interface="com.tibco.bpm.cdm.api.CaseLifecycleNotificationObserver">
	</service>
 * }
 * </pre>
 * 
 * @author smorgan
 * @since 2019
 */
public interface CaseLifecycleNotificationObserver
{
	/**
	 * Notifies that a lifecycle event has occurred. 
	 * @param cln
	 */
	public void process(CaseLifecycleNotification cln);
}
