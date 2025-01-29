package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ErrorDialogController
{
	public static class ContextAttributeBean
	{
		private StringProperty	nameProperty	= new SimpleStringProperty();

		private StringProperty	valueProperty	= new SimpleStringProperty();

		public ContextAttributeBean(String name, String value)
		{
			nameProperty.set(name);
			valueProperty.set(value);
		}

		public StringProperty nameProperty()
		{
			return nameProperty;
		}

		public StringProperty valueProperty()
		{
			return valueProperty;
		}
	}

	private static final ObjectMapper			om	= new ObjectMapper();

	private Stage								stage;

	@FXML
	TextArea									taStackTrace;

	@FXML
	TableView<ContextAttributeBean>				tvContextAttributes;

	@FXML
	Label										lTitle;

	@FXML
	Label										lCode;

	@FXML
	Label										lMessage;

	@FXML
	TableColumn<ContextAttributeBean, String>	tcName;

	@FXML
	TableColumn<ContextAttributeBean, String>	tcValue;

	public static ErrorDialogController make(String errorBody, int statusCode, Stage parentStage) throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/errordialog.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 640, 480);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		ErrorDialogController ctrl = loader.getController();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent t) {
				KeyCode key = t.getCode();
				if (key == KeyCode.ESCAPE){
					cstage.close();
				}
			}
		});
		cstage.setTitle("Error - ACE Cannon");
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		cstage.setOnCloseRequest(ev -> {
			//			ConfigManager.INSTANCE.getConfig().getUI().setPosition("error", cstage.getX(), cstage.getY(),
			//					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		if (parentStage != null)
		{
			cstage.initOwner(parentStage);
		}
		cstage.initModality(Modality.APPLICATION_MODAL);
		ctrl.populateFromError(errorBody, statusCode);
		ctrl.init();
		return ctrl;
	}

	private void init()
	{
		tcName.setCellValueFactory(new PropertyValueFactory<ContextAttributeBean, String>("name"));
		tcValue.setCellValueFactory(new PropertyValueFactory<ContextAttributeBean, String>("value"));
	}

	private void populateFromError(String errorBody, int statusCode)
	{
		JsonNode root;
		try
		{
			root = om.readTree(errorBody);
		}
		catch (IOException e)
		{
			lTitle.setText("Failed to reach REST endpoint");
			lCode.setText("IOException");
			lMessage.setText(errorBody);
			taStackTrace.setText("No stack trace");
			return;
		}
		if (root instanceof ObjectNode)
		{
			ObjectNode on = (ObjectNode) root;
			if (on.has("errorCode"))
			{
				lCode.setText(on.get("errorCode").asText());
			}
			else
			{
				lCode.setText("<no error code>");
			}
			if (on.has("errorMsg"))
			{
				lMessage.setText(on.get("errorMsg").asText());
			}
			else
			{
				lMessage.setText("<no message>");
			}
			String status;
			switch (statusCode)
			{
				case 500:
					status = "500 Internal Server Error";
					break;
				case 400:
					status = "400 Bad Request";
					break;
				case 404:
					status = "404 Not Found";
					break;
				case 403:
					status = "403 Forbidden";
					break;
				case -1:
					status = "Error";
					break;
				default:
					status = Integer.toString(statusCode);

			}
			lTitle.setText(status);
			tvContextAttributes.getItems().clear();
			if (on.has("contextAttributes"))
			{
				JsonNode caNode = on.get("contextAttributes");
				if (caNode instanceof ArrayNode)
				{
					ArrayNode ca = (ArrayNode) caNode;
					for (int i = 0; i < ca.size(); i++)
					{
						tvContextAttributes.getItems().add(new ContextAttributeBean(ca.get(i).get("name").asText(),
								ca.get(i).get("value").asText()));
					}
				}
			}
			if (on.has("stackTrace"))
			{
				taStackTrace.setText(on.get("stackTrace").asText());
			}
			else
			{
				taStackTrace.setText("No stack trace");
			}
		}
	}

	public Stage getStage()
	{
		return stage;
	}

	@FXML
	public void onButtonOKClicked(MouseEvent event)
	{
		stage.close();
	}
}
