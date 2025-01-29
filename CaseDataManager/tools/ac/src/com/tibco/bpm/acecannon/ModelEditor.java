/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon;

import java.util.ArrayList;
import java.util.List;

import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.da.dm.api.IdentifierInitialisationInfo;
import com.tibco.bpm.da.dm.api.Issue;
import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.da.dm.api.ValidationResult;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ModelEditor extends VBox
{
	// TODO Allow case type to be passed in and set initial focus/expansion of tree to highlight that type

	// TODO Display type for complex attributes with ... button to switch focus to that type in the tree
	// (testing parameters are still per-content (e.g. different source for home vs. work address, despite being same type)

	DataModel					model;

	Label						label	= new Label("Hello");

	HBox						hboxMain;

	TreeView<TItem>				tv;

	DataModelPropertyPane		dmpp;

	AttributePropertyPane		app;

	StructuredTypePropertyPane	stpp;

	private Button				buttonViewJSON;

	static class TItem
	{
		public Object entity;

		public TItem(Object entity)
		{
			this.entity = entity;
		}

		public String toString()
		{
			String result = null;
			if (entity instanceof StructuredType)
			{
				StructuredType st = (StructuredType) entity;
				result = st.getName() + (st.getIsCase() ? " (case)" : "");
			}
			else if (entity instanceof Attribute)
			{
				Attribute attr = (Attribute) entity;
				result = attr.getName();
				if (attr.getIsArray())
				{
					result += "[]";
				}
			}
			else if (entity instanceof DataModel)
			{
				result = "DataModel";
			}
			else if (entity instanceof String)
			{
				result = (String) entity;
			}
			return result;
		}
	}

	public static Stage makeStage(DataModel model, String title)
	{
		ModelEditor me = new ModelEditor(model);
		Stage stage = new Stage();
		stage.setTitle("Model Viewer - " + title);
		stage.setWidth(800);
		stage.setHeight(600);
		stage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));

		Scene scene = new Scene(me);
		scene.getStylesheets().add(ModelEditor.class.getResource("application.css").toExternalForm());
		stage.setScene(scene);
		return stage;
	}

	public ModelEditor(DataModel model)
	{
		// TODO For now, copy the model. Don't want to persist changes (yet).
		try
		{
			model = DataModel.deserialize(model.serialize());
			System.out.println("Rewritten model:\n" + model.serialize());
		}
		catch (DataModelSerializationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.model = model;

		tv = new TreeView<TItem>();
		tv.setPrefHeight(400);
		TreeItem<TItem> root = new TreeItem<TItem>(new TItem(model));
		populateTree(root, model);
		tv.setRoot(root);
		tv.getRoot().setExpanded(true);
		// Expand top level children
		tv.getRoot().getChildren().stream().forEach(c -> c.setExpanded(true));
		tv.getSelectionModel().selectedItemProperty().addListener((ob, old, nu) -> {
			label.setText(nu.toString());
			if (nu.getValue().entity instanceof Attribute)
			{
				app.bind((Attribute) nu.getValue().entity);
				if (!hboxMain.getChildren().contains(app))
				{
					hboxMain.getChildren().remove(dmpp);
					hboxMain.getChildren().remove(stpp);
					hboxMain.getChildren().add(app);
				}
			}
			else if (nu.getValue().entity instanceof StructuredType)
			{
				stpp.bind((StructuredType) nu.getValue().entity);
				if (!hboxMain.getChildren().contains(stpp))
				{
					hboxMain.getChildren().remove(dmpp);
					hboxMain.getChildren().remove(app);
					hboxMain.getChildren().add(stpp);
				}
			}
			else if (nu.getValue().entity instanceof String)
			{
				hboxMain.getChildren().remove(dmpp);
				hboxMain.getChildren().remove(stpp);
				hboxMain.getChildren().remove(app);
			}
			else
			{
				dmpp.bind((DataModel) nu.getValue().entity);
				if (!hboxMain.getChildren().contains(dmpp))
				{
					hboxMain.getChildren().remove(app);
					hboxMain.getChildren().remove(stpp);
					hboxMain.getChildren().add(dmpp);
				}
			}
		});
		dmpp = makeDataModelPropertyPane();
		dmpp.bind(model);
		app = makeAttributePropertyPane();
		stpp = makeStructuredTypePropertyPane();

		//		buttonViewJSON = new Button("View model as JSON");
		final DataModel fModel = model;
		//		buttonViewJSON.onMouseClickedProperty().set(ev -> {
		//			Stage jsonStage;
		//			try
		//			{
		//				jsonStage = GenericJsonController.makeStage("Data Model JSON", null, fModel.serialize());
		//				jsonStage.show();
		//			}
		//			catch (DataModelSerializationException e)
		//			{
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//
		//		});

		HBox.setHgrow(tv, Priority.ALWAYS);
		hboxMain = new HBox(tv, dmpp);
		hboxMain.setSpacing(5);
		getChildren().addAll(hboxMain);
		setPadding(new Insets(5, 5, 5, 5));
		setSpacing(5);
		setVgrow(hboxMain, Priority.ALWAYS);
		setVgrow(this, Priority.ALWAYS);
	}

	//	public ModelEditor(DataModel model, String title)
	//	{
	//		super();
	//
	//		// TODO For now, copy the model. Don't want to persist changes (yet).
	//		try
	//		{
	//			model = DataModel.deserialize(model.serialize());
	//		}
	//		catch (DataModelSerializationException e)
	//		{
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//
	//		setTitle("Model Viewer - " + title);
	//		setWidth(800);
	//		setHeight(600);
	//		getIcons().add(new Image(Main.class.getResourceAsStream("apps.png")));
	//		this.model = model;
	//
	//		tv = new TreeView<TItem>();
	//		TreeItem<TItem> root = new TreeItem<TItem>(new TItem(model));
	//		populateTree(root, model);
	//		tv.setRoot(root);
	//		tv.getRoot().setExpanded(true);
	//		tv.getSelectionModel().selectedItemProperty().addListener((ob, old, nu) -> {
	//			label.setText(nu.toString());
	//			if (nu.getValue().entity instanceof Attribute)
	//			{
	//				app.bind((Attribute) nu.getValue().entity);
	//				if (!hboxMain.getChildren().contains(app))
	//				{
	//					hboxMain.getChildren().remove(dmpp);
	//					hboxMain.getChildren().remove(stpp);
	//					hboxMain.getChildren().add(app);
	//				}
	//			}
	//			else if (nu.getValue().entity instanceof StructuredType)
	//			{
	//				stpp.bind((StructuredType) nu.getValue().entity);
	//				if (!hboxMain.getChildren().contains(stpp))
	//				{
	//					hboxMain.getChildren().remove(dmpp);
	//					hboxMain.getChildren().remove(app);
	//					hboxMain.getChildren().add(stpp);
	//				}
	//			}
	//			else
	//			{
	//				dmpp.bind((DataModel) nu.getValue().entity);
	//				if (!hboxMain.getChildren().contains(dmpp))
	//				{
	//					hboxMain.getChildren().remove(app);
	//					hboxMain.getChildren().remove(stpp);
	//					hboxMain.getChildren().add(dmpp);
	//				}
	//			}
	//		});
	//		dmpp = makeDataModelPropertyPane();
	//		dmpp.bind(model);
	//		app = makeAttributePropertyPane();
	//		stpp = makeStructuredTypePropertyPane();
	//		hboxMain = new HBox(tv, dmpp);
	//		hboxMain.setSpacing(5);
	//		Scene scene = new Scene(hboxMain);
	//		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	//		setScene(scene);
	//	}

	static class DataModelPropertyPane extends VBox
	{
		private static final String	NO_ISSUES	= "<no issues>";

		DataModel					model;

		TextField					tfFormatVersion;

		TextField					tfNamespace;

		Label						labelValidation;

		public DataModelPropertyPane()
		{
			super();

			VBox vboxBasic = new VBox();
			vboxBasic.setSpacing(5);
			vboxBasic.setPadding(new Insets(5, 0, 0, 5));

			tfFormatVersion = new TextField();
			tfNamespace = new TextField();

			GridPane grid = new GridPane();
			grid.setAlignment(Pos.CENTER_LEFT);
			grid.setHgap(5);
			grid.setVgap(5);
			grid.add(new Label("Format Version:"), 0, 0);
			grid.add(tfFormatVersion, 1, 0);
			grid.add(new Label("Namespace"), 0, 1);
			grid.add(tfNamespace, 1, 1);
			vboxBasic.getChildren().add(grid);

			labelValidation = new Label();
			labelValidation.setWrapText(true);
			VBox vboxValidation = new VBox();
			vboxValidation.setSpacing(5);
			vboxValidation.setPadding(new Insets(5, 0, 0, 5));
			vboxValidation.getChildren().add(labelValidation);
			Label labelHeadingValidation = new Label("Validation:");
			labelHeadingValidation.getStyleClass().add("section-heading");
			VBox vboxValidationOuter = new VBox(labelHeadingValidation, vboxValidation);

			VBox vbox = new VBox();
			vbox.setSpacing(10);
			vbox.setPadding(new Insets(5, 0, 0, 5));
			vbox.getChildren().addAll(vboxBasic, vboxValidationOuter);
			getChildren().add(vbox);
		}

		private void doValidation()
		{
			ValidationResult vr = model.validate();
			if (vr.containsIssues())
			{
				//				StringBuilder buf = new StringBuilder();
				//				for (Issue issue : vr.getIssues())
				//				{
				//					if (buf.length() != 0)
				//					{
				//						buf.append("\n");
				//					}
				//					buf.append(issue.getMessage());
				//				}
				//				labelValidation.setText(buf.toString());
				labelValidation.setText(vr.toReportMessage());
			}
			else
			{
				labelValidation.setText(NO_ISSUES);
			}
		}

		public void bind(DataModel model)
		{
			this.model = model;
			tfFormatVersion.setText(model.getFormatVersion().toString());
			tfNamespace.setText(model.getNamespace());
			doValidation();
		}
	}

	public static class StructuredTypePropertyPane extends Pane
	{
		public static class StateTableItem
		{
			private StringProperty	labelProperty;

			private StringProperty	valueProperty;

			private BooleanProperty	purgeableProperty;

			public StateTableItem(String label, String value, Boolean purgeable)
			{
				this.labelProperty = new SimpleStringProperty(label);
				this.valueProperty = new SimpleStringProperty(value);
				this.purgeableProperty = new SimpleBooleanProperty(purgeable);
			}

			public StringProperty labelProperty()
			{
				return labelProperty;
			}

			public StringProperty valueProperty()
			{
				return valueProperty;
			}

			public BooleanProperty purgeableProperty()
			{
				return purgeableProperty;
			}
		}

		private static final String				NO_ISSUES	= "<no issues>";

		TextField								tfName;

		TextField								tfLabel;

		TextField								tfDescription;

		TextField								tfIIIPrefix;

		TextField								tfIIISuffix;

		TextField								tfIIIMinNumLength;

		CheckBox								cbIsCase;

		StructuredType							type;

		Label									labelValidation;

		TableView<StateTableItem>				tableStates;

		ModelEditor								editor;

		public ObservableList<StateTableItem>	stateList	= FXCollections.observableArrayList();

		public StructuredTypePropertyPane(ModelEditor editor)
		{
			super();

			this.editor = editor;
			tableStates = new TableView<StateTableItem>();
			tableStates.setItems(stateList);
			tableStates.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			TableColumn<StateTableItem, String> colLabel = new TableColumn<StateTableItem, String>("Label");
			colLabel.setCellValueFactory(new PropertyValueFactory<StateTableItem, String>("label"));
			colLabel.setPrefWidth(100);

			TableColumn<StateTableItem, String> colValue = new TableColumn<StateTableItem, String>("Value");
			colValue.setCellValueFactory(new PropertyValueFactory<StateTableItem, String>("value"));
			colValue.setPrefWidth(100);

			TableColumn<StateTableItem, Boolean> colPurgeable = new TableColumn<StateTableItem, Boolean>("Terminal");
			colPurgeable.setCellValueFactory(new PropertyValueFactory<StateTableItem, Boolean>("purgeable"));
			colPurgeable.setCellFactory(CheckBoxTableCell.forTableColumn(colPurgeable));
			colPurgeable.setPrefWidth(80);

			tableStates.getColumns().add(colLabel);
			tableStates.getColumns().add(colValue);
			tableStates.getColumns().add(colPurgeable);
			tableStates.setPrefHeight(120);
			tableStates.setPrefWidth(300);

			VBox vboxBasic = new VBox();
			vboxBasic.setSpacing(5);
			vboxBasic.setPadding(new Insets(5, 0, 0, 5));
			tfName = new TextField();
			tfName.setPrefWidth(220);
			tfName.setEditable(false);
			tfLabel = new TextField();
			tfLabel.setPrefWidth(120);
			tfLabel.setEditable(false);
			tfDescription = new TextField();
			tfDescription.setEditable(false);

			GridPane grid = new GridPane();
			grid.setAlignment(Pos.CENTER_LEFT);
			grid.setHgap(5);
			grid.setVgap(5);
			grid.add(new Label("Name:"), 0, 0);
			grid.add(tfName, 1, 0);
			grid.add(new Label("Label:"), 0, 1);
			grid.add(tfLabel, 1, 1);
			grid.add(new Label("Description:"), 0, 2);
			grid.add(tfDescription, 1, 2);
			vboxBasic.getChildren().add(grid);

			cbIsCase = new CheckBox("Case");
			cbIsCase.selectedProperty().addListener((b) -> {
				type.setIsCase(((BooleanProperty) b).get());
				doValidation();
			});

			HBox vboxOptions = new HBox();
			vboxOptions.setSpacing(5);
			vboxOptions.setPadding(new Insets(5, 0, 0, 5));
			vboxOptions.getChildren().addAll(cbIsCase);
			Label labelHeadingOptions = new Label("Options:");
			labelHeadingOptions.getStyleClass().add("section-heading");
			VBox vboxOptionsOuter = new VBox(labelHeadingOptions, vboxOptions);

			VBox vboxStateModel = new VBox();
			vboxStateModel.setSpacing(5);
			vboxStateModel.setPadding(new Insets(5, 0, 0, 5));
			vboxStateModel.getChildren().addAll(tableStates);
			Label labelHeadingStateModel = new Label("State Model:");
			labelHeadingStateModel.getStyleClass().add("section-heading");
			VBox vboxStateModelOuter = new VBox(labelHeadingStateModel, vboxStateModel);

			tfIIIPrefix = new TextField();
			tfIIIPrefix.setPrefWidth(80);
			tfIIISuffix = new TextField();
			tfIIISuffix.setPrefWidth(80);
			tfIIIMinNumLength = new TextField();
			tfIIIMinNumLength.setPrefWidth(40);

			GridPane gridIII = new GridPane();
			gridIII.setAlignment(Pos.CENTER_LEFT);
			gridIII.setHgap(5);
			gridIII.setVgap(5);
			gridIII.add(new Label("Prefix:"), 0, 0);
			gridIII.add(tfIIIPrefix, 1, 0);
			gridIII.add(new Label("Min Num Length:"), 2, 0);
			gridIII.add(tfIIIMinNumLength, 3, 0);
			gridIII.add(new Label("Suffix:"), 0, 1);
			gridIII.add(tfIIISuffix, 1, 1);

			VBox vboxIII = new VBox();
			vboxIII.setSpacing(5);
			vboxIII.setPadding(new Insets(5, 0, 0, 5));
			vboxIII.getChildren().add(gridIII);
			Label labelHeadingIII = new Label("Identifier Initialisation Info:");
			labelHeadingIII.getStyleClass().add("section-heading");
			VBox vboxIIIOuter = new VBox(labelHeadingIII, vboxIII);

			labelValidation = new Label();
			labelValidation.setWrapText(true);
			VBox vboxValidation = new VBox();
			vboxValidation.setSpacing(5);
			vboxValidation.setPadding(new Insets(5, 0, 0, 5));
			vboxValidation.getChildren().add(labelValidation);
			Label labelHeadingValidation = new Label("Validation:");
			labelHeadingValidation.getStyleClass().add("section-heading");
			VBox vboxValidationOuter = new VBox(labelHeadingValidation, vboxValidation);

			VBox vbox = new VBox();
			vbox.setSpacing(10);
			vbox.setPadding(new Insets(5, 0, 0, 5));
			vbox.getChildren().addAll(vboxBasic, vboxOptionsOuter, vboxStateModelOuter, vboxIIIOuter,
					vboxValidationOuter);
			getChildren().add(vbox);
		}

		private void doValidation()
		{
			ValidationResult vr = type.validate();
			if (vr.containsIssues())
			{
				List<Issue> issues = new ArrayList<>();
				issues.addAll(vr.getIssues());
				if (type.getStateModel() != null)
				{
					ValidationResult smResult = vr.getResultFor(type.getStateModel());
					if (smResult != null)
					{
						issues.addAll(smResult.getIssues());
						for (State state : type.getStateModel().getStates())
						{
							if (vr.getResultFor(state) != null)
							{
								issues.addAll(vr.getResultFor(state).getIssues());
							}
						}
					}
				}

				StringBuilder buf = new StringBuilder();
				for (Issue issue : issues)
				{
					if (buf.length() != 0)
					{
						buf.append("\n");
					}
					buf.append(issue.getMessage());
				}
				labelValidation.setText(buf.toString());
			}
			else
			{
				labelValidation.setText(NO_ISSUES);
			}
			editor.refreshSelected();
		}

		public void bind(StructuredType type)
		{
			this.type = type;

			if (type != null)
			{
				tfName.setText(type.getName());
				tfLabel.setText(type.getLabel());
				tfDescription.setText(type.getDescription());
				IdentifierInitialisationInfo iii = type.getIdentifierInitialisationInfo();
				if (iii != null)
				{
					String prefix = iii.getPrefix();
					String suffix = iii.getSuffix();
					Integer minNumLength = iii.getMinNumLength();
					Long start = iii.getStart();
					tfIIIPrefix.setText(prefix == null ? "" : prefix);
					tfIIISuffix.setText(suffix == null ? "" : suffix);
					tfIIIMinNumLength.setText(minNumLength == null ? "" : String.valueOf(minNumLength));
				}
				else
				{
					tfIIIPrefix.clear();
					tfIIISuffix.clear();
					tfIIIMinNumLength.clear();
				}
				cbIsCase.setSelected(type.getIsCase());
				stateList.clear();
				if (type.getStateModel() != null)
				{
					for (State state : type.getStateModel().getStates())
					{
						stateList.add(new StateTableItem(state.getLabel(), state.getValue(), state.getIsTerminal()));
					}
				}
				doValidation();
			}
			else
			{
				//				labelValidation.setText("Select an attribute from the tree");
			}
		}
	}

	static class AttributePropertyPane extends Pane
	{
		private static final String	NO_ISSUES	= "<no issues>";

		RadioButton					rbText;

		RadioButton					rbNumber;

		RadioButton					rbFixedPointNumber;

		RadioButton					rbDate;

		RadioButton					rbTime;

		RadioButton					rbDateTimeTZ;

		RadioButton					rbBoolean;

		RadioButton					rbURI;

		CheckBox					cbIdentifier;

		CheckBox					cbSearchable;

		CheckBox					cbSummary;

		CheckBox					cbMandatory;

		CheckBox					cbArray;

		CheckBox					cbState;

		TextField					tfName;

		TextField					tfLabel;

		TextField					tfId;

		TextField					tfDescription;

		TextField					tfDefaultValue;

		Label						labelValidation;

		private Attribute			attribute;

		ModelEditor					editor;

		public AttributePropertyPane(ModelEditor editor)
		{
			super();
			this.editor = editor;
			VBox vbox = new VBox();
			vbox.setSpacing(10);
			vbox.setPadding(new Insets(5, 0, 0, 5));
			labelValidation = new Label(NO_ISSUES);
			labelValidation.setWrapText(true);
			labelValidation.setPrefWidth(300);

			final ToggleGroup tGroupType = new ToggleGroup();
			rbText = new RadioButton("Text");
			rbText.setToggleGroup(tGroupType);

			rbNumber = new RadioButton("Number");
			rbNumber.setToggleGroup(tGroupType);

			rbFixedPointNumber = new RadioButton("Fixed Point Number");
			rbFixedPointNumber.setToggleGroup(tGroupType);

			rbDate = new RadioButton("Date");
			rbDate.setToggleGroup(tGroupType);

			rbTime = new RadioButton("Time");
			rbTime.setToggleGroup(tGroupType);

			rbDateTimeTZ = new RadioButton("Date Time TZ");
			rbDateTimeTZ.setToggleGroup(tGroupType);

			rbBoolean = new RadioButton("Boolean");
			rbBoolean.setToggleGroup(tGroupType);

			rbURI = new RadioButton("URI");
			rbURI.setToggleGroup(tGroupType);

			//			rbUser = new RadioButton("User");
			//			rbUser.setToggleGroup(tGroupType);
			//
			//			rbGroup = new RadioButton("Group");
			//			rbGroup.setToggleGroup(tGroupType);

			tGroupType.selectedToggleProperty().addListener((obs, old, nu) -> {
				if (nu == rbText)
				{
					attribute.setTypeObject(BaseType.TEXT);
				}
				else if (nu == rbNumber)
				{
					attribute.setTypeObject(BaseType.NUMBER);
				}
				else if (nu == rbFixedPointNumber)
				{
					attribute.setTypeObject(BaseType.FIXED_POINT_NUMBER);
				}
				else if (nu == rbDate)
				{
					attribute.setTypeObject(BaseType.DATE);
				}
				else if (nu == rbTime)
				{
					attribute.setTypeObject(BaseType.TIME);
				}
				else if (nu == rbDateTimeTZ)
				{
					attribute.setTypeObject(BaseType.DATE_TIME_TZ);
				}
				else if (nu == rbBoolean)
				{
					attribute.setTypeObject(BaseType.BOOLEAN);
				}
				else if (nu == rbURI)
				{
					attribute.setTypeObject(BaseType.URI);
				}

				doValidation();
			});

			cbMandatory = new CheckBox("Mandatory");
			cbMandatory.selectedProperty().addListener((b) -> {
				attribute.setIsMandatory(((BooleanProperty) b).get());
				doValidation();
			});

			cbArray = new CheckBox("Array");
			cbArray.selectedProperty().addListener((b) -> {
				attribute.setIsArray(((BooleanProperty) b).get());
				doValidation();
			});

			cbState = new CheckBox("State");
			cbState.selectedProperty().addListener((b) -> {
				attribute.setIsState(((BooleanProperty) b).get());
				doValidation();
			});

			cbIdentifier = new CheckBox("Identifier");
			cbIdentifier.selectedProperty().addListener((b) -> {
				attribute.setIsIdentifier(((BooleanProperty) b).get());
				doValidation();
			});

			cbSearchable = new CheckBox("Searchable");
			cbSearchable.selectedProperty().addListener((b) -> {
				attribute.setIsSearchable(((BooleanProperty) b).get());
				doValidation();
			});

			cbSummary = new CheckBox("Summary");
			cbSummary.selectedProperty().addListener((b) -> {
				attribute.setIsSummary(((BooleanProperty) b).get());
				doValidation();
			});

			VBox vboxBasic = new VBox();
			vboxBasic.setSpacing(5);
			vboxBasic.setPadding(new Insets(5, 0, 0, 5));
			tfName = new TextField("");
			tfName.setPrefWidth(220);
			tfName.setEditable(false);
			tfLabel = new TextField("");
			tfLabel.setPrefWidth(120);
			tfLabel.setEditable(false);
			tfId = new TextField("");
			tfId.setEditable(false);
			tfDescription = new TextField("");
			tfDescription.setEditable(false);
			tfDefaultValue = new TextField("");
			tfDefaultValue.setEditable(true);
			tfDefaultValue.textProperty().addListener((s) -> {
				attribute.setDefaultValue(tfDefaultValue.getText());
				doValidation();
			});

			GridPane grid = new GridPane();
			grid.setAlignment(Pos.CENTER_LEFT);
			grid.setHgap(5);
			grid.setVgap(5);
			grid.add(new Label("Name:"), 0, 0);
			grid.add(tfName, 1, 0);
			grid.add(new Label("Label:"), 0, 1);
			grid.add(tfLabel, 1, 1);
			grid.add(new Label("Description:"), 0, 2);
			grid.add(tfDescription, 1, 2);
			grid.add(new Label("Default Value:"), 0, 3);
			grid.add(tfDefaultValue, 1, 3);

			vboxBasic.getChildren().add(grid);

			VBox vboxTypes = new VBox();
			vboxTypes.setSpacing(5);
			vboxTypes.setPadding(new Insets(5, 0, 0, 5));
			vboxTypes.getChildren().addAll(rbText, rbNumber, rbFixedPointNumber, rbDate, rbTime, rbDateTimeTZ,
					rbBoolean, rbURI);

			VBox vboxOptions = new VBox();
			vboxOptions.setSpacing(5);
			vboxOptions.setPadding(new Insets(5, 0, 0, 5));
			vboxOptions.getChildren().addAll(cbMandatory, cbArray, cbIdentifier, cbSearchable, cbState, cbSummary);

			VBox vboxValidation = new VBox();
			vboxValidation.setSpacing(5);
			vboxValidation.setPadding(new Insets(5, 0, 0, 5));
			vboxValidation.getChildren().add(labelValidation);

			Label labelHeadingType = new Label("Type:");
			labelHeadingType.getStyleClass().add("section-heading");
			Label labelHeadingOptions = new Label("Options:");
			labelHeadingOptions.getStyleClass().add("section-heading");
			Label labelHeadingValidation = new Label("Validation:");
			labelHeadingValidation.getStyleClass().add("section-heading");

			VBox vboxTypesOuter = new VBox(labelHeadingType, vboxTypes);
			VBox vboxOptionsOuter = new VBox(labelHeadingOptions, vboxOptions);
			VBox vboxValidationOuter = new VBox(labelHeadingValidation, vboxValidation);

			vbox.getChildren().addAll(vboxBasic, new HBox(vboxTypesOuter, vboxOptionsOuter), vboxValidationOuter);
			getChildren().add(vbox);
		}

		private void doValidation()
		{
			ValidationResult vr = attribute.validate();
			if (vr.containsIssues())
			{
				StringBuilder buf = new StringBuilder();
				//				for (Issue issue : vr.getIssues())
				//				{
				//					if (buf.length() != 0)
				//					{
				//						buf.append("\n");
				//					}
				//					buf.append(issue.getMessage());
				//				}
				buf.append(vr.toReportMessage());
				labelValidation.setText(buf.toString());
			}
			else
			{
				labelValidation.setText(NO_ISSUES);
			}
			editor.refreshSelected();
		}

		public void bind(Attribute attr)
		{
			this.attribute = attr;

			if (attr != null)
			{
				tfName.setText(attr.getName());
				tfLabel.setText(attr.getLabel());
				tfId.setText(attr.getId());
				tfDescription.setText(attr.getDescription());
				tfDefaultValue.setText(attr.getDefaultValue());
				rbText.setSelected(attr.getTypeObject() == BaseType.TEXT);
				rbNumber.setSelected(attr.getTypeObject() == BaseType.NUMBER);
				rbFixedPointNumber.setSelected(attr.getTypeObject() == BaseType.FIXED_POINT_NUMBER);
				rbDate.setSelected(attr.getTypeObject() == BaseType.DATE);
				rbTime.setSelected(attr.getTypeObject() == BaseType.TIME);
				rbDateTimeTZ.setSelected(attr.getTypeObject() == BaseType.DATE_TIME_TZ);
				rbBoolean.setSelected(attr.getTypeObject() == BaseType.BOOLEAN);
				rbURI.setSelected(attr.getTypeObject() == BaseType.URI);
				cbMandatory.setSelected(attr.getIsMandatory());
				cbArray.setSelected(attr.getIsArray());
				cbState.setSelected(attr.getIsState());
				cbIdentifier.setSelected(attr.getIsIdentifier());
				cbSearchable.setSelected(attr.getIsSearchable());
				cbSummary.setSelected(attr.getIsSummary());
				doValidation();
			}
			else
			{
				labelValidation.setText("Select an attribute from the tree");
			}
		}
	}

	public void refreshSelected()
	{
		TreeItem<TItem> ti = tv.getSelectionModel().getSelectedItem();
		TItem object = ti.getValue();
		// force reference change
		ti.setValue(null);
		ti.setValue(object);
	}

	private DataModelPropertyPane makeDataModelPropertyPane()
	{
		DataModelPropertyPane pane = new DataModelPropertyPane();
		return pane;
	}

	private AttributePropertyPane makeAttributePropertyPane()
	{
		AttributePropertyPane pane = new AttributePropertyPane(this);
		return pane;
	}

	private StructuredTypePropertyPane makeStructuredTypePropertyPane()
	{
		StructuredTypePropertyPane pane = new StructuredTypePropertyPane(this);
		return pane;
	}

	private void populateTree(TreeItem<TItem> root, DataModel model)
	{
		TreeItem<TItem> typesRoot = new TreeItem<TItem>(new TItem("Types"));
		root.getChildren().add(typesRoot);
		for (StructuredType type : model.getStructuredTypes())
		{
			TreeItem<TItem> stItem = new TreeItem<TItem>(new TItem(type));
			typesRoot.getChildren().add(stItem);
			populateTree(stItem, type);
		}
	}

	private void populateTree(TreeItem<TItem> modelItem, StructuredType type)
	{

		for (Attribute attr : type.getAttributes())
		{
			TreeItem<TItem> attrItem = new TreeItem<TItem>(new TItem(attr));
			modelItem.getChildren().add(attrItem);
			if (attr.getTypeObject() instanceof StructuredType)
			{
				// TODO Do this properly. For now, just preventing more than 10 levels of depth
				// to avoid endless recursion.
				int depth = 0;
				TreeItem<TItem> parent = attrItem;
				while (parent != null && depth < 10)
				{
					parent = parent.getParent();
					depth++;
				}

				if (depth < 10)
				{
					populateTree(attrItem, (StructuredType) attr.getTypeObject());
				}
			}
		}
	}
}
