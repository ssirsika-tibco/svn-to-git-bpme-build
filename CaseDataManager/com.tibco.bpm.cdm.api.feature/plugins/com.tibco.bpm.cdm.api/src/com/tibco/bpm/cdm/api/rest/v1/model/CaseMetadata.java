/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"createdBy", "creationTimestamp", "modifiedBy", "modificationTimestamp"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CaseMetadata
{

	private String	createdBy				= null;

	private String	creationTimestamp		= null;

	private String	modifiedBy				= null;

	private String	modificationTimestamp	= null;

	/**
	 * The GUID for the user that created the Case.
	 */
	@JsonProperty("createdBy")
	public String getCreatedBy()
	{
		return createdBy;
	}

	public void setCreatedBy(String aValue)
	{
		createdBy = aValue;
	}

	/**
	 * The date/time at which the Case was created.
	 */
	@JsonProperty("creationTimestamp")
	public String getCreationTimestamp()
	{
		return creationTimestamp;
	}

	public void setCreationTimestamp(String aValue)
	{
		creationTimestamp = aValue;
	}

	/**
	 * The GUID for the user that last modified the Case.
	 */
	@JsonProperty("modifiedBy")
	public String getModifiedBy()
	{
		return modifiedBy;
	}

	public void setModifiedBy(String aValue)
	{
		modifiedBy = aValue;
	}

	/**
	 * The date/time at which the Case was last modified.
	 */
	@JsonProperty("modificationTimestamp")
	public String getModificationTimestamp()
	{
		return modificationTimestamp;
	}

	public void setModificationTimestamp(String aValue)
	{
		modificationTimestamp = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("CaseMetadata[");
		sb.append(", createdBy=").append(createdBy);
		sb.append(", creationTimestamp=").append(creationTimestamp);
		sb.append(", modifiedBy=").append(modifiedBy);
		sb.append(", modificationTimestamp=").append(modificationTimestamp);
		sb.append("]");
		return sb.toString();
	}
}
