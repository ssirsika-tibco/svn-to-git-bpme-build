package com.tibco.bpm.auth.ldap;

import javax.security.auth.Subject;

import com.tibco.bpm.auth.DEDelegate;
import com.tibco.bpm.auth.api.BPMAuthenticationHandler;

public abstract class AbastractAuthenticationHandler implements BPMAuthenticationHandler{
	protected DEDelegate deDelegate;
	protected Subject subject;

	public DEDelegate getDeDelegate() {
		return deDelegate;
	}

	public void setDeDelegate(DEDelegate deDelegate) {
		this.deDelegate = deDelegate;
	}

	public Subject getSubject() {
		return subject;
	}

	
	
}
