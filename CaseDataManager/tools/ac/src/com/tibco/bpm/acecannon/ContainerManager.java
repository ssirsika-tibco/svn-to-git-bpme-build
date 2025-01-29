/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.container.ContainerInfo;
import com.tibco.bpm.acecannon.container.ContainerInfoBean;
import com.tibco.bpm.acecannon.container.DockerAPI;
import com.tibco.bpm.acecannon.container.DockerStateModel;
import com.tibco.bpm.acecannon.container.DockerStateModel.Action;
import com.tibco.bpm.acecannon.container.LoadFactorPollerThread;
import com.tibco.bpm.acecannon.container.StatSponge;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * UI for manipulating Docker containers
 *
 * <p/>&copy;2016 TIBCO Software Inc.
 * @author smorgan
 * @since 2016
 */
public class ContainerManager extends VBox
{
	//TODO Call /loadFactor API for each CM (and maybe other) instance
	// Load Factor graph

	private static final int					DEFAULT_HTTP_PORT	= 7_777;

	private static final int					DEFAULT_DEBUG_PORT	= 6_661;

	private DockerAPI							api;

	private int									nextCMNumber		= 100;

	private int									nextXXNumber		= 100;

	private TableView<ContainerInfoBean>		containerTable;

	private ObservableList<ContainerInfoBean>	containerList		= FXCollections.observableArrayList();

	private Label								dockerLabel;

	Button										buttonRefresh;

	Button										buttonCreateCM;

	Button										buttonCreateXX;

	Button										buttonClone;

	Button										buttonPause;

	Button										buttonUnpause;

	Button										buttonStart;

	Button										buttonStop;

	Button										buttonRemove;

	Label										cmOnlyLabel;

	CheckBox									cmOnly;

	boolean										onlyShowCM			= false;

	private StatSponge							sponge;

	private MainController						main;

	private TextConsumer						logger				= ((text) -> {
																		String oldText = dockerLabel.getText();
																		String newText = oldText += (oldText
																				.length() > 0 ? "\n" : "") + text;
																		String[] lines = newText.split("[\n]+");
																		if (lines.length >= 6)
																		{
																			newText = newText
																					.substring(lines[0].length() + 1);
																		}
																		final String finalText = newText;
																		Platform.runLater(
																				() -> dockerLabel.setText(finalText));
																	});

	private static SimpleDateFormat				format				= new SimpleDateFormat("HH:mm:ss");

	private Stage								stage;

	private List<LoadFactorPollerThread>		pollers				= new ArrayList<>();

	// Define at object level so start failures due to used ports can be ignored, but subsequent create attempts will use a new number
	int											highestHTTP			= 0;

	int											highestDebug		= 0;

	private LineChartPane						cpuLCP;

	private LineChartPane						lfLCP;

	private Stage								lcpStage;

	private String getLoadFactorPath(String image)
	{
		String result;
		if (image.contains("bpm-docker.emea.tibco.com:443/client/up"))
		{
			result = "/loadFactor";
		}
		else if (image.contains("bpm-docker.emea.tibco.com:443/client/wr"))
		{
			result = "/loadFactor";
		}
		else if (image.contains("bpm-docker.emea.tibco.com:443/runtime"))
		{
			result = "/bpm/loadFactor";
		}
		else if (image.contains("bpm-docker.emea.tibco.com:443/design"))
		{
			result = "/dt-service/loadFactor";
		}
		else if (image.contains("bpm-docker.emea.tibco.com:443/client/clientstate"))
		{
			result = "/clientState/loadFactor";
		}
		else
		{
			result = "/rest/loadFactor";
		}
		return result;
	}

	public StatSponge getSponge()
	{
		return sponge;
	}

	public List<LoadFactorPollerThread> getPollers()
	{
		return pollers;
	}

	public MainController getMain()
	{
		return main;
	}

	public static void putInStage(ContainerManager conMan, Stage stage)
	{
		conMan.stage = stage;
		//		stage.setWidth(ConfigManager.INSTANCE.getConfig().getUI().getConmanWidth());
		//		stage.setHeight(ConfigManager.INSTANCE.getConfig().getUI().getConmanHeight());

		stage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		stage.setTitle("Docker Containers - Case Cannon");
		Scene scene = new Scene(conMan);
		scene.getStylesheets().add(ContainerManager.class.getResource("application.css").toExternalForm());
		stage.setScene(scene);

		stage.setOnCloseRequest(ev -> {
			//			ConfigManager.INSTANCE.getConfig().getUI().setConmanWidth(stage.getWidth());
			//			ConfigManager.INSTANCE.getConfig().getUI().setConmanHeight(stage.getHeight());
			StatSponge sponge = conMan.getSponge();
			if (sponge != null)
			{
				sponge.term();
			}
			for (LoadFactorPollerThread p : conMan.getPollers())
			{
				p.term();
				p.interrupt();
			}
			MainController main = conMan.getMain();
			if (main != null)
			{
				main.onContainerManagerClose();
			}
		});
	}

