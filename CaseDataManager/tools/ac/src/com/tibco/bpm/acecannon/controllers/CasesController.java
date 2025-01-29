package com.tibco.bpm.acecannon.controllers;

import java.awt.Checkbox;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.ACEDAO;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StructuredType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CasesController
{
	public static class CaseBean
	{
		private StringProperty	raw				= new SimpleStringProperty();

		private StringProperty	caseReference	= new SimpleStringProperty();

		private StringProperty	casedata		= new SimpleStringProperty();

		private StringProperty	summary			= new SimpleStringProperty();

		private StringProperty	metadata		= new SimpleStringProperty();

		public StringProperty rawProperty()
		{
			return raw;
		}

		public StringProperty caseReferenceProperty()
		{
			return caseReference;
		}

		public StringProperty casedataProperty()
		{
			return casedata;
		}

		public StringProperty summaryProperty()
		{
			return summary;
		}

		public StringProperty metadataProperty()
		{
			return metadata;
		}
	}

	private static ObjectMapper om = new ObjectMapper();
	{
		om.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}

	private static ObjectMapper prettyOM = new ObjectMapper();
	static
	{
		prettyOM.enable(SerializationFeature.INDENT_OUTPUT);
		prettyOM.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}

	private static final String		NOT_SET		= "<not set>";

	private ACEDAO					dao;

	private Stage					stage;

	@FXML
	Button							buttonRefresh;

	@FXML
	TableView<CaseBean>				tvCases;

	TableColumn<CaseBean, String>	tcCaseReference;

	TableColumn<CaseBean, String>	tcCasedata;

	TableColumn<CaseBean, String>	tcSummary;

	TableColumn<CaseBean, String>	tcMetadata;

	private String					caseType;

	private int						majorVersion;

	@FXML
	SplitPane						spHorizontal;

	@FXML
	private TextField				tfSkip;

	@FXML
	private TextField				tfTop;

	@FXML
	private TextField				tfCid;

	@FXML
	private TextField				tfStateValue;

	@FXML
	TextField						tfSelect;

	@FXML
	TextField						tfSearch;

	@FXML
	TextField						tfModificationTimestamp;

	@FXML
	Button							buttonDelete;

	@FXML
	VBox							vbRight;

	@FXML
	VBox							vbBottom;

	VBox							vbRaw;

	GenericJsonController			rawCtrl;

	private boolean					selectRaw	= true;

	@FXML
	TextArea						taBottom;

	@FXML
	Button							buttonLinks;

	@FXML
	TextField						tfDQL;

	private List<String>			saNames;

	private AppsController			parentController;

	@FXML
	SplitPane						spVertical;

	@FXML
	Button							buttonUpdate;

	@FXML
	CheckBox						cbExcludeTerminal;

	public static CasesController make(AppsController parentController) throws IOException
	{
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/cases.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		CasesController ctrl = loader.getController();
		ctrl.parentController = parentController;
		Stage cstage = new Stage();
		ctrl.stage = cstage;
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("cases", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("cases", "splitHorizontal",
					ctrl.spHorizontal.getDividerPositions()[0]);
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("cases", "splitVertical",
					ctrl.spVertical.getDividerPositions()[0]);
			if (parentController != null)
			{
				parentController.onCasesClose(ctrl);
			}
		});
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		return ctrl;
	}

	public void setDAO(ACEDAO dao)
	{
		this.dao = dao;
	}

	public void setCaseType(String caseType)
	{
		this.caseType = caseType;
	}

	public void setMajorVersion(int l)
	{
		this.majorVersion = l;
	}

	public void initSplit()
	{
		Map<String, Object> uiState = ConfigManager.INSTANCE.getConfig().getUI().getState("cases");
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

	public void init()
	{
		tcCaseReference = new TableColumn<>("Ref");
		tcCaseReference.setCellValueFactory(new PropertyValueFactory<CaseBean, String>("caseReference"));
		tcCaseReference.setPrefWidth(300);
		tvCases.getColumns().add(tcCaseReference);

		tcCasedata = new TableColumn<>("Casedata");
		tcCasedata.setCellValueFactory(new PropertyValueFactory<CaseBean, String>("casedata"));
		tcCasedata.setPrefWidth(300);
		tvCases.getColumns().add(tcCasedata);

		tcSummary = new TableColumn<>("Summary");
		tcSummary.setCellValueFactory(new PropertyValueFactory<CaseBean, String>("summary"));
		tcSummary.setPrefWidth(300);
		tvCases.getColumns().add(tcSummary);

		tcMetadata = new TableColumn<>("Metadata");
		tcMetadata.setCellValueFactory(new PropertyValueFactory<CaseBean, String>("metadata"));
		tcMetadata.setPrefWidth(250);
		tvCases.getColumns().add(tcMetadata);

		// Remove all but the ref column (We'll add them back after building the summary columns);
		tvCases.getColumns().remove(1, 4);

		// Add columns for the type's summary attributes (in model order)
		for (String saName : saNames)
		{
			TableColumn<CaseBean, String> col = new TableColumn<>(saName);
			tvCases.getColumns().add(col);
			col.setCellValueFactory(new PropertyValueFactory<CaseBean, String>("summary")
			{
				@Override
				public ObservableValue<String> call(CellDataFeatures<CaseBean, String> param)
				{
					String result = "<error>";
					String summary = param.getValue().summaryProperty().get();
					if (summary == null)
					{
						result = "<not available>";
					}
					else
					{
						try
						{
							JsonNode root = new ObjectMapper().readTree(summary);
							JsonNode value = null;
							value = root.at("/" + saName);
							result = value instanceof MissingNode ? NOT_SET
									: (value instanceof ObjectNode ? value.toString() : value.asText());
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return new SimpleStringProperty(result);
				}
			});
		}
		tvCases.getColumns().addAll(tcCasedata, tcSummary, tcMetadata);

		tvCases.getSelectionModel().selectedItemProperty().addListener((obs, old, nu) -> {
			try
			{
				refreshSecondaryPanelsFromSelectedCase();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			buttonLinks.setDisable(nu == null);
		});

		taBottom.textProperty().addListener((old, nu, obs) -> {
			buttonUpdate.setDisable(false);
		});

		cbExcludeTerminal.selectedProperty().addListener(e -> {
			try
			{
				refresh();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

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

	private void refreshSecondaryPanelsFromSelectedCase() throws IOException
	{
		CaseBean bean = tvCases.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			String prettyRaw = prettyUp(bean.rawProperty().get());

			if (rawCtrl != null)
			{
				selectRaw = rawCtrl.getIsRaw();
			}

			GenericJsonController ctrl = GenericJsonController.makeVBox(prettyRaw, null);
			vbRaw = ctrl.getUI();
			ObservableList<Node> children = vbRight.getChildren();
			children.clear();
			children.add(vbRaw);
			ctrl.selectRaw(selectRaw);
			rawCtrl = ctrl;

			String casedata = bean.casedataProperty().get();
			taBottom.setText(casedata != null ? prettyUp(casedata) : "No casedata");
			buttonUpdate.setDisable(true);
		}
	}

	public void setType(String type)
	{
		stage.setTitle("Cases - " + type + " - ACE Cannon");
	}

	public Stage getStage()
	{
		return stage;
	}

	private void refresh() throws IOException
	{
		String skip = tfSkip.getText().trim();
		String top = tfTop.getText().trim();
		String cid = tfCid.getText().trim();
		String stateValue = tfStateValue.getText().trim();
		String select = tfSelect.getText().trim();
		String search = tfSearch.getText().trim();
		String dql = tfDQL.getText().trim();
		boolean excludeTerminal = cbExcludeTerminal.isSelected();

		String modificationTimestamp = tfModificationTimestamp.getText().trim();
		String query = (search.equals("") ? "" : "$search=" + URLEncoder.encode(search, "UTF-8") + "&")
				+ (select.equals("") ? "" : "$select=" + URLEncoder.encode(select, "UTF-8") + "&")
				+ (dql.equals("") ? "" : "$dql=" + URLEncoder.encode(dql, "UTF-8") + "&")
				+ (skip.equals("") ? "" : "$skip=" + URLEncoder.encode(skip, "UTF-8") + "&")
				+ (top.equals("") ? "" : "$top=" + URLEncoder.encode(top, "UTF-8") + "&") + "$filter="
				+ URLEncoder.encode("caseType eq '" + caseType + "' and applicationMajorVersion eq " + majorVersion
						+ (cid.equals("") ? "" : " and cid eq '" + cid + "'")
						+ (stateValue.equals("") ? "" : " and caseState eq '" + stateValue + "'")
						+ (modificationTimestamp.equals("") ? ""
								: " and modificationTimestamp le " + modificationTimestamp)
						+ (excludeTerminal ? " and isInTerminalState eq FALSE" : ""), "UTF-8");
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases?" + query;
		//		lblStatus.setText(url);
		//		lblStatus.setTooltip(new Tooltip(url));
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		try
		{
			caller.call(true);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		String responseBody = caller.getResponseBody();
		JsonNode root = om.readTree(responseBody);
		if (root instanceof ArrayNode)
		{
			ArrayNode an = (ArrayNode) root;
			tvCases.setItems(FXCollections.observableArrayList());
			for (int i = 0; i < an.size(); i++)
			{
				JsonNode entryNode = an.get(i);
				if (entryNode instanceof ObjectNode)
				{
					ObjectNode caseNode = (ObjectNode) entryNode;
					CaseBean bean = new CaseBean();
					populateBean(bean, caseNode);
					tvCases.getItems().add(bean);
				}
			}
		}
		if (tvCases.getItems().size() > 0)
		{
			tvCases.getSelectionModel().select(0);
		}
	}

	private void populateBean(CaseBean bean, ObjectNode caseNode) throws JsonProcessingException
	{
		bean.rawProperty().set(om.writeValueAsString(caseNode));
		if (caseNode.has("caseReference"))
		{
			bean.caseReferenceProperty().set(caseNode.get("caseReference").asText());
		}
		if (caseNode.has("casedata"))
		{
			bean.casedataProperty().set(caseNode.get("casedata").asText());
		}
		if (caseNode.has("metadata"))
		{
			bean.metadataProperty().set(om.writeValueAsString(caseNode.get("metadata")));
		}
		if (caseNode.has("summary"))
		{
			bean.summaryProperty().set(caseNode.get("summary").asText());
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

	private List<String> getStateValues(String type, int majorVersion) throws UnsupportedEncodingException, SQLException
	{
		String namespace = type.substring(0, type.lastIndexOf('.'));
		DataModel dm = dao.getDataModel(namespace, majorVersion);
		StructuredType st = dm.getStructuredTypeByName(type.substring(type.lastIndexOf('.') + 1));
		return st.getStateModel().getStates().stream().map(State::getValue).collect(Collectors.toList());
	}

	@FXML
	public void onButtonDeleteClicked(MouseEvent event)
	{
		try
		{
			// Determine all possible state values for the case type
			List<String> stateValues = getStateValues(caseType, majorVersion);
			System.out.println(stateValues);
			for (String stateValue : stateValues)
			{
				String query = "$filter=" + URLEncoder.encode("caseState eq '" + stateValue + "' and caseType eq '"
						+ caseType + "' and applicationMajorVersion eq " + majorVersion
						+ " and modificationTimestamp le 2999TZ", "UTF-8");
				String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases?"
						+ query;
				HTTPCaller caller = HTTPCaller.newDelete(url, null);
				caller.call();
				AceMain.log("Number of " + stateValue + " cases deleted: " + caller.getResponseBody());
			}
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e)
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

	@FXML
	public void onCasesTableClicked(MouseEvent event)
	{
		if (event.getClickCount() >= 2)
		{
			CaseBean bean = tvCases.getSelectionModel().getSelectedItem();
			if (bean != null)
			{
				String ref = bean.caseReferenceProperty().get();
				String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases/" + ref
						+ "?$select=c";
				HTTPCaller caller = HTTPCaller.newGet(url, null);
				try
				{
					caller.call(true);
					ObjectNode on = (ObjectNode) om.readTree(caller.getResponseBody());
					String casedata = prettyUp(on.get("casedata").asText());
					GenericJsonController.makeStage(bean.caseReference.get() + " - ACE Cannon", url, casedata).show();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String prettyUp(String json) throws IOException
	{
		JsonNode root = prettyOM.readTree(json);
		String pretty = prettyOM.writeValueAsString(root);
		return pretty;
	}

	@FXML
	public void onButtonLinksClicked(MouseEvent event)
	{
		NavigatorController navigatorController;
		try
		{
			CaseBean caseBean = tvCases.getSelectionModel().getSelectedItem();
			if (caseBean != null)
			{
				navigatorController = NavigatorController.make();
				navigatorController.setDAO(dao);
				navigatorController.setRef(caseBean.caseReferenceProperty().get());
				navigatorController.getStage().show();
			}
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

	public void setSummaryAttributeNames(List<String> saNames)
	{
		this.saNames = saNames;
	}

	@FXML
	public void onButtonUpdateClicked(MouseEvent event)
	{
		CaseBean bean = tvCases.getSelectionModel().getSelectedItem();
		if (bean != null)
		{
			String ref = bean.caseReferenceProperty().get();
			String casedata = taBottom.getText();
			String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases/" + ref;
			JsonNodeFactory fac = JsonNodeFactory.instance;
			ObjectNode root = fac.objectNode();
			root.put("casedata", casedata);
			try
			{
				String body = om.writeValueAsString(root);
				HTTPCaller caller = HTTPCaller.newPut(url, null, body);
				caller.call(true);
				refreshBean(bean);
				tvCases.refresh();
				refreshSecondaryPanelsFromSelectedCase();
				buttonUpdate.setDisable(true);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void refreshBean(CaseBean bean)
	{
		String ref = bean.caseReferenceProperty().get();
		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases/" + ref
				+ "?$select=cr,c,s,m";
		HTTPCaller caller = HTTPCaller.newGet(url, null);
		try
		{
			caller.call(true);
			ObjectNode on = (ObjectNode) om.readTree(caller.getResponseBody());
			populateBean(bean, on);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
