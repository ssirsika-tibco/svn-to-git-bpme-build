package com.tibco.bpm.cdm.core.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.api.rest.v1.model.Link;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfo;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoAttribute;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoAttributeConstraints;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoDependency;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoLink;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfoState;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.ApplicationIdAndMajorVersion;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoAttributeConstraintsDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoAttributeDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoDependencyDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoLinkDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoStateDTO;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * Converts between beans suitable for the REST interface and internal (not interface specific) DTO pojos.
 * @author smorgan
 * @since 2019
 */
public class DTOTransmuter
{
	/**
	 * Generates a GetTypeResponseItemAttribute REST response object reflecting the
	 * given Attribute.
	 * 
	 * @param attribute
	 * @return
	 */
	public static TypeInfoAttribute toTypeInfoAttribute(TypeInfoAttributeDTO dto)
	{
		//		CLFMethodContext clf = logCtx.getMethodContext("toGetTypeResponseItemAttribute");
		TypeInfoAttribute attrItem = new TypeInfoAttribute();
		attrItem.setName(dto.getName());
		attrItem.setLabel(dto.getLabel());
		if (dto.getIsIdentifier())
		{
			attrItem.setIsIdentifier(true);
			if (dto.getIsAutoIdentifier())
			{
				attrItem.setIsAutoIdentifier(true);
			}
		}
		if (dto.getIsState())
		{
			attrItem.setIsState(true);
		}

		attrItem.setType(dto.getType());
		if (dto.getIsStructuredType())
		{
			attrItem.setIsStructuredType(true);
		}

		if (dto.getIsArray())
		{
			attrItem.setIsArray(true);
		}
		if (dto.getIsMandatory())
		{
			attrItem.setIsMandatory(true);
		}
		if (dto.getIsSearchable())
		{
			attrItem.setIsSearchable(true);
		}
		if (dto.getIsSummary())
		{
			attrItem.setIsSummary(true);
		}

		attrItem.setConstraints(toTypeInfoAttributeConstraints(dto.getConstraints()));

		return attrItem;
	}

	private static TypeInfoAttributeConstraints toTypeInfoAttributeConstraints(
			TypeInfoAttributeConstraintsDTO constraints)
	{
		TypeInfoAttributeConstraints result = null;
		if (constraints != null)
		{
			result = new TypeInfoAttributeConstraints();
			result.setLength(constraints.getLength());
			result.setMinValue(constraints.getMinValue());
			result.setMinValueInclusive(constraints.getMinValueInclusive());
			result.setMaxValue(constraints.getMaxValue());
			result.setMaxValueInclusive(constraints.getMaxValueInclusive());
			result.setDecimalPlaces(constraints.getDecimalPlaces());
		}
		return result;
	}

	/**
	 * Produces a list of GetTypeResponseItemAttribute REST response objects: One for
	 * each of the supplied Attributes.
	 * 
	 * @param attributes
	 * @return
	 */
	public static List<TypeInfoAttribute> toTypeInfoAttributes(List<TypeInfoAttributeDTO> attributes)
	{
		List<TypeInfoAttribute> result = new ArrayList<>();
		for (TypeInfoAttributeDTO attribute : attributes)
		{
			result.add(toTypeInfoAttribute(attribute));
		}
		return result;
	}

	/**
	 * Converts the given TypeInfoDTO to a REST TypeInfo bean, copying only the
	 * requested aspects.
	 * 
	 * @param typeInfo
	 * @param includeBasic
	 * @param includeAttributes
	 * @param includeSummaryAttributes
	 * @param includeStates
	 * @param includeDependencies
	 * @param includeLinks
	 * @return
	 */
	public static TypeInfo toTypeInfo(TypeInfoDTO typeInfo, boolean includeBasic, boolean includeAttributes,
			boolean includeSummaryAttributes, boolean includeStates, boolean includeLinks, boolean includeDependencies)
	{
		TypeInfo responseItem = new TypeInfo();
		StructuredType type = typeInfo.getType();
		if (type != null)
		{
			if (includeBasic)
			{
				responseItem.setNamespace(typeInfo.getNamespace());
				responseItem.setApplicationMajorVersion(typeInfo.getApplicationMajorVersion());
				if (type.getIsCase())
				{
					responseItem.setIsCase(true);
				}
				responseItem.setName(type.getName());
				responseItem.setLabel(type.getLabel());
				responseItem.setApplicationId(typeInfo.getApplicationId());

			}

			if (includeAttributes)
			{
				responseItem.getAttributes().addAll(toTypeInfoAttributes(typeInfo.getAttributes()));
			}

			if (includeSummaryAttributes)
			{
				responseItem.getSummaryAttributes().addAll(toTypeInfoAttributes(typeInfo.getSummaryAttributes()));
			}

			if (includeStates && type.getIsCase())
			{
				responseItem.getStates().addAll(toTypeInfoStates(typeInfo.getStates()));
			}

			if (includeDependencies)
			{
				List<TypeInfoDependencyDTO> dependencies = typeInfo.getDependencies();
				if (dependencies != null)
				{
					responseItem.getDependencies().addAll(toTypeInfoDependencies(dependencies));
				}
			}
			if (includeLinks && type.getIsCase())
			{
				responseItem.getLinks().addAll(toTypeInfoLinks(typeInfo.getLinks()));
			}
		}
		return responseItem;
	}

