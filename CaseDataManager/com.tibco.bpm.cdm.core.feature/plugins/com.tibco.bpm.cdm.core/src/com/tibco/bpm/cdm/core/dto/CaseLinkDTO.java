package com.tibco.bpm.cdm.core.dto;

import java.math.BigInteger;

import com.tibco.bpm.cdm.api.dto.CaseReference;

public class CaseLinkDTO
{
	private String			name;

	private String			oppositeName;

	private CaseReference	caseReference;

	private BigInteger		typeId;

	public CaseLinkDTO(String name, CaseReference caseReference)
	{
		this.name = name;
		this.caseReference = caseReference;
	}

	public String getName()
	{
		return name;
	}

	public CaseReference getCaseReference()
	{
		return caseReference;
	}

	public BigInteger getTypeId()
	{
		return typeId;
	}

	public void setTypeId(BigInteger typeId)
	{
		this.typeId = typeId;
	}

	public String getOppositeName()
	{
		return oppositeName;
	}

	public void setOppositeName(String oppositeName)
	{
		this.oppositeName = oppositeName;
	}
}
