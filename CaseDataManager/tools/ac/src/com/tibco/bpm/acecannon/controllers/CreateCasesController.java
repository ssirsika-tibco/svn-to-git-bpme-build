package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class CreateCasesController
{
	private Stage				stage;

	@FXML
	TextField					tfCaseType;

	@FXML
	TextField					tfMajorVersion;

	@FXML
	TextArea					taPreview;

	private static ObjectMapper	om	= new ObjectMapper();
	static
	{
		om.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
	}

	private static ObjectMapper prettyOM = new ObjectMapper();
	static
	{
		prettyOM.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@FXML
	TextArea	taCases;

	@FXML
	Button		buttonSubmit;

	@FXML
	TextArea	taResponse;

	public static CreateCasesController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/createcases.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		CreateCasesController ctrl = loader.getController();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("createcases", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));

		ctrl.taCases.textProperty().addListener((old, nu, obs) -> {
			ctrl.populatePreview();
		});
		return ctrl;
	}

	public void setCaseType(String type)
	{
		tfCaseType.setText(type);
	}

	public void setMajorVersion(int majorVersion)
	{
		tfMajorVersion.setText(Integer.toString(majorVersion));
	}

	public Stage getStage()
	{
		return stage;
	}

	private String buildBody() throws IOException
	{
		StringBuilder buf = new StringBuilder();
		JsonNodeFactory fac = JsonNodeFactory.instance;
		ObjectNode root = fac.objectNode();
		root.set("caseType", fac.textNode(tfCaseType.getText()));
		root.set("applicationMajorVersion", fac.numberNode(Integer.parseInt(tfMajorVersion.getText())));

		JsonNode casesRoot = om.readTree(taCases.getText());
		if (casesRoot instanceof ObjectNode)
		{
			ObjectNode on = (ObjectNode) casesRoot;
			casesRoot = fac.arrayNode();
			((ArrayNode) casesRoot).add(on);
		}
		if (casesRoot instanceof ArrayNode)
		{
			ArrayNode an = (ArrayNode) casesRoot;
			for (int i = 0; i < an.size(); i++)
			{
				String caseJson = om.writeValueAsString(an.get(i));
				an.set(i, fac.textNode(caseJson));
			}
			root.set("casedata", an);
		}

		if (root.has("casedata"))
		{
			try
			{
				buf.append(prettyOM.writeValueAsString(root));
			}
			catch (JsonProcessingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return buf.toString();
	}

	public void populatePreview()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("POST /cases\n\n");
		try
		{
			buf.append(buildBody());
			taPreview.setText(buf.toString());
		}
		catch (IOException e)
		{
			taPreview.setText(e.toString());
		}
	}

	@FXML
	public void onButtonSubmitClicked(MouseEvent event)
	{
		try
		{
			String body = buildBody();
			if (body != null && body.length() != 0)
			{
				String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases";
				HTTPCaller caller = HTTPCaller.newPost(url, null, body);
				caller.call(true);
				String responseBody = caller.getResponseBody();
				try {
					JsonNode responseTree = om.readTree(responseBody);
					responseBody = prettyOM.writeValueAsString(responseTree);
				} catch (IOException e)
				{
				}
				
				taResponse.setText("HTTP " + caller.getResponseCode() + "\n\n" + caller.getResponseBody());
			}
		}
		catch (IOException e)
		{
			taResponse.setText(e.getMessage());
		}

	}
}
