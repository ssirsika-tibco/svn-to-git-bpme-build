/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"name", "label", "isCase", "namespace", "applicationId", "applicationMajorVersion", "dependencies",
		"attributes", "summaryAttributes", "states", "links"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfo
{

	private String						name					= null;

	private String						label					= null;

	private Boolean						isCase					= null;

	private String						namespace				= null;

	private String						applicationId			= null;

	private Integer						applicationMajorVersion	= null;

	private List<TypeInfoDependency>	dependencies			= new ArrayList<TypeInfoDependency>();

	private List<TypeInfoAttribute>		attributes				= new ArrayList<TypeInfoAttribute>();

	private List<TypeInfoAttribute>		summaryAttributes		= new ArrayList<TypeInfoAttribute>();

	private List<TypeInfoState>			states					= new ArrayList<TypeInfoState>();

	private List<TypeInfoLink>			links					= new ArrayList<TypeInfoLink>();

	/**
	 */
	@JsonProperty("name")
	public String getName()
	{
		return name;
	}

	public void setName(String aValue)
	{
		name = aValue;
	}

	/**
	 */
	@JsonProperty("label")
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String aValue)
	{
		label = aValue;
	}

	/**
	 */
	@JsonProperty("isCase")
	public Boolean getIsCase()
	{
		return isCase;
	}

	public void setIsCase(Boolean aValue)
	{
		isCase = aValue;
	}

	/**
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

	/**
	 * Describes data models in other applications on which this type depends. \n
	 */
	@JsonProperty("dependencies")
	public List<TypeInfoDependency> getDependencies()
	{
		return dependencies;
	}

	public void setDependencies(List<TypeInfoDependency> aValue)
	{
		dependencies = aValue;
	}

	/**
	 * The definitions of all attributes defined in the Type.\n
	 */
	@JsonProperty("attributes")
	public List<TypeInfoAttribute> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<TypeInfoAttribute> aValue)
	{
		attributes = aValue;
	}

	/**
	 * The definitions of all attributes that are included in the Case summary for the Type.\n
	 */
	@JsonProperty("summaryAttributes")
	public List<TypeInfoAttribute> getSummaryAttributes()
	{
		return summaryAttributes;
	}

	public void setSummaryAttributes(List<TypeInfoAttribute> aValue)
	{
		summaryAttributes = aValue;
	}

	/**
	 * The definitions of all states defined in the Type.
	 */
	@JsonProperty("states")
	public List<TypeInfoState> getStates()
	{
		return states;
	}

	public void setStates(List<TypeInfoState> aValue)
	{
		states = aValue;
	}

	/**
	 * The definition of all links for the Type.\n
	 */
	@JsonProperty("links")
	public List<TypeInfoLink> getLinks()
	{
		return links;
	}

	public void setLinks(List<TypeInfoLink> aValue)
	{
		links = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfo[");
		sb.append(", name=").append(name);
		sb.append(", label=").append(label);
		sb.append(", isCase=").append(isCase);
		sb.append(", namespace=").append(namespace);
		sb.append(", applicationId=").append(applicationId);
		sb.append(", applicationMajorVersion=").append(applicationMajorVersion);
		sb.append(", dependencies=").append(dependencies);
		sb.append(", attributes=").append(attributes);
		sb.append(", summaryAttributes=").append(summaryAttributes);
		sb.append(", states=").append(states);
		sb.append(", links=").append(links);
		sb.append("]");
		return sb.toString();
	}
}
