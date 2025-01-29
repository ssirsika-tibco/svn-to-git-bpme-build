package com.tibco.bpm.cdm.core.dto;

import java.math.BigInteger;

import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;

public class LinkDTO
{
	private BigInteger			id;

	private BigInteger			end1TypeId;

	private String				end1Name;

	private boolean				end1IsArray;

	private QualifiedTypeName	end1TypeQTN;

	private Integer				end1TypeMajorVersion;

	private BigInteger			end2TypeId;

	private String				end2Name;

	private boolean				end2IsArray;

	private QualifiedTypeName	end2TypeQTN;

	private Integer				end2TypeMajorVersion;

	public BigInteger getId()
	{
		return id;
	}

	public void setId(BigInteger id)
	{
		this.id = id;
	}

	public BigInteger getEnd1TypeId()
	{
		return end1TypeId;
	}

	public void setEnd1TypeId(BigInteger end1TypeId)
	{
		this.end1TypeId = end1TypeId;
	}

	public String getEnd1Name()
	{
		return end1Name;
	}

	public void setEnd1Name(String end1Name)
	{
		this.end1Name = end1Name;
	}

	public boolean isEnd1IsArray()
	{
		return end1IsArray;
	}

	public void setEnd1IsArray(boolean end1IsArray)
	{
		this.end1IsArray = end1IsArray;
	}

	public QualifiedTypeName getEnd1TypeQTN()
	{
		return end1TypeQTN;
	}

	public void setEnd1TypeQTN(QualifiedTypeName end1TypeQTN)
	{
		this.end1TypeQTN = end1TypeQTN;
	}

	public Integer getEnd1TypeMajorVersion()
	{
		return end1TypeMajorVersion;
	}

	public void setEnd1TypeMajorVersion(Integer end1TypeMajorVersion)
	{
		this.end1TypeMajorVersion = end1TypeMajorVersion;
	}

	public BigInteger getEnd2TypeId()
	{
		return end2TypeId;
	}

	public void setEnd2TypeId(BigInteger end2TypeId)
	{
		this.end2TypeId = end2TypeId;
	}

	public String getEnd2Name()
	{
		return end2Name;
	}

	public void setEnd2Name(String end2Name)
	{
		this.end2Name = end2Name;
	}

	public boolean isEnd2IsArray()
	{
		return end2IsArray;
	}

	public void setEnd2IsArray(boolean end2IsArray)
	{
		this.end2IsArray = end2IsArray;
	}

	public QualifiedTypeName getEnd2TypeQTN()
	{
		return end2TypeQTN;
	}

	public void setEnd2TypeQTN(QualifiedTypeName end2TypeQTN)
	{
		this.end2TypeQTN = end2TypeQTN;
	}

	public Integer getEnd2TypeMajorVersion()
	{
		return end2TypeMajorVersion;
	}

	public void setEnd2TypeMajorVersion(Integer end2TypeMajorVersion)
	{
		this.end2TypeMajorVersion = end2TypeMajorVersion;
	}
}
