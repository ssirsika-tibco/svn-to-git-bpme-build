package com.tibco.bpm.cdm.core.deployment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.NotAuthorisedException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.ApplicationIdAndMajorVersion;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.DataModelInfo;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.dto.DTOTransmuter;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoAttributeConstraintsDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoAttributeDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoDependencyDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoLinkDTO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoStateDTO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.Constraint;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.da.dm.api.Link;
import com.tibco.bpm.da.dm.api.LinkEnd;
import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StateModel;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.de.rest.model.AuthorizationRequest;
import com.tibco.bpm.de.rest.model.AuthorizationResponse;
import com.tibco.bpm.de.rest.model.SystemActionRequest;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.auth.SystemActionId;
import com.tibco.n2.de.api.services.SecurityService;

/**
 * Manages data models
 * @author smorgan
 * @since 2019
 */
public class DataModelManager
{
	static CLFClassContext		logCtx			= CloudLoggingFramework.init(DataModelManager.class,
			CDMLoggingInfo.instance);

	private ApplicationDAO		applicationDAO;

	private TypeDAO				typeDAO;

	private DataModelDAO		dataModelDAO;

	private static final String	BASE_PREFIX		= "base:";

	private SecurityService		securityService	= null;

    DAOFactory daoFactory;

	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		applicationDAO = daoFactory.getApplicationDAOImpl();
		dataModelDAO = daoFactory.getDataModelDAOImpl();
		typeDAO = daoFactory.getTypeDAOImpl();
	}
		
	// Called by Spring
	public void setSecurityService(SecurityService securityService)
	{
		this.securityService = securityService;
	}

	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	
	public boolean isActionAuthorised(SystemActionId eSystemAction)
	{
		CLFMethodContext clf = logCtx.getMethodContext("isActionAuthorised");

		boolean authorised = false;

		String component = String.valueOf(eSystemAction.getComponent());
		String name = String.valueOf(eSystemAction.getName());

		if (null != securityService)
		{
			AuthorizationRequest actions = new AuthorizationRequest();

			SystemActionRequest action = new SystemActionRequest();
			action.setComponent(component);
			action.setAction(name);
			actions.getActions().add(action);

			AuthorizationResponse response;
			try
			{
				response = securityService.isActionAuthorized(actions);

				authorised = response.getOverall().booleanValue();
			}
			catch (Exception e)
			{
				clf.local.info(e, "Exception checking authorisation for %s %s", component, name);
			}
		}

		if (!authorised)
		{
			clf.local.debug("Caller is not authorized for this action %s %s", component, name);
		}

		return authorised;
	}

	// Removes 'base:' from the beginning of a string
	private static String stripBasePrefix(String value)
	{
		if (value.startsWith(BASE_PREFIX))
		{
			value = value.substring(BASE_PREFIX.length());
		}
		return value;
	}

	private TypeInfoAttributeConstraintsDTO buildTypeInfoAttributeConstraintsDTO(Attribute attribute)
	{
		TypeInfoAttributeConstraintsDTO constraints = null;

		Constraint constraintLength = attribute.getConstraint(Constraint.NAME_LENGTH);
		if (constraintLength != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setLength(Integer.valueOf(constraintLength.getValue()));
		}

		Constraint constraintMinValue = attribute.getConstraint(Constraint.NAME_MIN_VALUE);
		if (constraintMinValue != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setMinValue(constraintMinValue.getValue());
		}

		Constraint constraintMinValueInclusive = attribute.getConstraint(Constraint.NAME_MIN_VALUE_INCLUSIVE);
		if (constraintMinValueInclusive != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setMinValueInclusive(Boolean.parseBoolean(constraintMinValueInclusive.getValue()));
		}

		Constraint constraintMaxValue = attribute.getConstraint(Constraint.NAME_MAX_VALUE);
		if (constraintMaxValue != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setMaxValue(constraintMaxValue.getValue());
		}

		Constraint constraintMaxValueInclusive = attribute.getConstraint(Constraint.NAME_MAX_VALUE_INCLUSIVE);
		if (constraintMaxValueInclusive != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setMaxValueInclusive(Boolean.parseBoolean(constraintMaxValueInclusive.getValue()));
		}

		Constraint constraintDecimalPlaces = attribute.getConstraint(Constraint.NAME_DECIMAL_PLACES);
		if (constraintDecimalPlaces != null)
		{
			if (constraints == null)
			{
				constraints = new TypeInfoAttributeConstraintsDTO();
			}
			constraints.setDecimalPlaces(Integer.valueOf(constraintDecimalPlaces.getValue()));
		}

		return constraints;
	}

	/**
	 * Get type information, optionally filtered.
	 * 
	 * @param applicationId
	 * @param namespace
	 * @param majorVersion
	 * @param isCase
	 * @param skip
	 * @param top
	 * @param includeStates
	 * @param includeLinks
	 * @param includeDependencies
	 * @return
	 * @throws PersistenceException
	 * @throws DataModelSerializationException
	 * @throws InternalException
	 * @throws ArgumentException 
	 */
	public List<TypeInfoDTO> getTypes(String applicationId, String namespace, Integer majorVersion, Boolean isCase,
			Integer skip, Integer top, boolean includeAttributes, boolean includeSummaryAttributes,
			boolean includeStates, boolean includeLinks, boolean includeDependencies)
			throws PersistenceException, DataModelSerializationException, InternalException, ArgumentException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("getTypes");
		}

		List<TypeInfoDTO> types = null;
		types = typeDAO.getTypes(applicationId, namespace, majorVersion, isCase, skip, top);
		BigInteger lastDataModelId = null;
		DataModel dm = null;

		// If we've been asked to populate dependendency information, work out which application(s) we're dealing
		// with and then obtain a map from each application to a list of the foreign namespaces (and containing
		// applications) each depends on.  When enumerating types later, we'll see which namespaces each actually
		// refers to and populate its dependency list accordingly.
		Map<ApplicationIdAndMajorVersion, List<TypeInfoDependencyDTO>> foreignNamespaceDependenciesByApp = null;
		if (includeDependencies)
		{
			// Build a list of distinct applicationId / major version combos
			List<ApplicationIdAndMajorVersion> applications = DTOTransmuter.getDistinctApplications(types);

			// For each application, get its dependencies. This map will be used below.
			foreignNamespaceDependenciesByApp = applicationDAO.getForeignNamespaceDependencies(applications);
		}

		//
		for (TypeInfoDTO dto : types)
		{
			BigInteger dataModelId = dto.getDataModelId();
			if (!dataModelId.equals(lastDataModelId))
			{
				DataModelInfo dmi = dataModelDAO.read(dataModelId);
				dm = DataModel.deserialize(dmi.getModelJson());
				lastDataModelId = dataModelId;
			}
			String typeName = dto.getName();
			StructuredType structuredType = dm.getStructuredTypeByName(typeName);
			if (structuredType != null)
			{
				dto.setLabel(structuredType.getLabel());
				dto.setType(structuredType);

				if (includeAttributes || includeSummaryAttributes)
				{
					List<TypeInfoAttributeDTO> attributeDTOs = new ArrayList<TypeInfoAttributeDTO>();
					List<TypeInfoAttributeDTO> summaryAttributeDTOs = new ArrayList<TypeInfoAttributeDTO>();
					for (Attribute attr : structuredType.getAttributes())
					{
						boolean isSummary = attr.getIsSummary();
						if (includeAttributes || (isSummary && includeSummaryAttributes))
						{
							TypeInfoAttributeDTO aDTO = new TypeInfoAttributeDTO();
							aDTO.setLabel(attr.getLabel());
							aDTO.setName(attr.getName());
							aDTO.setIsArray(attr.getIsArray());
							aDTO.setIsMandatory(attr.getIsMandatory());
							aDTO.setIsIdentifier(attr.getIsIdentifier());
							aDTO.setIsAutoIdentifier(attr.getParent().hasDynamicIdentifier());
							aDTO.setIsSearchable(attr.getIsSearchable());
							aDTO.setIsSummary(attr.getIsSummary());
							aDTO.setIsState(attr.getIsState());
							String type = attr.getType();
							type = stripBasePrefix(type);
							aDTO.setType(type);
							aDTO.setIsStructuredType(!(attr.getTypeObject() instanceof BaseType));
							aDTO.setConstraints(buildTypeInfoAttributeConstraintsDTO(attr));

							if (includeAttributes)
							{
								attributeDTOs.add(aDTO);
							}
							if (includeSummaryAttributes && isSummary)
							{
								summaryAttributeDTOs.add(aDTO);
							}
						}
					}
					if (includeAttributes)
					{
						dto.setAttributes(attributeDTOs);
					}
					if (includeSummaryAttributes)
					{
						dto.setSummaryAttributes(summaryAttributeDTOs);
					}
				}

				if (includeStates)
				{
					// Populate state model
					StateModel stateModel = structuredType.getStateModel();
					if (stateModel != null)
					{
						List<TypeInfoStateDTO> stateDTOs = new ArrayList<TypeInfoStateDTO>();
						for (State state : stateModel.getStates())
						{
							TypeInfoStateDTO stateDTO = new TypeInfoStateDTO();
							stateDTO.setLabel(state.getLabel());
							stateDTO.setValue(state.getValue());
							stateDTO.setIsTerminal(state.getIsTerminal());
							stateDTOs.add(stateDTO);
						}
						dto.setStates(stateDTOs);
					}
				}

				if (includeLinks)
				{
					List<TypeInfoLinkDTO> linkDTOs = new ArrayList<>();
					dto.setLinks(linkDTOs);
					for (Link link : dm.getLinks())
					{
						LinkEnd end1 = link.getEnd1();
						LinkEnd end2 = link.getEnd2();
						if (end1.getOwnerObject() == structuredType)
						{
							TypeInfoLinkDTO lDTO = new TypeInfoLinkDTO();
							lDTO.setName(end1.getName());
							lDTO.setLabel(end1.getLabel());
							lDTO.setIsArray(end1.getIsArray());
							// Type of _opposite_ (target) end
							String type = end2.getOwner();
							lDTO.setType(type);
							linkDTOs.add(lDTO);
						}
						if (end2.getOwnerObject() == structuredType)
						{
							TypeInfoLinkDTO lDTO = new TypeInfoLinkDTO();
							lDTO.setName(end2.getName());
							lDTO.setLabel(end2.getLabel());
							lDTO.setIsArray(end2.getIsArray());
							// Type of _opposite_ (target) end
							String type = end1.getOwner();
							lDTO.setType(type);
							linkDTOs.add(lDTO);
						}
					}
				}

				if (includeDependencies)
				{
					// Trawl namespaces from attributes' types
					List<String> referencedNamespaces = new ArrayList<>();
					for (Attribute attr : structuredType.getAttributes())
					{
						if (!(attr.getTypeObject() instanceof BaseType))
						{
							String aType = attr.getType();
							if (aType != null && aType.contains("."))
							{
								QualifiedTypeName qName = new QualifiedTypeName(attr.getType());
								String rNamespace = qName.getNamespace();
								if (!referencedNamespaces.contains(rNamespace))
								{
									referencedNamespaces.add(rNamespace);
								}
							}
						}
					}

					// Add dependency object to DTO for each required namespace.
					// (Some of the requiredNamespaces may refer to other namespaces within
					// _this_ project, so aren't needed)
					List<TypeInfoDependencyDTO> list = foreignNamespaceDependenciesByApp.get(
							new ApplicationIdAndMajorVersion(dto.getApplicationId(), dto.getApplicationMajorVersion()));
					if (list != null)
					{
						List<TypeInfoDependencyDTO> usedDeps = new ArrayList<>();
						for (TypeInfoDependencyDTO d : list)
						{
							if (referencedNamespaces.contains(d.getNamespace()))
							{
								usedDeps.add(d);
							}
						}
						if (!usedDeps.isEmpty())
						{
							dto.setDependencies(usedDeps);
						}
					}
				}
			}
		}
		return types;
	}
	
	public List<BigInteger> getApplicationIds() throws PersistenceException
	{
		return dataModelDAO.getApplicationIds();
	}
}
