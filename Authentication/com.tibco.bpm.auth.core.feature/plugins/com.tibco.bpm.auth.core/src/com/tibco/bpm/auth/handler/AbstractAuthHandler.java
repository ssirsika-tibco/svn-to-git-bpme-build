package com.tibco.bpm.auth.handler;

import javax.security.auth.Subject;

import com.tibco.bpm.auth.DEDelegate;

public abstract class AbstractAuthHandler {
	protected DEDelegate deDelegate;
	protected Subject subject;
	
	public Subject getSubject() {
		return subject;
	}

	public DEDelegate getDeDelegate() {
		return deDelegate;
	}

	public void setDeDelegate(DEDelegate deDelegate) {
		this.deDelegate = deDelegate;
	}
}
