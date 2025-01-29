package com.tibco.bpm.cdm.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CasedataException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.NotAuthorisedException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.api.exception.ValidationException;
import com.tibco.bpm.cdm.api.message.CaseLifecycleNotification.Event;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBodyItem;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.cln.CLNDispatcher;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO;
import com.tibco.bpm.cdm.core.dao.CaseDAO;
import com.tibco.bpm.cdm.core.dao.CaseDAO.CaseCreationInfo;
import com.tibco.bpm.cdm.core.dao.CaseLinkDAO;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.DataModelInfo;
import com.tibco.bpm.cdm.core.dao.IdentifierValueDAO;
import com.tibco.bpm.cdm.core.dao.LinkDAO;
import com.tibco.bpm.cdm.core.dao.StateDAO;
import com.tibco.bpm.cdm.core.dao.StateDAO.StateInfo;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.core.dto.LinkDTO;
import com.tibco.bpm.cdm.core.logging.CDMAuditMessages;
import com.tibco.bpm.cdm.core.logging.CDMDebugMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.search.model.DataModelModelStructuredType;
import com.tibco.bpm.cdm.libs.dql.DQLParser;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.util.CasedataIntrospector;
import com.tibco.bpm.cdm.util.CasedataValidator;
import com.tibco.bpm.cdm.util.TimestampOp;
import com.tibco.bpm.container.engine.api.ContainerEngineInstances;
import com.tibco.bpm.container.engine.model.InstanceInfo;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.de.rest.model.AuthorizationRequest;
import com.tibco.bpm.de.rest.model.AuthorizationResponse;
import com.tibco.bpm.de.rest.model.SystemActionRequest;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.auth.SystemActionId;
import com.tibco.n2.common.orm.SequenceDAO;
import com.tibco.n2.common.security.CurrentUser;
import com.tibco.n2.common.security.RequestContext;
import com.tibco.n2.common.util.SequenceID;
import com.tibco.n2.de.api.services.SecurityService;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * Manages case lifecycle operations.
 * @author smorgan
 * @since 2019
 */
public class CaseManager
{
	// Users are represented by a 36 character GUID, with the exception of the
	// special user tibco-admin, where it is 'tibco-admin'.
	
	/**
	 * Reference to the SequenceCache object used to get the next sequence ID
	 */
	private CaseSequenceCache					caseSequenceCache;
		
	public CaseSequenceCache getCaseSequenceCache() {
		return caseSequenceCache;
	}

	public void setCaseSequenceCache(CaseSequenceCache caseSequenceCache) {
		this.caseSequenceCache = caseSequenceCache;
	}
	private static final int			MAX_USER_GUID_LENGTH	= 36;

	static CLFClassContext				logCtx					= CloudLoggingFramework.init(CaseManager.class,
			CDMLoggingInfo.instance);
	
	private static final CaseAspectSelection	REF_AND_CASEDATA	= CaseAspectSelection
			.fromAspects(CaseAspectSelection.ASPECT_CASE_REFERENCE, CaseAspectSelection.ASPECT_CASEDATA);

	private static final ObjectMapper	om						= new ObjectMapper();

	private static final int CASE_SEQ_CACHE_SIZE = 50;
	
	static
	{
		om.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		om.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
		// We can't use this as it was added in 2.9.
		// This detects the 't' in: "{}t"
		// We can get away with not using this, as long as we always rewrite JSON before
		// attempting to store it in the database, but it would be better if we could reject it.
		//		om.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
	}

	private DataModelDAO				dataModelDAO;

	private StateDAO					stateDAO;

	private CaseDAO						caseDAO;

	private LinkDAO						linkDAO;

	private CaseLinkDAO					caseLinkDAO;

	private TypeDAO						typeDAO;

	private IdentifierValueDAO			identifierValueDAO;

	private ApplicationDAO				applicationDAO;

	private SecurityService				securityService	= null;

	private CLNDispatcher				clnDispatcher;

	private ContainerEngineInstances	processInstancesAPI;
	
    DAOFactory daoFactory;

	private long numSequenceIDs = 0;

	private long nextSequenceID = 1;

	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		dataModelDAO = daoFactory.getDataModelDAOImpl();
		stateDAO = daoFactory.getStateDAOImpl();
		caseDAO = daoFactory.getCaseDAOImpl();
		linkDAO = daoFactory.getLinkDAOImpl();
		caseLinkDAO = daoFactory.getCaseLinkDAOImpl();
		typeDAO = daoFactory.getTypeDAOImpl();
		identifierValueDAO = daoFactory.getIdentifierValueDAOImpl();			
		applicationDAO = daoFactory.getApplicationDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	// Called by Spring
	public void setProcessInstancesAPI(ContainerEngineInstances processInstancesAPI)
	{
		this.processInstancesAPI = processInstancesAPI;
	}

	// Called by Spring
	public void setClnDispatcher(CLNDispatcher clnDispatcher)
	{
		this.clnDispatcher = clnDispatcher;
	}


	// Called by Spring
	public void setSecurityService(SecurityService securityService)
	{
		this.securityService = securityService;
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

	private ObjectNode jsonStringToObjectNode(String jsonString) throws ValidationException
	{
		JsonNode node = null;
		if (jsonString == null)
		{
			throw ValidationException.newNotJSON(null);
		}
		try
		{
			node = om.readTree(jsonString);
			if (!(node instanceof ObjectNode))
			{
				throw ValidationException.newNotJSONObject();
			}
		}
		catch (IOException e)
		{
			throw ValidationException.newNotJSON(e);
		}
		return (ObjectNode) node;
	}

	/**
	 * Creates cases.
	 * @param qType
	 * @param majorVersion
	 * @param casedata
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException
	 * @throws InternalException
	 * @throws ValidationException 
	 * @throws ArgumentException 
	 * @throws NotAuthorisedException 
	 */
	public List<CaseReference> createCases(QualifiedTypeName qType, int majorVersion, List<String> casedata)
			throws PersistenceException, ReferenceException, InternalException, ValidationException, ArgumentException,
			NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("createCases");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("createCases");
		}

		if (qType == null)
		{
			throw ArgumentException.newTypeInvalid(null);
		}
		String namespace = qType.getNamespace();
		String typeName = qType.getName();
		if (namespace == null || namespace.length() == 0 || typeName.length() == 0)
		{
			throw ArgumentException.newTypeInvalid(qType.toString());
		}

