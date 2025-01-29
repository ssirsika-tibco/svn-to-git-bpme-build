package com.tibco.bpm.cdm.core.dto;

import java.math.BigInteger;
import java.util.Calendar;

import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;

/**
 * DTO for transferring info about a case
 * @author smorgan
 * @since 2019
 */
public class CaseInfoDTO
{

	private BigInteger			id;

	private Integer				version;

	private String				casedata;

	private String				summary;

	private String				createdBy;

	private Calendar			creationTimestamp;

	private String				modifiedBy;

	private Calendar			modificationTimestamp;

	private QualifiedTypeName	typeName;

	private int					majorVersion;

	public CaseInfoDTO()
	{
	}

	public BigInteger getId()
	{
		return id;
	}

	public void setId(BigInteger id)
	{
		this.id = id;
	}

	public Integer getVersion()
	{
		return version;
	}

	public void setVersion(Integer version)
	{
		this.version = version;
	}

	public String getCasedata()
	{
		return casedata;
	}

	public void setCasedata(String casedata)
	{
		this.casedata = casedata;
	}

	public String getSummary()
	{
		return summary;
	}

	public void setSummary(String summary)
	{
		this.summary = summary;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}

	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	public Calendar getCreationTimestamp()
	{
		return creationTimestamp;
	}

	public void setCreationTimestamp(Calendar creationTimestamp)
	{
		this.creationTimestamp = creationTimestamp;
	}

	public String getModifiedBy()
	{
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy)
	{
		this.modifiedBy = modifiedBy;
	}

	public Calendar getModificationTimestamp()
	{
		return modificationTimestamp;
	}

	public void setModificationTimestamp(Calendar modificationTimestamp)
	{
		this.modificationTimestamp = modificationTimestamp;
	}

	public QualifiedTypeName getTypeName()
	{
		return typeName;
	}

	public void setTypeName(QualifiedTypeName typeName)
	{
		this.typeName = typeName;
	}

	public int getMajorVersion()
	{
		return majorVersion;
	}

	public void setMajorVersion(int majorVersion)
	{
		this.majorVersion = majorVersion;
	}

}
