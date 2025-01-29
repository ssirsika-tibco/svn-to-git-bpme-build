package com.tibco.bpm.cdm.core.dto;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * DTO for transferring info about a type
 * @author smorgan
 * @since 2019
 */
public class TypeInfoDTO
{
	public static class TypeInfoStateDTO
	{
		private String	label;

		private String	value;

		private boolean	isTerminal;

		public String getLabel()
		{
			return label;
		}

		public void setLabel(String label)
		{
			this.label = label;
		}

		public String getValue()
		{
			return value;
		}

		public void setValue(String value)
		{
			this.value = value;
		}

		public boolean getIsTerminal()
		{
			return isTerminal;
		}

		public void setIsTerminal(boolean isTerminal)
		{
			this.isTerminal = isTerminal;
		}
	}

	public static class TypeInfoLinkDTO
	{
		private String	label;

		private String	name;

		private String	type;

		private boolean	isArray;

		public String getLabel()
		{
			return label;
		}

		public void setLabel(String label)
		{
			this.label = label;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public boolean getIsArray()
		{
			return isArray;
		}

		public void setIsArray(boolean isArray)
		{
			this.isArray = isArray;
		}
	}

	public static class TypeInfoAttributeDTO
	{
		private String							name;

		private String							label;

		private String							type;

		private boolean							isStructuredType;

		private boolean							isIdentifier;

		private boolean							isAutoIdentifier;

		private boolean							isState;

		private boolean							isArray;

		private boolean							isMandatory;

		private boolean							isSearchable;

		private boolean							isSummary;

		private TypeInfoAttributeConstraintsDTO	constraints;

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getLabel()
		{
			return label;
		}

		public void setLabel(String label)
		{
			this.label = label;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public boolean getIsStructuredType()
		{
			return isStructuredType;
		}

		public void setIsStructuredType(boolean isStructuredType)
		{
			this.isStructuredType = isStructuredType;
		}

		public boolean getIsIdentifier()
		{
			return isIdentifier;
		}

		public void setIsIdentifier(boolean isIdentifier)
		{
			this.isIdentifier = isIdentifier;
		}

		public boolean getIsAutoIdentifier()
		{
			return isAutoIdentifier;
		}

		public void setIsAutoIdentifier(boolean isAutoIdentifier)
		{
			this.isAutoIdentifier = isAutoIdentifier;
		}

		public boolean getIsState()
		{
			return isState;
		}

		public void setIsState(boolean isState)
		{
			this.isState = isState;
		}

		public boolean getIsArray()
		{
			return isArray;
		}

		public void setIsArray(boolean isArray)
		{
			this.isArray = isArray;
		}

		public boolean getIsMandatory()
		{
			return isMandatory;
		}

		public void setIsMandatory(boolean isMandatory)
		{
			this.isMandatory = isMandatory;
		}

		public boolean getIsSearchable()
		{
			return isSearchable;
		}

		public void setIsSearchable(boolean isSearchable)
		{
			this.isSearchable = isSearchable;
		}

		public boolean getIsSummary()
		{
			return isSummary;
		}

		public void setIsSummary(boolean isSummary)
		{
			this.isSummary = isSummary;
		}

		public TypeInfoAttributeConstraintsDTO getConstraints()
		{
			return constraints;
		}

		public void setConstraints(TypeInfoAttributeConstraintsDTO constraints)
		{
			this.constraints = constraints;
		}
	}

	public static class TypeInfoAttributeConstraintsDTO
	{
		private Integer	length;

		private String	minValue;

		private Boolean	minValueInclusive;

		private String	maxValue;

		private Boolean	maxValueInclusive;

		private Integer	decimalPlaces;

		public Integer getLength()
		{
			return length;
		}

		public void setLength(Integer length)
		{
			this.length = length;
		}

		public String getMinValue()
		{
			return minValue;
		}

		public void setMinValue(String minValue)
		{
			this.minValue = minValue;
		}

