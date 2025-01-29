package com.tibco.bpm.auth.saml.logging;

import com.tibco.bpm.logging.cloud.annotations.message.CLFAuditMessage;
import com.tibco.n2.logging.annotations.metadata.AuditProfile;
import com.tibco.n2.logging.context.interfaces.N2LFILogMessage;
import com.tibco.n2.logging.context.message.N2LogMessageContext;
import com.tibco.n2.logging.context.parser.N2LFContextContainer;
import com.tibco.n2.logging.enums.N2LFMessageCategory;

public enum AuthAuditMessages implements N2LFILogMessage
{

	// @formatter:off
	@CLFAuditMessage(messageId = "USER_LOGGED_IN", auditProfiles = {
			AuditProfile.SYSTEM_LOGIN}, message = "User Logged in", messageCategory = N2LFMessageCategory.SECURITY, attributeList = {})
	USER_LOGGED_IN,
	@CLFAuditMessage(messageId = "USER_LOGGED_OUT", auditProfiles = {
			AuditProfile.SYSTEM_LOGIN}, message = "User Logged out", messageCategory = N2LFMessageCategory.SECURITY, attributeList = {})
	USER_LOGGED_OUT;
	// @formatter:on

	protected N2LFContextContainer<N2LogMessageContext> ctx = new N2LFContextContainer<N2LogMessageContext>(this);

	public N2LogMessageContext getContext()
	{
		return this.ctx.get();
	}

	public void setContext(N2LogMessageContext aValue)
	{
		this.ctx.set(aValue);
	}
}