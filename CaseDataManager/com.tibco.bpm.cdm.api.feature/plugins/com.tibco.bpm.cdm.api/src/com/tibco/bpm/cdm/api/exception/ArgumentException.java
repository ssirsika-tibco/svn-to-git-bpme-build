package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates a problem with an argument passed to a CDM API (but not specifically the REST API)
 * @author smorgan
 * @since 2019
 */
public class ArgumentException extends UserApplicationError
{
	protected ArgumentException(ErrorData errorData)
	{
		super(errorData);
	}

	protected ArgumentException(ErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	protected ArgumentException(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	private static final long serialVersionUID = 1L;

	public static ArgumentException newSkipInvalid(Integer value)
	{
		return new ArgumentException(CDMErrorData.CDM_API_SKIP_INVALID, new String[]{"value", value.toString()});
	}

	public static ArgumentException newTopInvalid(Integer value)
	{
		return new ArgumentException(CDMErrorData.CDM_API_TOP_INVALID, new String[]{"value", value.toString()});
	}

	public static ArgumentException newTopMandatory()
	{
		return new ArgumentException(CDMErrorData.CDM_API_TOP_MANDATORY);
	}

	public static ArgumentException newTypeInvalid(String value)
	{
		return new ArgumentException(CDMErrorData.CDM_API_TYPE_INVALID, new String[]{"value", value});
	}

	public static ArgumentException newDQLNeedsLinkName()
	{
		return new ArgumentException(CDMErrorData.CDM_API_DQL_NEEDS_LINK_NAME);
	}

	public static ArgumentException newBadSelect(String select, CDMErrorData errorData)
	{
		return new ArgumentException(errorData, new String[]{"select", select});
	}

	public static ArgumentException newBadModificationTimestamp(String modificationTimestamp)
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASES_BAD_MODIFICATION_TIMESTAMP,
				new String[]{"modificationTimestamp", modificationTimestamp});
	}

	public static ArgumentException newCasesSameType()
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASES_SAME_TYPE);
	}

	public static ArgumentException newCasesSameMajorVersion()
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASES_SAME_MAJOR_VERSION);
	}

	public static ArgumentException newBadMajorVersion(String majorVersion)
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASES_BAD_MAJOR_VERSION,
				new String[]{"majorVersion", majorVersion});
	}

	public static ArgumentException newModificationTimestampMandatory()
	{
		return new ArgumentException(CDMErrorData.CDM_API_MODIFICATION_TIMESTAMP_MANDATORY);
	}

	public static ArgumentException newCaseTypeMandatory()
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASE_TYPE_MANDATORY);
	}

	public static ArgumentException newBadCaseType(String caseType, Throwable cause)
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASES_BAD_CASE_TYPE, new String[]{"caseType", caseType});
	}

	public static ArgumentException newCaseStateMandatory()
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASE_STATE_MANDATORY);
	}

	public static ArgumentException newLinkBadType(String caseReference, String linkName, String expectedType,
			String majorVersion)
	{
		return new ArgumentException(CDMErrorData.CDM_API_LINK_TARGET_WRONG_TYPE, new String[]{"caseReference",
				caseReference, "linkName", linkName, "expectedType", expectedType, "majorVersion", majorVersion});
	}

	public static ArgumentException newLinkRefsWithoutName()
	{
		return new ArgumentException(CDMErrorData.CDM_API_LINK_REFS_WITHOUT_NAME);
	}

	public static ArgumentException newBadDQL(String issues)
	{
		return new ArgumentException(CDMErrorData.CDM_API_BAD_DQL, new String[]{"issues", issues});
	}

	public static ArgumentException newDQLWithOtherSearchParameters()
	{
		return new ArgumentException(CDMErrorData.CDM_API_DQL_WITH_OTHER_SEARCH_PARAMETERS);
	}

	public static ArgumentException newDQLWithSearch()
	{
		return new ArgumentException(CDMErrorData.CDM_API_DQL_WITH_SEARCH);
	}

	public static ArgumentException newRefListInvalid()
	{
		return new ArgumentException(CDMErrorData.CDM_API_REF_LIST_INVALID);
	}

	public static ArgumentException newCasedataListInvalid()
	{
		return new ArgumentException(CDMErrorData.CDM_API_CASEDATA_LIST_INVALID);
	}

	public static ArgumentException newRefAndCasedataListsSizeMismatch()
	{
		return new ArgumentException(CDMErrorData.CDM_API_REF_AND_CASEDATA_LISTS_SIZE_MISMATCH);
	}

	public static ArgumentException newLinkNameMandatory()
	{
		return new ArgumentException(CDMErrorData.CDM_API_LINK_NAME_MANDATORY);
	}

	public static ArgumentException newNoCurrentUser()
	{
		return new ArgumentException(CDMErrorData.CDM_API_NO_CURRENT_USER);
	}

	public static ArgumentException newBadCurrentUserGUID(String guid)
	{
		return new ArgumentException(CDMErrorData.CDM_API_BAD_CURRENT_USER_GUID, new String[]{"guid", guid});
	}
}
