/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

public class ContainerInfo
{
	private String	id;

	private String	name;

	private Integer	debugPort;

	private Integer	httpPort;

	private String	status;

	private int		restartCount;

	private String	created;

	private String	started;

	private String	ip;

	private String	version;

	private String	inspection;

	private String	image;

	public ContainerInfo(String id, String name, Integer debugPort, Integer httpPort, String status, int restartCount,
			String created, String started, String ip, String image, String version, String inspection)
	{
		this.id = id;
		this.name = name;
		this.debugPort = debugPort;
		this.httpPort = httpPort;
		this.status = status;
		this.restartCount = restartCount;
		this.created = created;
		this.started = started;
		this.ip = ip;
		this.image = image;
		this.version = version;
		this.inspection = inspection;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public Integer getDebugPort()
	{
		return debugPort;
	}

	public Integer getHttpPort()
	{
		return httpPort;
	}

	public String getStatus()
	{
		return status;
	}

	public int getRestartCount()
	{
		return restartCount;
	}

	public String getCreated()
	{
		return created;
	}

	public String getStarted()
	{
		return started;
	}

	public String getIp()
	{
		return ip;
	}

	public String getImage()
	{
		return image;
	}

	public String getVersion()
	{
		return version;
	}

	public String getInspection()
	{
		return inspection;
	}

	public String toString()
	{
		return "Id: " + id + ", Name: " + name + ", Debug Port: " + debugPort + ", HTTP Port: " + httpPort;
	}

}
