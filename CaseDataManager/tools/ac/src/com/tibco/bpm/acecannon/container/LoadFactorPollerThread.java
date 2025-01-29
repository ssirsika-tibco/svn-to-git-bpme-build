package com.tibco.bpm.acecannon.container;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.ContainerManager;
import com.tibco.bpm.acecannon.LineChartPane;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

public class LoadFactorPollerThread extends Thread
{
	private static final ObjectMapper	om		= new ObjectMapper();

	ContainerManager					conMan;

	private String						containerId;

	private String						url;

	private boolean						term	= false;

	private String						containerName;

	// TODO Better to make this callback generic
	private LineChartPane				lcp;

	public LoadFactorPollerThread(ContainerManager conMan, String containerId, String containerName, String url,
			LineChartPane lcp)
	{
		super("lfp-" + containerName);
		this.containerName = containerName;
		this.conMan = conMan;
		this.containerId = containerId;
		this.url = url;
		this.lcp = lcp;
	}

	public String getContainerId()
	{
		return containerId;
	}

	public String getContainerName()
	{
		return containerName;
	}

	public void run()
	{
		AceMain.log("Starting loadFactor poller with URL: " + url);
		while (!term)
		{
			HTTPCaller caller = HTTPCaller.newGet(url, ConfigManager.INSTANCE.getCookie());
			try
			{
				caller.call();
				String resp = caller.getResponseBody();
				JsonNode jsonNode = om.readTree(resp);
				JsonNode lfNode = jsonNode.at("/LoadFactor");
				if (lfNode != null)
				{
					String lfString = lfNode.asText();
					try
					{
						Integer lfInteger = Integer.parseInt(lfString);
						if (lfInteger != null)
						{
							// Convert 0-100 range to 0-1 range 
							Double lfDouble = new Double(lfInteger) / 100.0d;
							// If this returns false, the container isn't known any more, so our work is done.
							AceMain.log("Load factor = " + lfInteger);
							lcp.setValue(containerName, lfInteger);
							term = !conMan.updateLoadFactor(containerId, lfDouble);
						}
					}
					catch (NumberFormatException e)
					{
						AceMain.log("Non-numeric load factor returned: " + lfString);
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				conMan.updateLoadFactor(containerId, null);
				AceMain.log("Exiting due to exception polling " + url + ": " + e.getMessage());
				term = true;
			}

//			try
//			{
//				Integer loadFactorPollInterval = ConfigManager.INSTANCE.getConfig().getUI().getLoadFactorPollInterval();
//				Thread.sleep(loadFactorPollInterval);
//			}
//			catch (InterruptedException e)
//			{
//				// ignore
//			}
		}
		lcp.removeSeries(containerName);
		conMan.removeLFP(this);
		AceMain.log("loadFactor poller exiting");
	}

	public void term()
	{
		term = true;
	}
}
