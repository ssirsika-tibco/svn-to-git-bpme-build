/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

public class NumberConjurer extends AbstractConjurer<Long> implements ValueConjurer<Long>
{
	public static Option	optionMinDigits	= new Option(OptionType.INTEGER, "minDigits", "Min. digits");

	public static Option	optionMaxDigits	= new Option(OptionType.INTEGER, "maxDigits", "Max. digits");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionMinDigits, optionMaxDigits);
	}

	@Override
	public Long conjure()
	{
		int minDigits = getOptionValues().containsKey(optionMinDigits) ? (int) getOptionValues().get(optionMinDigits)
				: 1;
		int maxDigits = getOptionValues().containsKey(optionMaxDigits) ? (int) getOptionValues().get(optionMaxDigits)
				: minDigits;

		// Constrain range to 1-18
		minDigits = Math.max(1, minDigits);
		minDigits = Math.min(18, minDigits);

		maxDigits = Math.max(1, maxDigits);
		maxDigits = Math.min(18, maxDigits);

		// Sanity check
		if (maxDigits < minDigits)
		{
			maxDigits = minDigits;
		}

		return ConjuringUtils.randomInteger(minDigits, maxDigits);
	}

	@Override
	public String getDescription()
	{
		return "Generates a random number, with a random length within the specified range.\n\nProduces a numeric JSON node.";
	}
}
