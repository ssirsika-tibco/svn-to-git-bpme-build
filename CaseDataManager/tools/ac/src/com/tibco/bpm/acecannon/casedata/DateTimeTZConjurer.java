/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class DateTimeTZConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static String randomDateTimeTZ(int minYear, int maxYear, boolean excludeNonZulu)
	{
		LocalDate ld = LocalDate.of(ConjuringUtils.randBetween(minYear, maxYear), ConjuringUtils.randBetween(1, 12),
				ConjuringUtils.randBetween(1, 28));
		ld.format(DateTimeFormatter.ISO_DATE);

		LocalTime lt = LocalTime.of(ConjuringUtils.randBetween(0, 23), ConjuringUtils.randBetween(0, 59),
				ConjuringUtils.randBetween(0, 59), ConjuringUtils.randBetween(0, 999) * 1000000);
		String lts = lt.format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"));

		StringBuilder buf = new StringBuilder();
		buf.append(ld.toString());
		buf.append("T");
		buf.append(lts);
		if (!excludeNonZulu && Math.random() > 0.1)
		{
			buf.append(Math.random() >= 0.5 ? "+" : "-");
			int tzHours = ConjuringUtils.randBetween(1, 12);
			buf.append(tzHours < 10 ? "0" : "");
			buf.append(tzHours);
			buf.append(":00");
		}
		else
		{
			buf.append("Z");
		}
		return buf.toString();
	}

	public static Option optionExcludeNonZulu = new Option(OptionType.BOOLEAN, "excludeNonZulu",
			"Exclude non-Zulu time-zones");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionExcludeNonZulu);
	}

	@Override
	public String conjure()
	{
		Boolean excludeNonZulu = (Boolean) getOptionValues().get(optionExcludeNonZulu);
		if (excludeNonZulu == null)
		{
			excludeNonZulu = false;
		}
		return randomDateTimeTZ(1970, 2017, excludeNonZulu);
	}

	@Override
	public String getDescription()
	{
		return "Generates DateTimeTZs in yyyy-MM-dd'T'HH:mm:ss.SSSZ format";
	}
}
