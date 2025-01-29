package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

/**
 * Generated UK format phone numbers
 * @author smorgan
 *
 */
public class PhoneNumberUKConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static Option	optionWhitespace	= new Option(OptionType.BOOLEAN, "includeWhitespace",
			"Include whitespace");

	public static Option	optionCountryCode	= new Option(OptionType.BOOLEAN, "includeCountryCode",
			"Include country code");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionWhitespace, optionCountryCode);
	}

	@Override
	public String conjure()
	{
		StringBuilder buf = new StringBuilder();
		Boolean includeCountryCode = (Boolean) getOptionValues().get(optionCountryCode);
		Boolean includeWhitespace = (Boolean) getOptionValues().get(optionWhitespace);
		buf.append(includeCountryCode != null && includeCountryCode
				? "+44" + (includeWhitespace != null && includeWhitespace ? " " : "") : "0");
		double random = Math.random();
		if (random > 0.7)
		{
			buf.append(7);
			buf.append(ConjuringUtils.randomInteger(3, 3));
		}
		else if (random > 0.3)
		{
			buf.append(2);
			buf.append(ConjuringUtils.randomInteger(2, 3));
		}
		else
		{
			buf.append(1);
			buf.append(ConjuringUtils.randomInteger(3, 3));
		}
		if (includeWhitespace != null && includeWhitespace)
		{
			buf.append(" ");
		}
		buf.append(ConjuringUtils.randomInteger(6, 6));
		return buf.toString();
	}
}
