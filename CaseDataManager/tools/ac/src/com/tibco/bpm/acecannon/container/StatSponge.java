/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.ContainerManager;

public class StatSponge extends Thread
{
	public static Pattern		LINE_PAT		= Pattern.compile("(\\S+)\\s+\\S+\\s+(\\d+\\.\\d+).*");

	public static Pattern		LINE_DEAD_PAT	= Pattern.compile("(\\w+)\\s+--.*");

	// CSI (Control Sequence Introducer)
	// CSI prefix:
	// ESC[ = 27, 91

	// 27, 91, 50, 74, 
	// 27, 91, 72

	private ContainerManager	containerManager;

	private boolean				sigterm;

	public StatSponge(ContainerManager containerManager)
	{
		super("StatSponge");
		this.containerManager = containerManager;
	}

	public void term()
	{
		sigterm = true;
	}

	public void munchRow(Map<String, Double> cpus, String row)
	{
		Matcher m = LINE_PAT.matcher(row);
		if (m.matches())
		{
			String containerId = m.group(1);
			Double cpu = null;
			try
			{
				cpu = Double.valueOf(m.group(2));
				cpus.put(containerId, cpu);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			m = LINE_DEAD_PAT.matcher(row);
			if (m.matches())
			{
				String containerId = m.group(1);
				cpus.put(containerId, -1d);
			}
		}
	}

	//	private void updateCPU(String containerId, Double cpu)
	//	{
	//		if (containerManager != null)
	//		{
	//			containerManager.updateCPU(containerId, cpu);
	//		}
	//	}

	public void run()
	{
		AceMain.log("StatSponge thread started");
		DockerCLI cli = new DockerCLI();

		try
		{
			InputStream is = cli.runCommandAndGetStream("stats");

			boolean ignoreTilBreak = false;

			byte[] byt = new byte[1024];
			int bufPos = 0;

			Map<String, Double> cpus = new HashMap<String, Double>();
			int escapeCount = -2;

			while (!sigterm)
			{
				int i = is.read();

				// Payload begins with some escape sequences followed by a heading row
				if (i == 27)
				{
					escapeCount++;
					if (escapeCount >= 2)
					{
						escapeCount = 0;
						// Pass container->cpu mappings across to Manager
						containerManager.updateCPUs(cpus);
						cpus.clear();
					}
					// Start of escape sequence. Ignore until a line break (end of heading row)
					ignoreTilBreak = true;
				}
				else
				{
					// Line break -> reset flag and consume buffer content
					if (i == 10)
					{
						ignoreTilBreak = false;
						if (bufPos > 0)
						{
							munchRow(cpus, new String(byt, 0, bufPos));
						}
						bufPos = 0;
					}
					else if (!ignoreTilBreak)
					{
						byt[bufPos++] = (byte) i;
					}
				}
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AceMain.log("StatSponge thread exiting");
	}
}
