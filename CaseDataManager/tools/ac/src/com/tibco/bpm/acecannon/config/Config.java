/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Config
{
	@JsonProperty
	private UI							ui;

	@JsonProperty
	private Map<String, Profile>		profiles;

	@JsonProperty
	private String						activeProfileName;

	@JsonIgnore
	private Profile						activeProfile;

	private static final ObjectMapper	om	= new ObjectMapper();

	static
	{
		om.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public Config()
	{
		ui = new UI();
	}
	
	public static Config readFromFile(File f) throws IOException
	{
		Config config = om.readValue(f, Config.class);
		if (config.ui == null)
		{
			config.ui = new UI();
		}
		return config;
	}

	public void writeToFile(File f) throws IOException
	{
		PrintWriter pw = new PrintWriter(f);
		pw.print(serialize());
		pw.flush();
		pw.close();
	}

	public Profile getActiveProfile()
	{
		return activeProfile;
	}

	//	public UI getUI()
	//	{
	//		return ui;
	//	}

	public Profile getProfile(String name)
	{
		if (!profiles.containsKey(name))
		{
			throw new IllegalArgumentException("No Profile named " + name);
		}
		return profiles.get(name);
	}

	public void setActiveProfile(String name)
	{
		activeProfile = getProfile(name);
		activeProfileName = name;
	}

	public void cloneProfile(String name, String newName) throws IOException
	{
		Profile oldProfile = getProfile(name);
		Profile newProfile = (Profile) oldProfile.copy();
		profiles.put(newName, newProfile);
	}

	public void addProfile(String name)
	{
		Profile newProfile = new Profile();
		profiles.put(name, newProfile);

	}

	public String serialize() throws JsonProcessingException
	{
		return om.writeValueAsString(this);
	}

	public static Config deserialize(String json) throws IOException
	{
		return om.readValue(json, Config.class);
	}

	@JsonIgnore
	public List<String> getProfileNames()
	{
		return new ArrayList<>(profiles.keySet());
	}

	@JsonIgnore
	public String getActiveProfileName()
	{
		return activeProfileName;
	}

	public void renameProfile(String name, String newName)
	{
		profiles.put(newName, profiles.get(name));
		profiles.remove(name);
	}

	public void deleteProfile(String name)
	{
		profiles.remove(name);
	}
	
	public UI getUI()
	{
		return ui;
	}
}
