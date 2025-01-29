package com.tibco.bpm.acecannon.controllers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.tibco.bpm.acecannon.ACEDAO;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.FileUtils;
import com.tibco.bpm.acecannon.ModelEditor;
import com.tibco.bpm.acecannon.casedata.ConjuringPane;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.UI.Position;
import com.tibco.bpm.acecannon.network.HTTPCaller;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.da.dm.api.ValidationResult;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AppsController
{
	public static class DeploymentBean
	{
		private StringProperty	idProperty				= new SimpleStringProperty();

		private StringProperty	appIdProperty			= new SimpleStringProperty();

		private StringProperty	appNameProperty			= new SimpleStringProperty();

		private StringProperty	appVersionProperty		= new SimpleStringProperty();

		private StringProperty	timeDeployedProperty	= new SimpleStringProperty();

		private StringProperty	timeCreatedProperty		= new SimpleStringProperty();

		private StringProperty	statusProperty			= new SimpleStringProperty();

		private StringProperty	artifactsProperty		= new SimpleStringProperty();

		public StringProperty idProperty()
		{
			return idProperty;
		}

		public StringProperty appIdProperty()
		{
			return appIdProperty;
		}

		public StringProperty appNameProperty()
		{
			return appNameProperty;
		}

		public StringProperty appVersionProperty()
		{
			return appVersionProperty;
		}

		public StringProperty timeDeployedProperty()
		{
			return timeDeployedProperty;
		}

		public StringProperty timeCreatedProperty()
		{
			return timeCreatedProperty;
		}

		public StringProperty statusProperty()
		{
			return statusProperty;
		}

		public StringProperty artifactsProperty()
		{
			return artifactsProperty;
		}
	}

	public static class TypeBean
	{
		private StringProperty	raw						= new SimpleStringProperty();

		private StringProperty	name					= new SimpleStringProperty();

		private StringProperty	nameDisplay				= new SimpleStringProperty();

		private StringProperty	label					= new SimpleStringProperty();

		private BooleanProperty	isCase					= new SimpleBooleanProperty();

		private StringProperty	namespace				= new SimpleStringProperty();

		private IntegerProperty	applicationMajorVersion	= new SimpleIntegerProperty();

		private StringProperty	applicationId			= new SimpleStringProperty();

		public StringProperty rawProperty()
		{
			return raw;
		}

		public StringProperty nameProperty()
		{
			return name;
		}

		public StringProperty nameDisplayProperty()
		{
			return nameDisplay;
		}

		public StringProperty labelProperty()
		{
			return label;
		}

		public BooleanProperty isCaseProperty()
		{
			return isCase;
		}

		public StringProperty namespaceProperty()
		{
			return namespace;
		}

		public IntegerProperty applicationMajorVersionProperty()
		{
			return applicationMajorVersion;
		}

		public StringProperty applicationIdProperty()
		{
			return applicationId;
		}

	}

	public static class AttributeBean
	{
		private StringProperty	raw			= new SimpleStringProperty();

		private StringProperty	name		= new SimpleStringProperty();

		private StringProperty	nameDisplay	= new SimpleStringProperty();

		private StringProperty	label		= new SimpleStringProperty();

		private StringProperty	type		= new SimpleStringProperty();

		public StringProperty rawProperty()
		{
			return raw;
		}

		public StringProperty nameProperty()
		{
			return name;
		}

		public StringProperty nameDisplayProperty()
		{
			return nameDisplay;
		}

		public StringProperty labelProperty()
		{
			return label;
		}

		public StringProperty typeProperty()
		{
			return type;
		}
	}

	public static class StateBean
	{
		private StringProperty	value		= new SimpleStringProperty();

		private StringProperty	label		= new SimpleStringProperty();

		private BooleanProperty	isTerminal	= new SimpleBooleanProperty();

		public StateBean(String value, String label, boolean isTerminal)
		{
			this.value.set(value);
			this.label.set(label);
			this.isTerminal.set(isTerminal);
		}

		public StringProperty valueProperty()
		{
			return value;
		}

		public StringProperty labelProperty()
		{
			return label;
		}

		public BooleanProperty isTerminalProperty()
		{
			return isTerminal;
		}
	}

	public static class LinkBean
	{
		private StringProperty	name	= new SimpleStringProperty();

		private StringProperty	label	= new SimpleStringProperty();

		private BooleanProperty	isArray	= new SimpleBooleanProperty();

		private StringProperty	type	= new SimpleStringProperty();

		public LinkBean(String name, String label, boolean isArray, String type)
		{
			this.name.set(name);
			this.label.set(label);
			this.isArray.set(isArray);
			this.type.set(type);
		}

		public StringProperty nameProperty()
		{
			return name;
		}

		public StringProperty labelProperty()
		{
			return label;
		}

		public BooleanProperty isArrayProperty()
		{
			return isArray;
		}

		public StringProperty typeProperty()
		{
			return type;
		}
	}

	public static class DepBean
	{
		private StringProperty	namespace		= new SimpleStringProperty();

		private StringProperty	appId			= new SimpleStringProperty();

		private IntegerProperty	majorVersion	= new SimpleIntegerProperty();

		public DepBean(String namespace, String appId, int majorVersion)
		{
			this.namespace.set(namespace);
			this.appId.set(appId);
			this.majorVersion.set(majorVersion);
		}

		public StringProperty namespaceProperty()
		{
			return namespace;
		}

		public StringProperty appIdProperty()
		{
			return appId;
		}

		public IntegerProperty majorVersionProperty()
		{
			return majorVersion;
		}
	}

	private static final ObjectMapper om = new ObjectMapper();
	static
	{
		om.enable(SerializationFeature.INDENT_OUTPUT);
	}

	private static final String			boundary	= "*****";

	private static final String			crlf		= "\r\n";

	private static final String			twoHyphens	= "--";

	private ACEDAO						dao;

	private Stage						stage;

	@FXML
	TableView<DeploymentBean>			tvDeployments;

	@FXML
	TableView<TypeBean>					tvTypes;

	@FXML
	TableColumn<DeploymentBean, String>	colDepId;

	@FXML
	TableColumn<DeploymentBean, String>	colDepStatus;

	@FXML
	TableColumn<DeploymentBean, String>	colDepApplicationId;

	@FXML
	TableColumn<DeploymentBean, String>	colDepVersion;

	@FXML
	TableColumn<DeploymentBean, String>	colDepName;

	@FXML
	TableColumn<DeploymentBean, String>	colDepArtifacts;

	@FXML
	TableColumn<DeploymentBean, String>	colDepTimeDeployed;

	@FXML
	TableColumn<DeploymentBean, String>	colDepTimeCreated;

	@FXML
	TableColumn<TypeBean, String>		colTypeName;

	@FXML
	TableColumn<TypeBean, String>		colTypeLabel;

	@FXML
	TableColumn<TypeBean, Boolean>		colTypeIsCase;

	@FXML
	TableColumn<TypeBean, String>		colTypeNamespace;

	@FXML
	TableColumn<TypeBean, Integer>		colTypeMajorVersion;

	@FXML
	TableColumn<TypeBean, String>		colTypeApplicationId;

	@FXML
	Button								buttonDeployFolder;

	@FXML
	Button								buttonDeployZip;

	@FXML
	CheckBox							cbValidate;

	@FXML
	Button								buttonUndeploy;

	@FXML
	Button								buttonForceUndeploy;

	@FXML
	Button								buttonViewModel;

	@FXML
	Button								buttonCases;

	@FXML
	Button								buttonCreateCases;

	@FXML
	Button								buttonCreateCasesManual;

	@FXML
	CheckBox							cbCaseOnly;

	//	List<Node>							vbTypeDetailDefaultChildren;
	//
	//	GenericJsonController				rawCtrl;
	//
	//	private boolean						selectRaw	= true;

	@FXML
	TextField							tfTypeAspects;

	@FXML
	TextField							tfSkip;

	@FXML
	TextField							tfTop;

	@FXML
	TabPane								tpTypeDetail;

	@FXML
	Tab									tabJSONTree;

	@FXML
	Tab									tabJSONRaw;

	@FXML
	TextArea							taJSONRaw;

	@FXML
	TreeView<String>					tvTypeJSON;

	private CasesController				casesController;

	@FXML
	TextField							tfApplicationId;

	@FXML
	TextField							tfNamespace;

	@FXML
	TextField							tfMajorVersion;

	@FXML
	public SplitPane					spVertical;

	@FXML
	public SplitPane					spHorizontal;

	@FXML
	TableView<AttributeBean>			tvAttributes;

	@FXML
	TableColumn<AttributeBean, String>	colAttrName;

	@FXML
	TableColumn<AttributeBean, String>	colAttrLabel;

	@FXML
	TableColumn<AttributeBean, String>	colAttrType;

	@FXML
	TableView<AttributeBean>			tvSumAttributes;

	@FXML
	TableColumn<AttributeBean, String>	colSumAttrName;

	@FXML
	TableColumn<AttributeBean, String>	colSumAttrLabel;

	@FXML
	TableColumn<AttributeBean, String>	colSumAttrType;

	@FXML
	TableView<StateBean>				tvStates;

	@FXML
	TableColumn<StateBean, String>		colStateValue;

	@FXML
	TableColumn<StateBean, String>		colStateLabel;

	@FXML
	TableColumn<StateBean, Boolean>		colStateTerminal;

	@FXML
	TableView<LinkBean>					tvLinks;

	@FXML
	TableColumn<LinkBean, String>		colLinkName;

	@FXML
	TableColumn<LinkBean, String>		colLinkLabel;

	@FXML
	TableColumn<LinkBean, Boolean>		colLinkIsArray;

	@FXML
	TableColumn<LinkBean, String>		colLinkType;

	@FXML
	TableView<DepBean>					tvDependencies;

	@FXML
	TableColumn<DepBean, String>		colDepNamespace;

	@FXML
	TableColumn<DepBean, String>		colDepAppId;

	@FXML
	TableColumn<DepBean, Integer>		colDepMajorVersion;

	public static AppsController make() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/apps.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 1100, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		AppsController ctrl = loader.getController();
		Stage cstage = new Stage();
		cstage.setTitle("Applications - ACE Cannon");
		ctrl.stage = cstage;
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		ctrl.init();
		return ctrl;
	}

	public void init()
	{
		colDepId.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("id"));
		colDepStatus.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("status"));
		colDepName.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("appName"));
		colDepApplicationId.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("appId"));
		colDepVersion.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("appVersion"));
		colDepArtifacts.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("artifacts"));
		colDepTimeDeployed.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("timeDeployed"));
		colDepTimeCreated.setCellValueFactory(new PropertyValueFactory<DeploymentBean, String>("timeCreated"));
		tvDeployments.getSelectionModel().selectedItemProperty().addListener((obs, old, nu) -> {
			if (nu != null)
			{
				buttonUndeploy.setDisable(nu == null);
				buttonForceUndeploy.setDisable(nu == null);
			}
			refreshTypes();
		});

		colTypeName.setCellValueFactory(new PropertyValueFactory<TypeBean, String>("nameDisplay"));
		colTypeIsCase.setCellValueFactory(new PropertyValueFactory<TypeBean, Boolean>("isCase"));
		colTypeNamespace.setCellValueFactory(new PropertyValueFactory<TypeBean, String>("namespace"));
		colTypeLabel.setCellValueFactory(new PropertyValueFactory<TypeBean, String>("label"));
		colTypeMajorVersion.setCellValueFactory(new PropertyValueFactory<TypeBean, Integer>("applicationMajorVersion"));
		colTypeApplicationId.setCellValueFactory(new PropertyValueFactory<TypeBean, String>("applicationId"));

		colAttrName.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("nameDisplay"));
		colAttrLabel.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("label"));
		colAttrType.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("type"));

		colSumAttrName.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("nameDisplay"));
		colSumAttrLabel.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("label"));
		colSumAttrType.setCellValueFactory(new PropertyValueFactory<AttributeBean, String>("type"));

		colStateValue.setCellValueFactory(new PropertyValueFactory<StateBean, String>("value"));
		colStateLabel.setCellValueFactory(new PropertyValueFactory<StateBean, String>("label"));
		colStateTerminal.setCellValueFactory(new PropertyValueFactory<StateBean, Boolean>("isTerminal"));

		colLinkName.setCellValueFactory(new PropertyValueFactory<LinkBean, String>("name"));
		colLinkLabel.setCellValueFactory(new PropertyValueFactory<LinkBean, String>("label"));
		colLinkIsArray.setCellValueFactory(new PropertyValueFactory<LinkBean, Boolean>("isArray"));
		colLinkType.setCellValueFactory(new PropertyValueFactory<LinkBean, String>("type"));

		colDepNamespace.setCellValueFactory(new PropertyValueFactory<DepBean, String>("namespace"));
		colDepAppId.setCellValueFactory(new PropertyValueFactory<DepBean, String>("appId"));
		colDepMajorVersion.setCellValueFactory(new PropertyValueFactory<DepBean, Integer>("majorVersion"));

		tvTypes.getSelectionModel().selectedItemProperty().addListener((obs, old, nu) -> {
			buttonViewModel.setDisable(nu == null);
			buttonCases.setDisable(nu == null || !nu.isCaseProperty().get());
			buttonCreateCases.setDisable(nu == null || !nu.isCaseProperty().get());
			buttonCreateCasesManual.setDisable(nu == null || !nu.isCaseProperty().get());
			if (nu != null)
			{
				String raw = nu.rawProperty().get();
				try
				{
					String prettyRaw = prettyUp(raw);

					//					if (rawCtrl != null)
					//					{
					//						selectRaw = rawCtrl.getIsRaw();
					//					}
					//
					//					GenericJsonController ctrl = GenericJsonController.makeVBox(prettyRaw, null);
					//					ctrl.setRootText("GET /types response for " + nu.nameProperty().get());
					//					VBox vbRaw = ctrl.getUI();
					//					ObservableList<Node> children = vbTypeDetail.getChildren();
					//					children.clear();
					//					children.add(vbRaw);
					//					ctrl.selectRaw(selectRaw);
					//					rawCtrl = ctrl;

					taJSONRaw.setText(prettyRaw);
					tvTypeJSON.setRoot(
							toTreeItem("GET /types response for " + nu.nameProperty().get(), om.readTree(raw)));
					handleTypeSelect(raw);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		cbCaseOnly.selectedProperty().addListener(e -> {
			refreshTypes();
		});

		//		vbTypeDetailDefaultChildren = new ArrayList<>(vbTypeDetail.getChildren());

		refreshDeployments(null);
		refreshTypes();
	}

	private void handleTypeSelect(String raw) throws IOException
	{
		tvAttributes.getItems().clear();
		tvSumAttributes.getItems().clear();
		tvStates.getItems().clear();
		tvLinks.getItems().clear();
		tvDependencies.getItems().clear();
		JsonNode root = om.readTree(raw);
		if (root instanceof ObjectNode)
		{
			ObjectNode on = (ObjectNode) root;
			if (on.has("attributes"))
			{
				JsonNode attrsNode = on.get("attributes");
				handleAttributeList(attrsNode, tvAttributes);
			}
			if (on.has("summaryAttributes"))
			{
				JsonNode attrsNode = on.get("summaryAttributes");
				handleAttributeList(attrsNode, tvSumAttributes);
			}
			if (on.has("states"))
			{
				JsonNode statesNode = on.get("states");
				handleStateList(statesNode);
			}
			if (on.has("links"))
			{
				JsonNode linksNode = on.get("links");
				handleLinkList(linksNode);
			}
			if (on.has("dependencies"))
			{
				JsonNode depsNode = on.get("dependencies");
				handleDepsList(depsNode);
			}
		}
	}

	private void handleStateList(JsonNode statesNode)
	{
		if (statesNode instanceof ArrayNode)
		{
			ArrayNode statesArray = (ArrayNode) statesNode;
			for (int i = 0; i < statesArray.size(); i++)
			{
				JsonNode stateNode = statesArray.get(i);
				if (stateNode instanceof ObjectNode)
				{
					ObjectNode stateObject = (ObjectNode) stateNode;
					boolean isTerminal = stateObject.has("isTerminal") && stateObject.get("isTerminal").asBoolean();
					StateBean bean = new StateBean(stateObject.get("value").asText(), stateObject.get("label").asText(),
							isTerminal);
					tvStates.getItems().add(bean);
				}
			}
		}
	}

	private void handleLinkList(JsonNode linksNode)
	{
		if (linksNode instanceof ArrayNode)
		{
			ArrayNode linksArray = (ArrayNode) linksNode;
			for (int i = 0; i < linksArray.size(); i++)
			{
				JsonNode linkNode = linksArray.get(i);
				if (linkNode instanceof ObjectNode)
				{
					ObjectNode linkObject = (ObjectNode) linkNode;
					boolean isArray = linkObject.has("isArray") && linkObject.get("isArray").asBoolean();
					LinkBean bean = new LinkBean(linkObject.get("name").asText(), linkObject.get("label").asText(),
							isArray, linkObject.get("type").asText());
					tvLinks.getItems().add(bean);
				}
			}
		}
	}

	private void handleDepsList(JsonNode depsNode)
	{
		if (depsNode instanceof ArrayNode)
		{
			ArrayNode depsArray = (ArrayNode) depsNode;
			for (int i = 0; i < depsArray.size(); i++)
			{
				JsonNode depNode = depsArray.get(i);
				if (depNode instanceof ObjectNode)
				{
					ObjectNode depObject = (ObjectNode) depNode;
					DepBean bean = new DepBean(depObject.get("namespace").asText(),
							depObject.get("applicationId").asText(), depObject.get("applicationMajorVersion").asInt());
					tvDependencies.getItems().add(bean);
				}
			}
		}
	}

	private void handleAttributeList(JsonNode attrsNode, TableView<AttributeBean> targetControl)
	{
		if (attrsNode instanceof ArrayNode)
		{
			ArrayNode attrsArray = (ArrayNode) attrsNode;
			for (int i = 0; i < attrsArray.size(); i++)
			{
				JsonNode attrNode = attrsArray.get(i);
				if (attrNode instanceof ObjectNode)
				{
					ObjectNode attrObject = (ObjectNode) attrNode;
					String name = attrObject.get("name").asText();
					String label = attrObject.get("label").asText();
					String type = attrObject.get("type").asText();
					AttributeBean bean = new AttributeBean();
					bean.nameProperty().set(name);
					String nameDisplay = name;
					if (attrObject.has("isArray") && attrObject.get("isArray").asText().equals("true"))
					{
						nameDisplay += " []";
					}
					if (attrObject.has("isSearchable") && attrObject.get("isSearchable").asText().equals("true"))
					{
						nameDisplay += " \uD83D\uDD0D";
					}
					if (attrObject.has("isIdentifier") && attrObject.get("isIdentifier").asText().equals("true"))
					{
						nameDisplay += " \uD83C\uDD94";
					}

					bean.nameDisplayProperty().set(nameDisplay);
					bean.labelProperty().set(label);
					bean.typeProperty().set(type);
					targetControl.getItems().add(bean);
				}
			}
		}
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

	public void refreshTypes()
	{
		DeploymentBean nu = tvDeployments.getSelectionModel().getSelectedItem();
		if (nu != null)
		{
			String appId = nu.appIdProperty.get();
			String version = nu.appVersionProperty.get();
			int idx = version.indexOf('.');
			if (idx != -1)
			{
				int majorVersion = Integer.parseInt(version.substring(0, idx));
				try
				{
					tfApplicationId.setText(appId);
					tfMajorVersion.setText(Integer.toString(majorVersion));
					tfNamespace.setText("");
					loadTypes();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			try
			{
				loadTypes();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void setDAO(ACEDAO dao)
	{
		this.dao = dao;
	}

	public Stage getStage()
	{
		return stage;
	}

	private String escape(String value)
	{
		return value.replaceAll("'", "\\\\'");
	}

	public void loadTypes() throws IOException
	{
		String applicationId = tfApplicationId.getText().trim();
		String majorVersion = tfMajorVersion.getText().trim();
		String namespace = tfNamespace.getText().trim();
		StringBuilder buf = new StringBuilder();
		StringBuilder filterBuf = new StringBuilder();
		buf.append("?$top=" + tfTop.getText().trim());
		String skip = tfSkip.getText().trim();
		if (!skip.equals(""))
		{
			buf.append("&$skip=" + URLEncoder.encode(skip, "UTF-8"));
		}
		String aspects = URLEncoder.encode(tfTypeAspects.getText().trim(), "UTF-8");
		if (!(aspects.equals("")))
		{
			buf.append("&$select=" + aspects);
		}
		boolean inFilter = false;
		if (applicationId != null && applicationId.length() != 0)
		{
			inFilter = true;
			filterBuf.append("applicationId eq '" + escape(applicationId) + "'");
		}
		if (majorVersion != null && majorVersion.length() != 0)
		{
			if (inFilter)
			{
				filterBuf.append(" and ");
			}
			inFilter = true;
			filterBuf.append("applicationMajorVersion eq " + majorVersion);
		}
		if (namespace != null && namespace.length() != 0)
		{
			if (inFilter)
			{
				filterBuf.append(" and ");
			}
			inFilter = true;
			filterBuf.append("namespace eq '" + namespace + "'");
		}
		if (cbCaseOnly.isSelected())
		{
			if (!inFilter)
			{
				inFilter = true;
			}
			else
			{
				filterBuf.append(" and ");
			}
			filterBuf.append("isCase eq TRUE");
		}

		if (filterBuf.length() != 0)
		{
			String filter = URLEncoder.encode(filterBuf.toString(), "UTF-8");
			buf.append("&");
			buf.append("$filter=" + filter);
		}

		String query = buf.toString();

		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/types" + query;

		HTTPCaller caller = HTTPCaller.newGet(url, null);
		caller.call(true);

		String json = caller.getResponseBody();

		JsonNode root = om.readTree(json);
		if (root instanceof ArrayNode)
		{
			ArrayNode an = (ArrayNode) root;
			tvTypes.getItems().clear();
			//			vbTypeDetail.getChildren().clear();
			//			vbTypeDetail.getChildren().addAll(vbTypeDetailDefaultChildren);
			//			if (rawCtrl != null)
			//			{
			//				selectRaw = rawCtrl.getIsRaw();
			//				rawCtrl = null;
			//			}
			taJSONRaw.setText("Select a type");
			tvTypeJSON.setRoot(new TreeItem<String>("Select a type"));
			tvAttributes.getItems().clear();
			tvSumAttributes.getItems().clear();
			tvStates.getItems().clear();
			tvLinks.getItems().clear();
			tvDependencies.getItems().clear();
			for (int i = 0; i < an.size(); i++)
			{
				JsonNode arrayMember = an.get(i);
				if (arrayMember instanceof ObjectNode)
				{
					ObjectNode typeNode = (ObjectNode) arrayMember;
					TypeBean bean = new TypeBean();
					bean.rawProperty().set(om.writeValueAsString(typeNode));
					boolean isCase = typeNode.has("isCase") ? typeNode.get("isCase").asBoolean() : false;
					if (typeNode.has("name"))
					{
						bean.nameProperty().set(typeNode.get("name").asText());
						bean.nameDisplayProperty().set(typeNode.get("name").asText() + (isCase ? " \uD83D\uDCBC" : ""));
					}
					else
					{
						bean.nameProperty().set("<unknown>");
						bean.nameDisplayProperty().set("<unknown>");
					}
					if (typeNode.has("label"))
					{
						bean.labelProperty().set(typeNode.get("label").asText());
					}
					else
					{
						bean.labelProperty().set("<unknown>");
					}
					if (isCase)
					{
						bean.isCaseProperty().set(isCase);
					}
					if (typeNode.has("namespace"))
					{
						bean.namespaceProperty().set(typeNode.get("namespace").asText());
					}
					else
					{
						bean.namespaceProperty().set("<unknown>");
					}
					if (typeNode.has("applicationMajorVersion"))
					{
						bean.applicationMajorVersionProperty().set(typeNode.get("applicationMajorVersion").asInt());
					}
					if (typeNode.has("applicationId"))
					{
						bean.applicationIdProperty().set(typeNode.get("applicationId").asText());
					}
					else
					{
						bean.applicationIdProperty().set("<unknown>");
					}
					tvTypes.getItems().add(bean);
				}
			}
		}

	}

	public void refreshDeployments(String highlightId)
	{
		tfApplicationId.setText("");
		tfNamespace.setText("");
		tfMajorVersion.setText("");
		buttonUndeploy.setDisable(true);
		buttonForceUndeploy.setDisable(true);
		buttonViewModel.setDisable(true);
		buttonCreateCases.setDisable(true);
		buttonCreateCasesManual.setDisable(true);
		buttonCases.setDisable(true);
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/deploy/v1/deployments";
		//		setStatus(url);
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		try
		{
			caller.call();
			String json = caller.getResponseBody();
			JsonNode root = om.readTree(json);
			if (root instanceof ArrayNode)
			{
				tvDeployments.getItems().clear();
				ArrayNode an = (ArrayNode) root;
				for (int i = 0; i < an.size(); i++)
				{
					JsonNode arrayMember = an.get(i);
					if (arrayMember instanceof ObjectNode)
					{
						ObjectNode deploymentNode = (ObjectNode) arrayMember;
						DeploymentBean bean = new DeploymentBean();
						if (deploymentNode.has("id"))
						{
							bean.idProperty.set(deploymentNode.get("id").asText());
						}
						bean.appIdProperty.set(deploymentNode.get("applicationId").asText());
						if (deploymentNode.has("applicationName"))
						{
							bean.appNameProperty.set(deploymentNode.get("applicationName").asText());
						}
						else
						{
							bean.appNameProperty.set("unknown");
						}
						bean.appVersionProperty.set(deploymentNode.get("applicationVersion").asText());
						if (deploymentNode.has("timeCreated"))
						{
							bean.timeCreatedProperty.set(deploymentNode.get("timeCreated").asText());
						}
						else
						{
							bean.timeCreatedProperty().set("unknown");
						}
						bean.timeDeployedProperty.set(deploymentNode.get("timeDeployed").asText());
						bean.statusProperty.set(deploymentNode.get("status").asText());

						JsonNode targetsNode = deploymentNode.get("targets");
						if (targetsNode instanceof ArrayNode)
						{
							int artifactCountTotal = 0;
							int artifactCountCDM = 0;
							ArrayNode targetsArray = (ArrayNode) targetsNode;
							for (Iterator<JsonNode> iter = targetsArray.iterator(); iter.hasNext();)
							{
								JsonNode targetNode = iter.next();
								if (targetNode instanceof ObjectNode)
								{
									ObjectNode targetObjectNode = (ObjectNode) targetNode;
									JsonNode countNode = targetObjectNode.get("count");
									if (countNode instanceof IntNode)
									{
										int targetCount = ((IntNode) countNode).asInt();
										artifactCountTotal += targetCount;
										JsonNode targetNameNode = targetObjectNode.get("target");
										if (targetNameNode instanceof TextNode)
										{
											if ("Case-Manager".equals(targetNameNode.asText()))
											{
												artifactCountCDM += targetCount;
											}
										}
									}
								}
							}
							// Not using total artifact count, although we've got it...
							bean.artifactsProperty.set(String.format("%d", artifactCountCDM));
						}
						tvDeployments.getItems().add(bean);
					}
				}
				if (highlightId != null)
				{
					DeploymentBean bean = tvDeployments.getItems().stream()
							.filter(b -> b.idProperty.get().equals(highlightId)).findFirst().orElse(null);
					if (bean != null)
					{
						tvDeployments.getSelectionModel().select(bean);
					}
				}
			}

		}
		catch (IOException e)
		{
			try
			{
				ErrorDialogController.make(e.getMessage(), -1, getStage()).getStage().showAndWait();
			}

			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonRefreshClicked(MouseEvent event)
	{
		refreshDeployments(null);
		refreshTypes();
	}

	@FXML
	public void onButtonUndeployClicked(MouseEvent event)
	{
		DeploymentBean bean = tvDeployments.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			String id = bean.idProperty.get();
			if (id != null)
			{
				try
				{
					undeploy(id);
					refreshDeployments(null);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void deleteAllCases(String namespace, String typeName, List<String> stateValues, int majorVersion)
	{
		try
		{
			for (String stateValue : stateValues)
			{
				String query = "$filter=" + URLEncoder.encode("caseState eq '" + stateValue + "' and caseType eq '"
						+ namespace + "." + typeName + "' and applicationMajorVersion eq " + majorVersion
						+ " and modificationTimestamp le 2999TZ", "UTF-8");
				String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases?"
						+ query;
				HTTPCaller caller = HTTPCaller.newDelete(url, null);
				caller.call();
				String resultMsg = "Number of " + typeName + " cases in state '" + stateValue + "' deleted: "
						+ caller.getResponseBody();
				AceMain.log(resultMsg);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void deleteCasesForApp(String appId) throws IOException
	{
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase()
				+ "/bpm/case/v1/types?$select=b,s&$top=999999&$filter="
				+ URLEncoder.encode("isCase eq true and applicationId eq '" + appId + "'", "UTF-8");
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		AceMain.log("Fetching info on case types and their state values for app " + appId + "...");
		caller.call();
		String body = caller.getResponseBody();
		ArrayNode an = (ArrayNode) om.readTree(body);

		for (int i = 0; i < an.size(); i++)
		{
			JsonNode typeNode = an.get(i);
			if (typeNode instanceof ObjectNode)
			{
				ObjectNode typeObject = (ObjectNode) typeNode;
				String typeName = typeObject.get("name").asText();
				String namespace = typeObject.get("namespace").asText();
				int majorVersion = typeObject.get("applicationMajorVersion").asInt();
				List<String> stateValues = new ArrayList<>();
				JsonNode statesNode = typeObject.get("states");
				if (statesNode instanceof ArrayNode)
				{
					ArrayNode statesArray = (ArrayNode) statesNode;
					for (int j = 0; j < statesArray.size(); j++)
					{
						JsonNode stateNode = statesArray.get(j);
						if (stateNode instanceof ObjectNode)
						{
							ObjectNode stateObject = (ObjectNode) stateNode;
							stateValues.add(stateObject.get("value").asText());
						}
					}
				}
				deleteAllCases(namespace, typeName, stateValues, majorVersion);
			}
		}
	}

	@FXML
	public void onButtonForceUndeployClicked(MouseEvent event)
	{
		DeploymentBean bean = tvDeployments.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			String id = bean.idProperty.get();
			if (id != null)
			{
				String appId = bean.appIdProperty.get();
				try
				{
					deleteCasesForApp(appId);
					AceMain.log("Cases gone, so undeploying...");
					undeploy(id);
					refreshDeployments(null);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void undeploy(String id) throws JsonProcessingException
	{
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode on = factory.objectNode();
		on.set("id", factory.textNode(id));
		on.set("status", factory.textNode("Undeployed"));
		String body = om.writeValueAsString(on);
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/deploy/v1/deployments";
		HTTPCaller caller = HTTPCaller.newPut(url, null, body);
		try
		{
			caller.call();
		}
		catch (IOException e)
		{
			try
			{
				ErrorDialogController.make(e.getMessage(), -1, getStage()).getStage().showAndWait();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public String deployZIP(File file, boolean validateModels) throws IOException, DataModelSerializationException
	{
		String response = null;
		boolean proceed = true;
		if (validateModels)
		{
			Map<String, DataModel> dataModelsFromRASC = FileUtils.getDataModelsFromRASC(file.toPath());
			for (Entry<String, DataModel> entry : dataModelsFromRASC.entrySet())
			{
				DataModel dm = entry.getValue();
				for (String ns : dm.getNamespaceDependencies())
				{
					DataModel foreignDM = dataModelsFromRASC.values().stream().filter(d -> d.getNamespace().equals(ns))
							.findFirst().orElse(null);
					if (foreignDM == null)
					{
						AceMain.log("Failed to resolve dependency on namespace " + ns + " from " + entry.getKey());
					}
					else
					{
						dm.getForeignModels().add(foreignDM);
					}
				}
				ValidationResult validationResult = entry.getValue().validate();
				if (validationResult.containsErrors())
				{
					Alert alert = new Alert(AlertType.ERROR,
							"Errors in " + entry.getKey() + "\n\n" + validationResult.toReportMessage());
					alert.setTitle(entry.getKey() + " validation errors");
					alert.showAndWait();
					proceed = false;
				}
				else
				{
					Alert alert = new Alert(AlertType.INFORMATION, "No errors in " + entry.getKey());
					alert.showAndWait();
				}
			}
		}
		if (proceed)
		{
			URL url = new URL(
					ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/deploy/v1/deployments");
			//TODO Enhance HTTPCaller to support this scenario
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true); // indicates POST method
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			String unencoded = String.format("%s:%s",
					ConfigManager.INSTANCE.getActiveProfile().getIdentification().getUserName(),
					ConfigManager.INSTANCE.getActiveProfile().getIdentification().getPassword());
			byte[] encodedBytes = Base64.getEncoder().encode(unencoded.getBytes());
			String encoded = new String(encodedBytes);
			conn.setRequestProperty("Authorization", "Basic " + encoded);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Cache-Control", "no-cache");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			DataOutputStream request = new DataOutputStream(conn.getOutputStream());

			String fileName = file.getName();
			request.writeBytes(twoHyphens + boundary + crlf);
			request.writeBytes(
					"Content-Disposition: form-data; name=\"appContents\";filename=\"" + fileName + "\"" + crlf);
			request.writeBytes(crlf);

			byte[] bytes = Files.readAllBytes(file.toPath());
			request.write(bytes);

			request.writeBytes(crlf);
			request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
			request.flush();
			request.close();

			// checks server's status code first
			AceMain.log("-> POST " + url + " (body: " + request.size() + " bytes)");
			long startTime = System.currentTimeMillis();
			int status = -1;
			try
			{
				status = conn.getResponseCode();
			}
			catch (IOException e)
			{
				try
				{
					ErrorDialogController.make(e.getMessage(), -1, getStage()).getStage().showAndWait();
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			AceMain.log("<- [" + status + "] " + (System.currentTimeMillis() - startTime) + "ms");
			InputStream responseStream = new BufferedInputStream(
					status == 200 ? conn.getInputStream() : conn.getErrorStream());

			BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

			String line = "";
			StringBuilder stringBuilder = new StringBuilder();

			while ((line = responseStreamReader.readLine()) != null)
			{
				stringBuilder.append(line).append("\n");
			}
			responseStreamReader.close();

			response = stringBuilder.toString();
			conn.disconnect();
			if (status != 200)
			{
				AceMain.log(response);
				ErrorDialogController.make(response, status, getStage()).getStage().showAndWait();
			}
		}
		return response != null ? response.trim() : null;
	}

	@FXML
	public void onDeployFolderClicked(MouseEvent event) throws DataModelSerializationException
	{
		final DirectoryChooser directoryChoose = new DirectoryChooser();
		String mruAppFolder = ConfigManager.INSTANCE.getConfig().getUI().getMRUAppFolder();
		File parentFolder = mruAppFolder == null ? null : new File(mruAppFolder);
		if (parentFolder != null)
		{
			if (parentFolder.exists() && parentFolder.isDirectory())
			{
				directoryChoose.setInitialDirectory(parentFolder);
			}
		}
		File folder = directoryChoose.showDialog(stage);

		if (folder != null)
		{
			ConfigManager.INSTANCE.getConfig().getUI().setMRUAppFolder(folder.getParentFile().getAbsolutePath());
			File file;
			try
			{
				file = FileUtils.buildZipFromFolderURI(folder.toURI().toURL(), true);
				file.deleteOnExit();
				String deployId = deployZIP(file, cbValidate.isSelected());
				refreshDeployments(deployId);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (URISyntaxException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onDeployZipClicked(MouseEvent event) throws IOException, DataModelSerializationException
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("ACE RASC (*.zip/*.rasc)", Arrays.asList("*.zip", "*.rasc")));
		File folder = new File(ConfigManager.INSTANCE.getConfig().getUI().getMRUAppFolder());
		if (folder != null)
		{
			if (folder.exists() && folder.isDirectory())
			{
				fileChooser.setInitialDirectory(folder);
			}
		}
		File file = fileChooser.showOpenDialog(stage);
		if (file != null)
		{
			ConfigManager.INSTANCE.getConfig().getUI().setMRUAppFolder(file.getParentFile().getAbsolutePath());
			String deployId = deployZIP(file, cbValidate.isSelected());
			refreshDeployments(deployId);
		}
	}

	@FXML
	public void onButtonViewModel(MouseEvent event)
	{
		TypeBean typeBean = tvTypes.getSelectionModel().getSelectedItem();
		if (typeBean != null)
		{
			String namespace = typeBean.namespace.get();
			Integer applicationMajorVersion = typeBean.applicationMajorVersion.get();
			try
			{
				DataModel dm = dao.getDataModel(namespace, applicationMajorVersion);
				Stage modelStage = ModelEditor.makeStage(dm,
						namespace + " (" + applicationMajorVersion + ".x) - ACE Cannon");
				modelStage.show();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonCasesClicked(MouseEvent event)
	{
		try
		{
			casesController = CasesController.make(this);
			casesController.setDAO(dao);
			TypeBean typeBean = tvTypes.getSelectionModel().getSelectedItem();
			casesController.setType(typeBean.namespaceProperty().get() + "." + typeBean.nameProperty().get() + " (v"
					+ typeBean.applicationMajorVersionProperty().get() + ".x)");
			casesController.setCaseType(typeBean.namespaceProperty().get() + "." + typeBean.nameProperty().get());
			casesController.setMajorVersion(typeBean.applicationMajorVersionProperty().get());

			// Get StructuredType from bean
			JsonNode rawRoot = om.readTree(typeBean.rawProperty().get());
			if (rawRoot instanceof ObjectNode)
			{
				ObjectNode rawObject = (ObjectNode) rawRoot;
				List<String> saNames = new ArrayList<>();
				if (rawObject.has("summaryAttributes"))
				{
					ArrayNode saArrayNode = (ArrayNode) rawObject.get("summaryAttributes");
					for (int i = 0; i < saArrayNode.size(); i++)
					{
						saNames.add(((ObjectNode) saArrayNode.get(i)).get("name").asText());
					}
				}
				else if (rawObject.has("attributes"))
				{
					ArrayNode aArrayNode = (ArrayNode) rawObject.get("attributes");
					for (int i = 0; i < aArrayNode.size(); i++)
					{
						ObjectNode aObject = (ObjectNode) aArrayNode.get(i);
						if (aObject.has("isSummary") && aObject.get("isSummary").asText().equals("true"))
						{
							saNames.add(aObject.get("name").asText());
						}
					}

				}
				System.out.println(saNames);
				casesController.setSummaryAttributeNames(saNames);
			}

			Stage childStage = casesController.getStage();
			Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("cases");
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
			casesController.init();
			casesController.getStage().show();
			casesController.initSplit();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	public void onButtonCreateCasesManualClicked(MouseEvent event)
	{
		TypeBean bean = tvTypes.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			CreateCasesController ctrl;
			try
			{
				ctrl = CreateCasesController.make();
				ctrl.setCaseType(bean.namespaceProperty().get() + "." + bean.nameProperty().get());
				ctrl.setMajorVersion(bean.applicationMajorVersion.get());
				ctrl.populatePreview();
				ctrl.getStage().setTitle(
						"Create " + bean.namespaceProperty().get() + "." + bean.nameProperty().get() + " - ACE Cannon");
				ctrl.getStage().show();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onButtonCreateCasesClicked(MouseEvent event)
	{
		ConjuringPane pane;
		try
		{
			pane = ConjuringPane.make();
			TypeBean typeBean = tvTypes.getSelectionModel().getSelectedItem();
			if (typeBean != null)
			{
				String namespace = typeBean.namespace.get();
				Integer applicationMajorVersion = typeBean.applicationMajorVersion.get();
				try
				{
					DataModel dm = dao.getDataModel(namespace, applicationMajorVersion);
					recursivelyLoadDependencies(dm, applicationMajorVersion);
					StructuredType st = dm.getStructuredTypeByName(typeBean.nameProperty().get());
					pane.setType(st);
					pane.setMajorVersion(typeBean.applicationMajorVersion.get());
					pane.getStage().setTitle("Create " + st.getName() + " cases - ACE Cannon");
					pane.getStage().showAndWait();
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		catch (InstantiationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void recursivelyLoadDependencies(DataModel dm, int applicationMajorVersion) throws SQLException
	{
		for (String nsd : dm.getNamespaceDependencies())
		{
			DataModel foreignDM = dao.getDataModel(nsd, applicationMajorVersion);
			if (!dm.getForeignModels().contains(foreignDM))
			{
				dm.getForeignModels().add(foreignDM);
				recursivelyLoadDependencies(foreignDM, applicationMajorVersion);
			}
		}
	}

	@FXML
	public void onTypesTableClicked(MouseEvent event)
	{
		if (event.getClickCount() >= 2)
		{
			TypeBean bean = tvTypes.getSelectionModel().getSelectedItem();
			if (bean != null)
			{
				String raw = bean.rawProperty().get();
				if (raw != null)
				{
					try
					{
						ObjectNode on = (ObjectNode) om.readTree(raw);
						raw = prettyUp(raw);
						GenericJsonController
								.makeStage("Type " + bean.nameProperty().get() + " - ACE Cannon", null, raw).show();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
	}

	private String prettyUp(String json) throws IOException
	{
		JsonNode root = om.readTree(json);
		String pretty = om.writeValueAsString(root);
		return pretty;
	}

	public void saveChildStageInfo()
	{
		// This will be the most recently opened Cases window when there are many.
		if (casesController != null)
		{
			Stage cstage = casesController.getStage();
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("cases", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("cases", "splitHorizontal",
					casesController.spHorizontal.getDividerPositions()[0]);
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("cases", "splitVertical",
					casesController.spVertical.getDividerPositions()[0]);
			cstage.close();
			casesController = null;
		}
	}

	public void onCasesClose(CasesController ctrl)
	{
		// If the Cases controller calling this was the most recently opened, clear our reference.
		// (If not, we don't care about it)
		if (casesController == ctrl)
		{
			casesController = null;
		}
	}

	@FXML
	public void onTextKeyReleased(KeyEvent ev)
	{
		if (ev.getCode() == KeyCode.ENTER)
		{
			try
			{
				loadTypes();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void initSplit()
	{
		Map<String, Object> uiState = ConfigManager.INSTANCE.getConfig().getUI().getState("applications");
		if (uiState != null)
		{
			Object splitHorizontal = uiState.get("splitHorizontal");
			if (splitHorizontal instanceof Double)
			{
				spHorizontal.setDividerPositions((double) splitHorizontal);
			}
			Object splitVertical = uiState.get("splitVertical");
			if (splitVertical instanceof Double)
			{
				spVertical.setDividerPositions((double) splitVertical);
			}
		}
	}
}
