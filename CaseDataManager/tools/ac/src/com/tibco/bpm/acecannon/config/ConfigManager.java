/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tibco.bpm.acecannon.network.CookieHolder;

public class ConfigManager implements CookieHolder
{
	public static ConfigManager	INSTANCE	= new ConfigManager();

	private Config				config;

	private ConfigManager()
	{
	}

	public void readConfigFromFile(File f) throws IOException
	{
		config = Config.readFromFile(f);
	}

	public void writeConfigToFile(File f) throws IOException
	{
		config.writeToFile(f);
	}

	public String serializeConfig() throws JsonProcessingException
	{
		return config.serialize();
	}

	public void setConfig(Config config)
	{
		this.config = config;
	}

	public Config getConfig()
	{
		return config;
	}

	public Profile getActiveProfile()
	{
		return config.getActiveProfile();
	}

	//	public String getSubscriptionId()
	//	{
	//		return getActiveProfile().getIdentification().getSubscriptionId();
	//	}
	//
	//	public void setSubscriptionId(String subscriptionId)
	//	{
	//		getActiveProfile().getIdentification().setSubscriptionId(subscriptionId);
	//	}
	//
	//	public String getSandboxId()
	//	{
	//		return getActiveProfile().getIdentification().getSandboxId();
	//	}
	//
	//	public void setSandboxId(String sandboxId)
	//	{
	//		getActiveProfile().getIdentification().setSandboxId(sandboxId);
	//	}
	//
	public String getCookie()
	{
		return null;
		//		return getActiveProfile().getIdentification().getCookie();
	}

	public void setCookie(String cookie)
	{
		//		getActiveProfile().getIdentification().setCookie(cookie);
	}
}
