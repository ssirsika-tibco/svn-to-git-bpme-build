package com.tibco.bpm.cdm.core;

import com.tibco.n2.logging.annotations.message.N2LFEntryMessage;
import com.tibco.n2.logging.annotations.message.N2LFExitMessage;
import com.tibco.n2.logging.api.N2LogMethodContext;
import com.tibco.n2.logging.context.interfaces.N2LFIMethodContext;
import com.tibco.n2.logging.context.parser.N2LFContextContainer;
import com.tibco.n2.logging.enums.N2LFMessageCategory;
import com.tibco.n2.logging.enums.N2LFSeverity;

/**
 *
 * @author PRIAGARW
 *
 */
public enum CDMMethodContext implements N2LFIMethodContext
{
	/**
	 *  <b>METHOD</b> - Add to Sequence Update Queue
	 * <br />Entry : DEBUG
	 * <br />Exit : DEBUG
	 * <br />
	 * This method is contained in package :
	 * com.tibco.n2.brm.services.impl.DaemonHandlerThread.java
	 * <p />
	 * Should match with
	 * <code>@N2LFMethodContext(methodId = "addToSequenceUpdateQueue")</code> annotation
	 * for any class for this component
	 */
	@N2LFEntryMessage(messageId = "BRM_WORKITEM_ENT_ADD_TO_SEQUENCE_UPDATE_QUEUE", message = "Start of add to Sequence Update Queue", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.CASE)
	@N2LFExitMessage(messageId = "BRM_WORKITEM_EX_ADD_TO_SEQUENCE_UPDATE_QUEUE", message = "End of add to Sequence Update Queue", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.CASE)
	addToSequenceUpdateQueue,

	/**
	 *  <b>METHOD</b> - Wake Up Daemon
	 * <br />Entry : DEBUG
	 * <br />Exit : DEBUG
	 * <br />
	 * This method is contained in package :
	 * com.tibco.n2.brm.services.impl.DaemonHandlerThread.java
	 * <p />
	 * Should match with
	 * <code>@N2LFMethodContext(methodId = "wakeUpDaemon")</code> annotation
	 * for any class for this component
	 */
	@N2LFEntryMessage(messageId = "BRM_COMPONENT_ENT_WAKEUP_DAEMON", message = "Start of Wake Up Daemon", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.COMPONENT)
	@N2LFExitMessage(messageId = "BRM_COMPONENT_EX_WAKEUP_DAEMON", message = "End of add to Wake Up Daemon", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.COMPONENT)
	wakeUpDaemon,

	/**
	 *  <b>METHOD</b> - Daemon Handler Thread
	 * <br />Entry : DEBUG
	 * <br />Exit : DEBUG
	 * <br />
	 * This method is contained in package :
	 * com.tibco.n2.brm.services.impl.DaemonHandlerThread.java
	 * <p />
	 * Should match with
	 * <code>@N2LFMethodContext(methodId = "DaemonHandlerThread_run")</code> annotation
	 * for any class for this component
	 */
	@N2LFEntryMessage(messageId = "BRM_COMPONENT_ENT_DAEMON_HANDLER", message = "Start of DaemonHandlerThread run", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.COMPONENT)
	@N2LFExitMessage(messageId = "BRM_COMPONENT_EX_DAEMON_HANDLER", message = "End of DaemonHandlerThread run", severity = N2LFSeverity.DEBUG, messageCategory = N2LFMessageCategory.COMPONENT)
	DaemonHandlerThread_run,;

	protected N2LFContextContainer<N2LogMethodContext> ctx = new N2LFContextContainer<N2LogMethodContext>(this);

	public N2LogMethodContext getContext()
	{
		return this.ctx.get();
	}

	public void setContext(N2LogMethodContext aValue)
	{
		ctx.set(aValue);
	}
}
