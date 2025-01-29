/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Constraints on permitted values for the attribute\n
 *
 * @GENERATED this is generated code; do not edit.
 */
//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"length", "minValue", "minValueInclusive", "maxValue", "maxValueInclusive", "decimalPlaces"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TypeInfoAttributeConstraints
{

	private Integer	length				= null;

	private String	minValue			= null;

	private Boolean	minValueInclusive	= null;

	private String	maxValue			= null;

	private Boolean	maxValueInclusive	= null;

	private Integer	decimalPlaces		= null;

	/**
	 * The maximum length permitted (only applicable to attributes of the Text and FixedPointNumber base types).\n
	 */
	@JsonProperty("length")
	public Integer getLength()
	{
		return length;
	}

	public void setLength(Integer aValue)
	{
		length = aValue;
	}

	/**
	 * The minimum value allowed for the attribute (only applicable to attributes of the Number and FixedPointNumber base types).\n
	 */
	@JsonProperty("minValue")
	public String getMinValue()
	{
		return minValue;
	}

	public void setMinValue(String aValue)
	{
		minValue = aValue;
	}

	/**
	 * If true, then the value specified in minValue is inclusive (otherwise exclusive).\n
	 */
	@JsonProperty("minValueInclusive")
	public Boolean getMinValueInclusive()
	{
		return minValueInclusive;
	}

	public void setMinValueInclusive(Boolean aValue)
	{
		minValueInclusive = aValue;
	}

	/**
	 * The maximum value allowed for the attribute (only applicable to attributes of the Number and FixedPointNumber base types).\n
	 */
	@JsonProperty("maxValue")
	public String getMaxValue()
	{
		return maxValue;
	}

	public void setMaxValue(String aValue)
	{
		maxValue = aValue;
	}

	/**
	 * If true, then the value specified in minValue is inclusive (otherwise exclusive).\n
	 */
	@JsonProperty("maxValueInclusive")
	public Boolean getMaxValueInclusive()
	{
		return maxValueInclusive;
	}

	public void setMaxValueInclusive(Boolean aValue)
	{
		maxValueInclusive = aValue;
	}

	/**
	 * The maximum number of decimal places permitted (only applicable to attributes of the FixedPointNumber base type)\n
	 */
	@JsonProperty("decimalPlaces")
	public Integer getDecimalPlaces()
	{
		return decimalPlaces;
	}

	public void setDecimalPlaces(Integer aValue)
	{
		decimalPlaces = aValue;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TypeInfoAttributeConstraints[");
		sb.append(", length=").append(length);
		sb.append(", minValue=").append(minValue);
		sb.append(", minValueInclusive=").append(minValueInclusive);
		sb.append(", maxValue=").append(maxValue);
		sb.append(", maxValueInclusive=").append(maxValueInclusive);
		sb.append(", decimalPlaces=").append(decimalPlaces);
		sb.append("]");
		return sb.toString();
	}
}
