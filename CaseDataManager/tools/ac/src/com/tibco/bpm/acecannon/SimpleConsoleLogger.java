package com.tibco.bpm.acecannon;

public class SimpleConsoleLogger implements Logger
{
	@Override
	public void log(String message)
	{
		System.out.println(Thread.currentThread().getName() + "\t" + message);
	}
}
