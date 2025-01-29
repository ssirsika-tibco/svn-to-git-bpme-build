/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FixedValuesConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static final String	ADD_NEW			= "(double-click here to add new)";

	public static Option		optionValues	= new Option(OptionType.TEXT_LIST, "values", "Values");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionValues);
	}

	@Override
	public String conjure()
	{
		Object object = getOptionValues().get(optionValues);
		@SuppressWarnings("unchecked")
		// Copy list so we can edit it
		List<String> values = new ArrayList<>((List<String>) object);
		// Remove blank rows and the special 'add' row
		values.removeIf(v -> v.equals("") || v.equals(ADD_NEW));
		return values.size() != 0 ? ConjuringUtils.randomString(values) : "";
	}
}
