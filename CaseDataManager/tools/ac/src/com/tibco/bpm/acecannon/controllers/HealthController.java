package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.MainController;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class HealthController extends BaseController
{
	private static final ObjectMapper	om	= new ObjectMapper();

	@FXML
	Label								lLive;

	@FXML
	Label								lReady;

	MainController						main;

	public HealthController()
	{
		super("health");
	}
	
	public static HealthController make(MainController main) throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/health.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 300, 100);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		HealthController ctrl = loader.getController();
		ctrl.main = main;
		Stage cstage = new Stage();
		cstage.setOnCloseRequest(ev -> {
			ctrl.saveStagePosition();
		});
		ctrl.stage = cstage;
		ctrl.stage.setTitle("Health - ACE Cannon");
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));

		return ctrl;
	}
	
	public void populateReadiness()
	{
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/adapter/v1/readiness";
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		try
		{
			caller.call(false);
			String json = caller.getResponseBody();
			JsonNode root = om.readTree(json);
			if (root instanceof ObjectNode)
			{
				ObjectNode on = (ObjectNode) root;
				if (on.has("ready"))
				{
					lReady.setText(on.get("ready").asBoolean() ? "Yes" : "No");
				}
			}
		}
		catch (IOException e)
		{
			lReady.setText("No");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void populateLiveness()
	{
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/adapter/v1/liveness";
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		try
		{
			caller.call(false);
			String json = caller.getResponseBody();
			JsonNode root = om.readTree(json);
			if (root instanceof ObjectNode)
			{
				ObjectNode on = (ObjectNode) root;
				if (on.has("healthy"))
				{
					lLive.setText(on.get("healthy").asBoolean() ? "Yes" : "No");
				}
			}
		}
		catch (IOException e)
		{
			lLive.setText("No");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void populate()
	{
		populateReadiness();
		populateLiveness();
	}

	public Stage getStage()
	{
		return stage;
	}

	@FXML
	public void onButtonRefreshClicked(MouseEvent event)
	{
		populate();
	}

	@FXML
	public void onButtonCloseClicked(MouseEvent event)
	{
		saveStagePosition();
		stage.close();
		main.onHealthClose(this);
	}
}
