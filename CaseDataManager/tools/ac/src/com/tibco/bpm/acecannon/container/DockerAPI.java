/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

import java.util.List;

public interface DockerAPI
{
	//	public static final String	CM_IMAGE	= Main.CDM_IMAGE;
	//
	//	public static final String	XX_IMAGE	= Main.XX_IMAGE;

	public List<ContainerInfo> getInfo(boolean cmOnly);

	public String createAndStartGeneric(String name, String image, String envFile, int debugPort, int nativeDebugPort,
			int httpPort);

	//	public String createAndStartCM(String name, int debugPort, int httpPort);
	//
	//	public String createAndStartXX(String name, int debugPort, int httpPort);

	public void pause(String id);

	public void unpause(String id);

	public void start(String id);

	public void stop(String id);

	public void rm(String id);
}
