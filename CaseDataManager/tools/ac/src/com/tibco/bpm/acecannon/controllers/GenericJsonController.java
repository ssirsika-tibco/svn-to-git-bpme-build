package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.tibco.bpm.acecannon.AceMain;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GenericJsonController
{
	private String				json;

	@FXML
	TextArea					taRaw;

	@FXML
	TreeView<String>			tvTree;

	@FXML
	Label						labelTitle;

	private VBox				ui;

	private static ObjectMapper	om	= new ObjectMapper();

	@FXML
	TabPane						tpFormat;
	static
	{
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}

	public void selectRaw(boolean select)
	{
		if (select)
		{
			tpFormat.getSelectionModel().selectLast();
		}
		else
		{
			tpFormat.getSelectionModel().selectFirst();
		}
	}

	public static GenericJsonController makeVBox(String json, String title)
	{
		GenericJsonController controller = null;
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/genericjson.fxml"));
		try
		{
			Parent load = loader.load();
			controller = loader.getController();
			//controller.setLabel(labelText);
			controller.setJson(json);

			VBox vbox = (VBox) load;
			VBox.setVgrow(vbox, Priority.ALWAYS);
			controller.setUI(vbox);
			if (title == null)
			{
				controller.removeTitle();
			}
			else
			{
				controller.setTitle(title);
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return controller;
	}

	private void setUI(VBox load)
	{
		// TODO Auto-generated method stub
		this.ui = load;
	}

	public VBox getUI()
	{
		return ui;
	}

	private void removeTitle()
	{
		ui.getChildren().remove(labelTitle);
	}

	public static Stage makeStage(String stageTitle, String labelText, String json)
	{
		Stage stage = new Stage();
		stage.setTitle(stageTitle);
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/genericjson.fxml"));
		try
		{
			VBox load = loader.load();
			GenericJsonController controller = loader.getController();
			controller.setUI(load);
			if (labelText != null)
			{
				controller.setTitle(labelText);
			}
			else
			{
				controller.removeTitle();
			}
			controller.setJson(json);

			Scene scene = new Scene(load);
			stage.setScene(scene);
			stage.setWidth(800);
			stage.setHeight(600);
			stage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stage;
	}

	private void setTitle(String labelText)
	{
		this.labelTitle.setText(labelText);

	}

	private TreeItem<String> toTreeItem(String label, JsonNode jNode)
	{
		TreeItem<String> result = null;
		if (jNode instanceof ValueNode)
		{
			String text = jNode.asText();
			try
			{
				JsonNode textRoot = om.readTree(text);
				if (textRoot instanceof ObjectNode)
				{
					result = toTreeItem(label + " (string containing JSON)", textRoot);
				}
			}
			catch (IOException e)
			{
			}
		}
		if (result == null)
		{
			result = new TreeItem<>(label + (jNode instanceof ContainerNode ? "" : ": ") + jNode.asText());
			if (jNode instanceof ObjectNode)
			{
				ObjectNode on = (ObjectNode) jNode;
				for (Iterator<String> iter = on.fieldNames(); iter.hasNext();)
				{
					String fieldName = iter.next();
					result.getChildren().add(toTreeItem(fieldName, on.get(fieldName)));
				}
			}
			else if (jNode instanceof ArrayNode)
			{
				ArrayNode an = (ArrayNode) jNode;
				for (int i = 0; i < an.size(); i++)
				{
					result.getChildren().add(toTreeItem("[" + i + "]", an.get(i)));
				}
			}
		}
		result.setExpanded(true);
		return result;
	}

	private void setJson(String json)
	{
		this.json = json;
		taRaw.setText(json);
		try
		{
			JsonNode rootNode = om.readTree(json);
			tvTree.setRoot(toTreeItem("Root", rootNode));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initialize()
	{
	}

	public boolean getIsRaw()
	{
		return tpFormat.getSelectionModel().getSelectedIndex() == 1;
	}
	
	public void setRootText(String text)
	{
		tvTree.getRoot().setValue(text);
	}
}
