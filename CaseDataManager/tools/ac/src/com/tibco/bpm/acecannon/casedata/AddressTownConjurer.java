/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

public class AddressTownConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static final String[] NAMES = {"Swindon", "Bristol", "Chippenham", "Bath", "Oxford", "Faringdon",
			"Marlborough", "Devizes", "Corsham", "Kington St. Michael", "Wroughton", "Paignton", "Torquay", "Totnes",
			"Goodrington", "Manchester", "London", "Thetford", "Exeter"};

	@Override
	public String conjure()
	{
		return ConjuringUtils.randomString(NAMES);
	}
}
