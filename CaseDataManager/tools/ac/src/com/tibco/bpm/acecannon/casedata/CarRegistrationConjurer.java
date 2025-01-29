/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Random;

public class CarRegistrationConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static final String[] PRESTIGE_PLATES = {"YT53NMZ", "V999TEC"};

	private int randomYearPart()
	{
		Random r = new Random();
		return 51 + r.nextInt(16);
	}

	private char randomLetter()
	{
		Random r = new Random();
		char c = (char) (r.nextInt(26) + 'A');
		return c;
	}

	@Override
	public String conjure()
	{
		String result = null;
		if (Math.random() >= 0.98)
		{
			// 2% of the time, wheel out a real classic.
			result = ConjuringUtils.randomString(PRESTIGE_PLATES);
		}
		else
		{
			result = "" + randomLetter() + randomLetter() + randomYearPart() + randomLetter() + randomLetter()
					+ randomLetter();
		}

		return result;
	}

}
