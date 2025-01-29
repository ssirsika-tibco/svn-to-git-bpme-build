/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;
public class AddressPostcodeConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	@Override
	public String conjure()
	{
		StringBuilder buf = new StringBuilder(ConjuringUtils.randomCapitalLetter());
		if (Math.random() > 0.2d)
		{
			buf.append(ConjuringUtils.randomCapitalLetter());
		}
		buf.append(ConjuringUtils.randomInteger(1, 2));
		buf.append(" ");
		buf.append(ConjuringUtils.randomInteger(1, 1));
		buf.append(ConjuringUtils.randomCapitalLetter());
		buf.append(ConjuringUtils.randomCapitalLetter());
		return buf.toString();
	}
}
