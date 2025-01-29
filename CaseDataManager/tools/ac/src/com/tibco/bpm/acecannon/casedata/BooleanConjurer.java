package com.tibco.bpm.acecannon.casedata;

public class BooleanConjurer extends AbstractConjurer<Boolean> implements ValueConjurer<Boolean>
{

	@Override
	public Boolean conjure()
	{
		return Math.random() >= 0.5;
	}

}
