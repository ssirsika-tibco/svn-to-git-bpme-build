/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ContainerInfoBean
{
	private StringProperty	idProperty;

	private StringProperty	nameProperty;

	private IntegerProperty	httpPortProperty;

	private IntegerProperty	debugPortProperty;

	private StringProperty	statusProperty;

	private IntegerProperty	restartCountProperty;

	private StringProperty	createdProperty;

	private StringProperty	startedProperty;

	// Range 0 - 1
	private DoubleProperty	cpuProperty;

	// Range 0 - 1
	private DoubleProperty	loadFactorProperty;

	private StringProperty	ipProperty;

	private StringProperty	versionProperty;

	private StringProperty	inspectionProperty;

	public ContainerInfoBean()
	{
		idProperty = new SimpleStringProperty();
		nameProperty = new SimpleStringProperty();
		httpPortProperty = new SimpleIntegerProperty();
		debugPortProperty = new SimpleIntegerProperty();
		statusProperty = new SimpleStringProperty();
		restartCountProperty = new SimpleIntegerProperty();
		createdProperty = new SimpleStringProperty();
		startedProperty = new SimpleStringProperty();
		cpuProperty = new SimpleDoubleProperty();
		loadFactorProperty = new SimpleDoubleProperty();
		ipProperty = new SimpleStringProperty();
		versionProperty = new SimpleStringProperty();
		inspectionProperty = new SimpleStringProperty();
	}

	public ContainerInfoBean apply(ContainerInfo info)
	{
		synchronized (this)
		{
			// TODO See if lazy setting reduces change notifications
			idProperty.set(info.getId());
			nameProperty.set(info.getName());
			if (info.getHttpPort() != null)
			{
				httpPortProperty.set(info.getHttpPort());
			}
			if (info.getDebugPort() != null)
			{
				debugPortProperty.set(info.getDebugPort());
			}
			statusProperty.set(info.getStatus());
			restartCountProperty.set(info.getRestartCount());
			createdProperty.set(info.getCreated());
			startedProperty.set(info.getStarted());
			ipProperty.set(info.getIp());
			versionProperty.set(info.getVersion());
			inspectionProperty.set(info.getInspection());
		}
		return this;
	}

	public void updateCPU(Double cpu)
	{
		synchronized (this)
		{
			if (cpu != null)
			{
				if (cpu != -1)
				{
					// Cap at 1 (100%) as CPU reading can exceed 100%
					cpuProperty.set(Math.min(1.0, cpu / 100));
				}
				else
				{
					// -1 means it couldn't be determined.  Show as 95%
					// TODO enhance UI to show as 'unknown'.
					cpuProperty.set(0.95d);
				}
			}
		}
	}

	public void updateLoadFactor(Double lf)
	{
		synchronized (this)
		{
			if (lf != null)
			{
				// Constrain to 0 - 1 range
				loadFactorProperty.set(Math.max(0, Math.min(1.0, lf)));
			}
		}
	}

	public StringProperty idProperty()
	{
		return idProperty;
	}

	public StringProperty nameProperty()
	{
		return nameProperty;
	}

	public IntegerProperty httpPortProperty()
	{
		return httpPortProperty;
	}

	public IntegerProperty debugPortProperty()
	{
		return debugPortProperty;
	}

	public StringProperty statusProperty()
	{
		return statusProperty;
	}

	public StringProperty createdProperty()
	{
		return createdProperty;
	}

	public StringProperty startedProperty()
	{
		return startedProperty;
	}

	public IntegerProperty restartCountProperty()
	{
		return restartCountProperty;
	}

	public DoubleProperty cpuProperty()
	{
		return cpuProperty;
	}

	public DoubleProperty loadFactorProperty()
	{
		return loadFactorProperty;
	}

	public StringProperty ipProperty()
	{
		return ipProperty;
	}

	public StringProperty versionProperty()
	{
		return versionProperty;
	}

	public StringProperty inspectionProperty()
	{
		return inspectionProperty;
	}
}
