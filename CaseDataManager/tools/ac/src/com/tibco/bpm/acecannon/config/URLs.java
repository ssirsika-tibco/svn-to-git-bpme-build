/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class URLs
{
	private String base = "http://localhost:8181";

	@JsonProperty("base")
	public String getBase()
	{
		return base;
	}

	public void setBase(String base)
	{
		this.base = base;
	}
}
