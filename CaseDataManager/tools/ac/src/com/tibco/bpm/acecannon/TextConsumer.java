/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon;

@FunctionalInterface
public interface TextConsumer
{
	public void consume(String text);
}