		// Look up type
		DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, true);
		DataModel dm = null;
		if (dataModelInfo == null)
		{
			throw ReferenceException.newUnknownNamespace(namespace, majorVersion);
		}
		dm = dataModelInfo.getDataModel();

		StructuredType structuredType = dm.getStructuredTypeByName(typeName);
		if (structuredType == null)
		{
			throw ReferenceException.newUnknownType(qType.toString(), majorVersion);
		}
		if (!structuredType.getIsCase())
		{
			throw ReferenceException.newNotCaseType(qType.toString(), majorVersion);
		}

		Map<String, BigInteger> typeIdMap = typeDAO.read(dataModelInfo.getId());
		BigInteger typeId = typeIdMap.get(typeName);

		List<StateInfo> stateInfos = stateDAO.get(typeName, namespace, majorVersion);

		Attribute stateAttribute = structuredType.getStateAttribute();
		Attribute cidAttribute = structuredType.getIdentifierAttribute();
		String cidAttributeName = cidAttribute.getName();
		boolean generateIdentifier = structuredType.hasDynamicIdentifier();

		// If auto-generated identifiers are required, get them all now, as it is far
		// more efficient to do this in one go.
		List<String> generatedIdentifierValues = null;
		if (generateIdentifier)
		{
			generatedIdentifierValues = Arrays.asList(caseSequenceCache.getCaseIds(typeId,  casedata.size(), identifierValueDAO));
			//generatedIdentifierValues = identifierValueDAO.getIdentifierValues(typeId, casedata.size());
		}

		List<CaseCreationInfo> ccis = new ArrayList<>();
		JsonNodeFactory fac = JsonNodeFactory.instance;
		for (int i = 0; i < casedata.size(); i++)
		{
			String caseJson = casedata.get(i);

			// Deserialize casedata as JSON
			ObjectNode caseObjectNode = jsonStringToObjectNode(caseJson);

			// Apply auto-generated identifier, if required
			if (generateIdentifier)
			{
				// Check that caller hasn't passed a value (not allowed when auto-generated)
				if (caseObjectNode.has(cidAttributeName))
				{
					JsonNode cidNode = caseObjectNode.get(cidAttributeName);
					if (!(cidNode instanceof MissingNode || cidNode instanceof NullNode))
					{
						throw CasedataException.newCIDWhenAuto(cidNode.asText());
					}
				}

				// Get the identifier we generated earler.
				String identifierValue = generatedIdentifierValues.get(i);

				// Construct a value node to suit the CID attribute's type
				JsonNode identifierValueNode = cidAttribute.getTypeObject() == BaseType.TEXT
						? fac.textNode(identifierValue)
						: fac.numberNode(new BigDecimal(identifierValue));
				caseObjectNode.set(cidAttributeName, identifierValueNode);
			}

			// Validate casedata against model
			Map<String, String> removals = CasedataValidator.validate(structuredType, caseObjectNode, true, false,
					false, true, true);

			// Rewrite JSON (currently to cleanse trailing tokens etc), but will become
			// crucial once default value and auto-identifier populate is done.
			try
			{
				caseJson = om.writeValueAsString(caseObjectNode);
			}
			catch (JsonProcessingException e)
			{
				throw InternalException.newInternalException(e);
			}

			JsonNode stateValue = caseObjectNode.get(stateAttribute.getName());
			final String stateValueString;
			if (stateValue instanceof TextNode)
			{
				stateValueString = stateValue.asText();
			}
			else
			{
				stateValueString = null;
			}

			String cidValueString = null;
			if (cidAttribute != null)
			{
				JsonNode cidValue = caseObjectNode.get(cidAttribute.getName());
				if (cidValue != null)
				{
					cidValueString = cidValue.asText();
				}
				if (cidValueString == null)
				{
					throw CasedataException.newIdentifierNotSet(cidAttribute.getName());
				}
			}

			// Find the id corresponding to the value
			StateInfo stateInfo = stateInfos.stream().filter(si -> si.getValue().equals(stateValueString)).findFirst()
					.orElse(null);

			if (stateInfo == null)
			{
				throw CasedataException.newUnknownStateValue(stateValueString);
			}

			CaseCreationInfo cci = new CaseCreationInfo(caseJson, cidValueString, stateInfo.getId(),
					stateInfo.getValue());

			// If any superfluous data was removed from the casedata, note this 
			// so we can audit it later.
			if (!removals.isEmpty())
			{
				cci.setRemovals(removals);
			}

			ccis.add(cci);
		}
		List<BigInteger> caseIds = caseDAO.create(typeId, ccis, acquireCurrentUserGUID());
		List<CaseReference> refs = new ArrayList<>();
		for (int i = 0; i < caseIds.size(); i++)
		{
			BigInteger caseId = caseIds.get(i);
			CaseReference ref = new CaseReference(qType, majorVersion, caseId, 0);
			refs.add(ref);
			CaseCreationInfo cci = ccis.get(i);
			clf.audit.audit(CDMAuditMessages.CDM_CASE_CREATED,
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, ref.toString()),
					clf.param(CloudMetaData.CASE_PAYLOAD, cci.getCasedata()),
					clf.param(CloudMetaData.CASE_STATE, cci.getStateValue()));

			Map<String, String> removals = cci.getRemovals();
			if (removals != null)
			{
				// Audit the fact that superfluous data was removed from the casedata
				// Note that the case_payload logging parameter is populated with the list of removals,
				// not the entire casedata.
				clf.audit.audit(CDMAuditMessages.CDM_CASE_UNRECOGNISED_CONTENT_REMOVED,
						clf.param(CommonMetaData.MANAGED_OBJECT_ID, ref.toString()),
						clf.param(CloudMetaData.CASE_PAYLOAD, removals.toString()));
			}
		}
		return refs;
	}

	/**
	 * Reads a single case by reference, returning just the requested aspects.
	 * 
	 * @param caseReference
	 * @param aspectSelection
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException
	 * @throws InternalException
	 * @throws NotAuthorisedException 
	 */
	public CaseInfoDTO readCase(CaseReference caseReference, CaseAspectSelection aspectSelection,
			boolean returnNullIfNotExists)
			throws PersistenceException, ReferenceException, InternalException, NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("readCase");
		}

		if (caseReference == null)
		{
			throw ReferenceException.newInvalidFormat(null);
		}

		CaseInfoDTO dto = caseDAO.read(caseReference, aspectSelection);
		if (dto != null && aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY))
		{
			populateSummaryFromCasedata(Collections.singletonList(dto));
		}
		if (dto == null)
		{
			// Not found, but is that because the _type_ doesn't exist?
			QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
			if (qtn != null)
			{
				String namespace = qtn.getNamespace();
				String name = qtn.getName();
				int majorVersion = caseReference.getApplicationMajorVersion();
				BigInteger typeId = typeDAO.getId(namespace, majorVersion, name);
				if (typeId == null)
				{
					// The type doesn't exist, so we'll report that as the problem,
					// rather than misleading the caller by merely saying the case doesn't exist.
					throw ReferenceException.newUnknownType(qtn.toString(), majorVersion);
				}
			}
		}
		if (dto == null && !returnNullIfNotExists)
		{
			// The caller wants us to fail rather than returning null when a case doesn't exist
			throw ReferenceException.newNotExist(caseReference.toString());
		}
		return dto;
	}

	/**
	 * Uses the casedata aspect in each DTO to populate the corresponding summary aspect.
	 * 
	 * @param dtos
	 * @throws PersistenceException
	 * @throws InternalException
	 */
	private void populateSummaryFromCasedata(List<CaseInfoDTO> dtos) throws PersistenceException, InternalException
	{
		// TODO The cases may be of the same type, or a mixture of types. We'll keep a list of models
		// we've loaded, so we can avoid fetching the same one twice.

		// Derive summary from casedata
		for (CaseInfoDTO dto : dtos)
		{
			QualifiedTypeName qName = dto.getTypeName();
			String namespace = qName.getNamespace();
			int majorVersion = dto.getMajorVersion();
			// No need to load dependencies as searchable attributes are always top-level.
			DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, false);
			if (dataModelInfo == null)
			{
				throw InternalException.newInternalException("Type missing");
			}
			DataModel dm = dataModelInfo.getDataModel();
			StructuredType structuredType = dm.getStructuredTypeByName(qName.getName());

			String casedata = dto.getCasedata();
			String summary = CasedataIntrospector.buildSummary(casedata, structuredType);
			dto.setSummary(summary);
		}
	}

	/**
	 * Reads one or more cases by reference, returning just the requested aspects.
	 * Returns cases in the same order as the reference list.
	 * 
	 * @param caseReferences
	 * @param aspectSelection
	 * @return
	 * @throws PersistenceException
	 * @throws InternalException
	 * @throws ReferenceException
	 * @throws NotAuthorisedException 
	 */
	public List<CaseInfoDTO> readCases(List<CaseReference> caseReferences, CaseAspectSelection aspectSelection)
			throws PersistenceException, InternalException, ReferenceException, NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("readCases");
		}

		List<CaseInfoDTO> dtos = caseDAO.read(caseReferences, aspectSelection);
		if (aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY))
		{
			populateSummaryFromCasedata(dtos);
		}

		return dtos;
	}

	public List<CaseInfoDTO> readCases(QualifiedTypeName qType, int majorVersion, Integer skip, Integer top, String cid,
			String stateValue, Calendar maxModificationTimestamp, TimestampOp opr, String search, String dql,
			CaseAspectSelection aspectSelection, boolean excludeTerminalState) throws PersistenceException,
			ReferenceException, InternalException, ArgumentException, NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("readCases");
		}

		CLFMethodContext clf = logCtx.getMethodContext("readCases");
		// Validate Top
		if (top == null)
		{
			throw ArgumentException.newTopMandatory();
		}
		else if (top < 0)
		{
			throw ArgumentException.newTopInvalid(top);
		}
		if (qType == null)
		{
			throw ArgumentException.newTypeInvalid(null);
		}
		String namespace = qType.getNamespace();
		String typeName = qType.getName();
		if (namespace == null || namespace.length() == 0 || typeName.length() == 0)
		{
			throw ArgumentException.newTypeInvalid(qType.toString());
		}
		StructuredType structuredType = null;

		BigInteger typeId = typeDAO.getId(namespace, majorVersion, typeName);
		if (typeId == null)
		{
			throw ReferenceException.newUnknownType(qType.toString(), majorVersion);
		}

		// When DQL is used, Search, CID, Case State and Modification Timestamp must not be used
		if (dql != null && (search != null || cid != null || stateValue != null || maxModificationTimestamp != null))
		{
			throw ArgumentException.newDQLWithOtherSearchParameters();
		}

		// Look up type, if needed.
		if (search != null || dql != null || aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY))
		{
			// No need to load dependencies as summary attributes are always top-level.
			DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, false);
			if (dataModelInfo == null)
			{
				throw ReferenceException.newUnknownNamespace(namespace, majorVersion);
			}
			DataModel dm = dataModelInfo.getDataModel();
			structuredType = dm.getStructuredTypeByName(typeName);
		}

		SearchConditionDTO condition = null;
		if (dql != null)
		{
			DQLParser parser = new DQLParser(new DataModelModelStructuredType(structuredType));
			condition = parser.parse(dql);
			if (parser.hasIssues())
			{
				throw ArgumentException.newBadDQL(parser.getIssues().toString());
			}
			clf.local.debug("DQL Parser returned: " + condition);
		}
		List<CaseInfoDTO> dtos = caseDAO.read(typeId, skip, top, cid, stateValue, maxModificationTimestamp, opr, search,
				condition, structuredType, aspectSelection, excludeTerminalState);

		// Augment the DTOs with type info
		dtos.forEach(dto -> {
			dto.setTypeName(qType);
			dto.setMajorVersion(majorVersion);
		});

		// If summary was requested, generate it from the casedata
		if (aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY))
		{
			// Derived summary from casedata
			for (CaseInfoDTO dto : dtos)
			{
				String casedata = dto.getCasedata();
				String summary = CasedataIntrospector.buildSummary(casedata, structuredType);
				dto.setSummary(summary);
			}
		}
		return dtos;
	}

	public void updateCases(List<CaseUpdateDTO> cases) throws PersistenceException, ReferenceException,
			InternalException, ArgumentException, ValidationException, NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("updateCases");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("updateCases");
		}

		if (cases != null && !cases.isEmpty())
		{
			// Validate that cases are all the same type
			QualifiedTypeName lastQName = null;
			Long lastMajorVersion = null;
			for (CaseUpdateDTO dto : cases) 
			{
				CaseReference caseReference = dto.getCaseReference();
				QualifiedTypeName qName = caseReference.getQualifiedTypeName();
				if (lastQName == null)
				{
					lastQName = qName;
				}
				else if (!lastQName.toString().equals(qName.toString()))
				{
					throw ArgumentException.newCasesSameType();
				}
				long majorVersion = caseReference.getApplicationMajorVersion();
				if (lastMajorVersion == null)
				{
					lastMajorVersion = majorVersion;
				}
				else if (!lastMajorVersion.equals(majorVersion))
				{
					throw ArgumentException.newCasesSameMajorVersion();
				}
			}

			// We've already established that all refs are the same type, so we can take the 
			// type info from the first, knowing it applies to all.
			QualifiedTypeName qType = cases.get(0).getCaseReference().getQualifiedTypeName();

			String namespace = qType.getNamespace();
			String typeName = qType.getName();

			int majorVersion = cases.get(0).getCaseReference().getApplicationMajorVersion();

			// Look up type
			DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, true);
			if (dataModelInfo == null)
			{
				throw ReferenceException.newUnknownNamespace(namespace, majorVersion);
			}
			DataModel dm = dataModelInfo.getDataModel();

			StructuredType structuredType = dm.getStructuredTypeByName(typeName);
			if (structuredType == null)
			{
				throw ReferenceException.newUnknownType(qType.toString(), majorVersion);
			}

			List<StateInfo> stateInfos = stateDAO.get(typeName, namespace, majorVersion);
			Attribute stateAttribute = structuredType.getStateAttribute();
			Attribute cidAttribute = structuredType.getIdentifierAttribute();

			for (CaseUpdateDTO caze : cases)
			{
				String casedataJson = caze.getCasedata();
				ObjectNode caseObjectNode = jsonStringToObjectNode(casedataJson);
				 
				//handle the scenario when the case id is auto,it is passed as null
				if(null!=caseObjectNode) {
					JsonNode valueNode = caseObjectNode.get(cidAttribute.getName());
					if(null==valueNode) {
						clf.local.debug("passed in the case ID :[%s] for is null, so fetch from DB and updated it",cidAttribute.getName());
						//now read the case object from the DB
						CaseInfoDTO caseInfo = readCase(caze.getCaseReference(), REF_AND_CASEDATA, false);
						ObjectNode caseDataFromDB = jsonStringToObjectNode(caseInfo.getCasedata());
						String caseIdValue = caseDataFromDB.get(cidAttribute.getName()).textValue();
						TextNode textNode = JsonNodeFactory.instance.textNode(caseIdValue);
						caseObjectNode.set(cidAttribute.getName(), textNode);
						clf.local.debug("set the value[%s] for case ID :[%s]",cidAttribute.getName(),caseIdValue);
					}
				}
				
				// Validate casedata against model
				Map<String, String> removals = CasedataValidator.validate(structuredType, caseObjectNode, true, false,
						false, true, true);

				// If any superfluous data was removed from the casedata, note this 
				// so we can audit it later.
				if (!removals.isEmpty())
				{
					caze.setRemovals(removals);
				}

				JsonNode stateValue = caseObjectNode.get(stateAttribute.getName());
				final String stateValueString;
				if (stateValue instanceof TextNode)
				{
					stateValueString = stateValue.asText();
				}
				else
				{
					stateValueString = null;
				}
				caze.setNewStateValue(stateValueString);

				StateInfo stateInfo = stateInfos.stream().filter(si -> si.getValue().equals(stateValueString))
						.findFirst().orElse(null);
				caze.setNewStateId(stateInfo.getId());

				if (cidAttribute != null)
				{
					JsonNode newCIDValue = caseObjectNode.get(cidAttribute.getName());
					if (newCIDValue instanceof TextNode || newCIDValue instanceof NumericNode)
					{
						String newCIDString = newCIDValue.asText();
						caze.setNewCID(newCIDString);
					}
				}

				// Rewrite JSON (currently to cleanse trailing tokens etc), but will become
				// crucial once default value and auto-identifier populate is done.
				try
				{
					caze.setCasedata(om.writeValueAsString(caseObjectNode));
				}
				catch (JsonProcessingException e)
				{
					throw InternalException.newInternalException(e);
				}
			}

			caseDAO.update(cases, acquireCurrentUserGUID());

			// Audit updates
			for (CaseUpdateDTO caze : cases)
			{
				if (!caze.getNewStateId().equals(caze.getOldStateId()))
				{
					// Audit case update (with state change)
					clf.audit.audit(CDMAuditMessages.CDM_CASE_UPDATED_STATE_CHANGE,
							clf.param(CommonMetaData.MANAGED_OBJECT_ID, caze.getNewCaseReference().toString()),
							clf.param(CloudMetaData.CASE_PAYLOAD, caze.getCasedata()),
							clf.param(CloudMetaData.CASE_STATE, caze.getNewStateValue()));
				}
				else
				{
					// Audit case update (without state change)
					clf.audit.audit(CDMAuditMessages.CDM_CASE_UPDATED_NO_STATE_CHANGE,
							clf.param(CommonMetaData.MANAGED_OBJECT_ID, caze.getNewCaseReference().toString()),
							clf.param(CloudMetaData.CASE_PAYLOAD, caze.getCasedata()),
							clf.param(CloudMetaData.CASE_STATE, caze.getNewStateValue()));
				}

				Map<String, String> removals = caze.getRemovals();
				if (removals != null)
				{
					// Audit the fact that superfluous data was removed from the casedata
					// Note that the case_payload logging parameter is populated with the list of removals,
					// not the entire casedata.
					clf.audit.audit(CDMAuditMessages.CDM_CASE_UNRECOGNISED_CONTENT_REMOVED,
							clf.param(CommonMetaData.MANAGED_OBJECT_ID, caze.getNewCaseReference().toString()),
							clf.param(CloudMetaData.CASE_PAYLOAD, removals.toString()));
				}
			}

			// Notify CLN observers
			if (clnDispatcher != null && clnDispatcher.hasObservers())
			{
				clnDispatcher.dispatch(Event.UPDATED,
						cases.stream().map(CaseUpdateDTO::getNewCaseReference).collect(Collectors.toList()));
			}

		}
	}

	public void deleteCase(CaseReference ref)
			throws ReferenceException, NotAuthorisedException, PersistenceException, InternalException
	{
		deleteCase(ref, false);
	}

	public void deleteCase(CaseReference ref, boolean skipAuthorisationCheck)
			throws PersistenceException, ReferenceException, InternalException, NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("deleteCase");

		// When called from auto-purge, this flag is true, as the system is always allowed to delete.
		if (!skipAuthorisationCheck)
		{
			boolean isActionAuthorised = isActionAuthorised(SystemActionId.deleteCase);

			if (!isActionAuthorised)
			{
				throw NotAuthorisedException.newNotAuthorisedException("deleteCase");
			}
		}
		if (ref == null)

		{
			throw ReferenceException.newInvalidFormat(null);
		}

        // Get the linked cases for this case reference.
        List<CaseReference> caseRefs = new ArrayList<CaseReference>();
        caseRefs.add(ref);
        List<CaseReference> linkedCases = getLinkedCases(caseRefs);

		// TODO delete links explicitly, then remove cascades from cdm_case_links's FKs (ACE-1471)
		caseLinkDAO.delete(ref, null, null, null);
		caseDAO.delete(ref);
		clf.audit.audit(CDMAuditMessages.CDM_CASE_DELETED, clf.param(CommonMetaData.MANAGED_OBJECT_ID, ref.toString()));

		// Notify CLN observers
		if (clnDispatcher != null && clnDispatcher.hasObservers())
		{
			clnDispatcher.dispatch(Event.DELETED, ref);
		}

        if (!linkedCases.isEmpty()) {
            // Audit each unlinked case
            for (CaseReference lref : linkedCases) {
                clf.audit.audit(CDMAuditMessages.CDM_CASE_IMPLICIT_UNLINKED,
                        clf.param(CommonMetaData.MANAGED_OBJECT_ID,
                                lref.toString()));
            }
        }
	}

	public int deleteCases(QualifiedTypeName qType, int majorVersion, String caseState,
			Calendar maxModificationTimestamp, Boolean isReferencedByProcess)
			throws PersistenceException, ReferenceException, ArgumentException, 
			NotAuthorisedException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("deleteCases");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.deleteCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("deleteCases");
		}

		String namespace = qType.getNamespace();
		String typeName = qType.getName();

		// If type has no namespace (i.e. wasn't fully qualified), reject it.
		if (namespace == null)
		{
			throw ReferenceException.newUnknownType(qType.toString(), majorVersion);
		}

		BigInteger typeId = typeDAO.getId(namespace, majorVersion, typeName);
		if (typeId == null)
		{
			throw ReferenceException.newUnknownType(qType.toString(), majorVersion);
		}

		// Case state is mandatory
		if (caseState == null)
		{
			throw ArgumentException.newCaseStateMandatory();
		}

		// Max modification timestamp is mandatory
		if (maxModificationTimestamp == null)
		{
			throw ArgumentException.newModificationTimestampMandatory();
		}

        List<CaseReference> linkedCases = new ArrayList<CaseReference>();
		// Delete cases, getting a list of their refs
        List<CaseReference> deletedRefs = new ArrayList<CaseReference>();
        if (isReferencedByProcess) {

            // Get the list of cases to be deleted and their linked cases.
            List<CaseReference> selectedCases =
                    caseDAO.getCasesByTypeStateTimestamp(typeId,
                            caseState,
                            maxModificationTimestamp);
            linkedCases = getLinkedCases(selectedCases);
            
            // Delete the links for the case references.
            for(CaseReference ref : selectedCases) {
            	caseLinkDAO.delete(ref, null, null, null);
            }

            // Delete these cases even if these are associated with running
            // process instances.
            deletedRefs =
                    caseDAO.delete(typeId, caseState, maxModificationTimestamp);
        } else {

            if (processInstancesAPI == null) {
                clf.local
                        .messageId(
                                CDMDebugMessages.CDM_JOB_AUTO_PURGE_CASE_TYPE)
                        .warn("ContainerEngineInstances service is not available, so it is not possible to determine if cases are associated with process instances.");
                return 0;
            }

            // Delete only the cases which are not associated with running
            // process instances.
            List<CaseReference> selectedCases =
                    caseDAO.getCasesByTypeStateTimestamp(typeId,
                            caseState,
                            maxModificationTimestamp);

            // From these case references filter out the ones associated with
            // the process instances.
            for (CaseReference ref : selectedCases) {
                List<String> gorefs = new ArrayList<String>();
                gorefs.add(ref.toString());
                try {
                    List<InstanceInfo> instances =
                            processInstancesAPI.findInstancesByGoRefs(gorefs);
                    if (instances == null) {
                        // No process instances associated with the given
                        // caseref.
                        // Its safe to delete it.
                        deletedRefs.add(ref);
                    } 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Get the linked cases for the cases to be deleted.
            linkedCases = getLinkedCases(deletedRefs);
            for (CaseReference ref : deletedRefs) {
            	
            	// First delete the case links for this case ref.
            	caseLinkDAO.delete(ref, null, null, null);
            	
                caseDAO.delete(ref);
            }

        }
		if (!deletedRefs.isEmpty())
		{
            // Audit each deletion
			for (CaseReference ref : deletedRefs)
			{
				clf.audit.audit(CDMAuditMessages.CDM_CASE_DELETED,
						clf.param(CommonMetaData.MANAGED_OBJECT_ID, ref.toString()));
			}

			// Notify CLN observers
			if (clnDispatcher != null && clnDispatcher.hasObservers())
			{
				clnDispatcher.dispatch(Event.DELETED, deletedRefs);
			}
		}

        if (!linkedCases.isEmpty()) {
            // Audit each unlinked case
            for (CaseReference ref : linkedCases) {
                clf.audit.audit(CDMAuditMessages.CDM_CASE_IMPLICIT_UNLINKED,
                        clf.param(CommonMetaData.MANAGED_OBJECT_ID,
                                ref.toString()));
            }
        }
		// Return count of deleted cases
		return deletedRefs.size();
	}

    /**
     * Gets the case references for the linked cases.
     * 
     * @param caserefs
     * @return the list of CaseReference for the linked cases.
     */
    private List<CaseReference> getLinkedCases(List<CaseReference> caserefs)
            throws PersistenceException, ReferenceException {

        List<CaseReference> linkedCases = new ArrayList<CaseReference>();
        for (CaseReference ref : caserefs) {
            List<CaseReference> links = caseDAO.getLinkedCases(ref, 1);
            if (links.size() > 0) {
                linkedCases.addAll(links);
            }
            links = caseDAO.getLinkedCases(ref, 2);
            if (links.size() > 0) {
                linkedCases.addAll(links);
            }
        }
        return linkedCases;
    }

	public void createCaseLinks(CaseReference caseReference, List<CaseLinkDTO> dtos) throws PersistenceException,
			ReferenceException, InternalException, ArgumentException, NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("createCaseLinks");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("createCaseLinks");
		}

		if (caseReference == null)
		{
			throw ReferenceException.newInvalidFormat(null);
		}

		QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
		BigInteger typeId = typeDAO.getId(qtn.getNamespace(), caseReference.getApplicationMajorVersion(),
				qtn.getName());

		if (typeId == null)
		{
			throw ReferenceException.newUnknownType(qtn.toString(), caseReference.getApplicationMajorVersion());
		}

		// Check for duplicate case references for a given link name
		for (CaseLinkDTO dto : dtos)
		{
			String dtoName = dto.getName();

			if (dto.getCaseReference() == null)
			{
				throw ReferenceException.newInvalidFormat(null);
			}
			// Validate link name
			if (dtoName == null)
			{
				throw ArgumentException.newLinkNameMandatory();
			}
			LinkDTO link = linkDAO.getLink(null, typeId, dtoName);
			if (link == null)
			{
				throw ReferenceException.newLinkNameNotExist(dtoName, qtn.toString(),
						caseReference.getApplicationMajorVersion());
			}

			CaseReference dtoRef = dto.getCaseReference();
			BigInteger dtoId = dtoRef.getId();

			// Count DTOs with the same link name and case reference (excluding version)
			long count = dtos.stream()
					.filter(d -> d.getName().equals(dtoName) && d.getCaseReference().getId().equals(dtoId)).count();

			// Same ref appears twice? Reject.
			if (count > 1)
			{
				throw ReferenceException.newDuplicateLink(dtoName, caseReference.toString(), dtoRef.toString());
			}
		}

		caseLinkDAO.create(typeId, caseReference, dtos);

		for (CaseLinkDTO dto : dtos)
		{
			// Audit event for the source case
			clf.audit.audit(CDMAuditMessages.CDM_CASE_LINKED,
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, caseReference.toString()),
					clf.param(CommonMetaData.ROLE_NAME, dto.getName()),
					clf.param(CommonMetaData.CASE_REFERENCE, dto.getCaseReference()));

			// Audit event for the target case
			clf.audit.audit(CDMAuditMessages.CDM_CASE_LINKED,
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, dto.getCaseReference()),
					clf.param(CommonMetaData.ROLE_NAME, dto.getOppositeName()),
					clf.param(CommonMetaData.CASE_REFERENCE, caseReference));
		}
	}

	public List<CaseLinkDTO> getCaseLinks(CaseReference caseReference, String name, Integer skip, Integer top,
			String search, String dql) throws PersistenceException, InternalException, ReferenceException,
			ArgumentException, NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getCaseLinks");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("readCaseLinks");
		}

		// Validate Top
		if (top == null)
		{
			throw ArgumentException.newTopMandatory();
		}
		else if (top < 0)
		{
			throw ArgumentException.newTopInvalid(top);
		}

		// When DQL is used, Search must not be used
		if (dql != null && search != null)
		{
			throw ArgumentException.newDQLWithSearch();
		}

		LinkDTO link = null;
		BigInteger typeId = null;
		// If name is specified, check that it is a valid link name
		if (name != null)
		{
			// Get type id for source type
			QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
			typeId = typeDAO.getId(qtn.getNamespace(), caseReference.getApplicationMajorVersion(), qtn.getName());
			if (typeId == null)
			{
				throw ReferenceException.newUnknownType(caseReference.getQualifiedTypeName().toString(),
						caseReference.getApplicationMajorVersion());
			}

			link = linkDAO.getLink(null, typeId, name);

			if (link == null)
			{
				throw ReferenceException.newLinkNameNotExist(name, qtn.toString(),
						caseReference.getApplicationMajorVersion());
			}
		}

		StructuredType targetType = null;
		SearchConditionDTO condition = null;
		if (search != null || dql != null)
		{
			// Search or DQL are only allowed when 'name' is set. Otherwise, we can't
			// be sure that all target cases will be of a single type (and hence
			// can't validate the DQL against a model)
			if (name == null)
			{
				throw ArgumentException.newDQLNeedsLinkName();
			}

			// If we reach here, given that 'name' is non-null, we can assume
			// that 'typeId' and 'link' will have been set when validating the name, above.

			String targetTypeNamespace = null;
			int targetTypeMajorVersion = 0;
			String targetTypeName = null;

			// Work out which end of the link is the target and get the type info 
			// for the target type.
			if (link.getEnd1Name().equals(name) && link.getEnd1TypeId().equals(typeId))
			{
				QualifiedTypeName end2QTN = link.getEnd2TypeQTN();
				targetTypeNamespace = end2QTN.getNamespace();
				targetTypeName = end2QTN.getName();
				targetTypeMajorVersion = link.getEnd2TypeMajorVersion();
			}
			else if (link.getEnd2Name().equals(name) && link.getEnd2TypeId().equals(typeId))
			{
				QualifiedTypeName end1QTN = link.getEnd1TypeQTN();
				targetTypeNamespace = end1QTN.getNamespace();
				targetTypeName = end1QTN.getName();
				targetTypeMajorVersion = link.getEnd1TypeMajorVersion();
			}
			else
			{
				// It should not be possible for the Link DAO to return a link where neither end matches
				throw InternalException.newInternalException("Link info inconsistent");
			}

			// No need to load dependencies as searchable attributes are always top-level.
			DataModelInfo dataModelInfo = dataModelDAO.read(targetTypeNamespace, targetTypeMajorVersion, false);
			if (dataModelInfo == null)
			{
				// It should not be possible to fail to find the Data Model that a Link refers to
				throw InternalException.newInternalException("Data Model info inconsistent");
			}
			DataModel dm = dataModelInfo.getDataModel();
			targetType = dm.getStructuredTypeByName(targetTypeName);

			// If DQL is specified, parse it. The other scenario is that a simple search string
			// was specified, in which case having the targetType is enough.
			if (dql != null)
			{
				DQLParser parser = new DQLParser(new DataModelModelStructuredType(targetType));
				condition = parser.parse(dql);
				if (parser.hasIssues())
				{
					throw ArgumentException.newBadDQL(parser.getIssues().toString());
				}
				clf.local.debug("DQL Parser returned: " + condition);
			}
		}

		List<CaseLinkDTO> links = caseLinkDAO.get(caseReference, targetType, name, skip, top, search, condition);
		if (links.isEmpty())
		{
			// No links found, so check that is legitimate and not cause by the case no existing
			if (!caseDAO.exists(caseReference))
			{
				// Case doesn't exist, so see if type exists, so appropriate error can be reported
				QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
				int majorVersion = caseReference.getApplicationMajorVersion();
				BigInteger existingTypeId = typeDAO.getId(qtn.getNamespace(), majorVersion, qtn.getName());
				if (existingTypeId == null)
				{
					// Type doesn't exist
					throw ReferenceException.newUnknownType(qtn.toString(), majorVersion);
				}
				else
				{
					// Type exists, but specific case doesn't
					throw ReferenceException.newNotExist(caseReference.toString());
				}
			}
		}
		return links;
	}

	public int deleteCaseLinks(CaseReference caseReference, List<CaseReference> targetCaseReferences, String name)
			throws PersistenceException, ReferenceException, InternalException, ArgumentException,
			NotAuthorisedException
	{
		CLFMethodContext clf = logCtx.getMethodContext("deleteCaseLinks");

		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("deleteCaseLinks");
		}

		QualifiedTypeName qtn = caseReference.getQualifiedTypeName();

		// Check type exists
		BigInteger typeId = typeDAO.getId(qtn.getNamespace(), caseReference.getApplicationMajorVersion(),
				qtn.getName());

		if (typeId == null)
		{
			throw ReferenceException.newUnknownType(qtn.toString(), caseReference.getApplicationMajorVersion());
		}

		// Check link exists
		LinkDTO link = null;
		QualifiedTypeName targetTypeQTN = null;
		int targetTypeMajorVersion = 0;
		Integer sourceEnd = null;

		if (name != null)
		{
			link = linkDAO.getLink(null, typeId, name);
			if (link == null)
			{
				throw ReferenceException.newLinkNameNotExist(name, qtn.toString(),
						caseReference.getApplicationMajorVersion());
			}

			if (name.equals(link.getEnd1Name()) && typeId.equals(link.getEnd1TypeId()))
			{
				targetTypeQTN = link.getEnd2TypeQTN();
				targetTypeMajorVersion = link.getEnd2TypeMajorVersion();
				sourceEnd = 1;

			}
			else if (name.equals(link.getEnd2Name()) && typeId.equals(link.getEnd2TypeId()))
			{
				targetTypeQTN = link.getEnd1TypeQTN();
				targetTypeMajorVersion = link.getEnd1TypeMajorVersion();
				sourceEnd = 2;
			}
			else
			{
				// Theoretically impossible
				throw InternalException.newInternalException("link mismatch");
			}
		}

		if (name == null && (targetCaseReferences != null && !targetCaseReferences.isEmpty()))
		{
			// Fail, as target case reference could be ambiguous if not qualified with a link name
			throw ArgumentException.newLinkRefsWithoutName();
		}

		// Check target ref(s) are of the right type
		if (targetCaseReferences != null)
		{
			for (CaseReference targetCaseReference : targetCaseReferences)
			{
				QualifiedTypeName targetQTN = targetCaseReference.getQualifiedTypeName();
				if (!targetQTN.equals(targetTypeQTN))
				{
					throw ArgumentException.newLinkBadType(targetCaseReference.toString(), name,
							targetTypeQTN.toString(), Integer.toString(targetTypeMajorVersion));
				}
			}
		}

		List<CaseLinkDTO> deletedDTOs = caseLinkDAO.delete(caseReference, targetCaseReferences, link, sourceEnd);

		for (CaseLinkDTO deletedDTO : deletedDTOs)
		{
			// Audit unlink
			clf.audit.audit(CDMAuditMessages.CDM_CASE_UNLINKED,
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, caseReference),
					clf.param(CommonMetaData.ROLE_NAME, deletedDTO.getName()),
					clf.param(CommonMetaData.CASE_REFERENCE, deletedDTO.getCaseReference()));

			// Audit unlink for target case
			clf.audit.audit(CDMAuditMessages.CDM_CASE_UNLINKED,
					clf.param(CommonMetaData.MANAGED_OBJECT_ID, deletedDTO.getCaseReference().toString()),
					clf.param(CommonMetaData.ROLE_NAME, deletedDTO.getOppositeName()),
					clf.param(CommonMetaData.CASE_REFERENCE, caseReference));
		}

		return deletedDTOs.size();
	}

    public void validate(QualifiedTypeName type, int majorVersion, String data,
            boolean strictTypeCheck) throws PersistenceException,
			InternalException, ReferenceException, ValidationException, NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("validate");
		}

		String namespace = type.getNamespace();
		String typeName = type.getName();

		// Look up type
		DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, true);
		DataModel dm = null;
		if (dataModelInfo == null)
		{
			throw ReferenceException.newUnknownNamespace(namespace, majorVersion);
		}
		dm = dataModelInfo.getDataModel();

		StructuredType structuredType = dm.getStructuredTypeByName(typeName);
		if (structuredType == null)
		{
			throw ReferenceException.newUnknownType(type.toString(), majorVersion);
		}
		ObjectNode objectNode = jsonStringToObjectNode(data);
        CasedataValidator.validate(structuredType, objectNode, false, true, false, false, false,
                strictTypeCheck);
	}

	public String processData(QualifiedTypeName type, int majorVersion, String data, boolean removeNulls)
			throws PersistenceException, InternalException, ReferenceException, ValidationException,
			NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("processData");
		}

		String namespace = type.getNamespace();
		String typeName = type.getName();

		// Look up type
		DataModelInfo dataModelInfo = dataModelDAO.read(namespace, majorVersion, true);
		DataModel dm = null;
		if (dataModelInfo == null)
		{
			throw ReferenceException.newUnknownNamespace(namespace, majorVersion);
		}
		dm = dataModelInfo.getDataModel();

		StructuredType structuredType = dm.getStructuredTypeByName(typeName);
		if (structuredType == null)
		{
			throw ReferenceException.newUnknownType(type.toString(), majorVersion);
		}
		ObjectNode objectNode = jsonStringToObjectNode(data);
		String result = data;
		CasedataValidator.validate(structuredType, objectNode, false, true, false, false, removeNulls);
		if (removeNulls)
		{
			// removeNulls is set, so the call to CasedataValidator.validate may have changed
			// the ObjectNode; Therefore, serialize it and return that rather than just
			// returning what we were given.
			try
			{
				result = om.writeValueAsString(objectNode);
			}
			catch (JsonProcessingException e)
			{
				// Shouldn't happen.
				throw InternalException.newInternalException(e);
			}
		}
		return result;
	}

	public boolean isActive(CaseReference caseReference) throws PersistenceException, ReferenceException,
			InternalException, ArgumentException, NotAuthorisedException
	{
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("isActive");
		}

		if (caseReference == null)
		{
			throw ReferenceException.newInvalidFormat(null);
		}

		Boolean isTerminal = caseDAO.getIsTerminalState(caseReference);
		if (isTerminal == null)
		{
			// Not found, but is that because the _type_ doesn't exist?
			QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
			if (qtn != null)
			{
				String namespace = qtn.getNamespace();
				String name = qtn.getName();
				int majorVersion = caseReference.getApplicationMajorVersion();
				BigInteger typeId = typeDAO.getId(namespace, majorVersion, name);
				if (typeId == null)
				{
					// The type doesn't exist, so we'll report that as the problem,
					// rather than misleading the caller by merely saying the case doesn't exist.
					throw ReferenceException.newUnknownType(qtn.toString(), majorVersion);
				}
			}
			throw ReferenceException.newNotExist(caseReference.toString());
		}
		// Invert result
		return !isTerminal;
	}

	public boolean isCaseApp(BigInteger caseId) {
		boolean isCaseApp=false;
		//boolean isCaseApp = applicationDAO.isCaseApp(cases.get(0).getCaseReference().getId());
		if(null==caseId) return false;
		try {
			isCaseApp = applicationDAO.isCaseApp(caseId);
		}catch(PersistenceException e) {
			e.printStackTrace();
		}
		
		return isCaseApp;
	}
	private String acquireCurrentUserGUID() throws ArgumentException
	{
		String result = null;
		RequestContext context = RequestContext.getCurrent();
		if (context != null)
		{
			CurrentUser currentUser = context.getCurrentUser();
			if (currentUser != null)
			{
				result = currentUser.getGuid();
			}
		}
		if (result == null)
		{
			// When called from REST, a RequestContext should always exist.  When invoked
			// from a process, Process Engine manufactures a RequestContext with the current
			// user GUID set to 'tibco-admin'.  However, there is nothing to stop other components
			// calling the BDS private service and failing to set a RequestContext.  This code
			// is to protect against that scenario.  Such badly behaved callers would immediately
			// see the error of their ways.
			throw ArgumentException.newNoCurrentUser();
		}
		else if (result.length() > MAX_USER_GUID_LENGTH)
		{
			// Reject a malformed GUID of >36 characters, or we'll be in trouble when
			// we attempt to store this in our 36 character database column.
			throw ArgumentException.newBadCurrentUserGUID(result);
		}
		return result;
	}
	
