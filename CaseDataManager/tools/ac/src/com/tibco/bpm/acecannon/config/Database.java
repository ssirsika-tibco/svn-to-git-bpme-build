package com.tibco.bpm.acecannon.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"host", "user", "password", "name"})
public class Database
{
	private String	host		= "docker";

	private String	user		= "bpm_ace_user";

	private String	password	= "bpm_ace_Staff123";

	private String	name		= "postgres";

	@JsonProperty("host")
	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	@JsonProperty("user")
	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	@JsonProperty("password")
	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	@JsonProperty("name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
