/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

public class PrefixedNumberConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static Option	optionPrefix	= new Option(OptionType.TEXT, "prefix", "Prefix");

	public static Option	optionDigits	= new Option(OptionType.INTEGER, "digits", "Number of digits");

	public static Option	optionPadNumber	= new Option(OptionType.BOOLEAN, "padNumber", "Zero-pad number");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionPrefix, optionDigits, optionPadNumber);
	}

	@Override
	public String conjure()
	{
		StringBuilder buf = new StringBuilder();
		String prefix = (String) getOptionValues().get(optionPrefix);
		if (prefix != null)
		{
			buf.append(prefix);
		}
		int numDigits = 1;
		Integer digits = (Integer) getOptionValues().get(optionDigits);
		if (digits != null)
		{
			numDigits = digits;
		}
		numDigits = Math.max(1, Math.min(18, numDigits));
		StringBuilder number = new StringBuilder(Long.toString(ConjuringUtils.randomInteger(1, numDigits)));
		Boolean pad = (Boolean) getOptionValues().get(optionPadNumber);
		while (pad != null && pad && number.length() < numDigits)
		{
			number.insert(0, "0");
		}
		buf.append(number);
		return buf.toString();
	}
}
