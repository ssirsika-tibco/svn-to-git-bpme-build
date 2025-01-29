/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a Case type&#39;s states. Used within GetTypeResponseItem\n
 *
 * @GENERATED this is generated code; do not edit.
 */
//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"label", "value", "isTerminal"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfoState
{

	private String	label		= null;

	private String	value		= null;

	private Boolean	isTerminal	= null;

	/**
	 * The label for the state.\n
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
	 * The value for the state (as it appears within casedata).\n
	 */
	@JsonProperty("value")
	public String getValue()
	{
		return value;
	}

	public void setValue(String aValue)
	{
		value = aValue;
	}

	/**
	 * true if the state is a 'terminal' state (one in which Cases are no longer considered active).\n
	 */
	@JsonProperty("isTerminal")
	public Boolean getIsTerminal()
	{
		return isTerminal;
	}

	public void setIsTerminal(Boolean aValue)
	{
		isTerminal = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfoState[");
		sb.append(", label=").append(label);
		sb.append(", value=").append(value);
		sb.append(", isTerminal=").append(isTerminal);
		sb.append("]");
		return sb.toString();
	}
}