	public static Stage makeStage(ContainerManager conMan)
	{
		Stage stage = new Stage();
		conMan.stage = stage;
		//		stage.setWidth(ConfigManager.INSTANCE.getConfig().getUI().getConmanWidth());
		//		stage.setHeight(ConfigManager.INSTANCE.getConfig().getUI().getConmanHeight());
		stage.setWidth(960);
		stage.setHeight(400);

		stage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		stage.setTitle("Docker Containers - ACE Cannon");
		Scene scene = new Scene(conMan);
		scene.getStylesheets().add(ContainerManager.class.getResource("application.css").toExternalForm());
		stage.setScene(scene);

		stage.setOnCloseRequest(ev -> {
			//			ConfigManager.INSTANCE.getConfig().getUI().setConmanWidth(stage.getWidth());
			//			ConfigManager.INSTANCE.getConfig().getUI().setConmanHeight(stage.getHeight());
			StatSponge sponge = conMan.getSponge();
			if (sponge != null)
			{
				sponge.term();
			}
			for (LoadFactorPollerThread p : conMan.getPollers())
			{
				p.term();
				p.interrupt();
			}
			conMan.cpuLCP.term();
			//			conMan.lfLCP.term();
			//			conMan.lcpStage.close();
			MainController main = conMan.getMain();
			if (main != null)
			{
				main.onContainerManagerClose();
			}
		});

		return stage;
	}

	public ContainerManager(MainController main, DockerAPI api)
	{

		//		lcpStage = new Stage();
		//		lcpStage.setTitle("CPU %");
		//		lcp = new LineChartPane("Time (s)", "CPU", true, 100d, false);
		//		Scene lcpScene = new Scene(lcp);
		//		lcpStage.setScene(lcpScene);
		//		lcpStage.setWidth(600);
		//		lcpStage.setHeight(600);
		//		lcpStage.hide();
		//		lcpStage.setOnCloseRequest(ev -> {
		//			lcp.term();
		//		});

		this.main = main;
		this.api = api;
		setVgrow(this, Priority.ALWAYS);

		containerTable = new TableView<ContainerInfoBean>();
		containerTable.setPrefHeight(1000);
		//		containerTable.setMaxHeight(2000);
		containerTable.setItems(containerList);
		containerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		containerTable
				.setTooltip(new Tooltip("Double-click a container to view a plethora of 'interesting' information"));
		containerTable.setPlaceholder(new Label("Waiting for container information..."));

		TableColumn<ContainerInfoBean, String> colId = new TableColumn<>("Id");
		colId.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("id"));
		colId.setPrefWidth(100);

