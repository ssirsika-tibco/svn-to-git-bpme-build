package com.tibco.bpm.cdm.api.dto;

import java.math.BigInteger;

import com.tibco.bpm.cdm.api.exception.ReferenceException;

/**
 * Contains a case reference and corresponding casedata.
 * @author smorgan
 * @since 2019
 */
public class CaseInfo
{
	private CaseReference	reference;

	private String			casedata;

	private CaseInfo(CaseReference reference, String casedata)
	{
		this.reference = reference;
		this.casedata = casedata;
	}

	public static CaseInfo makeWithCasedata(CaseReference reference, String casedata)
	{
		return new CaseInfo(reference, casedata);
	}

	public static CaseInfo makeWithCasedata(BigInteger id, QualifiedTypeName type, int majorVersion, int version,
			String casedata) throws ReferenceException
	{
		return new CaseInfo(new CaseReference(type, majorVersion, id, version), casedata);
	}

	public CaseReference getReference()
	{
		return reference;
	}

	public String getCasedata()
	{
		return casedata;
	}
}
