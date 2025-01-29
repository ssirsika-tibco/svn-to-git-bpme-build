package com.tibco.bpm.acecannon.casedata;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class FixedPointNumberConjurer extends AbstractConjurer<BigDecimal> implements ValueConjurer<BigDecimal>
{
	public static Option	optionLength			= new Option(OptionType.INTEGER, "length", "Length");

	public static Option	optionDecimalPlaces		= new Option(OptionType.INTEGER, "decimalPlaces", "Decimal places");

	public static Option	optionMinValue			= new Option(OptionType.BIG_DECIMAL, "minValue", "Min. Value");

	public static Option	optionMinValueInclusive	= new Option(OptionType.BOOLEAN, "minValueInclusive",
			"Min. Value Inclusive");

	public static Option	optionMaxValue			= new Option(OptionType.BIG_DECIMAL, "maxValue", "Max. Value");

	public static Option	optionMaxValueInclusive	= new Option(OptionType.BOOLEAN, "maxValueInclusive",
			"Max. Value Inclusive");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionLength, optionDecimalPlaces, optionMinValue, optionMinValueInclusive,
				optionMaxValue, optionMaxValueInclusive);
	}

	@Override
	public BigDecimal conjure()
	{
		int length = getOptionValues().containsKey(optionLength) ? (int) getOptionValues().get(optionLength) : 15;
		int decimalPlaces = getOptionValues().containsKey(optionDecimalPlaces)
				? (int) getOptionValues().get(optionDecimalPlaces)
				: 0;

		BigDecimal minValue = getOptionValues().containsKey(optionMinValue) ? (BigDecimal)getOptionValues().get(optionMinValue)
				: null;

		BigDecimal maxValue = getOptionValues().containsKey(optionMinValue) ? (BigDecimal)getOptionValues().get(optionMaxValue)
				: null;

		boolean minValueInclusive = false;
		Boolean minValueInclusiveOpt = (Boolean) getOptionValues().get(optionMinValueInclusive);
		if (minValueInclusiveOpt != null && minValueInclusiveOpt)
		{
			minValueInclusive = true;
		}

		boolean maxValueInclusive = false;
		Boolean maxValueInclusiveOpt = (Boolean) getOptionValues().get(optionMaxValueInclusive);
		if (maxValueInclusiveOpt != null && maxValueInclusiveOpt)
		{
			maxValueInclusive = true;
		}

		if (length > 15)
		{
			length = 15;
		}
		if (length < 0)
		{
			length = 0;
		}
		if (decimalPlaces > 15)
		{
			decimalPlaces = 15;
		}
		if (decimalPlaces < 0)
		{
			decimalPlaces = 0;
		}
		if (decimalPlaces > length)
		{
			decimalPlaces = length;
		}
		
		StringBuilder buf = new StringBuilder();
		int maxLeftSig = length - decimalPlaces;
		
		// To be helpful, if maxLeftSig exceeds the maxValue, cap it.
		if (maxValue != null)
		{
			int maxValueLeftDigits = maxValue.toBigInteger().toString().length();
			if (maxLeftSig > maxValueLeftDigits)
			{
				maxLeftSig = maxValueLeftDigits;
			}
		}
		
		if (maxLeftSig == 0)
		{
			// All 15 significant digits used up on the right, so can only have zero on the left.
			buf.append("0");
		}
		else
		{
			buf.append(ConjuringUtils.randomInteger(0, maxLeftSig));
		}
		buf.append(".");
		for (int i = 0; i < decimalPlaces; i++)
		{
			buf.append(ConjuringUtils.randBetween(1, 9));
		}
		BigDecimal bd = new BigDecimal(buf.toString());
		if (minValue != null)
		{
			if (minValueInclusive)
			{
				if (bd.compareTo(minValue) < 0)
				{
					bd = minValue;
				}
			}
			else
			{
				if (bd.compareTo(minValue) <= 0)
				{
					// +1 not ideal...
					bd = minValue.add(BigDecimal.ONE);
				}
			}
		}
		if (maxValue != null)
		{
			if (maxValueInclusive)
			{
				if (bd.compareTo(maxValue) > 0)
				{
					bd = maxValue;
				}
			}
			else
			{
				if (bd.compareTo(maxValue) >= 0)
				{
					// -1 not ideal...
					bd = maxValue.subtract(BigDecimal.ONE);
				}
			}
		}
		return bd;
	}
}
