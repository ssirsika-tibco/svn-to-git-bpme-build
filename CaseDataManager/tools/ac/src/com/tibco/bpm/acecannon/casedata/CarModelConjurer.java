/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

public class CarModelConjurer extends AbstractConjurer<String>
{
	private static final String[] VALUES = {"Zafira", "Accord", "Prius", "BX", "X3", "Q3", "45A", "Galaxy", "Bongo Friendee"};

	@Override
	public String conjure()
	{
		String result = ConjuringUtils.randomString(VALUES);
		return result;
	}
}