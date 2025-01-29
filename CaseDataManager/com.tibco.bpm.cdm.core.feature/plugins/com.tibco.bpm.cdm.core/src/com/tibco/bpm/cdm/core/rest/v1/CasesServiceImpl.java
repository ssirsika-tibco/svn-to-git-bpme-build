package com.tibco.bpm.cdm.core.rest.v1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.NotAuthorisedException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.api.rest.v1.api.CasesService;
import com.tibco.bpm.cdm.api.rest.v1.model.CaseInfo;
import com.tibco.bpm.cdm.api.rest.v1.model.CaseMetadata;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesGetResponseBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPostRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPostResponseBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRefRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBody;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBodyItem;
import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutResponseBody;
import com.tibco.bpm.cdm.api.rest.v1.model.Link;
import com.tibco.bpm.cdm.api.rest.v1.model.LinksPostRequestBody;
import com.tibco.bpm.cdm.core.AbstractService;
import com.tibco.bpm.cdm.core.CaseManager;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.core.dto.DTOTransmuter;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.rest.RESTRequestException;
import com.tibco.bpm.cdm.util.TimestampOp;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.auth.SystemActionId;

/**
 * Implementation of the CasesService interface for the .../cases REST resource
 * @author smorgan
 * @since 2019
 */
public class CasesServiceImpl extends AbstractService implements CasesService
{
	static CLFClassContext								logCtx							= CloudLoggingFramework
			.init(CasesServiceImpl.class, CDMLoggingInfo.instance);

