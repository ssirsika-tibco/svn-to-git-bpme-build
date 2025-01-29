package com.tibco.bpm.cdm.core.logging;

import com.tibco.bpm.logging.cloud.annotations.message.CLFExceptionMessage;
import com.tibco.n2.logging.context.interfaces.N2LFIExceptionMessage;
import com.tibco.n2.logging.context.message.N2LogExceptionContext;
import com.tibco.n2.logging.context.parser.N2LFContextContainer;
import com.tibco.n2.logging.enums.N2LFMessageCategory;

/**
 * Audit messages for failures scenarios.
 * @author smorgan
 * @since 2019
 */
public enum CDMExceptionMessages implements N2LFIExceptionMessage
{

	@CLFExceptionMessage(messageId = "CDM_APPLICATION_DEPLOYMENT_FAILED", message = "Deployment Failed", messageCategory = N2LFMessageCategory.CASE_DATA, serviceFault = true, attributeList = {})
	CDM_APPLICATION_DEPLOYMENT_FAILED,

	@CLFExceptionMessage(messageId = "CDM_APPLICATION_UNDEPLOYMENT_FAILED", message = "Undeployment Failed", messageCategory = N2LFMessageCategory.CASE_DATA, serviceFault = true, attributeList = {})
	CDM_APPLICATION_UNDEPLOYMENT_FAILED;

	protected N2LFContextContainer<N2LogExceptionContext> ctx = new N2LFContextContainer<N2LogExceptionContext>(this);

	public N2LogExceptionContext getContext()
	{
		return this.ctx.get();
	}

	public void setContext(N2LogExceptionContext ctx)
	{
		this.ctx.set(ctx);
	}

	// To implement ErrorMessage interface to allow Exception Constructors to be passed the enums from this class
	public String getMessage()
	{
		return (ctx.get().getMessage());
	}

	// Extract the messageId from the N2LF logging exception message for reporting in the fault errorCode
	public String getMessageId()
	{
		return (ctx.get().getMessageId());
	}
}
