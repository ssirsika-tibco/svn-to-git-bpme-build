package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;
import java.net.URLEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

public class AdminPropertiesController extends BaseController
{
	public static class PropertyBean
	{
		private StringProperty	nameProperty			= new SimpleStringProperty();

		private StringProperty	valueProperty			= new SimpleStringProperty();

		private StringProperty	groupIdProperty			= new SimpleStringProperty();

		private StringProperty	descriptionProperty		= new SimpleStringProperty();

		private StringProperty	modifiedDateProperty	= new SimpleStringProperty();

		private IntegerProperty	idProperty				= new SimpleIntegerProperty();

		public StringProperty nameProperty()
		{
			return nameProperty;
		}

		public StringProperty valueProperty()
		{
			return valueProperty;
		}

		public StringProperty groupIdProperty()
		{
			return groupIdProperty;
		}

		public StringProperty descriptionProperty()
		{
			return descriptionProperty;
		}

		public StringProperty modifiedDateProperty()
		{
			return modifiedDateProperty;
		}

		public IntegerProperty idProperty()
		{
			return idProperty;
		}
	}

	private static final ObjectMapper	om	= new ObjectMapper();

	@FXML
	TableView<PropertyBean>				tvProperties;

	@FXML
	TableColumn<PropertyBean, String>	colGroupId;

	@FXML
	TableColumn<PropertyBean, String>	colName;

	@FXML
	TableColumn<PropertyBean, String>	colValue;

	@FXML
	TableColumn<PropertyBean, Integer>	colId;

	@FXML
	TableColumn<PropertyBean, String>	colDescription;

	@FXML
	TableColumn<PropertyBean, String>	colModifiedDate;

	@FXML
	Button								buttonRefresh;

	@FXML
	CheckBox							cbCDMOnly;

	public AdminPropertiesController()
	{
		super("adminProperties");
	}

	private void populate() throws IOException
	{
		tvProperties.getItems().clear();
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/admin/v1/properties";
		if (cbCDMOnly.isSelected())
		{
			url += "?$filter=" + URLEncoder.encode("groupId eq cdm", "UTF-8");
		}
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		caller.call();

		JsonNode root = om.readTree(caller.getResponseBody());

		if (root instanceof ArrayNode)
		{
			ArrayNode arrayNode = (ArrayNode) root;
			for (int i = 0; i < arrayNode.size(); i++)
			{
				JsonNode arrayMember = arrayNode.get(i);
				if (arrayMember instanceof ObjectNode)
				{
					ObjectNode obj = (ObjectNode) arrayMember;
					PropertyBean bean = new PropertyBean();
					bean.nameProperty().set(obj.get("name").asText());
					bean.valueProperty().set(obj.get("value").asText());
					bean.groupIdProperty().set(obj.get("groupId").asText());
					bean.descriptionProperty().set(obj.get("description").asText());
					bean.modifiedDateProperty().set(obj.get("modifiedDate").asText());
					bean.idProperty().set(obj.get("id").asInt());
					tvProperties.getItems().add(bean);
				}
			}
		}

	}

	public static AdminPropertiesController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/adminproperties.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		AdminPropertiesController ctrl = loader.getController();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("clns", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		cstage.setTitle("Admin Properties - ACE Cannon");
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		ctrl.init();
		return ctrl;
	}

	private void init()
	{
		colGroupId.setCellValueFactory(new PropertyValueFactory<PropertyBean, String>("groupId"));
		colName.setCellValueFactory(new PropertyValueFactory<PropertyBean, String>("name"));
		colValue.setCellValueFactory(new PropertyValueFactory<PropertyBean, String>("value"));
		colDescription.setCellValueFactory(new PropertyValueFactory<PropertyBean, String>("description"));
		colModifiedDate.setCellValueFactory(new PropertyValueFactory<PropertyBean, String>("modifiedDate"));
		colId.setCellValueFactory(new PropertyValueFactory<PropertyBean, Integer>("id"));

		try
		{
			populate();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cbCDMOnly.selectedProperty().addListener(e -> {
			try
			{
				populate();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
	}

	@FXML
	public void onButtonRefreshClicked(MouseEvent event)
	{
		try
		{
			populate();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
