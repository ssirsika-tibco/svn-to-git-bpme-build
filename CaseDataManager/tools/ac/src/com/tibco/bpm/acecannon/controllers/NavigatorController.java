package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.ACEDAO;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.UI.Position;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class NavigatorController
{
	public static class LinkBean
	{
		private StringProperty	nameProperty	= new SimpleStringProperty();

		private StringProperty	refProperty		= new SimpleStringProperty();

		public StringProperty nameProperty()
		{
			return nameProperty;
		}

		public StringProperty refProperty()
		{
			return refProperty;
		}
	}

	private static ObjectMapper		om	= new ObjectMapper();

	private Stage					stage;

	@FXML
	Label							lCaseReference;

	@FXML
	TableView<LinkBean>				tvLinks;

	@FXML
	TableColumn<LinkBean, String>	colName;

	@FXML
	TableColumn<LinkBean, String>	colRef;

	@FXML
	Button							buttonFollowLink;

	@FXML
	TextField						tfSkip;

	@FXML
	TextField						tfTop;

	@FXML
	TextField						tfName;

	@FXML
	TextField						tfDQL;

	private ACEDAO					dao;

	public void setDAO(ACEDAO dao)
	{
		this.dao = dao;
	}

	public void init()
	{
		colName.setCellValueFactory(new PropertyValueFactory<LinkBean, String>("name"));
		colRef.setCellValueFactory(new PropertyValueFactory<LinkBean, String>("ref"));
		tvLinks.getSelectionModel().selectedItemProperty().addListener((obs, old, nu) -> {
			buttonFollowLink.setDisable(nu == null);
		});
	}

	public static NavigatorController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/navigator.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		NavigatorController ctrl = loader.getController();
		ctrl.init();
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		ctrl.stage.setTitle("Links - ACE Cannon");
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("navigator", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
		});
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));

		return ctrl;
	}

	public void setRef(String ref) throws IOException
	{
		lCaseReference.setText(ref);
		refresh();
	}

	public void refresh() throws IOException
	{
		String skip = tfSkip.getText().trim();
		String top = tfTop.getText().trim();
		String name = tfName.getText().trim();
		String dql = tfDQL.getText().trim();
		StringBuilder url = new StringBuilder();
		url.append(ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases/"
				+ lCaseReference.getText() + "/links");
		boolean inQuery = false;
		if (!"".equals(skip))
		{
			url.append("?$skip=" + URLEncoder.encode(skip, "UTF-8"));
			inQuery = true;
		}
		if (!"".equals(top))
		{
			if (!inQuery)
			{
				url.append("?");
				inQuery = true;
			}
			else
			{
				url.append("&");
			}
			url.append("$top=" + URLEncoder.encode(top, "UTF-8"));
		}
		if (!"".equals(dql))
		{
			if (!inQuery)
			{
				url.append("?");
				inQuery = true;
			}
			else
			{
				url.append("&");
			}
			url.append("$dql=" + URLEncoder.encode(dql, "UTF-8"));
		}
		if (!"".equals(name))
		{
			if (!inQuery)
			{
				url.append("?");
				inQuery = true;
			}
			else
			{
				url.append("&");
			}
			url.append("$filter=" + URLEncoder.encode("name eq '" + name + "'", "UTF-8"));
		}
		HTTPCaller caller = HTTPCaller.newGet(url.toString(), null);
		try
		{
			caller.call(true);
		}
		catch (IOException e)
		{
			try
			{
				ErrorDialogController.make(e.getMessage(), -1, getStage()).getStage().showAndWait();
				return;
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		String json = caller.getResponseBody();
		JsonNode node = om.readTree(json);
		if (node instanceof ArrayNode)
		{
			tvLinks.getItems().clear();
			ArrayNode an = (ArrayNode) node;
			for (int i = 0; i < an.size(); i++)
			{
				JsonNode itemNode = an.get(i);
				if (itemNode instanceof ObjectNode)
				{
					ObjectNode itemObject = (ObjectNode) itemNode;
					LinkBean bean = new LinkBean();
					bean.nameProperty.set(itemObject.get("name").asText());
					bean.refProperty.set(itemObject.get("caseReference").asText());
					tvLinks.getItems().add(bean);
				}
			}
		}
	}

	public Stage getStage()
	{
		return stage;
	}

	@FXML
	public void onButtonFollowLinkClicked(MouseEvent event)
	{
		LinkBean bean = tvLinks.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			try
			{
				setRef(bean.refProperty.get());
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonRefreshClicked(MouseEvent event)
	{
		try
		{
			refresh();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void onTextKeyReleased(KeyEvent ev)
	{
		if (ev.getCode() == KeyCode.ENTER)
		{
			try
			{
				refresh();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonCreateLinksClicked(MouseEvent event)
	{
		try
		{
			CreateLinksController ctrl = CreateLinksController.make();
			String ref = lCaseReference.getText();
			ctrl.setSourceRef(ref);
			String[] frags = ref.split("-");
			String qName = frags[1];
			String namespace = qName.substring(0, qName.lastIndexOf('.'));
			String name = qName.substring(qName.lastIndexOf('.') + 1);
			List<String> linkNames = dao.getLinkNamesByType(namespace, name);
			Stage childStage = ctrl.getStage();
			ctrl.setLinkNames(linkNames);
			Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("createlinks");
			if (position != null)
			{
				Double x = position.getX();
				Double y = position.getY();
				if (x != null)
				{
					childStage.setX(x);
				}
				if (y != null)
				{
					childStage.setY(y);
				}
				Double width = position.getWidth();
				Double height = position.getHeight();
				if (position.getIsMaximised())
				{
					childStage.setMaximized(true);
				}
				else
				{

					if (width != null)
					{
						childStage.setWidth(width);
					}
					if (height != null)
					{
						childStage.setHeight(height);
					}
				}
			}
			childStage.show();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
