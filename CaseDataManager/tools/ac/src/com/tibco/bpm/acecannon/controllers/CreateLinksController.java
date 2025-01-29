package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class CreateLinksController
{
	private static final ObjectMapper	om	= new ObjectMapper();

	private Stage						stage;

	@FXML
	ComboBox<String>					cbLinkName;

	@FXML
	TextField							tfSourceRef;

	@FXML
	TextField							tfTargetRef;

	public static CreateLinksController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/createlinks.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 640, 240);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		CreateLinksController ctrl = loader.getController();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		cstage.setTitle("Create Links - ACE Cannon");
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		ctrl.tfTargetRef.requestFocus();
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("createlinks", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		return ctrl;
	}

	public Stage getStage()
	{
		return stage;
	}

	public void setSourceRef(String ref)
	{
		tfSourceRef.setText(ref);
	}

	@FXML
	public void onButtonCreateClicked(MouseEvent event)
	{
		String sourceRef = tfSourceRef.getText().trim();
		String targetRef = tfTargetRef.getText().trim();
		String name = (String) cbLinkName.getValue();

		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases/" + sourceRef
				+ "/links";

		JsonNodeFactory fac = JsonNodeFactory.instance;
		ArrayNode an = fac.arrayNode();
		ObjectNode on = fac.objectNode();
		an.add(on);
		on.set("name", fac.textNode(name));
		on.set("caseReference", fac.textNode(targetRef));
		try
		{
			String body = om.writeValueAsString(an);
			HTTPCaller caller = HTTPCaller.newPost(url, null, body);
			caller.call(true);
			int responseCode = caller.getResponseCode();
			if (responseCode == 200)
			{
				new Alert(AlertType.INFORMATION, "Created link").showAndWait();
			}
			else
			{
				ErrorDialogController.make(caller.getResponseBody(), responseCode, getStage()).getStage().showAndWait();
			}
		}
		catch (IOException e)
		{
			try
			{
				ErrorDialogController.make(e.getMessage(), -1, getStage()).getStage().showAndWait();
			}
			catch (JsonProcessingException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonCloseClicked(MouseEvent event)
	{
		stage.close();
	}

	public void setLinkNames(List<String> linkNames)
	{
		Collections.sort(linkNames);
		cbLinkName.setItems(FXCollections.observableArrayList(linkNames));
		if (linkNames.size() > 0)
		{
			cbLinkName.getSelectionModel().select(0);
		}
	}

}
