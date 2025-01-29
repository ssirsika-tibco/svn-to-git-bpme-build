/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

public class CarMakeConjurer extends AbstractConjurer<String>
{
	private static final String[] VALUES = {"Vauxhall", "Lexus", "Ford", "Nissan", "Audi", "BMW", "Honda", "Yugo", "Mazda"};

	@Override
	public String conjure()
	{
		String result = ConjuringUtils.randomString(VALUES);
		return result;
	}
}
