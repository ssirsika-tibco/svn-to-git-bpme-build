/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

public class FingerprintConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static int			number	= 0;

	private static final String	FORMAT	= "CC:%d-%d";

	@Override
	public String conjure()
	{
		//		String uuid = UUID.randomUUID().toString();
		return String.format(FORMAT, System.currentTimeMillis() % 1_000_000, ++number);
	}

	public String getDescription()
	{
		return "Generates a unique value that Case Cannon can use to search for a case it has created via UP "
				+ "(given that invoking a Case Creator gives no way of determining the resulting case reference)\n\n"
				+ "NOTE: Must be assigned to a searchable attribute (look out for a tiny magnifying glass in the 'Type' column).";
	}
}
