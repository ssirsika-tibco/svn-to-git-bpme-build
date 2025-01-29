/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.ModelEditor;
import com.tibco.bpm.acecannon.casedata.ValueConjurer.ChoiceOption;
import com.tibco.bpm.acecannon.casedata.ValueConjurer.Option;
import com.tibco.bpm.acecannon.casedata.ValueConjurer.OptionType;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.Constraint;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.da.dm.api.ValidatableEntity;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ConjuringPane extends VBox
{
	private static ObjectMapper prettyOM = new ObjectMapper();
	static
	{
		prettyOM.enable(SerializationFeature.INDENT_OUTPUT);
		prettyOM.enable(Feature.WRITE_BIGDECIMAL_AS_PLAIN);
	}

	public static class TypeInfo
	{
		private StructuredType	type;

		private BigInteger		appId;

		public TypeInfo(StructuredType type, BigInteger appId)
		{
			this.type = type;
			this.appId = appId;
		}

		public StructuredType getType()
		{
			return type;
		}

		public BigInteger getAppId()
		{
			return appId;
		}

	}

	public static class ConjuringPreviewThread extends Thread
	{
		boolean					running	= true;

		private ConjuringPane	conjuringPane;

		public ConjuringPreviewThread(ConjuringPane cp)
		{
			super("ConjuringPreviewThread");
			this.conjuringPane = cp;
		}

		public void term()
		{
			running = false;
		}

		public void run()
		{
			boolean skipCycle = false;
			while (running)
			{
				if (!skipCycle)
				{
					conjuringPane.conjureNewValues();
				}
				else
				{
					skipCycle = false;
				}
				try
				{
					long sleep = -(long) conjuringPane.slider.getValue();
					Thread.sleep(sleep);
				}
				catch (InterruptedException e)
				{
					skipCycle = true;
				}
			}
		}
	}

	public static class OptionValue
	{
		public OptionValue(Option option, Object value)
		{
			this.option = option;
			this.value = value;
		}

		public Option	option;

		public Object	value;
	}

	public static class ConjuringBean
	{
		private ObjectProperty<ValidatableEntity>	entity			= new SimpleObjectProperty<ValidatableEntity>();

		private ObjectProperty<ValueConjurer< ? >>	conjurer		= new SimpleObjectProperty<ValueConjurer< ? >>();

		private ObjectProperty<Object>				value			= new SimpleObjectProperty<Object>();

		private boolean								isArray;

		private boolean								isUser;

		private ObjectProperty<Integer>				arrayMinSize	= new SimpleObjectProperty<Integer>(3);

		private ObjectProperty<Integer>				arrayMaxSize	= new SimpleObjectProperty<Integer>(3);

		// Dynamic property, only the primitive boolean ones
		private BooleanProperty						isExcluded		= new SimpleBooleanProperty();

		public ConjuringBean(ValidatableEntity entity, boolean isArray, boolean isUser)
		{
			this.entity.set(entity);
			this.isArray = isArray;
			this.isUser = isUser;
		}

		public boolean getIsObject()
		{
			return (entity.get() instanceof Attribute
					&& (((Attribute) entity.get()).getTypeObject() instanceof StructuredType));
		}

		public String getName()
		{
			String name = null;
			if (entity.get() instanceof StructuredType)
			{
				name = ((StructuredType) entity.get()).getName();
			}
			else if (entity.get() instanceof Attribute)
			{
				name = ((Attribute) entity.get()).getName();
			}
			return name;
		}

		public String getType()
		{
			String name = null;
			if (entity.get() instanceof Attribute)
			{
				Attribute attr = (Attribute) entity.get();
				AbstractType typeObject = attr.getTypeObject();
				if (typeObject instanceof BaseType)
				{
					name = ((BaseType) typeObject).getName();
					if (isArray)
					{
						name += "[]";
					}
					if (attr.getIsSearchable())
					{
						name += " \uD83D\uDD0D";
					}
					if (attr.getIsIdentifier())
					{
						if (attr.getParent().hasDynamicIdentifier())
						{
							name += " \uD83D\uDE97";
						}
						name += " \uD83C\uDD94";
					}
				}
				else
				{
					if (isArray)
					{
						name = "[]";
					}
				}
			}
			return name;
		}

		public ObjectProperty<Object> valueProperty()
		{
			return value;
		}

		public ObjectProperty<ValueConjurer< ? >> conjurerProperty()
		{
			return conjurer;
		}

		public boolean getIsArray()
		{
			return isArray;
		}

		public boolean getIsUser()
		{
			return isUser;
		}

		public boolean hasAllowedValues()
		{
			ValidatableEntity< ? > validatableEntity = entity.get();
			return (validatableEntity instanceof Attribute)
					&& !((Attribute) validatableEntity).getAllowedValues().isEmpty();
		}

		public BooleanProperty isExcludedProperty()
		{
			return isExcluded;
		}
	}

	public static class GeneratorChoice
	{
		private String	className;

		private String	label;

		public GeneratorChoice(String className, String label)
		{
			this.className = className;
			this.label = label;
		}

		public String toString()
		{
			return label;
		}
	}

	public static class OptionChoice
	{
		private String	label;

		private String	name;

		public OptionChoice(String name, String label)
		{
			this.name = name;
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		public String getName()
		{
			return name;
		}

		public String toString()
		{
			return label;
		}
	}

	private static final ObjectMapper om = new ObjectMapper();
	static
	{
		om.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static final String						SQL_READ	= "SELECT model, application_id FROM bpm_cm_user.CDM_casemodels "
			+ "WHERE subscription_id=? AND sandbox_id=?";

	private DataSource								dataSource;

	private TreeTableView<ConjuringBean>			ttv;

	private VBox									arraySettingsVBox;

	private VBox									exclusionSettingsVBox;

	private ChoiceBox<GeneratorChoice>				cbGenerator;

	private Button									buttonSave;

	private VBox									optionsVBox;

	private ChangeListener<TreeItem<ConjuringBean>>	treeChangeListener;

	private ChangeListener<GeneratorChoice>			generatorChangeListener;

	private ConjuringPreviewThread					conjuringPreviewThread;

	ChoiceBox										cbType;

	private List<TypeInfo>							typeInfos;

	private AceMain									main;

	private Stage									stage;

	private Map<String, Class< ? >>					gMap;

	private Map<String, List<OptionValue>>			oMap;

	private Slider									slider;

	private boolean									hasNoTypes;

	private TextField								tfNumber;

	private TextField								tfBatchSize;

	private StructuredType							type;

	private int										majorVersion;

	private ConjuringNode treeNodeToModelNode(TreeItem<ConjuringBean> treeNode)
	{
		ConjuringNode conjuringNode = new ConjuringNode();
		ConjuringBean conjuringBean = treeNode.getValue();
		ValidatableEntity< ? > entity = conjuringBean.entity.get();
		if (entity instanceof Attribute)
		{
			conjuringNode.setTargetAttribute((Attribute) entity);
		}
		ValueConjurer< ? > conjurer = conjuringBean.conjurerProperty().get();
		conjuringNode.setName(conjuringBean.getName());
		conjuringNode.setIsUser(conjuringBean.getIsUser());
		boolean isArray = conjuringBean.getIsArray();
		conjuringNode.setIsArray(isArray);
		if (isArray)
		{
			conjuringNode.setArrayMinSize(conjuringBean.arrayMinSize.get());
			conjuringNode.setArrayMaxSize(conjuringBean.arrayMaxSize.get());
		}

		boolean isExcluded = conjuringBean.isExcludedProperty().get();
		if (isExcluded)
		{
			conjuringNode.setIsExcluded(true);
		}

		if (conjurer != null)
		{
			conjuringNode.setConjurer(conjurer.getClass().getName());
			Map<Option, Object> optionsMap = conjurer.getOptionValues();
			for (Entry<Option, Object> option : optionsMap.entrySet())
			{
				conjuringNode.getOptions().put(option.getKey().getName(), option.getValue());
			}
		}
		for (TreeItem<ConjuringBean> child : treeNode.getChildren())
		{
			conjuringNode.getAttributes().add(treeNodeToModelNode(child));
		}
		return conjuringNode;
	}

	private ConjuringModel treeToModel()
	{
		ConjuringModel model = new ConjuringModel();
		//model.setApplicationId(typeInfos.get(cbType.getSelectionModel().getSelectedIndex()).getAppId().toString());
		TreeItem<ConjuringBean> treeRoot = ttv.getRoot();
		for (TreeItem<ConjuringBean> child : treeRoot.getChildren())
		{
			model.getAttributes().add(treeNodeToModelNode(child));
		}

		// Find fingerprint attribute
		//TODO
		// Only top-level attributes can be searchable (according to DT at least)
		TreeItem<ConjuringBean> fingerPrintTreeItem = treeRoot.getChildren().stream()
				.filter(ti -> ti.getValue().conjurerProperty().get() != null
						&& ti.getValue().conjurerProperty().get().getClass() == FingerprintConjurer.class)
				.findFirst().orElse(null);
		if (fingerPrintTreeItem != null)
		{
			model.setFingerprintAttributeName(fingerPrintTreeItem.getValue().getName());
		}

		return model;
	}

	public boolean hasNoTypes()
	{
		return hasNoTypes;
	}

	private void childrenToTree(List<TreeItem<ConjuringBean>> children, List<ConjuringNode> conjuringNodes)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		for (TreeItem<ConjuringBean> childTreeItem : children)
		{
			ConjuringBean childBean = childTreeItem.getValue();
			String attributeName = childBean.getName();

			// Find an attribute with that name in the model
			ConjuringNode match = conjuringNodes.stream().filter(a -> a.getName().equals(attributeName)).findFirst()
					.orElse(null);
			if (match != null)
			{
				String conjurerName = match.getConjurer();
				if (conjurerName != null)
				{
					ValueConjurer< ? > conjurer;
					if (conjurerName.equals("com.tibco.bpm.acecannon.casedata.StateConjurer"))
					{
						// Special construction for state conjurer as it needs the type
						conjurer = new StateConjurer(
								typeInfos.get(cbType.getSelectionModel().getSelectedIndex()).getType());
					}
					else if (conjurerName.equals("com.tibco.bpm.acecannon.casedata.GroupConjurer"))
					{
						// Special construction for state conjurer as it needs the entity references
						conjurer = new GroupConjurer(match.getTargetAttribute());
					}
					else
					{
						conjurer = constructGenerator(Class.forName(conjurerName), childBean);
					}
					for (Entry<String, Object> optionMapEntry : match.getOptions().entrySet())
					{
						String optionName = optionMapEntry.getKey();
						Option option = conjurer.getOptions().stream().filter(o -> o.getName().equals(optionName))
								.findFirst().orElse(null);
						if (option != null)
						{
							switch (option.getType())
							{
								case BOOLEAN:
									conjurer.getOptionValues().put(option,
											Boolean.parseBoolean(optionMapEntry.getValue().toString()));
									break;
								case INTEGER:
									conjurer.getOptionValues().put(option,
											Integer.parseInt(optionMapEntry.getValue().toString()));
									break;
								case BIG_DECIMAL:
									conjurer.getOptionValues().put(option,
											new BigDecimal(optionMapEntry.getValue().toString()));
									break;
								case TEXT_LIST:
									conjurer.getOptionValues().put(option, optionMapEntry.getValue());
								default:
									conjurer.getOptionValues().put(option, optionMapEntry.getValue());
							}
						}
					}
					childBean.conjurerProperty().set(conjurer);
				}
				else
				{
					List<ConjuringNode> underNodes = match.getAttributes();
					childrenToTree(childTreeItem.getChildren(), underNodes);
				}
				if (childBean.getIsArray())
				{
					childBean.arrayMinSize.set(match.getArrayMinSize());
					childBean.arrayMaxSize.set(match.getArrayMaxSize());
				}
				if (match.getIsExcluded())
				{
					childBean.isExcludedProperty().set(true);
				}

			}
			//			}
		}
	}

	private void modelToTree(ConjuringModel model)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		TreeItem<ConjuringBean> rootTreeItem = ttv.getRoot();
		childrenToTree(rootTreeItem.getChildren(), model.getAttributes());
	}

	private void conjureNewValues()
	{
		if (ttv.getRoot() != null)
		{
			conjureValueFor(ttv.getRoot(), false);
		}
	}

	private void conjureValueFor(TreeItem<ConjuringBean> ti, boolean clear)
	{
		ConjuringBean bean = ti.getValue();
		if (clear)
		{
			bean.valueProperty().set("");
		}
		else
		{
			ValueConjurer< ? > conjurer = bean.conjurerProperty().get();
			if (conjurer != null)
			{
				if (bean.isArray)
				{
					List< ? extends Object> newValues = conjurer.conjureMany(2);
					bean.valueProperty().set(newValues);
				}
				else
				{
					Object newValue = conjurer.conjure();
					bean.valueProperty().set(newValue);
				}
			}
		}
		boolean clearChildren = clear || ti.getValue().isExcludedProperty().get();
		for (TreeItem<ConjuringBean> child : ti.getChildren())
		{
			conjureValueFor(child, clearChildren);
		}
	}

	private TreeItem<ConjuringBean> buildTree(ValidatableEntity< ? > ve, StructuredType caseType)
			throws InstantiationException, IllegalAccessException
	{
		TreeItem<ConjuringBean> ti = new TreeItem<ConjuringBean>(
				new ConjuringBean(ve, (ve instanceof Attribute && ((Attribute) ve).getIsArray()),
						(ve instanceof Attribute && ((Attribute) ve).getTypeObject() == BaseType.USER)));
		if (ve instanceof StructuredType)
		{
			StructuredType st = (StructuredType) ve;
			for (Attribute attr : st.getAttributes())
			{
				ti.getChildren().add(buildTree(attr, caseType));
			}
		}
		ValueConjurer< ? > conjurer = null;
		if (ve instanceof Attribute)
		{
			Attribute attr = (Attribute) ve;
			AbstractType attrType = attr.getTypeObject();
			if (attr.getIsState())
			{
				conjurer = new StateConjurer(caseType);
			}
			else if (!attr.getAllowedValues().isEmpty())
			{
				conjurer = new FixedValuesConjurer();
				conjurer.getOptionValues().put(FixedValuesConjurer.optionValues, FXCollections.observableArrayList(
						attr.getAllowedValues().stream().map(av -> av.getValue()).collect(Collectors.toList())));
			}
			else if (attrType instanceof StructuredType)
			{
				StructuredType nestedType = (StructuredType) attrType;
				for (Attribute nestedAttr : nestedType.getAttributes())
				{
					ti.getChildren().add(buildTree(nestedAttr, caseType));
				}
			}
			else if (attrType instanceof BaseType)
			{
				BaseType baseType = (BaseType) attrType;
				if (attr.getIsIdentifier() && attr.getParent().hasDynamicIdentifier())
				{
					conjurer = new NoValueConjurer();
				}
				else if (baseType == BaseType.TEXT)
				{
				}
				else if (baseType == BaseType.NUMBER)
				{
					conjurer = new NumberConjurer();
				}
				else if (baseType == BaseType.FIXED_POINT_NUMBER)
				{
					conjurer = new FixedPointNumberConjurer();
					Constraint conLength = attr.getConstraint(Constraint.NAME_LENGTH);
					Constraint conDP = attr.getConstraint(Constraint.NAME_DECIMAL_PLACES);
					Constraint conMinValue = attr.getConstraint(Constraint.NAME_MIN_VALUE);
					Constraint conMinValueInclusive = attr.getConstraint(Constraint.NAME_MIN_VALUE_INCLUSIVE);
					Constraint conMaxValue = attr.getConstraint(Constraint.NAME_MAX_VALUE);
					Constraint conMaxValueInclusive = attr.getConstraint(Constraint.NAME_MAX_VALUE_INCLUSIVE);
					if (conLength != null)
					{
						int conLengthValue;
						try
						{
							conLengthValue = Integer.parseInt(conLength.getValue());
							conjurer.getOptionValues().put(FixedPointNumberConjurer.optionLength, conLengthValue);
						}
						catch (NumberFormatException e)
						{
						}
					}
					if (conDP != null)
					{
						int conDPValue;
						try
						{
							conDPValue = Integer.parseInt(conDP.getValue());
							conjurer.getOptionValues().put(FixedPointNumberConjurer.optionDecimalPlaces, conDPValue);
						}
						catch (NumberFormatException e)
						{
						}
					}
					if (conMinValue != null)
					{
						BigDecimal conMinValueValue = new BigDecimal(conMinValue.getValue());
						conjurer.getOptionValues().put(FixedPointNumberConjurer.optionMinValue, conMinValueValue);
					}
					if (conMaxValue != null)
					{
						BigDecimal conMaxValueValue = new BigDecimal(conMaxValue.getValue());
						conjurer.getOptionValues().put(FixedPointNumberConjurer.optionMaxValue, conMaxValueValue);
					}
					if (conMinValueInclusive != null)
					{
						boolean conMinValueInclusiveValue = Boolean.parseBoolean(conMinValueInclusive.getValue());
						conjurer.getOptionValues().put(FixedPointNumberConjurer.optionMinValueInclusive,
								conMinValueInclusiveValue);
					}
					if (conMaxValueInclusive != null)
					{
						boolean conMaxValueInclusiveValue = Boolean.parseBoolean(conMaxValueInclusive.getValue());
						conjurer.getOptionValues().put(FixedPointNumberConjurer.optionMaxValueInclusive,
								conMaxValueInclusiveValue);
					}
				}
				else if (baseType == BaseType.DATE)
				{
					conjurer = new DateConjurer();
				}
				else if (baseType == BaseType.TIME)
				{
					conjurer = new TimeConjurer();
				}
				else if (baseType == BaseType.DATE_TIME_TZ)
				{
					conjurer = new DateTimeTZConjurer();
				}
				else if (baseType == BaseType.BOOLEAN)
				{
					conjurer = new BooleanConjurer();
				}
				else if (baseType == BaseType.URI)
				{
					conjurer = new URIConjurer();
				}
				String attrName = ((Attribute) ve).getName().toLowerCase();
				if (!(conjurer instanceof NoValueConjurer) && gMap.containsKey(attrName))
				{
					conjurer = constructGenerator(gMap.get(attrName), ti.getValue());
					if (oMap.containsKey(attrName))
					{
						for (OptionValue ov : oMap.get(attrName))
						{
							conjurer.getOptionValues().put(ov.option, ov.value);
						}
					}
				}
				else
				{
					// Try without the _vx suffix
					if (attrName.contains("_v"))
					{
						attrName = attrName.substring(0, attrName.indexOf("_v"));
						if (gMap.containsKey(attrName))
						{
							conjurer = constructGenerator(gMap.get(attrName), ti.getValue());
							if (oMap.containsKey(attrName))
							{
								for (OptionValue ov : oMap.get(attrName))
								{
									conjurer.getOptionValues().put(ov.option, ov.value);
								}
							}
						}
					}

					if (baseType == BaseType.TEXT)
					{
						Constraint formatContstraint = attr.getConstraint("format");
						if (formatContstraint != null && "email".equals(formatContstraint.getValue()))
						{
							conjurer = new EmailAddressConjurer();
						}
					}

					if (conjurer == null)
					{
						conjurer = new BizarreObjectConjurer();
						conjurer.getOptionValues().put(BizarreObjectConjurer.optionAdjective, true);
						conjurer.getOptionValues().put(BizarreObjectConjurer.optionColour, true);
					}
				}

			}
			if (conjurer != null)
			{
				ti.getValue().conjurerProperty().set(conjurer);
				Object value = conjurer.conjure();
				ti.getValue().valueProperty().set(value != null ? value.toString() : null);
			}
		}
		return ti;
	}

	private ValueConjurer< ? > constructGenerator(Class< ? > clazz, ConjuringBean conjuringBean)
			throws InstantiationException, IllegalAccessException
	{
		ValidatableEntity< ? > entity = conjuringBean.entity.get();
		ValueConjurer< ? > conjurer = null;
		if (entity instanceof Attribute)
		{
			Attribute attr = (Attribute) entity;
			if (clazz == GroupConjurer.class)
			{
				conjurer = new GroupConjurer(attr);
			}
		}

		if (conjurer == null)
		{
			conjurer = (ValueConjurer< ? >) clazz.newInstance();
		}
		return conjurer;
	}

	private void handleGeneratorChange(GeneratorChoice choice)
	{
		TreeItem<ConjuringBean> selectedItem = ttv.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
		{
			try
			{
				Class< ? > clazz = Class.forName(choice.className);
				if (clazz != null)
				{
					if (clazz == StateConjurer.class)
					{
						selectedItem.getValue().conjurerProperty().set(new StateConjurer(type));
						//						// Prevent these special conjurers being selected by flipping to BizarreOC
						//						cbGenerator.getSelectionModel().select(0);
					}
					else
					{
						ValueConjurer< ? > conjurer = constructGenerator(clazz, selectedItem.getValue());

						if (conjurer instanceof FixedValuesConjurer)
						{
							conjurer.getOptionValues().put(FixedValuesConjurer.optionValues,
									FXCollections.observableArrayList(new String[]{FixedValuesConjurer.ADD_NEW}));
						}

						// Generate controls for generator's options
						optionsVBox.getChildren().clear();
						List<Option> options = conjurer.getOptions();
						populateOptionsPane(options, conjurer.getOptionValues(), conjurer.getDescription());

						// Apply new conjurer to item
						selectedItem.getValue().conjurerProperty().set(conjurer);
					}
				}
			}
			catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ConjuringModel cm = treeToModel();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		String json;
		try
		{
			json = om.writeValueAsString(cm);
		}
		catch (JsonProcessingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void expandTree(TreeItem<ConjuringBean> ti)
	{
		ti.setExpanded(true);
		for (TreeItem<ConjuringBean> child : ti.getChildren())
		{
			expandTree(child);
		}
	}

	public void setMajorVersion(int majorVersion)
	{
		this.majorVersion = majorVersion;
	}

	public void setType(StructuredType ty)
	{
		this.type = ty;
		optionsVBox.getChildren().clear();
		cbGenerator.setVisible(false);

		TreeItem<ConjuringBean> t;
		try
		{
			t = buildTree(ty, ty);
			ttv.setRoot(t);
			expandTree(ttv.getRoot());
			if (ttv.getRoot().getChildren().size() > 0)
			{
				ttv.getSelectionModel().select(1);
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
	}

	public static ConjuringPane make()
			throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		ConjuringPane pane = new ConjuringPane();

		Scene scene = new Scene(pane, 950, 600);
		scene.getStylesheets().add(AceMain.class.getResource("application.css").toExternalForm());
		Stage cstage = new Stage();
		pane.stage = cstage;
		cstage.setOnCloseRequest(ev -> {
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("autoCreate", cstage.getX(), cstage.getY(),
					cstage.getWidth(), cstage.getHeight(), cstage.isMaximized());
			pane.termThread();
		});
		cstage.setScene(scene);
		cstage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
		return pane;
	}

	private ConjuringPane() throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		gMap = new HashMap<String, Class< ? >>();
		oMap = new HashMap<String, List<OptionValue>>();
		gMap.put("customername", PersonNameConjurer.class);
		gMap.put("claimantname", PersonNameConjurer.class);
		gMap.put("name", PersonNameConjurer.class);
		gMap.put("field2", PersonNameConjurer.class);
		gMap.put("field3", PrefixedNumberConjurer.class);
		oMap.put("field3",
				Arrays.asList(new OptionValue(PrefixedNumberConjurer.optionPrefix, "F3-"),
						new OptionValue(PrefixedNumberConjurer.optionDigits, 8),
						new OptionValue(PrefixedNumberConjurer.optionPadNumber, true)));
		gMap.put("number", NumberConjurer.class);
		oMap.put("number", Arrays.asList(new OptionValue(NumberConjurer.optionMinDigits, 15),
				new OptionValue(NumberConjurer.optionMaxDigits, 15)));
		gMap.put("lossadjuster", UserConjurer.class);
		gMap.put("support", GroupConjurer.class);
		gMap.put("line1", AddressFirstLineConjurer.class);
		gMap.put("firstline", AddressFirstLineConjurer.class);
		gMap.put("line2", AddressTownConjurer.class);
		gMap.put("secondline", AddressTownConjurer.class);
		gMap.put("postcode", AddressPostcodeConjurer.class);
		gMap.put("type", AddressPostcodeConjurer.class);
		gMap.put("make", CarMakeConjurer.class);
		gMap.put("model", CarModelConjurer.class);
		gMap.put("registration", CarRegistrationConjurer.class);

		gMap.put("phone", PhoneNumberUKConjurer.class);
		gMap.put("telephone", PhoneNumberUKConjurer.class);
		gMap.put("phonenumber", PhoneNumberUKConjurer.class);
		gMap.put("telephonenumber", PhoneNumberUKConjurer.class);

		gMap.put("departmentcode", PrefixedNumberConjurer.class);
		oMap.put("departmentcode",
				Arrays.asList(new OptionValue(PrefixedNumberConjurer.optionPrefix, "Dept-"),
						new OptionValue(PrefixedNumberConjurer.optionDigits, 5),
						new OptionValue(PrefixedNumberConjurer.optionPadNumber, true)));
		gMap.put("srid", PrefixedNumberConjurer.class);
		oMap.put("srid",
				Arrays.asList(new OptionValue(PrefixedNumberConjurer.optionPrefix, "SR-"),
						new OptionValue(PrefixedNumberConjurer.optionDigits, 6),
						new OptionValue(PrefixedNumberConjurer.optionPadNumber, true)));

		HBox typeHBox = new HBox();
		typeHBox.setAlignment(Pos.CENTER_LEFT);
		typeHBox.setSpacing(5);
		//		typeHBox.getChildren().addAll(buttonGenerateJSON);

		optionsVBox = new VBox();
		optionsVBox.setSpacing(4);
		ttv = new TreeTableView<ConjuringBean>();
		ttv.setPrefHeight(2000);
		TreeTableColumn<ConjuringBean, String> colName = new TreeTableColumn<ConjuringBean, String>("Attribute Name");
		colName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		colName.setPrefWidth(200);
		TreeTableColumn<ConjuringBean, String> colType = new TreeTableColumn<ConjuringBean, String>("Type");
		colType.setCellValueFactory(new TreeItemPropertyValueFactory<>("type"));
		colType.setPrefWidth(130);
		TreeTableColumn<ConjuringBean, String> colValue = new TreeTableColumn<ConjuringBean, String>("Preview");
		colValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		colValue.prefWidthProperty()
				.bind(ttv.widthProperty().subtract(colName.widthProperty()).subtract(colType.widthProperty().add(16d)));
		ttv.getColumns().add(colName);
		ttv.getColumns().add(colType);
		ttv.getColumns().add(colValue);
		if (typeInfos != null && !typeInfos.isEmpty())
		{
			StructuredType st = typeInfos.get(0).getType();
			ttv.setRoot(buildTree(st, st));
			expandTree(ttv.getRoot());
		}

		ObservableList<GeneratorChoice> choices = FXCollections.<GeneratorChoice> observableArrayList();
		cbGenerator = new ChoiceBox<GeneratorChoice>(choices);
		cbGenerator.setVisible(false);
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.BooleanConjurer", "Boolean"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.BizarreObjectConjurer", "Bizarre object"));
		//		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.IdentifierConjurer",
		//				"Identifier (auto-generated)"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.NaughtyStringConjurer", "Naughty string"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.StateConjurer", "State"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.FixedValuesConjurer", "Fixed values"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.NumberConjurer", "Number"));
		choices.add(
				new GeneratorChoice("com.tibco.bpm.acecannon.casedata.FixedPointNumberConjurer", "Fixed Point Number"));
		choices.add(
				new GeneratorChoice("com.tibco.bpm.acecannon.casedata.PrefixedNumberConjurer", "Number with prefix"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.DateConjurer", "Date"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.TimeConjurer", "Time"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.DateTimeTZConjurer", "Date Time TZ"));
		choices.add(
				new GeneratorChoice("com.tibco.bpm.acecannon.casedata.CarRegistrationConjurer", "Car registration"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.CarMakeConjurer", "Car make"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.CarModelConjurer", "Car model"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.AddressFirstLineConjurer",
				"Address: first line"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.AddressTownConjurer", "Address: town/city"));
		choices.add(
				new GeneratorChoice("com.tibco.bpm.acecannon.casedata.AddressPostcodeConjurer", "Address: postcode"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.PhoneNumberUKConjurer", "Phone number (UK)"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.PersonNameConjurer", "Person's name"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.EmailAddressConjurer", "Email address"));
		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.URIConjurer", "URI"));
		choices.add(
				new GeneratorChoice("com.tibco.bpm.acecannon.casedata.NoValueConjurer", "No value (omit attribute)"));
		//		choices.add(new GeneratorChoice("com.tibco.bpm.acecannon.casedata.FingerprintConjurer", "Fingerprint"));

		arraySettingsVBox = new VBox();
		exclusionSettingsVBox = new VBox();

		VBox vboxGen = new VBox(cbGenerator, optionsVBox, exclusionSettingsVBox, arraySettingsVBox);
		vboxGen.setPrefWidth(310);
		vboxGen.setSpacing(10);

		slider = new Slider();
		slider.setPrefWidth(220d);
		slider.setMaxWidth(-1d);
		slider.setMax(-20);
		slider.setMin(-3000);
		slider.setValue(-500);

		HBox hboxSlider = new HBox();
		hboxSlider.setSpacing(5);
		hboxSlider.setAlignment(Pos.CENTER);
		hboxSlider.getChildren().addAll(new Label("Preview speed:"), slider);

		AnchorPane anchorPane = new AnchorPane();
		anchorPane.getChildren().addAll(vboxGen, hboxSlider);
		AnchorPane.setBottomAnchor(hboxSlider, 0d);

		HBox hboxCentral = new HBox();
		hboxCentral.setSpacing(10);
		HBox.setHgrow(ttv, Priority.ALWAYS);
		hboxCentral.getChildren().addAll(ttv, anchorPane);

		HBox hboxParams = new HBox();
		hboxParams.setAlignment(Pos.CENTER_LEFT);
		hboxParams.setSpacing(10);
		hboxParams.getChildren().add(new Label("Number of cases to create: "));
		tfNumber = new TextField("1000");
		hboxParams.getChildren().add(tfNumber);
		hboxParams.getChildren().add(new Label("Cases per request: "));
		tfBatchSize = new TextField("1000");
		hboxParams.getChildren().add(tfBatchSize);

		HBox hboxButtons = new HBox();
		hboxButtons.setSpacing(5);
		//		hboxButtons.setPadding(new Insets(5, 5, 5, 5));
		Button buttonOK = new Button("Create Cases");
		buttonOK.setOnMouseClicked((ev) -> {
			handleOKClick();
		});
		Button buttonCancel = new Button("Close");
		buttonCancel.setOnMouseClicked((ev) -> {
			termThread();
			stage.close();
		});

		hboxButtons.getChildren().addAll(buttonOK, buttonCancel);
		hboxButtons.setAlignment(Pos.CENTER_LEFT);

		VBox vbox = new VBox(typeHBox, hboxCentral, hboxParams, hboxButtons);
		vbox.setSpacing(5);
		vbox.setPadding(new Insets(0, 5, 5, 5));
		VBox.setVgrow(vbox, Priority.ALWAYS);
		VBox.setVgrow(ttv, Priority.ALWAYS);
		VBox.setVgrow(this, Priority.ALWAYS);
		getChildren().add(vbox);

		generatorChangeListener = new ChangeListener<GeneratorChoice>()
		{
			@Override
			public void changed(ObservableValue< ? extends GeneratorChoice> obs, GeneratorChoice old,
					GeneratorChoice nu)
			{
				handleGeneratorChange(nu);
			}
		};

		cbGenerator.getSelectionModel().selectedItemProperty().addListener(generatorChangeListener);

		CasedataDesign design = null; //main.getCasedataDesign();
		if (design != null)
		{
			ConjuringModel model = design.getModel();
			if (model != null)
			{
				String applicationId = model.getApplicationId();
				if (applicationId != null)
				{
					TypeInfo ti = typeInfos.stream().filter(t -> t.getAppId().toString().equals(applicationId))
							.findFirst().orElse(null);
					if (ti != null)
					{
						cbType.getSelectionModel().select(typeInfos.indexOf(ti));
						modelToTree(model);
					}

				}
			}
		}

		treeChangeListener = new ChangeListener<TreeItem<ConjuringBean>>()
		{
			public void changed(ObservableValue< ? extends TreeItem<ConjuringBean>> obs, TreeItem<ConjuringBean> old,
					TreeItem<ConjuringBean> nu)
			{
				handleTreeChange((TreeItem<ConjuringBean>) nu);
			}
		};

		ttv.getSelectionModel().selectedItemProperty().addListener(treeChangeListener);

		conjuringPreviewThread = new ConjuringPreviewThread(this);
		conjuringPreviewThread.start();

		slider.valueProperty().addListener((a) -> {
			conjuringPreviewThread.interrupt();
		});
	}

	private void openJSONWindow()
	{
		ConjuringModel model = treeToModel();
		StructuredType type = typeInfos.get(cbType.getSelectionModel().getSelectedIndex()).getType();
		//		main.openExampleWindow(false, typeInfos.get(cbType.getSelectionModel().getSelectedIndex()).getType(),
		//				new ConjuringCaseProvider(model, type));
	}

	private void onViewModelClick()
	{
		int selectedIndex = cbType.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0)
		{
			StructuredType type = typeInfos.get(selectedIndex).getType();
			if (type != null)
			{
				Stage s = ModelEditor.makeStage(type.getDataModel(),
						"Model for " + cbType.getItems().get(selectedIndex).toString());
				s.show();
			}
		}
	}

	public void autoSelect(BigInteger applicationId)
	{
		// Look up the type
		TypeInfo type = typeInfos.stream().filter(ti -> ti.getAppId().equals(applicationId)).findFirst().orElse(null);
		// If found, set the drop-down to the corresponding item and trigger the OK button behaviour.
		if (type != null)
		{
			int idx = typeInfos.indexOf(type);
			cbType.getSelectionModel().select(idx);
			handleOKClick();
		}
	}

	private String buildBody(CaseProvider cp, int quantity) throws Exception
	{
		StringBuilder buf = new StringBuilder();
		JsonNodeFactory fac = JsonNodeFactory.instance;
		ObjectNode root = fac.objectNode();
		root.set("caseType", fac.textNode(type.getParent().getNamespace() + "." + type.getName()));
		root.set("applicationMajorVersion", fac.numberNode(majorVersion));

		ArrayNode arrayNode = fac.arrayNode();
		for (int i = 0; i < quantity; i++)
		{
			String casedata = cp.buildCase(null).getCasedata();
			arrayNode.add(fac.textNode(casedata));
		}
		root.set("casedata", arrayNode);
		buf.append(prettyOM.writeValueAsString(root));

		return buf.toString();
	}

	private void handleOKClick()
	{
		try
		{
			ConjuringModel model = treeToModel();
			ConjuringCaseProvider ccp = new ConjuringCaseProvider(model, type);
			int quantity = Integer.parseInt(tfNumber.getText());
			int batchSize = Integer.parseInt(tfBatchSize.getText());
			String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getBase() + "/bpm/case/v1/cases";
			int done = 0;
			while (done < quantity)
			{
				int num = Math.min(batchSize, quantity - done);
				String body = buildBody(ccp, num);
				HTTPCaller caller = HTTPCaller.newPost(url, null, body);
				caller.call(true);
				String responseBody = caller.getResponseBody();
				JsonNode responseTree = om.readTree(responseBody);
				responseBody = prettyOM.writeValueAsString(responseTree);
				done += num;
			}
			Alert alert = new Alert(AlertType.INFORMATION,
					"Successfully created " + quantity + " case" + (quantity == 1 ? "" : "s"));
			alert.showAndWait();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void populateArrayOptionsPane(VBox vbox, ConjuringBean conjuringBean)
	{
		vbox.getChildren().clear();
		if (conjuringBean.getIsArray())
		{
			GridPane grid = new GridPane();
			grid.setAlignment(Pos.CENTER_LEFT);
			grid.setHgap(5);
			grid.setVgap(5);
			vbox.getChildren().add(grid);
			grid.add(new Label("Array min size"), 0, 0);
			grid.add(new Label("Array max size"), 0, 1);
			TextField tfMin = new TextField();
			TextField tfMax = new TextField();
			grid.add(tfMin, 1, 0);
			grid.add(tfMax, 1, 1);
			tfMin.textProperty().addListener((obs, old, nu) -> {
				conjuringBean.arrayMinSize.set(new Integer(nu));
			});
			tfMax.textProperty().addListener((obs, old, nu) -> {
				conjuringBean.arrayMaxSize.set(new Integer(nu));
			});
			tfMin.setText(conjuringBean.arrayMinSize.get().toString());
			tfMax.setText(conjuringBean.arrayMaxSize.get().toString());
		}
	}

	private void populateExclusionOptionsPane(VBox vbox, ConjuringBean conjuringBean)
	{
		vbox.getChildren().clear();
		if (conjuringBean.getIsObject())
		{
			CheckBox cbExclude = new CheckBox("Exclude branch");
			cbExclude.selectedProperty().addListener((obs, old, nu) -> {
				conjuringBean.isExcluded.set(nu);
			});
			cbExclude.setSelected(conjuringBean.isExcludedProperty().get());
			vbox.getChildren().add(cbExclude);
		}
	}

	private void populateOptionsPane(List<Option> options, Map<Option, Object> optionValues, String description)
	{
		optionsVBox.getChildren().clear();

		// Add description label (if that's a description). This would better belong
		// outside this pane.
		if (description != null)
		{
			Label descriptionLabel = new Label(description);
			descriptionLabel.setWrapText(true);
			optionsVBox.getChildren().addAll(descriptionLabel, new Separator());
		}

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER_LEFT);
		grid.setHgap(5);
		grid.setVgap(5);
		optionsVBox.getChildren().add(grid);
		int rowNum = 0;
		if (options != null)
		{
			for (Option option : options)
			{
				final Map<Option, Object> fOptionValues = optionValues;
				final Option fOption = option;
				if (option.getType() == OptionType.BOOLEAN)
				{
					CheckBox cbOption = new CheckBox();
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(cbOption, 1, rowNum);
					// If value known for option, set checkbox accordingly
					if (optionValues != null && optionValues.containsKey(option))
					{
						cbOption.setSelected((Boolean) optionValues.get(option));
					}
					cbOption.selectedProperty().addListener((value) -> {
						Boolean checked = ((BooleanProperty) value).get();
						fOptionValues.put(fOption, checked);
					});
				}
				else if (option.getType() == OptionType.TEXT)
				{
					TextField tfOption = new TextField();
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(tfOption, 1, rowNum);
					tfOption.textProperty().addListener((obs, old, nu) -> {
						fOptionValues.put(fOption, tfOption.getText());
					});
					// If value known for option, set text accordingly
					if (optionValues != null && optionValues.containsKey(option))
					{
						tfOption.setText("" + optionValues.get(option));
					}
				}
				else if (option.getType() == OptionType.INTEGER)
				{
					TextField tfOption = new TextField();
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(tfOption, 1, rowNum);
					tfOption.textProperty().addListener((obs, old, nu) -> {
						int i = 1;
						try
						{
							i = Integer.parseInt(nu);
							fOptionValues.put(fOption, i);
						}
						catch (NumberFormatException e)
						{
							// ignore
						}
					});
					// If value known for option, set text accordingly
					if (optionValues != null && optionValues.containsKey(option))
					{
						tfOption.setText("" + optionValues.get(option));
					}
				}
				else if (option.getType() == OptionType.BIG_DECIMAL)
				{
					TextField tfOption = new TextField();
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(tfOption, 1, rowNum);
					tfOption.textProperty().addListener((obs, old, nu) -> {
						BigDecimal bd;
						try
						{
							bd = new BigDecimal(nu);
							fOptionValues.put(fOption, bd);
						}
						catch (NumberFormatException e)
						{
							// ignore
						}
					});
					// If value known for option, set text accordingly
					if (optionValues != null && optionValues.containsKey(option))
					{
						tfOption.setText("" + optionValues.get(option));
					}
				}
				else if (option.getType() == OptionType.TEXT_LIST)
				{
					ListView<String> lv = new ListView<String>();
					Callback<ListView<String>, ListCell<String>> editableCell = TextFieldListCell.forListView();
					lv.setCellFactory(editableCell);
					lv.setEditable(true);
					lv.setOnEditStart((t) -> {
						String oldValue = lv.getItems().get(t.getIndex());
						if (FixedValuesConjurer.ADD_NEW.equals(oldValue))
						{
							lv.getItems().set(t.getIndex(), "");
						}
					});
					lv.setOnEditCommit((t) -> {
						{
							String newValue = t.getNewValue();
							if (!newValue.equals(""))
							{
								lv.getItems().set(t.getIndex(), newValue);
								String lastItem = lv.getItems().get(lv.getItems().size() - 1);
								if (!FixedValuesConjurer.ADD_NEW.equals(lastItem))
								{
									lv.getItems().add(FixedValuesConjurer.ADD_NEW);
								}
							}
							else
							{
								boolean isLastItem = t.getIndex() >= lv.getItems().size() - 1;
								lv.getItems().remove(t.getIndex());
								// If we just removed the last item, add the 'add' row
								if (isLastItem)
								{
									lv.getItems().add(FixedValuesConjurer.ADD_NEW);
								}
							}
						}

					});
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(lv, 1, rowNum);
					lv.setItems((ObservableList<String>) optionValues.get(option));
				}
				else if (option.getType() == OptionType.SINGLE_CHOICE)
				{
					ChoiceOption cOption = (ChoiceOption) option;
					ObservableList<OptionChoice> list = FXCollections.observableArrayList();
					for (int i = 0; i < cOption.getOptionLabels().size(); i++)
					{
						list.add(new OptionChoice(cOption.getOptionNames().get(i), cOption.getOptionLabels().get(i)));
					}
					ChoiceBox<OptionChoice> cb = new ChoiceBox<>();
					cb.setItems(list);
					grid.add(new Label(option.getLabel()), 0, rowNum);
					grid.add(cb, 1, rowNum);
					// If value known for option, select appropriately
					if (optionValues != null && optionValues.containsKey(option))
					{
						OptionChoice match = list.stream().filter(v -> v.getName().equals(optionValues.get(option)))
								.findFirst().orElse(null);
						if (match != null)
						{
							cb.getSelectionModel().select(match);
						}
					}
					else
					{
						// No value set, so default to first choice
						cb.getSelectionModel().select(0);
					}
					cb.getSelectionModel().selectedItemProperty().addListener((e) -> {
						fOptionValues.put(fOption, cb.getSelectionModel().getSelectedItem().getName());
					});

				}
				rowNum++;
			}
		}
	}

	private void handleTreeChange(TreeItem<ConjuringBean> item)
	{
		if (item != null)
		{
			ValueConjurer< ? > valueConjurer = item.getValue().conjurerProperty().get();
			if (valueConjurer != null)
			{
				// Highlight the generator in the choicebox
				GeneratorChoice choice = cbGenerator.getItems().stream()
						.filter(i -> i.className.equals(valueConjurer.getClass().getName())).findFirst().orElse(null);
				if (choice != null)
				{
					// Temporarily remove change listener to avoid selection change calling it.
					cbGenerator.getSelectionModel().selectedItemProperty().removeListener(generatorChangeListener);
					cbGenerator.getSelectionModel().select(choice);
					//					// Prevent generator selection being changed if this is the top-level 'state' attribute
					//					if (item.getValue().getName().equals("state") && item.getParent().getParent() == null)
					//					{
					//						cbGenerator.setDisable(true);
					//					}
					//					else if (item.getValue().hasAllowedValues())
					//					{
					//						cbGenerator.setDisable(true);
					//					}
					//					else
					//					{
					//						cbGenerator.setDisable(false);
					//					}
					//
					//					// Disable the options box if attr has allowed values
					//					// (it will be set to FixedValueConjurer with the allowed values in it)
					//					if (item.getValue().hasAllowedValues())
					//					{
					//						optionsVBox.setDisable(true);
					//					}
					//					else
					//					{
					//						optionsVBox.setDisable(false);
					//					}
					cbGenerator.getSelectionModel().selectedItemProperty().addListener(generatorChangeListener);
					cbGenerator.setVisible(true);
				}
				populateOptionsPane(valueConjurer.getOptions(), valueConjurer.getOptionValues(),
						valueConjurer.getDescription());
			}
			else
			{
				cbGenerator.setVisible(false);
				optionsVBox.getChildren().clear();
			}
			populateArrayOptionsPane(arraySettingsVBox, item.getValue());
			populateExclusionOptionsPane(exclusionSettingsVBox, item.getValue());
		}
	}

	public void termThread()
	{
		if (conjuringPreviewThread != null)
		{
			conjuringPreviewThread.term();
		}
	}

	public static List<TypeInfo> getAppsFromDatabase(BigInteger subscriptionId, BigInteger sandboxId, Connection conn)
			throws DataModelSerializationException
	{
		List<TypeInfo> result = new ArrayList<>();
		PreparedStatement ps = null;
		try
		{
			if (conn != null)
			{
				ps = conn.prepareStatement(SQL_READ);

				// subscription_id
				ps.setBigDecimal(1, new BigDecimal(subscriptionId));
				// sandbox_id
				ps.setBigDecimal(2, new BigDecimal(sandboxId));

				ResultSet rs = ps.executeQuery();
				// Read first (and theoretically only) row
				while (rs.next())
				{
					// model
					String dmJSON = rs.getString(1);
					DataModel dm = DataModel.deserialize(dmJSON);

					// application_id
					BigInteger appId = rs.getBigDecimal(2).toBigInteger();
					for (StructuredType type : dm.getStructuredTypes())
					{
						if (type.getIsCase())
						{
							result.add(new TypeInfo(type, appId));
						}
					}
				}

			}

		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if (ps != null)
			{
				try
				{
					ps.close();
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public Long getSpeed()
	{
		return (long) -slider.getValue();
	}

	public Stage getStage()
	{
		// TODO Auto-generated method stub
		return stage;
	}
}
