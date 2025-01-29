package com.tibco.bpm.cdm.core.dto;

import java.math.BigInteger;
import java.util.Map;

import com.tibco.bpm.cdm.api.dto.CaseReference;

/**
 * DTO for a CaseReference and its casedata JSON, used for updating a persisted case. Also contains
 * a flag (set during the actual update) to indicate whether the update included a change to the state attribute.
 * @author smorgan
 * @since 2019
 */
public class CaseUpdateDTO
{
	private CaseReference		caseReference;

	private CaseReference		newCaseReference;

	private String				casedata;

	private BigInteger			oldStateId;

	private BigInteger			newStateId;

	private String				newStateValue;

	private String				newCID;

	private Map<String, String>	removals;

	public CaseUpdateDTO(CaseReference caseReference, String casedata)
	{
		this.caseReference = caseReference;
		this.casedata = casedata;
	}

	public CaseReference getCaseReference()
	{
		return caseReference;
	}

	public String getCasedata()
	{
		return casedata;
	}

	public void setCasedata(String casedata)
	{
		this.casedata = casedata;
	}

	public void setNewStateValue(String newStateValue)
	{
		this.newStateValue = newStateValue;
	}

	public String getNewStateValue()
	{
		return newStateValue;
	}

	public BigInteger getOldStateId()
	{
		return oldStateId;
	}

	public void setOldStateId(BigInteger oldStateId)
	{
		this.oldStateId = oldStateId;
	}

	public void setNewStateId(BigInteger newStateId)
	{
		this.newStateId = newStateId;
	}

	public BigInteger getNewStateId()
	{
		return newStateId;
	}

	public void setNewCID(String newCID)
	{
		this.newCID = newCID;
	}

	public String getNewCID()
	{
		return newCID;
	}

	public Map<String, String> getRemovals()
	{
		return removals;
	}

	public void setRemovals(Map<String, String> removals)
	{
		this.removals = removals;
	}

	public CaseReference getNewCaseReference()
	{
		return newCaseReference;
	}

	public void setNewCaseReference(CaseReference newCaseReference)
	{
		this.newCaseReference = newCaseReference;
	}
}
