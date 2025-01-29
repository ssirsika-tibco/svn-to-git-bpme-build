/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Generic pane wrapping a line chart with ability to be fed data and update itself at configurable intervals
 *
 * <p/>&copy;2017 TIBCO Software Inc.
 * @author smorgan
 * @since 2017
 */
public class LineChartPane extends VBox
{
	LineChart<Number, Number>					chart;

	Map<String, XYChart.Series<Number, Number>>	seriesMap;

	Map<String, Number>							lastFrame;

	Map<String, Number>							frame;

	private boolean								term;

	private final NumberAxis					xAxis;

	private int									interval	= 2000;

	private int									xPos		= -interval / 1000;

	private Thread								t;

	public LineChartPane(String title, String threadNameSuffix, String xLabel, String yLabel, boolean forceZeroOnY,
			Double yMax, boolean showButtons)
	{
		seriesMap = new HashMap<String, XYChart.Series<Number, Number>>();
		lastFrame = new HashMap<String, Number>();
		frame = new HashMap<String, Number>();

		xAxis = new NumberAxis();
		xAxis.setLabel(xLabel);
		xAxis.setAutoRanging(true);
		//		xAxis.setLowerBound(-(interval / 1000) * 9);
		//		xAxis.setUpperBound(0);

		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel(yLabel);
		if (yMax != null)
		{
			yAxis.setUpperBound(yMax);
			yAxis.setAutoRanging(false);
		}
		else
		{
			yAxis.setAutoRanging(true);
		}
		chart = new LineChart<Number, Number>(xAxis, yAxis);

		chart.setLegendVisible(true);
		chart.setLegendSide(Side.RIGHT);

		chart.setAnimated(true);
		chart.setTitle(title);

		xAxis.forceZeroInRangeProperty().set(false);
		yAxis.forceZeroInRangeProperty().set(forceZeroOnY);

		Button buttonAdd = new Button("Add");
		buttonAdd.setOnMouseClicked((ev) -> {
			addSeries("" + System.currentTimeMillis());
		});
		Button buttonAddX = new Button("Add X");
		buttonAddX.setOnMouseClicked((ev) -> {
			addSeries("X");
		});
		Button buttonRemoveX = new Button("Remove X");
		buttonRemoveX.setOnMouseClicked((ev) -> {
			removeSeries("X");
		});

		Button buttonTick = new Button("Tick");
		buttonTick.setOnMouseClicked((ev) -> {
			tick();
		});
		Button buttonRandom = new Button("Frame = random");
		buttonRandom.setOnMouseClicked((ev) -> {
			frameRandom();
		});
		//		VBox vbox = new VBox();
		//		vbox.getChildren().addAll(chart, buttonAdd, buttonTick, buttonRandom);
		chart.setPrefHeight(1000);
		getChildren().addAll(chart);
		if (showButtons)
		{
			getChildren().addAll(buttonAdd, buttonAddX, buttonRemoveX, buttonTick, buttonRandom);
		}
		VBox.setVgrow(this, Priority.ALWAYS);

		t = new Thread(() -> {
			while (!term)
			{
				try
				{
					Thread.sleep(interval / 2);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tick();
			}
			AceMain.log("LCP animator thread exiting");
		}, "lcp-ticker-" + threadNameSuffix);
	}

	private void frameRandom()
	{
		for (String name : seriesMap.keySet())
		{
			setValue(name, Math.random() * 100);
		}
	}

	public void addSeries(String name)
	{
		synchronized (frame)
		{
			if (!seriesMap.containsKey(name))
			{
				XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
				series.setName(name);
				//				for (int i=10; i>0; i--)
				//				{
				//					series.getData().add(new XYChart.Data<Number, Number>(xPos - (i*2),0));
				//				}
				seriesMap.put(name, series);
				Platform.runLater(() -> {
					chart.getData().add(series);
				});
			}
		}
	}

	public void removeSeries(String name)
	{
		synchronized (frame)
		{
			XYChart.Series<Number, Number> series = seriesMap.remove(name);
			if (series != null)
			{
				Platform.runLater(() -> {
					chart.getData().remove(series);
				});
				lastFrame.remove(name);
				frame.remove(name);
			}
		}
	}

	public void setValue(String name, Number value)
	{
		synchronized (frame)
		{
			frame.put(name, value);
		}
	}

	public void start()
	{
		t.start();
	}

	public void tick()
	{
		synchronized (frame)
		{
			// Inherit missing values from last frame
			if (lastFrame != null)
			{
				for (String name : lastFrame.keySet())
				{
					if (!frame.containsKey(name))
					{
						frame.put(name, lastFrame.get(name));
					}
				}
			}

			// For each series
			xPos += interval / 1000;
			for (String name : seriesMap.keySet())
			{
				// Get the value from the frame
				final Number value = frame.get(name);

				// Add to series data
				Series<Number, Number> series = seriesMap.get(name);
				List<Data<Number, Number>> data = series.getData();

				Platform.runLater(() -> {
					data.add(new XYChart.Data<Number, Number>(xPos, value == null ? 0 : value));
					if (data.size() > 10)
					{
						data.remove(0);
					}

					//					// Trim data
					//					if (data.size() >= 10)
					//					{
					//						// Shuffle down (removing zeroth node and adding one to the end doesn't animate reliably)
					//						for (int i = 0; i < data.size() - 1; i++)
					//						{
					//							data.get(i).setXValue(data.get(i + 1).getXValue());
					//							data.get(i).setYValue(data.get(i + 1).getYValue());
					//						}
					//					}
					//					if (data.size() < 10)
					//					{
					//						// Not reached full length, so add one
					//						// Default to zero when no value available
					//						data.add(new XYChart.Data<Number, Number>(xPos, value == null ? 0 : value));
					//					}
					//					else
					//					{
					//						// Reached full length, so replace last entry
					//						// Default to zero when no value available
					//						data.set(9, new XYChart.Data<Number, Number>(xPos, value == null ? 0 : value));
					//					}
				});
			}

			// Save frame and prepare for next frame
			lastFrame.clear();
			lastFrame.putAll(frame);
			frame.clear();
		}
	}

	public void term()
	{
		term = true;
	}

	public static void test()
	{
		Stage conjuringStage = new Stage();
		conjuringStage.setTitle("Testing LineChartPane");

		LineChartPane lcp = new LineChartPane("Test Chart", "test", "X Axis", "Y Axis", true, null, true);
		Thread t = new Thread(() -> {
			while (true)
			{
				lcp.addSeries("" + System.currentTimeMillis());
				try
				{
					Thread.sleep(60000);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.start();

		Scene scene = new Scene(lcp);
		conjuringStage.setScene(scene);
		conjuringStage.show();
	}
}
