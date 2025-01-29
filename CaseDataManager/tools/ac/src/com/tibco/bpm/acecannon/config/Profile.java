/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.config;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder({"name", "urls"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Profile implements Cloneable
{
	@JsonProperty
	private URLs						urls	= new URLs();

	@JsonProperty
	private Database					database		= new Database();

	@JsonProperty
	private Identification				identification	= new Identification();

	//	@JsonProperty
	//	private Type						type			= new Type();

	private static final ObjectMapper om = new ObjectMapper();

	public Profile copy() throws IOException
	{
		String json = om.writeValueAsString(this);
		Profile copy = om.readValue(json, Profile.class);
		return copy;
	}

	public Profile clone() throws CloneNotSupportedException
	{
		return (Profile) super.clone();
	}

	public URLs getURLs()
	{
		return urls;
	}

	public void setURLs(URLs urls)
	{
		this.urls = urls;
	}

	public void setDatabase(Database database)
	{
		this.database = database;
	}

	public void setIdentification(Identification identification)
	{
		this.identification = identification;
	}

	public Identification getIdentification()
	{
		return identification;
	}

	public Database getDatabase()
	{
		return database;
	}
	//
	//	public Type getType()
	//	{
	//		return type;
	//	}
}
