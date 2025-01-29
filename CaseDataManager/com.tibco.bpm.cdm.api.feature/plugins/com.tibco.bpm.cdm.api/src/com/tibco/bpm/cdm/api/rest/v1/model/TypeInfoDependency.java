/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"namespace", "applicationId", "applicationMajorVersion"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfoDependency
{

	private String	namespace				= null;

	private String	applicationId			= null;

	private Integer	applicationMajorVersion	= null;

	/**
	 * Namespace of the data model in another application\n
	 */
	@JsonProperty("namespace")
	public String getNamespace()
	{
		return namespace;
	}

	public void setNamespace(String aValue)
	{
		namespace = aValue;
	}

	/**
	 * Application id for the other application\n
	 */
	@JsonProperty("applicationId")
	public String getApplicationId()
	{
		return applicationId;
	}

	public void setApplicationId(String aValue)
	{
		applicationId = aValue;
	}

	/**
	 * Application major version for the other application\n
	 */
	@JsonProperty("applicationMajorVersion")
	public Integer getApplicationMajorVersion()
	{
		return applicationMajorVersion;
	}

	public void setApplicationMajorVersion(Integer aValue)
	{
		applicationMajorVersion = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfoDependency[");
		sb.append(", namespace=").append(namespace);
		sb.append(", applicationId=").append(applicationId);
		sb.append(", applicationMajorVersion=").append(applicationMajorVersion);
		sb.append("]");
		return sb.toString();
	}
}