	// Formatter for rendering creation/modification stamps to String.
	// SimpleDateFormat is not thread-safe, but we don't want to create a new one
	// each time we use it, so this essentially gives us a per-thread singleton.
	private static final ThreadLocal<SimpleDateFormat>	FORMATTER						= ThreadLocal
			.withInitial(() -> {
																									SimpleDateFormat result = new SimpleDateFormat(
																											"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
																									result.setTimeZone(
																											TimeZone.getTimeZone(
																													"UTC"));
																									return (result);
																								});

	// Allowed in $filter for GET /cases
	private static final List<String>					FILTER_PROPS_FOR_GET			= Arrays.asList("caseType",
			"applicationMajorVersion", "caseReference-in", "cid", "caseState", "modificationTimestamp",
			"isInTerminalState");

	// Allowed in $filter for DELETE /cases
	private static final List<String>					FILTER_PROPS_FOR_DELETE			= Arrays.asList("caseType",
			"applicationMajorVersion", "caseState", "modificationTimestamp", "isReferencedByProcess");

	// Allowed in $filter for GET /cases/{ref}/links
	private static final List<String>					FILTER_PROPS_FOR_GET_LINKS		= Arrays.asList("name");

	// Allowed in $filter for DELETE /cases/{ref}/links
	private static final List<String>					FILTER_PROPS_FOR_DELETE_LINKS	= Arrays
			.asList("targetCaseReference", "targetCaseReference-in", "name");

	private CaseManager									caseManager;

	public void setCaseManager(CaseManager caseManager)
	{
		this.caseManager = caseManager;
	}

	@Override
	public Response casesGet(String filter, String search, String dql, String select, Integer skip, Integer top,
			Boolean count) throws Exception
	{
		// TODO support $count
		CLFMethodContext clf = logCtx.getMethodContext("casesGet");
		
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.readCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("getTypes");
		}

		try
		{
			CaseAspectSelection aspectSelection = CaseAspectSelection.fromSelectExpression(select);

			FilterParser parser = new FilterParser(filter);

			// Check that only valid expressions were given (i.e. none that were
			// either not recognised by the parser or were recognised, but are
			// not appropriate for this API).
			parser.validate(FILTER_PROPS_FOR_GET);

			String stateValue = parser.getStateValue();

			// Validate $skip
			if (skip != null && skip < 0)
			{
				throw ArgumentException.newSkipInvalid(skip);
			}

			// Validate modification timestamp
			Calendar modificationTimestamp = parser.getModificationTimestamp();
			TimestampOp	timestampOperator = parser.getTimestampOperator();
			if (modificationTimestamp == null)
			{
				// If the string form is non-null, that indicates a parsing problem
				String modificationTimestampString = parser.getModificationTimestampString();
				if (modificationTimestampString != null)
				{
					throw ArgumentException.newBadModificationTimestamp(modificationTimestampString);
				}

			}

			List<CaseReference> caseReferences = parser.getCaseReferences();
			if (caseReferences == null)
			{
				String caseReferencesString = parser.getCaseReferencesString();
				if (caseReferencesString != null)
				{
					throw RESTRequestException.newBadCaseReferences(caseReferencesString);
				}
			}

			String cid = parser.getCID();

			// Get isInTerminalState from $filter
			String isInTerminalStateString = parser.getIsInTerminalState();
			Boolean isInTerminalState = null;
			if (isInTerminalStateString != null) {
				if (parser.isIsInTerminalStateValid())
				{
					isInTerminalState = parser.getIsInTerminalStateAsBoolean();
				}
				else
				{
					throw RESTRequestException.newBadIsInTerminalState(isInTerminalStateString);
				}
			}

			// There are two ways of calling this API: Either caseReferences in(...), or a bunch
			// of other filter parameters. They can't be combined.
			List<CaseInfoDTO> dtos = null;

			if (caseReferences != null)
			{
				// Prevent other parameters (caseReferences in(...) must be used alone)
				if (top != null || skip != null || count != null || search != null || dql != null
						|| parser.getAllExpressions().size() != 1)
				{
					throw RESTRequestException.newCaseReferencePreventsOthers();
				}
				dtos = caseManager.readCases(caseReferences, aspectSelection);
			}
			else
			{
				// Validate $top
				if (top == null)
				{
					throw ArgumentException.newTopMandatory();
				}
				else if (top < 0)
				{
					throw ArgumentException.newTopInvalid(top);
				}

				String caseType = parser.getCaseType();
				QualifiedTypeName qType;
				if (caseType == null)
				{
					throw ArgumentException.newCaseTypeMandatory();
				}
				try
				{
					qType = new QualifiedTypeName(caseType);
				}
				catch (IllegalArgumentException e)
				{
					throw ArgumentException.newBadCaseType(caseType, e);
				}

				String majorVersionInFilter = parser.getApplicationMajorVersion();
				int majorVersion = parseMajorVersion(majorVersionInFilter);

				dtos = caseManager.readCases(qType, majorVersion, skip, top, cid, stateValue, modificationTimestamp,
                        timestampOperator,
                        search,
                        dql,
                        aspectSelection,
                        (Boolean.FALSE.equals(isInTerminalState)));
			}

			List<CaseInfo> infos = toCaseInfos(dtos, aspectSelection);
			CasesGetResponseBody responseBody = new CasesGetResponseBody();
			responseBody.addAll(infos);
			return Response.ok(responseBody).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	private int parseMajorVersion(String majorVersionString) throws ArgumentException
	{
		if (majorVersionString == null)
		{
			throw ArgumentException.newBadMajorVersion(majorVersionString);
		}
		int majorVersion;
		try
		{
			majorVersion = Integer.parseInt(majorVersionString);
		}
		catch (NumberFormatException e)
		{
			throw ArgumentException.newBadMajorVersion(majorVersionString);
		}
		if (majorVersion < 0)
		{
			throw ArgumentException.newBadMajorVersion(majorVersionString);
		}
		return majorVersion;
	}

	private CaseInfo toCaseInfo(CaseInfoDTO dto, CaseAspectSelection aspectSelection) throws ReferenceException
	{
		boolean includeCaseReference = aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_CASE_REFERENCE);
		boolean includeCasedata = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_CASEDATA);
		boolean includeSummary = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY);
		boolean includeMetadata = aspectSelection
				.includesOrIncludesSubAspectsOfOrIsNothing(CaseAspectSelection.ASPECT_METADATA);

