/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonPropertyOrder({"applicationId", "typeId", "fingerprintAttributeName"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ConjuringModel
{
	private String				applicationId;

	private String				typeId;

	private String				fingerprintAttributeName;

	@JsonProperty(value = "attributes")
	private List<ConjuringNode>	attributes	= new ArrayList<>();

	@JsonGetter(value = "applicationId")
	public String getApplicationId()
	{
		return applicationId;
	}

	@JsonSetter(value = "applicationId")
	public void setApplicationId(String applicationId)
	{
		this.applicationId = applicationId;
	}

	@JsonGetter(value = "typeId")
	public String getTypeId()
	{
		return typeId;
	}

	@JsonSetter(value = "typeId")
	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}

	public List<ConjuringNode> getAttributes()
	{
		return attributes;
	}

	public String getFingerprintAttributeName()
	{
		return fingerprintAttributeName;
	}

	public void setFingerprintAttributeName(String fingerprintAttributeName)
	{
		this.fingerprintAttributeName = fingerprintAttributeName;
	}
}
