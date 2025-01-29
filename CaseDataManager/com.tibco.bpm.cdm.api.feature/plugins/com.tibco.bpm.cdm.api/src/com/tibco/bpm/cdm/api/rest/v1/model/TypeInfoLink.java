/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a Case type&#39;s links. Used within GetTypeResponseItem\n
 *
 * @GENERATED this is generated code; do not edit.
 */
//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"name", "label", "type", "isArray"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfoLink
{

	private String	name	= null;

	private String	label	= null;

	private String	type	= null;

	private Boolean	isArray	= null;

	/**
	 * The name for the link.\n
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
	 * The label for the link.\n
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
	 * The name of the target case type.\n
	 */
	@JsonProperty("type")
	public String getType()
	{
		return type;
	}

	public void setType(String aValue)
	{
		type = aValue;
	}

	/**
	 * true if the link is allowed multiple target cases\n
	 */
	@JsonProperty("isArray")
	public Boolean getIsArray()
	{
		return isArray;
	}

	public void setIsArray(Boolean aValue)
	{
		isArray = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfoLink[");
		sb.append(", name=").append(name);
		sb.append(", label=").append(label);
		sb.append(", type=").append(type);
		sb.append(", isArray=").append(isArray);
		sb.append("]");
		return sb.toString();
	}
}
