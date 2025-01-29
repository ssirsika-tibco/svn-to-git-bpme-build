/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static String randomTime()
	{
		LocalTime lt = LocalTime.of(ConjuringUtils.randBetween(0, 23), ConjuringUtils.randBetween(0, 59),
				ConjuringUtils.randBetween(0, 59));
		return lt.format(DateTimeFormatter.ofPattern("hh:mm:ss"));
	}

	@Override
	public String conjure()
	{
		return randomTime();
	}

	@Override
	public String getDescription()
	{
		return "Generates times in hh:mm:ss format";
	}
}
