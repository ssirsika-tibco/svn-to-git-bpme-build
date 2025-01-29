/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.TextConsumer;

public class DockerCLI implements DockerAPI
{
	private static final ObjectMapper	om			= new ObjectMapper();

	private static final String			RUN_CMD		= "cmd.exe /C \"%s\"";

	private static final String			PREFIX		= "@echo off & (FOR /f \"tokens=*\" %i IN ('docker-machine env') DO %i) & ";

	private static final Pattern		PAT_STAMP	= Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}).*");

	private static final Pattern		PAT_IMAGE	= Pattern.compile(".*:(.*)");

	private TextConsumer				logger;

	private static SimpleDateFormat		format		= new SimpleDateFormat("HH:mm:ss");

	public void setLogger(TextConsumer logger)
	{
		this.logger = logger;
	}

	protected String readInputStreamToString(InputStream inputStream) throws IOException
	{
		char[] buffer = new char[1024];
		StringBuilder buf = new StringBuilder();
		Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		int count = 0;
		while (count >= 0)
		{
			count = in.read(buffer, 0, buffer.length);
			if (count > 0)
			{
				buf.append(buffer, 0, count);
			}
		}
		return buf.toString();
	}

	private String convertStamp(String stamp)
	{
		String result = null;
		Matcher m = PAT_STAMP.matcher(stamp);
		if (m.matches())
		{
			result = String.format("%s %s", m.group(1), m.group(2));
		}
		return result;
	}

	@Override
	public List<ContainerInfo> getInfo(boolean cmOnly)
	{
		List<ContainerInfo> result = new ArrayList<>();

		// Note: This is matching the exact version, so won't be any good if we ever want
		// to support co-existing CMs of different versions. If that's the case, it should probably
		// get them all and then filter the results.
		String info = runDockerCommand("ps -a -q");

		if (!info.isEmpty())
		{
			// That has returned a line-break delimited list of container ID, so construct call
			// to 'inspect' those.
			StringBuffer cmd = new StringBuffer("inspect");
			for (String id : info.split("[\r\n]"))
			{
				cmd.append(" ");
				cmd.append(id);
			}
			String inspectionJSON = runDockerCommand(cmd.toString());

			if (inspectionJSON != null && inspectionJSON.length() != 0)
			{
				try
				{
					JsonNode inspectionRoot = om.readTree(inspectionJSON);

					if (inspectionRoot instanceof ArrayNode)
					{
						for (Iterator<JsonNode> iter = ((ArrayNode) inspectionRoot).iterator(); iter.hasNext();)
						{
							JsonNode containerNode = iter.next();
							String id = containerNode.at("/Id").asText();
							String name = containerNode.at("/Name").asText();
							// Chop off leading slash
							if (name.startsWith("/"))
							{
								name = name.substring(1);
							}
							String status = containerNode.at("/State/Status").asText();
							int restartCount = containerNode.at("/RestartCount").asInt();
							String created = convertStamp(containerNode.at("/Created").asText());
							String started = convertStamp(containerNode.at("/State/StartedAt").asText());
							String image = containerNode.at("/Config/Image").asText();
							Integer debugPort = null;
							Integer httpPort = null;

							if (image.contains("bpm-docker.emea.tibco.com:443/client")
									|| image.contains("bpm-docker.emea.tibco.com:443/runtime")
									|| image.contains("bpm-docker.emea.tibco.com:443/design"))
							{
								JsonNode ports = containerNode.at("/NetworkSettings/Ports");
								for (Iterator<String> portIter = ports.fieldNames(); portIter.hasNext();)
								{
									String portName = portIter.next();
									if (portName.equals("6661/tcp") || portName.equals("6662/tcp")
											|| portName.equals("5005/tcp"))
									{
										JsonNode portNode = ports.get(portName);
										if (portNode != null && !(portNode instanceof NullNode))
										{
											debugPort = portNode.get(0).get("HostPort").asInt();
										}
									}
									else if (portName.equals("8080/tcp"))
									{
										JsonNode portNode = ports.get(portName);
										if (portNode != null && !(portNode instanceof NullNode))
										{
											httpPort = portNode.get(0).get("HostPort").asInt();
										}
									}
									else if (portName.equals("8181/tcp"))
									{
										// Some MSs listen on 8181 rather than 8080 (e.g. AE)
										JsonNode portNode = ports.get(portName);
										if (portNode != null && !(portNode instanceof NullNode))
										{
											httpPort = portNode.get(0).get("HostPort").asInt();
										}
									}
								}
							}

							String ip = containerNode.at("/NetworkSettings/Networks/bpm-network/IPAddress").asText();

							// Extract version suffix from image name
							String version = null;
							Matcher m = PAT_IMAGE.matcher(image);
							if (m.matches())
							{
								version = m.group(1);
							}

							String containerInspection = om.writeValueAsString(containerNode);
							result.add(new ContainerInfo(id, name, debugPort, httpPort, status, restartCount, created,
									started, ip, image, version, containerInspection));
						}
					}
				}
				catch (JsonProcessingException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public String createAndStartGeneric(String name, String image, String envFile, int debugPort, int nativeDebugPort,
			int httpPort)
	{
		String containerId = null;
		containerId = runDockerCommand(
				"create --hostname " + name + " --name " + name + " --net bpm-network --restart always -p " + debugPort
						+ ":" + nativeDebugPort + " -p " + httpPort + ":8080 --env-file " + envFile + " " + image);
		if (containerId != null && !containerId.isEmpty())
		{
			containerId = containerId.trim();
			runDockerCommand("start " + containerId);
		}

		return containerId;
	}

	//	@Override
	//	public String createAndStartCM(String name, int debugPort, int httpPort)
	//	{
	//		String containerId = null;
	//		containerId = runDockerCommand(
	//				"create --hostname " + name + " --name " + name + " --net bpm-network --restart always -p " + debugPort
	//						+ ":6661 -p " + httpPort + ":8080 --env-file " + Main.CDM_ENV_FILE + " " + CM_IMAGE);
	//		if (containerId != null && !containerId.isEmpty())
	//		{
	//			containerId = containerId.trim();
	//			runDockerCommand("start " + containerId);
	//		}
	//
	//		return containerId;
	//	}
	//
	//	@Override
	//	public String createAndStartXX(String name, int debugPort, int httpPort)
	//	{
	//		String containerId = null;
	//		containerId = runDockerCommand(
	//				"create --hostname " + name + " --name " + name + " --net bpm-network --restart always -p " + debugPort
	//						+ ":6662 -p " + httpPort + ":8080 --env-file " + Main.XX_ENV_FILE + " " + XX_IMAGE);
	//		if (containerId != null && !containerId.isEmpty())
	//		{
	//			containerId = containerId.trim();
	//			runDockerCommand("start " + containerId);
	//		}
	//
	//		return containerId;
	//	}

	public InputStream runCommandAndGetStream(String command) throws IOException
	{
		log("docker " + command);
		String osName = System.getProperty("os.name");
		String line = null;
		if (osName.startsWith("Windows"))
		{
			line = String.format(RUN_CMD, PREFIX + "(docker " + command + ")");
		}
		else
		{
			line = "docker " + command;
		}
		Process p = Runtime.getRuntime().exec(line);
		InputStream inputStream = p.getInputStream();
		return inputStream;
	}

	public String runDockerCommand(String command)
	{
		try
		{
			log("docker " + command);
			String osName = System.getProperty("os.name");
			String line = null;
			if (osName.startsWith("Windows"))
			{
				line = String.format(RUN_CMD, PREFIX + "(docker " + command + ")");
			}
			else
			{
				line = "docker " + command;
			}
			Process p = Runtime.getRuntime().exec(line);
			InputStream inputStream = p.getInputStream();
			String msg = readInputStreamToString(inputStream);
			InputStream errorStream = p.getErrorStream();
			String errorMsg = readInputStreamToString(errorStream);
			if (errorMsg != null & !errorMsg.isEmpty())
			{
				logError(errorMsg);
			}
			return msg;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		}
	}

	private void log(String msg)
	{
		msg = msg.trim();
		Calendar stamp = Calendar.getInstance();
		msg = format.format(stamp.getTime()) + "  " + msg;
		AceMain.log(msg);
		if (logger != null)
		{
			logger.consume(msg);
		}
	}

	private void logError(String msg)
	{
		log(msg);
	}

	@Override
	public void pause(String id)
	{
		runDockerCommand("pause " + id);
	}

	public void unpause(String id)
	{
		runDockerCommand("unpause " + id);
	}

	public void start(String id)
	{
		runDockerCommand("start " + id);
	}

	public void stop(String id)
	{
		runDockerCommand("stop " + id);
	}

	public void rm(String id)
	{
		runDockerCommand("rm " + id);
	}
}
