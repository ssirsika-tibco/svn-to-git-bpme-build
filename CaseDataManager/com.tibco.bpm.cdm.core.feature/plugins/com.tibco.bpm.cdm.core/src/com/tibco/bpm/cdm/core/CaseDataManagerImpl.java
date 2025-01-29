package com.tibco.bpm.cdm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.CaseDataManager;
import com.tibco.bpm.cdm.api.dto.CaseInfo;
import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.api.exception.ValidationException;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class CaseDataManagerImpl extends AbstractService implements CaseDataManager
{
	static CLFClassContext						logCtx				= CloudLoggingFramework
			.init(CaseDataManagerImpl.class, CDMLoggingInfo.instance);

	private static final CaseAspectSelection	REF					= CaseAspectSelection
			.fromAspects(CaseAspectSelection.ASPECT_CASE_REFERENCE);

	private static final CaseAspectSelection	REF_AND_CASEDATA	= CaseAspectSelection
			.fromAspects(CaseAspectSelection.ASPECT_CASE_REFERENCE, CaseAspectSelection.ASPECT_CASEDATA);

	private CaseManager							caseManager;

	public void setCaseManager(CaseManager caseManager)
	{
		this.caseManager = caseManager;
	}

	private void throwException(CDMException e) throws ArgumentException, InternalException, ValidationException
	{
		if (e instanceof ArgumentException)
		{
			throw (ArgumentException) e;
		}
		else if (e instanceof InternalException)
		{
			throw (InternalException) e;
		}
		else if (e instanceof ValidationException)
		{
			throw (ValidationException) e;
		}
		else
		{
			// Shouldn't happen
			throw InternalException.newInternalException(e);
		}
	}

	private void throwArgumentOrInternalException(CDMException e) throws ArgumentException, InternalException
	{
		if (e instanceof ArgumentException)
		{
			throw (ArgumentException) e;
		}
		else if (e instanceof InternalException)
		{
			throw (InternalException) e;
		}
	}

	@Override
	public CaseReference createCase(QualifiedTypeName type, int majorVersion, String casedata)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("createCase");
		clf.local.trace("enter");
		CaseReference result = null;
		try
		{
			List<CaseReference> refs = caseManager.createCases(type, majorVersion, Collections.singletonList(casedata));
			result = refs != null && !refs.isEmpty() ? refs.get(0) : null;
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
		return result;
	}

	@Override
	public List<CaseReference> createCases(QualifiedTypeName type, int majorVersion, List<String> casedata)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("createCases");
		clf.local.trace("enter");
		List<CaseReference> result = null;
		try
		{
			result = caseManager.createCases(type, majorVersion, casedata);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.api.CaseDataManager#readCase(java.lang.String)
	 */
	@Override
	public CaseInfo readCase(CaseReference ref) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("readCase");
		clf.local.trace("enter");
		CaseInfo result = null;
		try
		{
			CaseInfoDTO info = caseManager.readCase(ref, REF_AND_CASEDATA, false);
			result = CaseInfo.makeWithCasedata(info.getId(), info.getTypeName(), info.getMajorVersion(),
					info.getVersion(), info.getCasedata());
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.api.CaseDataManager#readCase(java.lang.String, int, java.lang.String)
	 */
	@Override
	public CaseInfo readCase(QualifiedTypeName type, int majorVersion, String caseIdentifier)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("readCase");
		clf.local.trace("enter");
		CaseInfo result = null;
		try
		{
			// Can return at most one object (given CIDs are unique per type)
			List<CaseInfoDTO> readCases = caseManager.readCases(type, majorVersion, null, 1, caseIdentifier, null, null,
					null, null, null, REF_AND_CASEDATA, true);
			if (readCases != null && readCases.size() == 1)
			{
				CaseInfoDTO info = readCases.get(0);
				result = CaseInfo.makeWithCasedata(info.getId(), info.getTypeName(), info.getMajorVersion(),
						info.getVersion(), info.getCasedata());
			}
			else
			{
				throw ReferenceException.newCIDNotExist(caseIdentifier);
			}
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.api.CaseDataManager#readCases(java.util.List)
	 */
	@Override
	public List<CaseInfo> readCases(List<CaseReference> refs) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("readCases");
		clf.local.trace("enter");
		List<CaseInfo> infos = new ArrayList<>();
		try
		{
			List<CaseInfoDTO> dtos = caseManager.readCases(refs, REF_AND_CASEDATA);
			for (CaseInfoDTO dto : dtos)
			{
				infos.add(CaseInfo.makeWithCasedata(dto.getId(), dto.getTypeName(), dto.getMajorVersion(),
						dto.getVersion(), dto.getCasedata()));
			}
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return infos;
	}

	@Override
	public List<CaseInfo> readCases(QualifiedTypeName type, int majorVersion, Integer skip, Integer top, String search,
			String dql) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("readCases");
		clf.local.trace("enter");
		List<CaseInfo> infos = new ArrayList<>();
		try
		{
			List<CaseInfoDTO> dtos = caseManager.readCases(type, majorVersion, skip, top, null, null, null, null, search, dql,
					REF_AND_CASEDATA, true);
			for (CaseInfoDTO dto : dtos)
			{
				infos.add(CaseInfo.makeWithCasedata(dto.getId(), dto.getTypeName(), dto.getMajorVersion(),
						dto.getVersion(), dto.getCasedata()));
			}

		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return infos;
	}

	@Override
	public CaseReference updateCase(CaseReference ref, String casedata)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("updateCase");
		clf.local.trace("enter");
		CaseReference newRef = null;
		try
		{
			CaseUpdateDTO dto = new CaseUpdateDTO(ref, casedata);
			caseManager.updateCases(Collections.singletonList(dto));
			// Case reference will be updated to contain the new version number.
			newRef = dto.getNewCaseReference();
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
		return newRef;
	}

	@Override
	public List<CaseReference> updateCases(List<CaseReference> refs, List<String> casedataList)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("updateCases");
		clf.local.trace("enter");
		List<CaseReference> result = new ArrayList<>();
		try
		{
			if (refs == null || refs.isEmpty())
			{
				throw com.tibco.bpm.cdm.api.exception.ArgumentException.newRefListInvalid();
			}
			if (casedataList == null || casedataList.isEmpty())
			{
				throw com.tibco.bpm.cdm.api.exception.ArgumentException.newCasedataListInvalid();
			}
			if (refs.size() != casedataList.size())
			{
				throw com.tibco.bpm.cdm.api.exception.ArgumentException.newRefAndCasedataListsSizeMismatch();
			}
			List<CaseUpdateDTO> dtos = new ArrayList<>();
			for (int i = 0; i < refs.size(); i++)
			{
				CaseReference ref = refs.get(i);
				String casedata = casedataList.get(i);
				dtos.add(new CaseUpdateDTO(ref, casedata));
			}
			caseManager.updateCases(dtos);
			dtos.forEach(dto -> result.add(dto.getNewCaseReference()));
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return result;
	}

	@Override
	public void deleteCase(CaseReference ref) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("deleteCase");
		clf.local.trace("enter");
		try
		{
			caseManager.deleteCase(ref);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void linkCase(CaseReference ref, String linkName, CaseReference targetRef)
			throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("linkCase");
		clf.local.trace("enter");
		try
		{
			CaseLinkDTO dto = new CaseLinkDTO(linkName, targetRef);
			caseManager.createCaseLinks(ref, Collections.singletonList(dto));
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void linkCases(CaseReference ref, String linkName, List<CaseReference> targetRefs)
			throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("linkCases");
		clf.local.trace("enter");
		try
		{
			List<CaseLinkDTO> dtos = new ArrayList<>();
			for (CaseReference targetRef : targetRefs)
			{
				CaseLinkDTO dto = new CaseLinkDTO(linkName, targetRef);
				dtos.add(dto);
			}
			caseManager.createCaseLinks(ref, dtos);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void unlinkCase(CaseReference ref, String linkName, CaseReference targetRef)
			throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("unlinkCase");
		clf.local.trace("enter");
		try
		{
			caseManager.deleteCaseLinks(ref, Collections.singletonList(targetRef), linkName);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void unlinkCases(CaseReference ref, String linkName, List<CaseReference> targetRefs)
			throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("unlinkCases");
		clf.local.trace("enter");
		try
		{
			caseManager.deleteCaseLinks(ref, targetRefs, linkName);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void unlinkCases(CaseReference ref, String linkName) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("unlinkCases");
		clf.local.trace("enter");
		try
		{
			caseManager.deleteCaseLinks(ref, null, linkName);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
	}

	@Override
	public void validate(QualifiedTypeName type, int majorVersion, String data)
			throws ArgumentException, ValidationException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("validate");
		clf.local.trace("enter");
		try
		{
            caseManager.validate(type, majorVersion, data, true);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
	}

    @Override
    public void validate(QualifiedTypeName type, int majorVersion, String data, boolean strictTypeCheck)
            throws ArgumentException, ValidationException, InternalException {
        CLFMethodContext clf =
                logCtx.getMethodContext("validate");
        clf.local.trace("enter");
        try {
            caseManager.validate(type, majorVersion, data, strictTypeCheck);
        } catch (CDMException e) {
            logException(clf, e);
            throwException(e);
        }
    }

	@Override
	public String processData(QualifiedTypeName type, int majorVersion, String data, boolean removeNulls)
			throws ArgumentException, InternalException, ValidationException
	{
		CLFMethodContext clf = logCtx.getMethodContext("processData");
		clf.local.trace("enter");
		String result = null;
		try
		{
			result = caseManager.processData(type, majorVersion, data, removeNulls);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwException(e);
		}
		return result;
	}

	@Override
	public boolean exists(CaseReference ref) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("exists");
		clf.local.trace("enter");
		boolean result = false;
		try
		{
			CaseInfoDTO info = caseManager.readCase(ref, REF, true);
			result = info != null;
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return result;
	}

	@Override
	public boolean exists(String ref) throws ArgumentException, InternalException
	{
		return exists(new CaseReference(ref));
	}

	@Override
	public boolean isActive(CaseReference ref) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("isActive");
		clf.local.trace("enter");
		Boolean result = false;
		try
		{
			result = caseManager.isActive(ref);
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return result;
	}

	@Override
	public List<CaseReference> navigateLinks(CaseReference ref, String linkName, Integer skip, Integer top,
			String search, String dql) throws ArgumentException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("navigateLinks");
		clf.local.trace("enter");
		List<CaseReference> result = null;
		try
		{
			List<CaseLinkDTO> dtos = caseManager.getCaseLinks(ref, linkName, skip, top, search, dql);
			result = dtos.stream().map(CaseLinkDTO::getCaseReference).collect(Collectors.toList());
		}
		catch (CDMException e)
		{
			logException(clf, e);
			throwArgumentOrInternalException(e);
		}
		return result;

	}
}