		public Boolean getMinValueInclusive()
		{
			return minValueInclusive;
		}

		public void setMinValueInclusive(Boolean minValueInclusive)
		{
			this.minValueInclusive = minValueInclusive;
		}

		public String getMaxValue()
		{
			return maxValue;
		}

		public void setMaxValue(String maxValue)
		{
			this.maxValue = maxValue;
		}

		public Boolean getMaxValueInclusive()
		{
			return maxValueInclusive;
		}

		public void setMaxValueInclusive(Boolean maxValueInclusive)
		{
			this.maxValueInclusive = maxValueInclusive;
		}

		public Integer getDecimalPlaces()
		{
			return decimalPlaces;
		}

		public void setDecimalPlaces(Integer decimalPlaces)
		{
			this.decimalPlaces = decimalPlaces;
		}
	}

	public static class TypeInfoDependencyDTO
	{
		private String	namespace;

		private String	applicationId;

		private int		applicationMajorVersion;

		public String getNamespace()
		{
			return namespace;
		}

		public void setNamespace(String namespace)
		{
			this.namespace = namespace;
		}

		public String getApplicationId()
		{
			return applicationId;
		}

		public void setApplicationId(String applicationId)
		{
			this.applicationId = applicationId;
		}

		public int getApplicationMajorVersion()
		{
			return applicationMajorVersion;
		}

		public void setApplicationMajorVersion(int applicationMajorVersion)
		{
			this.applicationMajorVersion = applicationMajorVersion;
		}
	}

	private StructuredType				type;

	private String						name;

	private String						label;

	private boolean						isCase;

	private String						namespace;

	private int							applicationMajorVersion;

	private String						applicationId;

	private BigInteger					dataModelId;

	private List<TypeInfoAttributeDTO>	attributes;

	private List<TypeInfoAttributeDTO>	summaryAttributes;

	private List<TypeInfoStateDTO>		states;

	private List<TypeInfoLinkDTO>		links;

	private List<TypeInfoDependencyDTO>	dependencies;

	public StructuredType getType()
	{
		return type;
	}

	public void setType(StructuredType type)
	{
		this.type = type;
	}

	public void setCase(boolean isCase)
	{
		this.isCase = isCase;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public boolean getIsCase()
	{
		return isCase;
	}

	public void setIsCase(boolean isCase)
	{
		this.isCase = isCase;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}

	public int getApplicationMajorVersion()
	{
		return applicationMajorVersion;
	}

	public void setApplicationMajorVersion(int applicationMajorVersion)
	{
		this.applicationMajorVersion = applicationMajorVersion;
	}

	public String getApplicationId()
	{
		return applicationId;
	}

	public void setApplicationId(String applicationId)
	{
		this.applicationId = applicationId;
	}

	public BigInteger getDataModelId()
	{
		return dataModelId;
	}

	public void setDataModelId(BigInteger dataModelId)
	{
		this.dataModelId = dataModelId;
	}

	public List<TypeInfoAttributeDTO> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<TypeInfoAttributeDTO> attributes)
	{
		this.attributes = attributes;
	}

	public List<TypeInfoAttributeDTO> getSummaryAttributes()
	{
		return summaryAttributes;
	}

	public void setSummaryAttributes(List<TypeInfoAttributeDTO> summaryAttributes)
	{
		this.summaryAttributes = summaryAttributes;
	}

	public List<TypeInfoStateDTO> getStates()
	{
		return states;
	}

	public void setStates(List<TypeInfoStateDTO> states)
	{
		this.states = states;
	}

	public List<TypeInfoLinkDTO> getLinks()
	{
		return links;
	}

	public void setLinks(List<TypeInfoLinkDTO> links)
	{
		this.links = links;
	}

	public List<TypeInfoDependencyDTO> getDependencies()
	{
		return dependencies;
	}

	public void setDependencies(List<TypeInfoDependencyDTO> dependencies)
	{
		this.dependencies = dependencies;
	}

}
