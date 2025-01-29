package com.tibco.bpm.auth.saml.logging;

import com.tibco.bpm.logging.cloud.annotations.message.CLFExceptionMessage;
import com.tibco.n2.logging.context.interfaces.N2LFIExceptionMessage;
import com.tibco.n2.logging.context.message.N2LogExceptionContext;
import com.tibco.n2.logging.context.parser.N2LFContextContainer;
import com.tibco.n2.logging.enums.N2LFMessageCategory;

public enum AuthExceptionMessages implements N2LFIExceptionMessage {

	@CLFExceptionMessage(messageId = "AUTHENTICATION_FAILED", message = "Authentication Failed", messageCategory = N2LFMessageCategory.SECURITY, serviceFault = true, attributeList = {}) AUTHENTICATION_FAILED;

	protected N2LFContextContainer<N2LogExceptionContext> ctx = new N2LFContextContainer<N2LogExceptionContext>(this);

	public N2LogExceptionContext getContext() {
		return this.ctx.get();
	}

	public void setContext(N2LogExceptionContext ctx) {
		this.ctx.set(ctx);
	}

	// To implement ErrorMessage interface to allow Exception Constructors to be
	// passed the enums from this class
	public String getMessage() {
		return (ctx.get().getMessage());
	}

	// Extract the messageId from the N2LF logging exception message for
	// reporting in the fault errorCode
	public String getMessageId() {
		return (ctx.get().getMessageId());
	}
}