		TableColumn<ContainerInfoBean, String> colName = new TableColumn<>("Name");
		colName.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("name"));
		colName.setPrefWidth(100);

		TableColumn<ContainerInfoBean, String> colStatus = new TableColumn<>("Status");
		colStatus.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("status"));
		colStatus.setPrefWidth(100);

		TableColumn<ContainerInfoBean, Integer> colHttpPort = new TableColumn<>("HTTP");
		colHttpPort.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Integer>("httpPort"));
		colHttpPort.setPrefWidth(50);

		TableColumn<ContainerInfoBean, Integer> colDebugPort = new TableColumn<>("Debug");
		colDebugPort.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Integer>("debugPort"));
		colDebugPort.setPrefWidth(50);

		TableColumn<ContainerInfoBean, Integer> colRestartCount = new TableColumn<>("Restarts");
		colRestartCount.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Integer>("restartCount"));
		colRestartCount.setPrefWidth(50);

		TableColumn<ContainerInfoBean, String> colCreated = new TableColumn<>("Created");
		colCreated.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("created"));
		colCreated.setPrefWidth(110);

		TableColumn<ContainerInfoBean, String> colIp = new TableColumn<>("IP");
		colIp.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("ip"));
		colIp.setPrefWidth(80);

		TableColumn<ContainerInfoBean, String> colVersion = new TableColumn<>("Version");
		colVersion.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("version"));
		colVersion.setPrefWidth(60);

		TableColumn<ContainerInfoBean, String> colStarted = new TableColumn<>("Started");
		colStarted.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, String>("started"));
		colStarted.setPrefWidth(110);

		//		TableColumn<ContainerInfoBean, Double> colLoadFactor = new TableColumn<>("LoadFactor");
		//		colLoadFactor.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Double>("loadFactor"));
		//		colLoadFactor.setCellFactory(
		//				new Callback<TableColumn<ContainerInfoBean, Double>, TableCell<ContainerInfoBean, Double>>()
		//				{
		//					@Override
		//					public TableCell<ContainerInfoBean, Double> call(TableColumn<ContainerInfoBean, Double> param)
		//					{
		//						return new ProgressBarTableCell<ContainerInfoBean>()
		//						{
		//							@Override
		//							public void updateItem(Double val, boolean empty)
		//							{
		//								super.updateItem(val, empty);
		//								List<String> sc = getStyleClass();
		//								if (!empty && val != null && val >= 0.75d)
		//								{
		//									if (val >= 1.0d)
		//									{
		//										sc.remove("green-bar");
		//										sc.remove("orange-bar");
		//										sc.add("red-bar");
		//									}
		//									else
		//									{
		//										sc.remove("green-bar");
		//										sc.remove("red-bar");
		//										sc.add("orange-bar");
		//									}
		//								}
		//								else
		//								{
		//									sc.remove("red-bar");
		//									sc.remove("orange-bar");
		//									sc.add("green-bar");
		//								}
		//							}
		//						};
		//					}
		//				});
		//
		//		colLoadFactor.setPrefWidth(110);

		TableColumn<ContainerInfoBean, Double> colLoadFactorNumber = new TableColumn<>("LF#");
		colLoadFactorNumber.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Double>("loadFactor"));
		colLoadFactorNumber.setPrefWidth(30);

		TableColumn<ContainerInfoBean, Double> colCPU = new TableColumn<>("CPU");
		colCPU.setCellValueFactory(new PropertyValueFactory<ContainerInfoBean, Double>("cpu"));
		colCPU.setCellFactory(
				new Callback<TableColumn<ContainerInfoBean, Double>, TableCell<ContainerInfoBean, Double>>()
				{
					@Override
					public TableCell<ContainerInfoBean, Double> call(TableColumn<ContainerInfoBean, Double> param)
					{
						return new ProgressBarTableCell<ContainerInfoBean>()
						{
							@Override
							public void updateItem(Double cpu, boolean empty)
							{
								super.updateItem(cpu, empty);
								List<String> sc = getStyleClass();
								if (!empty && cpu != null && cpu >= 0.75d)
								{
									if (cpu >= 1.0d)
									{
										sc.remove("green-bar");
										sc.remove("orange-bar");
										sc.add("red-bar");
									}
									else
									{
										sc.remove("green-bar");
										sc.remove("red-bar");
										sc.add("orange-bar");
									}
								}
								else
								{
									sc.remove("red-bar");
									sc.remove("orange-bar");
									sc.add("green-bar");
								}
							}
						};
					}
				});
		colCPU.setPrefWidth(110);

		containerTable.getColumns().add(colName);
		containerTable.getColumns().add(colStatus);
		containerTable.getColumns().add(colCPU);
		//		containerTable.getColumns().add(colLoadFactor);
		containerTable.getColumns().add(colVersion);
		containerTable.getColumns().add(colHttpPort);
		containerTable.getColumns().add(colDebugPort);
		containerTable.getColumns().add(colLoadFactorNumber);
		containerTable.getColumns().add(colIp);
		containerTable.getColumns().add(colCreated);
		containerTable.getColumns().add(colStarted);
		containerTable.getColumns().add(colId);
		containerTable.getColumns().add(colRestartCount);
		containerTable.getSortOrder().add(colName);
		colName.setSortable(true);
		colName.setSortType(SortType.ASCENDING);

		containerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			List<ContainerInfoBean> beans = new ArrayList<>();
			//TODO This, and anywhere else that calls getSelectedItem to call
			// a method that takes the table and returns a COPY of the items list.
			beans.addAll(containerTable.getSelectionModel().getSelectedItems());
			handleSelectionChange(beans);
		});

		containerTable.setOnKeyReleased((ev) -> {
			List<ContainerInfoBean> beans = new ArrayList<>();
			beans.addAll(containerTable.getSelectionModel().getSelectedItems());
			handleSelectionChange(beans);
		});

		containerTable.setOnMouseClicked((ev) -> handleTableClick(ev));

		dockerLabel = new Label();
		dockerLabel.setMaxHeight(100);
		dockerLabel.setMinHeight(100);
		dockerLabel.setAlignment(Pos.TOP_LEFT);
		buttonRefresh = new Button("Refresh");
		buttonRefresh.setOnMouseClicked(ev -> {
			new Thread(() -> getInfo()).start();
		});

		//		buttonCreateCM = new Button("Create CM");
		//		buttonCreateCM.setOnMouseClicked(ev -> {
		//			new Thread(() -> createCM()).start();
		//		});

		//		buttonCreateXX = new Button("Create XX");
		//		buttonCreateXX.setOnMouseClicked(ev -> {
		//			new Thread(() -> createXX()).start();
		//		});

		buttonClone = new Button("Clone");
		buttonClone.setTooltip(new Tooltip(
				"Clone the selected container using the same image & environment, but with new name and ports"));
		buttonClone.setOnMouseClicked(ev -> {
			Alert alert = new Alert(AlertType.INFORMATION, "Clone feature coming soon!");
			alert.showAndWait();
			//new Thread(() -> cloneContainer()).start();
		});

		buttonPause = new Button("Pause");
		buttonPause.setOnMouseClicked(ev -> {
			final List<ContainerInfoBean> items = copySelection(containerTable);
			new Thread(() -> pauseAll(items)).start();
		});

		buttonUnpause = new Button("Unpause");
		buttonUnpause.setOnMouseClicked(ev -> {
			final List<ContainerInfoBean> items = copySelection(containerTable);
			new Thread(() -> unpauseAll(items)).start();
		});

		buttonStart = new Button("Start");
		buttonStart.setOnMouseClicked(ev -> {
			final List<ContainerInfoBean> items = copySelection(containerTable);
			new Thread(() -> startAll(items)).start();
		});

		buttonStop = new Button("Stop");
		buttonStop.setOnMouseClicked(ev -> {
			final List<ContainerInfoBean> items = copySelection(containerTable);
			new Thread(() -> stopAll(items)).start();
		});

		buttonRemove = new Button("Remove");
		buttonRemove.setOnMouseClicked(ev -> {
			final List<ContainerInfoBean> items = copySelection(containerTable);
			new Thread(() -> removeAll(items)).start();
		});

		//		Button buttonLCP = new Button("CPU Graph");
		//		buttonLCP.setOnMouseClicked(ev -> {
		//			if (lcpStage.isShowing())
		//			{
		//				lcpStage.hide();
		//			}
		//			else
		//			{
		//				lcpStage.show();
		//			}
		//		});

		// TODO enable once fixed false auto-refresh 
		//		cmOnly = new CheckBox();
		//		cmOnly.selectedProperty().addListener((observable, oldValue, newValue) -> {
		//			onlyShowCM = newValue != null && newValue;
		//			new Thread(() -> getInfo()).start();
		//		});
		//
		//		cmOnlyLabel = new Label("CM only:");

		//		HBox hboxConfig = new HBox();
		//		hboxConfig.getChildren().addAll(cmOnlyLabel, cmOnly);
		//		hboxConfig.setSpacing(2);
		//		hboxConfig.setAlignment(Pos.CENTER);
		//		hboxConfig.setPadding(new Insets(2, 0, 0, 8));

		HBox hboxMisc = new HBox();
		hboxMisc.getChildren().addAll(buttonRefresh, buttonClone);
		hboxMisc.setSpacing(2);
		hboxMisc.setPadding(new Insets(2, 0, 0, 2));

		HBox hboxPausing = new HBox();
		hboxPausing.getChildren().addAll(buttonPause, buttonUnpause);
		hboxPausing.setSpacing(2);
		hboxPausing.setPadding(new Insets(2, 0, 0, 8));

		HBox hboxStarting = new HBox();
		hboxStarting.getChildren().addAll(buttonStart, buttonStop, buttonRemove);
		hboxStarting.setSpacing(2);
		hboxStarting.setPadding(new Insets(2, 0, 0, 8));

		HBox hboxButtons = new HBox();
		hboxButtons.setSpacing(5);
		hboxButtons.setPadding(new Insets(3,5,5,5));
		hboxButtons.getChildren().addAll(hboxMisc, hboxPausing, hboxStarting); //,hboxConfig);

		//		VBox vboxMaster = new VBox();
		//		VBox.setVgrow(vboxMaster, Priority.ALWAYS);
		//		vboxMaster.setSpacing(2);
		//		vboxMaster.setPadding(new Insets(2, 0, 0, 2));
		//		vboxMaster.getChildren().addAll(hboxButtons, containerTable, dockerLabel);
		//		getChildren().add(vboxMaster);

		SplitPane sp = new SplitPane();
		cpuLCP = new LineChartPane("CPU usage", "conman-cpu", "Time (s)", "CPU %", true, 100d, false);
		cpuLCP.start();

		//		lfLCP = new LineChartPane("Load Factor", "conman-lf", "Time (s)", "Load Factor", true, 100d, false);
		//		lfLCP.start();

		SplitPane rightSplit = new SplitPane();
		//		rightSplit.getItems().add(lfLCP);
		rightSplit.getItems().add(cpuLCP);
		rightSplit.setOrientation(Orientation.VERTICAL);
		rightSplit.setDividerPosition(0, 0.6);

		sp.getItems().add(containerTable);
		sp.getItems().add(rightSplit);

		setSpacing(2);
		setPadding(new Insets(2, 0, 0, 2));
		getChildren().addAll(sp, dockerLabel, hboxButtons);

		sponge = new StatSponge(this);
		sponge.start();
	}

	private void handleTableClick(MouseEvent ev)
	{
		if (ev.getClickCount() >= 2)
		{
			ObservableList<ContainerInfoBean> selectedItems = containerTable.getSelectionModel().getSelectedItems();
			if (selectedItems.size() == 1)
			{
				String name = selectedItems.get(0).nameProperty().get();
				String inspection = selectedItems.get(0).inspectionProperty().get();
				openInspectionWindow(name, inspection);
			}
		}
	}

	private TreeItem<String> convertJsonNodeToTreeItem(JsonNode node, String prefix)
	{
		TreeItem<String> ti;
		if (node instanceof ArrayNode)
		{
			ArrayNode an = (ArrayNode) node;
			ti = new TreeItem<String>(prefix + " (" + (an.size() == 0 ? "empty " : "") + "array)");
			for (int i = 0; i < an.size(); i++)
			{
				ti.getChildren().add(convertJsonNodeToTreeItem(an.get(i), "[" + i + "]"));
			}
		}
		else if (node instanceof ObjectNode)
		{
			ti = new TreeItem<String>(prefix);
			for (Iterator<Entry<String, JsonNode>> iter = node.fields(); iter.hasNext();)
			{
				Entry<String, JsonNode> entry = iter.next();
				if (entry.getValue() instanceof ArrayNode || entry.getValue() instanceof ObjectNode)
				{
					ti.getChildren().add(convertJsonNodeToTreeItem(entry.getValue(), entry.getKey()));
				}
				else
				{
					String msg = entry.getKey() + ": " + entry.getValue().asText();
					TreeItem<String> item = new TreeItem<String>(msg);
					ti.getChildren().add(item);
				}
			}
		}
		else
		{
			ti = new TreeItem<String>(prefix + " " + node.asText());
		}
		return ti;
	}

	private void openInspectionWindow(String name, String json)
	{
		try
		{
			Stage inspStage = new Stage();
			inspStage.setWidth(750);
			inspStage.setHeight(530);

			VBox vbox = new VBox();
			vbox.setSpacing(2);
			HBox hbox = new HBox();
			hbox.setSpacing(20);
			hbox.setPadding(new Insets(2, 0, 0, 2));

			final ToggleGroup tGroup = new ToggleGroup();
			RadioButton rbTree = new RadioButton("Tree");
			rbTree.setToggleGroup(tGroup);
			rbTree.setSelected(true);
			RadioButton rbJSON = new RadioButton("JSON");
			rbJSON.setToggleGroup(tGroup);

			ObjectMapper om = new ObjectMapper();
			om.enable(SerializationFeature.INDENT_OUTPUT);
			JsonNode rootNode = om.readTree(json);
			String jsonPretty = om.writeValueAsString(rootNode);
			TextArea ta = new TextArea(jsonPretty);
			ta.setEditable(false);
			TreeView<String> tv = new TreeView<String>();
			JsonNode interestingRoot = rootNode instanceof ArrayNode ? ((ArrayNode) rootNode).get(0) : rootNode;
			tv.setRoot(convertJsonNodeToTreeItem(interestingRoot, "Container '" + name + "'"));
			tv.getRoot().setExpanded(true);

			hbox.getChildren().addAll(rbTree, rbJSON);
			vbox.getChildren().addAll(hbox, tv);
			Scene scene = new Scene(vbox);
			tv.setPrefWidth(2000);
			tv.setPrefHeight(2000);
			tGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
			{
				public void changed(ObservableValue< ? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle)
				{
					if (tGroup.getSelectedToggle() != null)
					{
						Toggle selectedToggle = tGroup.getSelectedToggle();
						if (selectedToggle.equals(rbTree))
						{
							vbox.getChildren().add(tv);
							tv.setPrefSize(scene.getWidth(), scene.getHeight());
							vbox.getChildren().remove(ta);
						}
						else
						{
							vbox.getChildren().add(ta);
							ta.setPrefSize(scene.getWidth(), scene.getHeight());
							vbox.getChildren().remove(tv);
						}
					}
				}
			});
			inspStage.setScene(scene);
			inspStage.setTitle("Container Inspection: " + name);
			inspStage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			inspStage.widthProperty().addListener(new ChangeListener<Number>()
			{
				@Override
				public void changed(ObservableValue< ? extends Number> observableValue, Number oldSceneWidth,
						Number newSceneWidth)
				{
					ta.setPrefWidth(newSceneWidth.intValue());
					tv.setPrefWidth(newSceneWidth.intValue());
				}
			});
			inspStage.heightProperty().addListener(new ChangeListener<Number>()
			{
				@Override
				public void changed(ObservableValue< ? extends Number> observableValue, Number oldSceneHeight,
						Number newSceneHeight)
				{
					ta.setPrefHeight(newSceneHeight.intValue());
					tv.setPrefHeight(newSceneHeight.intValue());
				}
			});
			inspStage.show();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private List<ContainerInfoBean> copySelection(TableView<ContainerInfoBean> table)
	{
		// Copy the list of selected items in the given table.  The resulting list can then
		// be safely used without picking up later changes to the selection.
		List<ContainerInfoBean> beans = new ArrayList<>();
		beans.addAll(table.getSelectionModel().getSelectedItems());
		return beans;
	}

	private void handleSelectionChange(List<ContainerInfoBean> selectedItems)
	{
		// TODO Only do this if NOT waiting for Docker
		setButtonEnablement(true);
	}

	public void setButtonEnablement(boolean state)
	{
		if (state)
		{
			// Enable just those button applicable based on the table selection
			Platform.runLater(() -> {
				buttonRefresh.setDisable(false);
				//				buttonCreateCM.setDisable(false);
				//				buttonCreateXX.setDisable(false);
				Set<String> states = new HashSet<String>();
				List<ContainerInfoBean> selection = copySelection(containerTable);
				for (ContainerInfoBean cib : selection)
				{
					String string = cib.statusProperty().get();
					if (string != null)
					{
						states.add(string);
					}
				}

				List<Action> actions = new DockerStateModel().getActionsAllowedInStates(states);
				buttonStart.setDisable(actions == null || !actions.contains(Action.START));
				buttonStop.setDisable(actions == null || !actions.contains(Action.STOP));
				buttonPause.setDisable(actions == null || !actions.contains(Action.PAUSE));
				buttonUnpause.setDisable(actions == null || !actions.contains(Action.UNPAUSE));
				buttonRemove.setDisable(actions == null || !actions.contains(Action.REMOVE));
				buttonClone.setDisable(selection.size() != 1 || actions == null || !actions.contains(Action.CLONE));
				//				cmOnlyLabel.setDisable(false);
				//				cmOnly.setDisable(false);
			});
		}
		else
		{
			// Disable all buttons
			Platform.runLater(() -> {
				buttonRefresh.setDisable(true);
				//				buttonCreateCM.setDisable(true);
				//				buttonCreateXX.setDisable(true);
				buttonClone.setDisable(true);
				buttonPause.setDisable(true);
				buttonUnpause.setDisable(true);
				buttonStart.setDisable(true);
				buttonStop.setDisable(true);
				buttonRemove.setDisable(true);
				//				cmOnlyLabel.setDisable(true);
				//				cmOnly.setDisable(true);
			});
		}
	}

	public TextConsumer getLogger()
	{
		return logger;
	}

	private void pauseAll(List<ContainerInfoBean> items)
	{
		setButtonEnablement(false);
		for (ContainerInfoBean item : items)
		{
			String name = item.nameProperty().get();
			api.pause(name);
		}
		getInfo();
		setButtonEnablement(true);
	}

	private void unpauseAll(List<ContainerInfoBean> items)
	{
		setButtonEnablement(false);
		for (ContainerInfoBean item : items)
		{
			String name = item.nameProperty().get();
			api.unpause(name);
		}
		getInfo();
		setButtonEnablement(true);
	}

	private void startAll(List<ContainerInfoBean> items)
	{
		setButtonEnablement(false);
		for (ContainerInfoBean item : items)
		{
			String name = item.nameProperty().get();
			api.start(name);
		}
		getInfo();
		setButtonEnablement(true);
	}

	private void stopAll(List<ContainerInfoBean> items)
	{
		setButtonEnablement(false);
		for (ContainerInfoBean item : items)
		{
			String name = item.nameProperty().get();
			api.stop(name);
		}
		getInfo();
		setButtonEnablement(true);
	}

	private void removeAll(List<ContainerInfoBean> items)
	{
		setButtonEnablement(false);
		for (ContainerInfoBean item : items)
		{
			String name = item.nameProperty().get();
			api.rm(name);
		}
		containerTable.getSelectionModel().clearSelection();
		getInfo();
		setButtonEnablement(true);
	}

	private ContainerInfoBean getBeanById(String id)
	{
		//TODO b shouldn't be null
		return containerList.stream().filter(b -> b != null && b.idProperty().get().startsWith(id)).findFirst()
				.orElse(null);
	}

	public boolean updateLoadFactor(String containerId, Double lf)
	{
		boolean result = false;
		ContainerInfoBean bean = getBeanById(containerId);
		if (bean != null)
		{
			bean.updateLoadFactor(lf);
			// Return true to indicate that LF was applied to something
			result = true;
		}
		return result;
	}

	//	public void updateCPU(String containerId, Double cpu)
	//	{
	//		ContainerInfoBean bean = getBeanById(containerId);
	//		if (bean != null)
	//		{
	//			bean.updateCPU(cpu);
	//		}
	//	}

	public void updateCPUs(Map<String, Double> cpu)
	{
		List<ContainerInfoBean> seenContainers = new ArrayList<>();
		boolean seenExtras = false;

		synchronized (containerList)
		{
			Long activeCount = containerList.stream().filter(cib -> !cib.statusProperty().get().equals("exited")
					&& !cib.statusProperty().get().equals("created")).collect(Collectors.counting());

			for (Entry<String, Double> entry : cpu.entrySet())
			{
				ContainerInfoBean bean = getBeanById(entry.getKey());
				if (bean == null)
				{
					seenExtras = true;
				}
				else
				{
					bean.updateCPU(entry.getValue());
					cpuLCP.setValue(bean.nameProperty().get(), Math.min(100d, entry.getValue()));
					seenContainers.add(bean);
				}
			}

			// Zero CPU for unseen containers
			for (ContainerInfoBean cib : containerList)
			{
				if (!seenContainers.contains(cib))
				{
					cib.cpuProperty().set(0d);
				}
			}

			if (seenExtras || seenContainers.size() < activeCount)
			{
				log("Auto-refreshing...");
				getInfo();
			}

		}
	}

	private void log(String msg)
	{
		msg = msg.trim();
		Calendar stamp = Calendar.getInstance();
		msg = format.format(stamp.getTime()) + "  " + msg;
		AceMain.log(msg);
		if (logger != null)
		{
			logger.consume(msg);
		}
	}

	void getInfo()
	{
		setButtonEnablement(false);
		List<ContainerInfo> infos = api.getInfo(onlyShowCM);
		List<ContainerInfoBean> dirtyBeans = new ArrayList<>();

		synchronized (containerList)
		{

			// Merge into beans, updating when present, adding if not.
			for (ContainerInfo info : infos)
			{
				ContainerInfoBean targetBean = null;
				String name = info.getName();
				for (ContainerInfoBean aBean : containerList)
				{
					if (aBean.nameProperty().get().equals(name))
					{
						// Found it. We'll put the data into this bean.
						targetBean = aBean;
						break;
					}
				}

				// If we didn't find it, add a new one.
				if (targetBean == null)
				{
					targetBean = new ContainerInfoBean();
					containerList.add(targetBean);
					cpuLCP.addSeries(name);
				}

				//				// If there isn't already a LFPT for this container, but could be, start one.
				//				String image = info.getImage();
				//				if (image.contains("bpm-docker.emea.tibco.com:443/client")
				//						|| image.contains("bpm-docker.emea.tibco.com:443/runtime")
				//						|| image.contains("bpm-docker.emea.tibco.com:443/design"))
				//				{
				//					if (pollers.stream().filter(p -> p.getContainerId().equals(info.getId())).findFirst()
				//							.orElse(null) == null)
				//					{
				//						if (info.getHttpPort() == null)
				//						{
				//							log(info.getName()
				//									+ "'s HTTP port is not exposed, so can't get loadFactor. Add a mapping in your docker-compose.yml.");
				//						}
				//						else
				//						{
				//							String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getDocker() + ":"
				//									+ info.getHttpPort() + getLoadFactorPath(image);
				//							LoadFactorPollerThread t = new LoadFactorPollerThread(this, info.getId(), info.getName(),
				//									url, lfLCP);
				//							lfLCP.addSeries(info.getName());
				//							pollers.add(t);
				//							t.start();
				//						}
				//					}
				//				}

				// Patch the bean with whatever's changed
				targetBean.apply(info);
				dirtyBeans.add(targetBean);
			}

			// Purge any beans we didn't touch
			for (int i = 0; i < containerList.size();)
			{
				if (!dirtyBeans.contains(containerList.get(i)))
				{
					// Didn't see it this time, so remove. No need to i++ as the next
					// item is now in that position.
					ContainerInfoBean removed = containerList.remove(i);
					// ...and remove from graph.
					String name = removed.nameProperty().get();
					AceMain.log("Removed: " + name);
					cpuLCP.removeSeries(name);
				}
				else
				{
					i++;
				}
			}
		}

		Platform.runLater(() -> {
			containerTable.sort();
		});
		setButtonEnablement(true);
	}

	private boolean containerExistsWithName(String name)
	{
		boolean found = false;
		for (Iterator<ContainerInfoBean> iter = containerList.iterator(); !found && iter.hasNext();)
		{
			ContainerInfoBean cib = iter.next();
			String aName = cib.nameProperty().get();
			if (aName.equals(name))
			{
				found = true;
			}
		}
		return found;
	}

	private boolean containerExistsWithHTTPPort(int portNumber)
	{
		boolean found = false;
		for (Iterator<ContainerInfoBean> iter = containerList.iterator(); !found && iter.hasNext();)
		{
			ContainerInfoBean cib = iter.next();
			int aPort = cib.httpPortProperty().get();
			if (aPort == portNumber)
			{
				found = true;
			}
		}
		return found;
	}

	private boolean containerExistsWithDebugPort(int portNumber)
	{
		boolean found = false;
		for (Iterator<ContainerInfoBean> iter = containerList.iterator(); !found && iter.hasNext();)
		{
			ContainerInfoBean cib = iter.next();
			int aPort = cib.debugPortProperty().get();
			if (aPort == portNumber)
			{
				found = true;
			}
		}
		return found;
	}

	//	private void createCM()
	//	{
	//		setButtonEnablement(false);
	//		int number = 0;
	//		String name = null;
	//		int debugPort = 0;
	//		int httpPort = 0;
	//
	//		while (number == 0 || containerExistsWithName(name))
	//		{
	//			number = nextCMNumber++;
	//			name = "bpm-cm" + number;
	//		}
	//
	//		// Use the default ports, if not already taken
	//		if (!containerExistsWithDebugPort(DEFAULT_DEBUG_PORT) && !containerExistsWithHTTPPort(DEFAULT_HTTP_PORT))
	//		{
	//			debugPort = DEFAULT_DEBUG_PORT;
	//			httpPort = DEFAULT_HTTP_PORT;
	//		}
	//		else
	//		{
	//			debugPort = number + 12000;
	//			httpPort = number + 24000;
	//		}
	//		api.createAndStartCM(name, debugPort, httpPort);
	//		getInfo();
	//		setButtonEnablement(true);
	//	}

	private void cloneContainer()
	{
		final List<ContainerInfoBean> items = copySelection(containerTable);
		// Only support single selection
		if (items.size() == 1)
		{
			ContainerInfoBean existingCIB = items.get(0);
			String existingName = existingCIB.nameProperty().get();

			// Extract existing environment from inspection
			String inspectionJSON = existingCIB.inspectionProperty().get();
			ObjectMapper om = new ObjectMapper();
			try
			{
				JsonNode root = om.readTree(inspectionJSON);
				JsonNode env = root.at("/Config/Env");
				JsonNode image = root.at("/Config/Image");
				if (env instanceof ArrayNode)
				{
					// Write env to temp file
					File file = File.createTempFile("cannon", ".env");
					PrintWriter pw = new PrintWriter(file);
					ArrayNode envArray = (ArrayNode) env;
					for (int i = 0; i < envArray.size(); i++)
					{
						JsonNode envNode = envArray.get(i);
						String envText = envNode.asText();
						AceMain.log(envText);
						pw.println(envText);
					}
					pw.close();
					AceMain.log(pw.toString());

					String imageName = image.asText();

					// Find an available name by appending 100, or a larger number if taken
					int suffix = 99;
					String name = null;
					do
					{
						suffix++;
						String possibleName = existingName + suffix;
						boolean available = containerList.stream()
								.filter(c -> c.nameProperty().get().equals(possibleName)).findFirst()
								.orElse(null) == null;
						if (available)
						{
							name = possibleName;
						}
					}
					while (name == null);

					// Find free port num
					synchronized (containerList)
					{
						for (ContainerInfoBean cib : containerList)
						{
							if (cib.httpPortProperty().get() > highestHTTP)
							{
								highestHTTP = cib.httpPortProperty().get();
							}
							if (cib.debugPortProperty().get() > highestDebug)
							{
								highestDebug = cib.debugPortProperty().get();
							}
						}
					}

					// Use next available number, but at least 30/40000
					highestHTTP++;
					highestDebug++;
					int httpPort = Math.max(30000, highestHTTP);
					int debugPort = Math.max(40000, highestDebug);

					// CM/XX listen for debug on 6661/6662, respectively.  Assume other containers are the conventional 8000.
					int nativeDebugPort = 8000;
					if (imageName.contains("/client/cm"))
					{
						nativeDebugPort = 6661;
					}
					else if (imageName.contains("/client/xx"))
					{
						nativeDebugPort = 6662;
					}
					else if (imageName.contains("/runtime/containeredition"))
					{
						nativeDebugPort = 5005;
					}
					api.createAndStartGeneric(name, imageName, file.toString(), debugPort, nativeDebugPort, httpPort);
				}
				AceMain.log(env.toString());
			}
			catch (JsonProcessingException e)
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
	}

	//	private void createXX()
	//	{
	//		setButtonEnablement(false);
	//		int number = 0;
	//		String name = null;
	//		int debugPort = 0;
	//		int httpPort = 0;
	//
	//		while (number == 0 || containerExistsWithName(name))
	//		{
	//			number = nextXXNumber++;
	//			name = "bpm-xx" + number;
	//		}
	//
	//		// Use the default ports, if not already taken
	//		if (!containerExistsWithDebugPort(DEFAULT_DEBUG_PORT) && !containerExistsWithHTTPPort(DEFAULT_HTTP_PORT))
	//		{
	//			debugPort = DEFAULT_DEBUG_PORT;
	//			httpPort = DEFAULT_HTTP_PORT;
	//		}
	//		else
	//		{
	//			debugPort = number + 36000;
	//			httpPort = number + 48000;
	//		}
	//		api.createAndStartXX(name, debugPort, httpPort);
	//		getInfo();
	//		setButtonEnablement(true);
	//	}

	public void term()
	{
		if (sponge != null)
		{
			sponge.term();
		}
		for (LoadFactorPollerThread p : pollers)
		{
			p.term();
			p.interrupt();
		}
		cpuLCP.term();
		//		lfLCP.term();
		//		lcpStage.close();
		//		ConfigManager.INSTANCE.getConfig().getUI().setConmanWidth(stage.getWidth());
		//		ConfigManager.INSTANCE.getConfig().getUI().setConmanHeight(stage.getHeight());
	}

	public void removeLFP(LoadFactorPollerThread lfp)
	{
		pollers.remove(lfp);
		lfLCP.removeSeries(lfp.getContainerName());
	}

	public Stage getStage()
	{
		return stage;
	}
}
