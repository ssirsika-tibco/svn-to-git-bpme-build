/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.glass.ui.Screen;
import com.tibco.bpm.acecannon.AceMain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UI
{
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	public static class Position
	{
		@JsonProperty(value = "x")
		private Double	x;

		@JsonProperty(value = "y")
		private Double	y;

		@JsonProperty(value = "width")
		private Double	width;

		@JsonProperty(value = "height")
		private Double	height;

		@JsonProperty(value = "isMaximised")
		private boolean	isMaximised;

		public Position()
		{

		}

		public Position(Double x, Double y, Double width, Double height, boolean isMaximized)
		{
			// Filter crazy coordinates
			if (Math.abs(x) > 4000)
			{
				System.out.println("Ignoring crazy x coordinate: " + x);
				x = 0.0;
			}
			if (Math.abs(y) > 4000)
			{
				System.out.println("Ignoring crazy y coordinate: " + x);
				y = 0.0;
			}
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.isMaximised = isMaximized;
		}

		public Double getX()
		{
			return x;
		}

		public void setX(Double x)
		{
			this.x = x;
		}

		public Double getY()
		{
			return y;
		}

		public void setY(Double y)
		{
			this.y = y;
		}

		public Double getWidth()
		{
			return width;
		}

		public void setWidth(Double width)
		{
			this.width = width;
		}

		public Double getHeight()
		{
			return height;
		}

		public void setHeight(Double height)
		{
			this.height = height;
		}

		public boolean getIsMaximised()
		{
			return isMaximised;
		}

		public void setIsMaximised(boolean isMaximised)
		{
			this.isMaximised = isMaximised;
		}
	}

	public String								mruAppFolder;

	@JsonProperty(value = "positions")
	private Map<String, Position>				positions;

	@JsonProperty(value = "states")
	private Map<String, Map<String, Object>>	states;

	public UI()
	{
		positions = new HashMap<>();
		states = new HashMap<>();
	}

	public void setPosition(String positionable, Double x, Double y, Double width, Double height, boolean isMaximised)
	{
		synchronized (this)
		{
			if (positions == null)
			{
				positions = new HashMap<>();
			}
			positions.put(positionable, new Position(x, y, width, height, isMaximised));
		}
	}

	public Position getPosition(String name)
	{
		return positions.get(name);
	}

	public Map<String, Object> getState(String name)
	{
		return states.get(name);
	}

	public void setState(String name, Map<String, Object> map)
	{
		states.put(name, map);
	}
	
	public void setStateProperty(String name, String key, Object value)
	{
		if (!states.containsKey(name))
		{
			states.put(name, new HashMap<>());
		}
		states.get(name).put(key, value);
	}

	@JsonProperty("mruAppFolder")
	public String getMRUAppFolder()
	{
		return mruAppFolder;
	}

	public void setMRUAppFolder(String mruAppFolder)
	{
		this.mruAppFolder = mruAppFolder;
	}

	public void fixOutOfBoundsPositions()
	{
		if (Screen.getScreens().size() == 1)
		{
			int width = Screen.getScreens().get(0).getWidth();
			int height = Screen.getScreens().get(0).getHeight();
			for (Entry<String, Position> entry : positions.entrySet())
			{
				Position value = entry.getValue();
				Double x = value.getX();
				Double y = value.getY();
				if (x < 0.0)
				{
					AceMain.log("X position (" + x + ") is out of range for " + entry.getKey() + ". Changing to 0");
					value.setX(0.0);
				}
				if (x > width - 1)
				{
					Double newX = width - value.getWidth();
					AceMain.log(
							"X position (" + x + ") is out of range for " + entry.getKey() + ". Changing to " + newX);
					value.setX(newX);
				}
				if (y < 0.0)
				{
					AceMain.log("Y position (" + x + ") is out of range for " + entry.getKey() + ". Changing to 0");
					value.setY(0.0);
				}
				if (y > height - 1)
				{
					Double newY = height - value.getHeight();
					AceMain.log(
							"Y position (" + x + ") is out of range for " + entry.getKey() + ". Changing to " + newY);
					value.setY(newY);
				}
			}
		}
	}
}
