package com.tibco.bpm.acecannon.controllers;

import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.UI.Position;

import javafx.stage.Stage;

public abstract class BaseController
{
	protected String	name;

	protected Stage		stage;

	protected BaseController(String name)
	{
		this.name = name;
	}

	public Stage getStage()
	{
		return stage;
	}

	public void saveStagePosition()
	{
		ConfigManager.INSTANCE.getConfig().getUI().setPosition(name, stage.getX(), stage.getY(), stage.getWidth(),
				stage.getHeight(), stage.isMaximized());
	}

	public void setStagePosition()
	{
		Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition(name);
		if (position != null)
		{
			Double x = position.getX();
			Double y = position.getY();
			if (x != null)
			{
				stage.setX(x);
			}
			if (y != null)
			{
				stage.setY(y);
			}
			Double width = position.getWidth();
			Double height = position.getHeight();
			if (position.getIsMaximised())
			{
				stage.setMaximized(true);
			}
			else
			{
				if (width != null)
				{
					stage.setWidth(width);
				}
				if (height != null)
				{
					stage.setHeight(height);
				}
			}
		}
	}
}
