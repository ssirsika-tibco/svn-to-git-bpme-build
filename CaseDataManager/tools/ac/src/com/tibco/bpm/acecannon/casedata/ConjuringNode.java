/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.tibco.bpm.da.dm.api.Attribute;

@JsonPropertyOrder({"name", "conjurer"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ConjuringNode
{
	private Attribute			targetAttribute;

	private String				name;

	private String				conjurer;

	private boolean				isArray;

	private boolean				isUser;

	private boolean				isExcluded;

	private int					arrayMinSize;

	private int					arrayMaxSize;

	@JsonProperty(value = "attributes")
	private List<ConjuringNode>	attributes	= new ArrayList<>();

	@JsonProperty(value = "options")
	private Map<String, Object>	options		= new HashMap<>();

	@JsonIgnore
	public Attribute getTargetAttribute()
	{
		return targetAttribute;
	}

	@JsonIgnore
	public void setTargetAttribute(Attribute targetAttribute)
	{
		this.targetAttribute = targetAttribute;
	}

	@JsonGetter(value = "name")
	public String getName()
	{
		return name;
	}

	@JsonSetter(value = "name")
	public void setName(String name)
	{
		this.name = name;
	}

	@JsonGetter(value = "conjurer")
	public String getConjurer()
	{
		return conjurer;
	}

	@JsonSetter(value = "conjurer")
	public void setConjurer(String conjurer)
	{
		this.conjurer = conjurer;
	}

	public Map<String, Object> getOptions()
	{
		return options;
	}

	public List<ConjuringNode> getAttributes()
	{
		return attributes;
	}

	@JsonGetter(value = "isArray")
	public boolean getIsArray()
	{
		return isArray;
	}

	@JsonSetter(value = "isArray")
	public void setIsArray(boolean isArray)
	{
		this.isArray = isArray;
	}

	@JsonGetter(value = "isUser")
	public boolean getIsUser()
	{
		return isUser;
	}

	@JsonSetter(value = "isUser")
	public void setIsUser(boolean isUser)
	{
		this.isUser = isUser;
	}

	@JsonGetter(value = "arrayMinSize")
	public int getArrayMinSize()
	{
		return arrayMinSize;
	}

	@JsonSetter(value = "arrayMinSize")
	public void setArrayMinSize(int arrayMinSize)
	{
		this.arrayMinSize = arrayMinSize;
	}

	@JsonGetter(value = "arrayMaxSize")
	public int getArrayMaxSize()
	{
		return arrayMaxSize;
	}

	@JsonSetter(value = "arrayMaxSize")
	public void setArrayMaxSize(int arrayMaxSize)
	{
		this.arrayMaxSize = arrayMaxSize;
	}

	@JsonGetter(value = "isExcluded")
	public boolean getIsExcluded()
	{
		return isExcluded;
	}

	@JsonSetter(value = "isExcluded")
	public void setIsExcluded(boolean isExcluded)
	{
		this.isExcluded = isExcluded;
	}

}
