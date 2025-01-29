package com.tibco.bpm.cdm.core;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfo;
import com.tibco.bpm.cdm.core.aspect.TypeAspectSelection;
import com.tibco.bpm.cdm.core.deployment.DataModelManager;
import com.tibco.bpm.cdm.core.dto.DTOTransmuter;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.rest.RESTRequestException;
import com.tibco.bpm.cdm.core.rest.v1.FilterParser;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public  class TypesPrivateServiceImpl  extends AbstractService implements TypesPrivateService {

	static CLFClassContext		logCtx						= CloudLoggingFramework.init(TypesPrivateServiceImpl.class,
			CDMLoggingInfo.instance);
	
	private static final int	APPLICATION_ID_MAX_LENGTH	= 256;

	private static final int	NAMESPACE_MAX_LENGTH		= 256;

	private DataModelManager	dataModelManager;

	private List<String>		FILTER_PROPS				= Arrays.asList("applicationId", "applicationMajorVersion",
			"isCase", "namespace");
	
	@Override
	public List<TypeInfo> typesGet(String filter, String select, Integer skip, Integer top) throws Exception {

		CLFMethodContext clf = logCtx.getMethodContext("typesGet");
		try
		{

			// Parse $select
			// As there is only one aspect (for now), we merely validate this; It has no effect on our behaviour.
			TypeAspectSelection aspectSelection = TypeAspectSelection.fromSelectExpression(select);

			// Parse $filter
			FilterParser parser = new FilterParser(filter);

			// Check that only valid expressions were given (i.e. none that were
			// either not recognised by the parser or were recognised, but are
			// not appropriate for this API).
			parser.validate(FILTER_PROPS);

			// Get applicationId from $filter
			String applicationId = parser.getApplicationId();
			if (applicationId != null && applicationId.length() > APPLICATION_ID_MAX_LENGTH)
			{
				throw RESTRequestException.newApplicationIdInvalid(applicationId);
			}

			// Get namespace from $filter
			String namespace = parser.getNamespace();
			if (namespace != null && namespace.length() > NAMESPACE_MAX_LENGTH)
			{
				throw RESTRequestException.newNamespaceInvalid(namespace);
			}

			// Get majorVersion from $filter
			String majorVersionString = parser.getApplicationMajorVersion();
			Integer majorVersion = null;
			if (majorVersionString != null)
			{
				try
				{
					majorVersion = Integer.parseInt(majorVersionString);
				}
				catch (NumberFormatException e)
				{
					throw RESTRequestException.newApplicationMajorVersionInvalid(majorVersionString);
				}
			}

			// Get isCase from $filter
			Boolean isCase = null;
			if (parser.isIsCaseValid())
			{
				isCase = parser.getIsCaseAsBoolean();
			}

			// Validate $skip
			if (skip != null && skip < 0)
			{
				throw ArgumentException.newSkipInvalid(skip);
			}

			// Validate $top
			if (top == null)
			{
				throw ArgumentException.newTopMandatory();
			}
			else if (top < 0)
			{
				throw ArgumentException.newTopInvalid(top);
			}

			boolean includeBasic = aspectSelection.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_BASIC);
			boolean includeAttributes = aspectSelection.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_ATTRIBUTES);
			boolean includeSummaryAttributes = aspectSelection
					.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_SUMMARY_ATTRIBUTES);
			boolean includeStates = aspectSelection.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_STATES);
			boolean includeLinks = aspectSelection.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_LINKS);
			boolean includeDependencies = aspectSelection
					.includesAnyOrIsNothing(TypeAspectSelection.ASPECT_DEPENDENCIES);

			List<TypeInfoDTO> typeDTOs = dataModelManager.getTypes(applicationId, namespace, majorVersion, isCase, skip,
					top, includeAttributes, includeSummaryAttributes, includeStates, includeLinks, includeDependencies);
			List<TypeInfo> typeBeans = typeDTOs
					.stream().map(t -> DTOTransmuter.toTypeInfo(t, includeBasic, includeAttributes,
							includeSummaryAttributes, includeStates, includeLinks, includeDependencies))
					.collect(Collectors.toList());
			return typeBeans;
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	
		
	}

	public DataModelManager getDataModelManager() {
		return dataModelManager;
	}

	public void setDataModelManager(DataModelManager dataModelManager) {
		this.dataModelManager = dataModelManager;
	}


}
