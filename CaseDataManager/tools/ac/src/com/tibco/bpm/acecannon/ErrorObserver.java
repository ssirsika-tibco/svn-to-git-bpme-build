package com.tibco.bpm.acecannon;

public interface ErrorObserver
{
	public void notifyError(String message, int statusCode);
}