//	private  long[] getCaseIDs(int itemBatchSize)
//	{
//		long[] idArray = getCaseSequenceIDs(itemBatchSize, CASE_SEQ_CACHE_SIZE, identifierValueDAO);
//
//		// Now notify the daemon thread so that it can check if more IDs should be
//		// cached in preparation for the next call
//		daemonHandlerThread.addToSequenceUpdateQueue(itemBatchSize, CASE_SEQ_CACHE_SIZE, identifierValueDAO);
//
//		return idArray;
//	}
//	
//	/*
//	 * =====================================================
//	 * METHOD : getSequenceIDs
//	 * =====================================================
//	 */
//	/**
//	 * This method returns a list of sequence IDs
//	 *
//	 * @param itemBatchSize		The number of sequence IDs to return
//	 * @param itemCacheSize		The cache size for sequence IDs
//	 * @param sequenceDAO 		DAO for sequences
//	 * @return sequenceIDs		Array of the next sequence IDs
//	 */
//	private long[] getCaseSequenceIDs( int itemBatchSize, int itemCacheSize, IdentifierValueDAO identifierValueDAO)
//	{
//		long sequenceIds[] = new long[itemBatchSize];
//		
//		/*
//		 * Get the start of the batch of sequence IDs in a single call.
//		 */
//		long startBatchIDs = getStartSequenceID (itemBatchSize, itemCacheSize, identifierValueDAO, true);
//		
//		for (int i = 0; i < itemBatchSize; i++)
//		{
//			sequenceIds[i] = startBatchIDs++;
//		}
//		
//		return sequenceIds;
//	}
//	
//	/**
//	 * This method returns the ID from where it is safe to use the
//	 * requested batch size of IDs.  For example, passing a batch 
//	 * size of 20 will result in an ID be returned from which point
//	 * 20 IDs can be safely used.
//	 * 
//	 * If there isn't enough in the cache for the requested batch size
//	 * it will throw away what's left in the cache and go to the DB
//	 * to get a set of IDs based on the larger of the batch and cache
//	 * sizes.
//	 *
//	 * @param itemBatchSize		size of batched sequence IDs
//	 * @param itemCacheSize 	size of sequence cache
//	 * @param sequenceDAO		DAO for sequences
//	 * @param bReturnID			Whether we need to return an ID
//	 * @return sequenceID	the next available sequence ID
//	 */
//	private synchronized long getStartSequenceID(int itemBatchSize, int itemCacheSize, SequenceDAO sequenceDAO, boolean bReturnID)
//	{
//		long	retID = -1;
//
//		/*
//		 * If there are no more sequence IDs left in the cache, or there
//		 * aren't enough to satisfy the batch size requested, go to the DB and
//		 * get a new set of IDs. 
//		 * 
//		 * This does mean that any IDs left in the cache will be lost.
//		 */
//		if ((numSequenceIDs <= 0) || (numSequenceIDs < itemBatchSize))
//		{
//			int size = (itemBatchSize > itemCacheSize) ? itemBatchSize : itemCacheSize;
//
//			/*
//			 * get the next ID to use from the database
//			 */
//			nextSequenceID = sequenceDAO.cacheID(sequenceType, size);
//			
//			/*
//			 *  now that more IDs have been allocated, add them to the count of available IDs
//			 */
//			numSequenceIDs = size;
//	}
//
//	/*
//		 * Only return an ID if requested to do so.  This maybe because a thread has
//		 * called this method just to ensure that there are sufficient IDs in the cache
//		 * for another batch ID API call.
//		 */
//		if (bReturnID)
//		{
//			retID = nextSequenceID;
//			
//			/*
//			 * Use up the number of sequences requested by the batch size.
//			 */
//			numSequenceIDs -= itemBatchSize;
//			nextSequenceID += itemBatchSize;
//		}
//		
//		return (retID);
//	}
	
}
