/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractConjurer<T> implements ValueConjurer<T>
{
	private Map<Option, Object> optionValues = new HashMap<Option, Object>();

	@Override
	public List<T> conjureMany(int quantity)
	{
		List<T> list = new ArrayList<>();
		for (int i = 0; i < quantity; i++)
		{
			list.add(conjure());
		}
		return list;
	}

	@Override
	public List<Option> getOptions()
	{
		return null;
	}
	
	@Override
	public Map<Option, Object> getOptionValues()
	{
		return optionValues;
	}
	
	@Override
	public String getDescription()
	{
		return null;
	}
}
