/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface CaseProvider
{
	public static class Gubbins 
	{
		private String casedata;
		
		public Gubbins(String casedata)
		{
			this.casedata = casedata;
		}
		
		public String getCasedata()
		{
			return casedata;
		}
	}
	
	// Returns a new casedata with the appropriate attribute set to hold the provided
	// unique id.  An attribute should be populate with zero; This will
	// later be changed by setVersion.
	public Gubbins buildCase(BigInteger uniqueId) throws Exception;

	// Modified the supplied version to set whichever attribute is designated as the
	// 'version' to be the supplied number.
	public String setVersion(String oldCasedata, int version);

	// Provides the app id of the application to which the casedata corresponds
	public BigInteger getAppId();

	public String incrementVersion(String oldCasedata) throws JsonProcessingException, IOException;
}
