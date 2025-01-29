/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

public class NoValueConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	@Override
	public String conjure()
	{
		return null;
	}

	@Override
	public String getDescription()
	{
		return "A property for this attribute will not appear in the casedata at all. "
				+ "This is useful when testing the application of default values, or preventing "
				+ "illegal assignment of a value to an auto-generated identifier ( \uD83D\uDE97 \uD83C\uDD94 )";
	}
}
