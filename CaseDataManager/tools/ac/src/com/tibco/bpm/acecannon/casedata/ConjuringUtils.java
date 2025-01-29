/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConjuringUtils
{
	public static String randomString(String[] values)
	{
		return values[ThreadLocalRandom.current().nextInt(values.length)];
	}

	public static String randomString(List<String> values)
	{
		return values.get(ThreadLocalRandom.current().nextInt(values.size()));
	}

	public static Double randomDouble()
	{
		return new Double(Math.random() * 10000);
	}

	public static long randomInteger(int minDigits, int maxDigits)
	{
		ThreadLocalRandom r = ThreadLocalRandom.current();
		long minValue = (long) (Math.pow(10, minDigits - 1));
		long maxValue = (long) Math.pow(10, maxDigits) - 1;
		long number = r.nextLong(minValue, maxValue);
		return number;
	}

	public static String randomCapitalLetter()
	{
		return Character.toString((char) (65 + (Math.random() * 26)));
	}

	public static char randomChar(String s)
	{
		return s.charAt((int) (Math.random() * s.length()));
	}

	public static int randBetween(int start, int end)
	{
		return start + (int) Math.round(Math.random() * (end - start));
	}
}