		CaseInfo caseInfo = new CaseInfo();
		if (includeCaseReference)
		{
			CaseReference ref = new CaseReference(dto.getTypeName(), dto.getMajorVersion(), dto.getId(),
					dto.getVersion());
			caseInfo.setCaseReference(ref.toString());
		}
		if (includeCasedata)
		{
			caseInfo.setCasedata(dto.getCasedata());
		}
		if (includeSummary)
		{
			caseInfo.setSummary(dto.getSummary());
		}
		if (includeMetadata)
		{
			CaseMetadata metadata = new CaseMetadata();
			metadata.setCreatedBy(dto.getCreatedBy());
			metadata.setCreationTimestamp(FORMATTER.get().format(dto.getCreationTimestamp().getTime()));
			metadata.setModifiedBy(dto.getModifiedBy());
			metadata.setModificationTimestamp(FORMATTER.get().format(dto.getModificationTimestamp().getTime()));
			caseInfo.setMetadata(metadata);
		}
		return caseInfo;
	}

	private List<CaseInfo> toCaseInfos(List<CaseInfoDTO> dtos, CaseAspectSelection aspectSelection) throws ReferenceException
	{
		boolean includeCaseReference = aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_CASE_REFERENCE);
		boolean includeCasedata = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_CASEDATA);
		boolean includeSummary = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_SUMMARY);
		boolean includeMetadata = aspectSelection
				.includesOrIncludesSubAspectsOfOrIsNothing(CaseAspectSelection.ASPECT_METADATA);

		List<CaseInfo> infos = new ArrayList<>();
		for (CaseInfoDTO dto : dtos)
		{
			CaseInfo caseInfo = new CaseInfo();
			if (includeCaseReference)
			{
				CaseReference ref = new CaseReference(dto.getTypeName(), dto.getMajorVersion(), dto.getId(),
						dto.getVersion());
				caseInfo.setCaseReference(ref.toString());
			}
			if (includeCasedata)
			{
				caseInfo.setCasedata(dto.getCasedata());
			}
			if (includeSummary)
			{
				caseInfo.setSummary(dto.getSummary());
			}
			if (includeMetadata)
			{
				CaseMetadata metadata = new CaseMetadata();
				metadata.setCreatedBy(dto.getCreatedBy());
				metadata.setCreationTimestamp(FORMATTER.get().format(dto.getCreationTimestamp().getTime()));
				metadata.setModifiedBy(dto.getModifiedBy());
				metadata.setModificationTimestamp(FORMATTER.get().format(dto.getModificationTimestamp().getTime()));
				caseInfo.setMetadata(metadata);
			}
			infos.add(caseInfo);
		}
		return infos;
	}

	@Override
	public Response casesPut(CasesPutRequestBody body) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesPut");
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("createUpdateCase");
		}
		try
		{
			List<CaseUpdateDTO> cases = new ArrayList<>();
			int size = body.size();
			for (int i = 0; i < size; i++)
			{
				CasesPutRequestBodyItem item = body.get(i);
				CaseReference caseReference = new CaseReference(item.getCaseReference());
				cases.add(new CaseUpdateDTO(caseReference, item.getCasedata()));
			}

			//Dont't allow put operations if it is a case app
			dontAllowPutForCaseApp(cases);
			
			caseManager.updateCases(cases);
			List<String> newRefs = cases.stream().map(CaseUpdateDTO::getNewCaseReference).map(CaseReference::toString)
					.collect(Collectors.toList());

			CasesPutResponseBody responseBody = new CasesPutResponseBody();
			responseBody.addAll(newRefs);
			return Response.ok(responseBody).build();
		}
		catch (ReferenceException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
			
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
			
		}
		
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.api.rest.v1.api.CasesService#casesPost(com.tibco.bpm.cdm.api.rest.v1.model.CasesPostRequestBody)
	 */
	@Override
	public Response casesPost(CasesPostRequestBody body) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesPost");
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.createUpdateCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("createUpdateCase");
		}
		try
		{
			// Get and validate majorVersion request property
			Integer majorVersion = body.getApplicationMajorVersion();
			if (majorVersion == null || majorVersion < 1)
			{
				throw RESTRequestException.newInvalidRequestProperty("majorVersion",
						majorVersion == null ? null : majorVersion.toString());
			}

			// Get and validate caseType request property
			String caseType = body.getCaseType();
			QualifiedTypeName qType;
			try
			{
				qType = new QualifiedTypeName(caseType);
			}
			catch (IllegalArgumentException e)
			{
				throw RESTRequestException.newInvalidRequestProperty("caseType", caseType);
			}

			// Get and validate casedata request property		
			List<String> casedata = body.getCasedata();
			if (casedata == null || casedata.isEmpty())
			{
				throw RESTRequestException.newInvalidRequestProperty("casedata",
						casedata == null ? "null" : casedata.toString());
			}

			// Call CaseManager to do the work
			List<CaseReference> refs = caseManager.createCases(qType, majorVersion, casedata);

			// Prepare response
			CasesPostResponseBody responseBody = new CasesPostResponseBody();
			for (CaseReference ref : refs)
			{
				responseBody.add(ref.toString());
			}

			return Response.ok(responseBody).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesDelete(String filter) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesDelete");
		boolean isActionAuthorised = isActionAuthorised(SystemActionId.deleteCase);

		if (!isActionAuthorised)
		{
			throw NotAuthorisedException.newNotAuthorisedException("deleteCase");
		}

		try
		{

			FilterParser parser = new FilterParser(filter);

			// Check that only valid expressions were given (i.e. none that were
			// either not recognised by the parser or were recognised, but are
			// not appropriate for this API).
			parser.validate(FILTER_PROPS_FOR_DELETE);

			String caseType = parser.getCaseType();
			if (caseType == null)
			{
				throw ArgumentException.newCaseTypeMandatory();
			}
			QualifiedTypeName qType = new QualifiedTypeName(caseType);

			String majorVersionInFilter = parser.getApplicationMajorVersion();
			int majorVersion = parseMajorVersion(majorVersionInFilter);

			String stateValue = parser.getStateValue();

			// Validate modification timestamp
			Calendar modificationTimestamp = parser.getModificationTimestamp();
			if (modificationTimestamp == null)
			{
				// If the string form is non-null, that indicates a parsing problem
				String modificationTimestampString = parser.getModificationTimestampString();
				if (modificationTimestampString != null)
				{
					throw ArgumentException.newBadModificationTimestamp(modificationTimestampString);
				}
			}

            String isReferencedByProcessValue =
                    parser.getIsReferencedByProcess();
            Boolean isReferencedByProcess = true;
            if (isReferencedByProcessValue != null) {
                isReferencedByProcess = new Boolean(isReferencedByProcessValue);
            }
			int deletionCount = caseManager.deleteCases(qType, majorVersion, stateValue, modificationTimestamp,
					isReferencedByProcess);

			// See comment on REST Guidelines where having DELETE return a body was approved by Nathan:
			// http://confluence.tibco.com/display/BPM/Ariel+REST+Service+Guidelines?focusedCommentId=173273737#comment-173273737
			return Response.ok(deletionCount).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferenceGet(String caseReference, String select) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferenceGet");
		try
		{
			CaseReference ref = new CaseReference(caseReference);
			CaseAspectSelection aspectSelection = CaseAspectSelection.fromSelectExpression(select);
			CaseInfoDTO dto = caseManager.readCase(ref, aspectSelection, false);
			CaseInfo info = toCaseInfo(dto, aspectSelection);
			return Response.ok(info).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferencePut(String caseReference, CasesPutRefRequestBody body) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferencePut");
		try
		{
			List<CaseUpdateDTO> cases = Collections
					.singletonList(new CaseUpdateDTO(new CaseReference(caseReference), body.getCasedata()));
			
			//Dont't allow put operations if it is a case app
			dontAllowPutForCaseApp(cases);
			
			caseManager.updateCases(cases);
			CasesPutResponseBody responseBody = new CasesPutResponseBody();
			responseBody.add(cases.get(0).getNewCaseReference().toString());
			return Response.ok(responseBody).build();
		}
		catch (ReferenceException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
			
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferenceDelete(String caseReference) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferenceDelete");
		try
		{
			CaseReference ref = new CaseReference(caseReference);
			caseManager.deleteCase(ref);
			return Response.ok(1).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferenceLinksGet(String caseReference, String filter, String dql, Integer skip,
			Integer top) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferenceLinksGet");
		try
		{
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

			FilterParser parser = new FilterParser(filter);
			parser.validate(FILTER_PROPS_FOR_GET_LINKS);
			CaseReference ref = new CaseReference(caseReference);
			List<CaseLinkDTO> dtos = caseManager.getCaseLinks(ref, parser.getName(), skip, top, null, dql);
			List<Link> links = DTOTransmuter.toLinks(dtos);
			return Response.ok(links).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferenceLinksPost(String caseReferenceString, LinksPostRequestBody body) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferenceLinksPost");
		try
		{
			CaseReference caseReference = new CaseReference(caseReferenceString);
			List<CaseLinkDTO> caseLinkDTOs = DTOTransmuter.toCaseLinkDTOs(body);
			caseManager.createCaseLinks(caseReference, caseLinkDTOs);
			return Response.ok().build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	@Override
	public Response casesCaseReferenceLinksDelete(String caseReference, String filter) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("casesCaseReferenceLinksDelete");
		try
		{
			CaseReference ref = new CaseReference(caseReference);
			FilterParser parser = new FilterParser(filter);
			parser.validate(FILTER_PROPS_FOR_DELETE_LINKS);

			List<CaseReference> targetRefs = new ArrayList<>();

			List<CaseReference> targetCaseReferences = parser.getTargetCaseReferences();
			if (targetCaseReferences == null)
			{
				String targetCaseReferencesString = parser.getTargetCaseReferencesString();
				if (targetCaseReferencesString != null)
				{
					throw RESTRequestException.newBadTargetCaseReferences(targetCaseReferencesString);
				}
			}
			else
			{
                Map<CaseReference, Long> countMap =
                        targetCaseReferences.stream()
                                .collect(Collectors.toMap(Function.identity(),
                                        v -> 1L,
                                        Long::sum));
                Iterator<CaseReference> itr = countMap.keySet().iterator();
                boolean duplicateFound = false;
                List<String> duplicateCaseref = new ArrayList<String>();
                while (itr.hasNext()) {
                    CaseReference key = itr.next();
                    if (countMap.get(key) > 1) {
                        duplicateFound = true;
                        duplicateCaseref.add(key.toString());
                    }
                }
                if (duplicateFound) {
                    throw RESTRequestException
                            .newBadTagetCaseReferencesWithDuplicates(
                                    duplicateCaseref.toString());
                }

				targetRefs.addAll(targetCaseReferences);
			}

			CaseReference targetCaseReference = parser.getTargetCaseReference();
			if (targetCaseReference == null)
			{
				String targetCaseReferenceString = parser.getTargetCaseReferenceString();
				if (targetCaseReferenceString != null)
				{
					throw ReferenceException.newInvalidFormat(targetCaseReferenceString);
				}
			}

			if (targetCaseReference != null)
			{
				if (!targetRefs.isEmpty())
				{
					throw RESTRequestException.newBadDeleteLinksFilter(filter);
				}
				targetRefs.add(targetCaseReference);
			}

			String name = parser.getName();

			int numDeleted = caseManager.deleteCaseLinks(ref, targetRefs, name);

			return Response.ok(numDeleted).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	public void dontAllowPutForCaseApp(List<CaseUpdateDTO> cases) throws ReferenceException {
		//if it is a case app then we dont allow put
		boolean isCaseApp=false;
		if(null!=cases.get(0) && null!= cases.get(0).getCaseReference()){
			isCaseApp=caseManager.isCaseApp(cases.get(0).getCaseReference().getId());
}
		
		if (isCaseApp)
		{
			throw ReferenceException.newNotCaseApp(cases.get(0).getCaseReference().getQualifiedTypeName().toString());
		}
	}

}
