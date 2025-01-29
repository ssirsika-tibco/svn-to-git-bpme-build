/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoAttributeConstraints;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes the attributes of a Case or Structured type. Used within TypeInfo.\n
 *
 * @GENERATED this is generated code; do not edit.
 */
//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"name", "label", "type", "isStructuredType", "isIdentifier", "isAutoIdentifier", "isState",
		"isArray", "isMandatory", "isSearchable", "isSummary", "constraints"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfoAttribute
{

	private String							name				= null;

	private String							label				= null;

	private String							type				= null;

	private Boolean							isStructuredType	= null;

	private Boolean							isIdentifier		= null;

	private Boolean							isAutoIdentifier	= null;

	private Boolean							isState				= null;

	private Boolean							isArray				= null;

	private Boolean							isMandatory			= null;

	private Boolean							isSearchable		= null;
	
	private Boolean isSummary = null;

	private TypeInfoAttributeConstraints	constraints			= null;

	/**
	 * The name of the attribute.\n
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
	 * The label for the attribute.\n
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
	 * If the attribute refers to a structured type, the name of that type (fully qualified if it resides in\na different namespace), or a base type name ('Text', 'Number', etc.). If the former, then\nsibling property isStructuredType=true will also be set.\n
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
	 * If true, this indicates that the value for 'type' is the name of a Structured Type.\n
	 */
	@JsonProperty("isStructuredType")
	public Boolean getIsStructuredType()
	{
		return isStructuredType;
	}

	public void setIsStructuredType(Boolean aValue)
	{
		isStructuredType = aValue;
	}

	/**
	 * true if the attribute is the Type's designated 'identifier' attribute.\n
	 */
	@JsonProperty("isIdentifier")
	public Boolean getIsIdentifier()
	{
		return isIdentifier;
	}

	public void setIsIdentifier(Boolean aValue)
	{
		isIdentifier = aValue;
	}

	/**
	 * true if the containing type is configured to auto-generation of identifiers \n(i.e. implying that this attribute should not be supplied when creating a case)\n
	 */
	@JsonProperty("isAutoIdentifier")
	public Boolean getIsAutoIdentifier()
	{
		return isAutoIdentifier;
	}

	public void setIsAutoIdentifier(Boolean aValue)
	{
		isAutoIdentifier = aValue;
	}

	/**
	 * true if the attribute is the Type's designated 'state' attribute.\n
	 */
	@JsonProperty("isState")
	public Boolean getIsState()
	{
		return isState;
	}

	public void setIsState(Boolean aValue)
	{
		isState = aValue;
	}

	/**
	 * true if the attribute is an array.\n
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

	/**
	 * true if the attribute must be populated\n
	 */
	@JsonProperty("isMandatory")
	public Boolean getIsMandatory()
	{
		return isMandatory;
	}

	public void setIsMandatory(Boolean aValue)
	{
		isMandatory = aValue;
	}

	/**
	 * true is the attribute is searchable\n
	 */
	@JsonProperty("isSearchable")
	public Boolean getIsSearchable()
	{
		return isSearchable;
	}

	public void setIsSearchable(Boolean aValue)
	{
		isSearchable = aValue;
	}

	/**
	 * true is the attribute is summary\n
	 */
	@JsonProperty("isSummary")
	public Boolean getIsSummary()
	{
		return isSummary;
	}

	public void setIsSummary(Boolean aValue)
	{
		isSummary = aValue;
	}

	/**
	 */
	@JsonProperty("constraints")
	public TypeInfoAttributeConstraints getConstraints()
	{
		return constraints;
	}

	public void setConstraints(TypeInfoAttributeConstraints aValue)
	{
		constraints = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfoAttribute[");
		sb.append(", name=").append(name);
		sb.append(", label=").append(label);
		sb.append(", type=").append(type);
		sb.append(", isStructuredType=").append(isStructuredType);
		sb.append(", isIdentifier=").append(isIdentifier);
		sb.append(", isAutoIdentifier=").append(isAutoIdentifier);
		sb.append(", isState=").append(isState);
		sb.append(", isArray=").append(isArray);
		sb.append(", isMandatory=").append(isMandatory);
		sb.append(", isSearchable=").append(isSearchable);
		sb.append(", constraints=").append(constraints);
		sb.append("]");
		return sb.toString();
	}
}
