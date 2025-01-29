/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static String randomDate(int minYear, int maxYear)
	{
		LocalDate ld = LocalDate.of(ConjuringUtils.randBetween(minYear, maxYear), ConjuringUtils.randBetween(1, 12),
				ConjuringUtils.randBetween(1, 28));
		ld.format(DateTimeFormatter.ISO_DATE);
		return ld.toString();
	}

	@Override
	public String conjure()
	{
		return randomDate(1970, 2017);
	}

	@Override
	public String getDescription()
	{
		return "Generates dates in yyyy-MM-dd format";
	}
}