	/**
	 * Converts the given TypeInfoLinkDTOs to a REST TypeInfoLink beans.
	 * @param dtos
	 * @return
	 */
	private static List<TypeInfoLink> toTypeInfoLinks(List<TypeInfoLinkDTO> dtos)
	{
		return dtos.stream().map(DTOTransmuter::toTypeInfoLink).collect(Collectors.toList());
	}

	/**
	 * Converts the given TypeInfoLinkDTO to a REST TypeInfoLink bean.
	 * @param dto
	 * @return
	 */
	private static TypeInfoLink toTypeInfoLink(TypeInfoLinkDTO dto)
	{
		TypeInfoLink bean = new TypeInfoLink();
		bean.setName(dto.getName());
		bean.setLabel(dto.getLabel());
		bean.setType(dto.getType());
		if (dto.getIsArray())
		{
			bean.setIsArray(true);
		}
		return bean;
	}

	/**
	 * Converts the given TypeInfoDependencyDTOs to REST TypeInfoDependency beans.
	 * @param dtos
	 * @return
	 */
	private static List<TypeInfoDependency> toTypeInfoDependencies(List<TypeInfoDependencyDTO> dtos)
	{
		return dtos.stream().map(DTOTransmuter::toTypeInfoDependency).collect(Collectors.toList());
	}

	/**
	 * Converts the given TypeInfoDependencyDTO to a REST TypeInfoDependency bean.
	 * @param dto
	 * @return
	 */
	private static TypeInfoDependency toTypeInfoDependency(TypeInfoDependencyDTO dto)
	{
		TypeInfoDependency bean = new TypeInfoDependency();
		bean.setNamespace(dto.getNamespace());
		bean.setApplicationId(dto.getApplicationId());
		bean.setApplicationMajorVersion(dto.getApplicationMajorVersion());
		return bean;
	}

	/**
	 * Converts the given TypeInfoStateDTOs to REST TypeInfoState beans.
	 * @param dto
	 * @return
	 */
	private static List<TypeInfoState> toTypeInfoStates(List<TypeInfoStateDTO> dtos)
	{
		return dtos.stream().map(DTOTransmuter::toTypeInfoState).collect(Collectors.toList());
	}

	/**
	 * Converts the given TypeInfoStateDTO to a REST TypeInfoState bean.
	 * @param dto
	 * @return
	 */
	private static TypeInfoState toTypeInfoState(TypeInfoStateDTO dto)
	{
		TypeInfoState bean = new TypeInfoState();
		bean.setLabel(dto.getLabel());
		bean.setValue(dto.getValue());
		if (dto.getIsTerminal())
		{
			bean.setIsTerminal(true);
		}
		return bean;
	}

	/**
	 * Returns a list of distinct applications (id/version) that appear in the supplied
	 * list of type infos.
	 * @param typeDTOs
	 * @return
	 */
	public static List<ApplicationIdAndMajorVersion> getDistinctApplications(List<TypeInfoDTO> typeDTOs)
	{
		List<ApplicationIdAndMajorVersion> result = new ArrayList<>();
		for (TypeInfoDTO typeDTO : typeDTOs)
		{
			ApplicationIdAndMajorVersion idAndVersion = new ApplicationIdAndMajorVersion(typeDTO.getApplicationId(),
					typeDTO.getApplicationMajorVersion());
			if (!result.contains(idAndVersion))
			{
				result.add(idAndVersion);
			}
		}
		return result;
	}

	public static CaseLinkDTO toCaseLinkDTO(Link link) throws ArgumentException
	{
		String refString = link.getCaseReference();
		if (refString == null)
		{
			throw ReferenceException.newInvalidFormat("null");
		}
		CaseReference ref = new CaseReference(refString);
		CaseLinkDTO dto = new CaseLinkDTO(link.getName(), ref);
		return dto;
	}

	public static List<CaseLinkDTO> toCaseLinkDTOs(List<Link> links) throws ArgumentException
	{
		List<CaseLinkDTO> result = new ArrayList<>();
		for (Link link : links)
		{
			result.add(toCaseLinkDTO(link));
		}
		return result;
	}

	public static Link toLink(CaseLinkDTO dto)
	{
		Link link = new Link();
		link.setName(dto.getName());
		link.setCaseReference(dto.getCaseReference().toString());
		return link;
	}

	public static List<Link> toLinks(List<CaseLinkDTO> dtos)
	{
		return dtos.stream().map(DTOTransmuter::toLink).collect(Collectors.toList());
	}
}
