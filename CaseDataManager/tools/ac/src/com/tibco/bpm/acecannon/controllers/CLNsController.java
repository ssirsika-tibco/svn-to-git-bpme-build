package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class CLNsController extends BaseController
{

	public static class CLNBean
	{
		private StringProperty	eventProperty	= new SimpleStringProperty();

		private StringProperty	refsProperty	= new SimpleStringProperty();

		public StringProperty eventProperty()
		{
			return eventProperty;
		}

		public StringProperty refsProperty()
		{
			return refsProperty;
		}
	}

	private static final ObjectMapper	om	= new ObjectMapper();

	@FXML
	CheckBox							cbEnabled;

	@FXML
	Button								buttonFetch;

	@FXML
	TableColumn<CLNBean, String>		colAllEvent;

	@FXML
	TableColumn<CLNBean, String>		colAllCaseReferences;

	@FXML
	TableView<CLNBean>					tvAll;

	@FXML
	TableView<CLNBean>					tvUpdated;

	@FXML
	TableColumn<CLNBean, String>		colUpdatedEvent;

	@FXML
	TableColumn<CLNBean, String>		colUpdatedCaseReferences;

	@FXML
	TableView<CLNBean>					tvDeleted;

	@FXML
	TableColumn<CLNBean, String>		colDeletedEvent;

	@FXML
	TableColumn<CLNBean, String>		colDeletedCaseReferences;

	@FXML
	TableView<CLNBean>					tvUpdatedAndDeleted;

	@FXML
	TableColumn<CLNBean, String>		colUpdatedAndDeletedEvent;

	@FXML
	TableColumn<CLNBean, String>		colUpdatedAndDeletedCaseReferences;

	public CLNsController()
	{
		super("clns");
	}

	public static CLNsController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/clns.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		CLNsController ctrl = loader.getController();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("clns", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		cstage.setTitle("CLNs - ACE Cannon");
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		ctrl.init();
		return ctrl;
	}

	private boolean getIsEnabled() throws IOException
	{
		boolean result = false;
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/yy/clns";
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		caller.call();
		String responseBody = caller.getResponseBody();
		JsonNode root = om.readTree(responseBody);
		if (root instanceof ObjectNode)
		{
			JsonNode enabledNode = ((ObjectNode) root).get("enabled");
			if (enabledNode instanceof BooleanNode)
			{
				result = ((BooleanNode) enabledNode).asBoolean();
			}
		}
		return result;
	}

	public void init() throws IOException
	{
		colAllEvent.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("event"));
		colAllCaseReferences.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("refs"));

		colUpdatedEvent.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("event"));
		colUpdatedCaseReferences.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("refs"));

		colDeletedEvent.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("event"));
		colDeletedCaseReferences.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("refs"));

		colUpdatedAndDeletedEvent.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("event"));
		colUpdatedAndDeletedCaseReferences.setCellValueFactory(new PropertyValueFactory<CLNBean, String>("refs"));

		setEnabledControlState();
		cbEnabled.selectedProperty().addListener(e -> {
			try
			{
				setEnabled(cbEnabled.isSelected());
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		fetch();
	}

	public void setEnabledControlState() throws IOException
	{
		boolean isEnabled = getIsEnabled();
		cbEnabled.setSelected(isEnabled);
	}
	
	public void setEnabled(boolean enabled) throws IOException
	{
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/yy/clns?enabled=" + enabled;
		HTTPCaller.newGet(url, null).call();
	}

	public void populateTable(TableView<CLNBean> tv, ArrayNode clnArray)
	{
		tv.getItems().clear();
		for (int i = 0; i < clnArray.size(); i++)
		{
			JsonNode itemNode = clnArray.get(i);
			if (itemNode instanceof ObjectNode)
			{
				ObjectNode itemObject = (ObjectNode) itemNode;
				JsonNode eventNode = itemObject.get("event");
				JsonNode refsNode = itemObject.get("caseReferences");
				if (eventNode instanceof TextNode && refsNode instanceof ArrayNode)
				{
					ArrayNode refsArray = (ArrayNode) refsNode;
					CLNBean bean = new CLNBean();
					bean.eventProperty().set(eventNode.asText());
					bean.refsProperty().set(refsArray.toString());
					tv.getItems().add(bean);
				}
			}
		}
	}

	public void fetch() throws IOException
	{
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/yy/clns/messages";
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		caller.call();
		String responseBody = caller.getResponseBody();
		JsonNode root = om.readTree(responseBody);
		if (root instanceof ObjectNode)
		{
			JsonNode allNode = ((ObjectNode) root).get("all");
			if (allNode instanceof ObjectNode)
			{
				JsonNode clnsNode = ((ObjectNode) allNode).get("clns");
				if (clnsNode instanceof ArrayNode)
				{
					populateTable(tvAll, (ArrayNode) clnsNode);
				}
			}
			JsonNode updatedNode = ((ObjectNode) root).get("updated");
			if (updatedNode instanceof ObjectNode)
			{
				JsonNode clnsNode = ((ObjectNode) updatedNode).get("clns");
				if (clnsNode instanceof ArrayNode)
				{
					populateTable(tvUpdated, (ArrayNode) clnsNode);
				}
			}
			JsonNode deletedNode = ((ObjectNode) root).get("deleted");
			if (deletedNode instanceof ObjectNode)
			{
				JsonNode clnsNode = ((ObjectNode) deletedNode).get("clns");
				if (clnsNode instanceof ArrayNode)
				{
					populateTable(tvDeleted, (ArrayNode) clnsNode);
				}
			}
			JsonNode updatedAndDeletedNode = ((ObjectNode) root).get("updatedAndDeleted");
			if (updatedAndDeletedNode instanceof ObjectNode)
			{
				JsonNode clnsNode = ((ObjectNode) updatedAndDeletedNode).get("clns");
				if (clnsNode instanceof ArrayNode)
				{
					populateTable(tvUpdatedAndDeleted, (ArrayNode) clnsNode);
				}
			}
		}
	}

	@FXML
	public void onButtonFetchClicked(MouseEvent event)
	{
		try
		{
			fetch();
			setEnabledControlState();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
