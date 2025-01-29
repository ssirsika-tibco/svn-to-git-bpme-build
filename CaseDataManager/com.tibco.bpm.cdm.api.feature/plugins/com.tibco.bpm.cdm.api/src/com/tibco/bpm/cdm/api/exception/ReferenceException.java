package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates a problem with a reference, whether that be a fully formed case reference, or a component
 * part, such as namespace or major version, or related concepts such as type names, fully-qualified 
 * names and other model references.
 * @author smorgan
 * @since 2019
 */
public class ReferenceException extends ArgumentException
{
	private static final long serialVersionUID = 1L;

	protected ReferenceException(ErrorData errorData)
	{
		super(errorData);
	}

	protected ReferenceException(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	protected ReferenceException(CDMErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	public static ReferenceException newUnknownNamespace(String namespace, int majorVersion)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_UNKNOWN_NAMESPACE,
				new String[]{"namespace", namespace, "majorVersion", Integer.toString(majorVersion)});
	}

	public static ReferenceException newUnknownType(String type, int majorVersion)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_UNKNOWN_TYPE,
				new String[]{"type", type, "majorVersion", Integer.toString(majorVersion)});
	}

	public static ReferenceException newNotCaseType(String type, int majorVersion)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_NOT_CASE_TYPE, new String[]{"type", type});
	}

	public static ReferenceException newInvalidId(String caseReference, String id, Throwable cause)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_INVALID_ID, cause,
				new String[]{"caseReference", caseReference, "id", id});
	}

	public static ReferenceException newInvalidVersion(String caseReference, String version, Throwable cause)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_INVALID_VERSION, cause,
				new String[]{"caseReference", caseReference, "version", version});
	}

	public static ReferenceException newInvalidMajorVersion(String caseReference, String majorVersion, Throwable cause)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_INVALID_MAJOR_VERSION, cause,
				new String[]{"caseReference", caseReference, "majorVersion", majorVersion});
	}

	public static ReferenceException newInvalidType(String type, Throwable cause)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_INVALID_TYPE, cause, new String[]{"type", type});
	}

	public static ReferenceException newInvalidFormat(String caseReference)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_INVALID_FORMAT,
				new String[]{"caseReference", caseReference});
	}

	public static ReferenceException newNotExist(String caseReference)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_NOT_EXIST,
				new String[]{"caseReference", caseReference});
	}

	public static ReferenceException newCIDNotExist(String caseIdentifier)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_CID_NOT_EXIST,
				new String[]{"caseIdentifier", caseIdentifier});
	}

	public static ReferenceException newLinkBadType(String caseReference, String targetCaseReference, String linkName)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_WRONG_TYPE, new String[]{"caseReference",
				caseReference, "targetCaseReference", targetCaseReference, "linkName", linkName});
	}

	public static ReferenceException newLinkNameNotExist(String linkName, String typeName, int majorVersion)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_NAME_NOT_EXIST, new String[]{"linkName",
				linkName, "typeName", typeName, "majorVersion", Integer.toString(majorVersion)});
	}

	public static ReferenceException newAlreadyLinked(String caseReference, String targetCaseReference, String linkName)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_ALREADY_LINKED, new String[]{"caseReference",
				caseReference, "targetCaseReference", targetCaseReference, "linkName", linkName});
	}

	public static ReferenceException newLinkIsNotArray(String typeName, int majorVersion, String linkName, int size)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_NOT_ARRAY, new String[]{"linkName", linkName,
				"typeName", typeName, "majorVersion", Integer.toString(majorVersion), "size", Integer.toString(size)});
	}

	public static ReferenceException newLinkOppositeIsNotArray(String typeName, int majorVersion, String linkName,
			String oppositeLinkName)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_OPPOSITE_NOT_ARRAY,
				new String[]{"linkName", linkName, "oppositeLinkName", oppositeLinkName, "typeName", typeName,
						"majorVersion", Integer.toString(majorVersion)});
	}

	public static ReferenceException newLinkNotLinked(String caseReference, String targetCaseReference, String linkName)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_LINK_NOT_LINKED, new String[]{"caseReference",
				caseReference, "targetCaseReference", targetCaseReference, "linkName", linkName});
	}

	public static ReferenceException newDuplicateLink(String linkName, String caseReference, String targetCaseReference)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_DUPLICATE_LINK_TARGET, new String[]{"linkName",
				linkName, "caseReference", caseReference, "targetCaseReference", targetCaseReference});
	}

	public static ReferenceException newTerminalStatePreventsUpdate(String caseReference)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_TERMINAL_STATE_PREVENTS_UPDATE,
				new String[]{"caseReference", caseReference});
	}
	
	public static ReferenceException newTerminalStatePreventsLinking(String caseReference)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_TERMINAL_STATE_PREVENTS_LINKING,
				new String[]{"caseReference", caseReference});
	}
	
	public static ReferenceException newNotCaseApp(String caseType)
	{
		return new ReferenceException(CDMErrorData.CDM_REFERENCE_NOT_CASE_APP,
				new String[]{"caseType", caseType});
	}
}
