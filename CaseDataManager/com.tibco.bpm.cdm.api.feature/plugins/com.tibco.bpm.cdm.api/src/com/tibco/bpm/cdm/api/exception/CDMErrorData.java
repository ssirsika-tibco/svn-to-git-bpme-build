package com.tibco.bpm.cdm.api.exception;

/**
 * Define error codes and related details for the CDM service.
 * @author smorgan
 * @since 2019
 */
public class CDMErrorData extends ErrorData
{
	/**
	 * Used for any unexpected error that is not otherwise caught and dealt with specifically.
	 */
	public static final CDMErrorData	CDM_INTERNAL										= new CDMErrorData(
			"CDM_INTERNAL_ERROR", "An internal error occurred: {message}", INTERNAL_SERVER_ERROR_500);

	/**
	 * CDM_DATA_* messages concern problems with data, whether that be casedata or non-case data 
	 */
	public static final CDMErrorData	CDM_DATA_NOT_JSON									= new CDMErrorData(
			"CDM_DATA_NOT_JSON", "Not JSON: {message}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DATA_NOT_JSON_OBJECT							= new CDMErrorData(
			"CDM_DATA_NOT_JSON_OBJECT", "Not JSON object", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DATA_INVALID									= new CDMErrorData(
			"CDM_DATA_INVALID", "Data is not a valid instance of the specified type: {details}", BAD_REQUEST_400);

	/**
	 * CDM_CASEDATA_* messages concern problems with a case's data content
	 */
	public static final CDMErrorData	CDM_CASEDATA_NON_UNIQUE_CID							= new CDMErrorData(
			"CDM_CASEDATA_NON_UNIQUE_CID", "Case identifier is not unique: {cid}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_CASEDATA_UNKNOWN_STATE_VALUE					= new CDMErrorData(
			"CDM_CASEDATA_UNKNOWN_STATE_VALUE", "Unknown state value: {value}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_CASEDATA_IDENTIFIER_NOT_SET						= new CDMErrorData(
			"CDM_CASEDATA_IDENTIFIER_NOT_SET",
			"Identifier attribute '{name}' is not set. Note: Auto-identifier population is not implemented "
					+ "yet, so always needs setting explictly.",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_CASEDATA_CID_CHANGED							= new CDMErrorData(
			"CDM_CASEDATA_CID_CHANGED",
			"The case identifier for an existing case must not be changed (from {oldCID} to {newCID})",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_CASEDATA_CID_WHEN_AUTO							= new CDMErrorData(
			"CDM_CASEDATA_CID_WHEN_AUTO",
			"A case identifier must not be specified when auto-generation is configured: {cid}", BAD_REQUEST_400);

	/**
	 * CDM_REFERENCE_* messages concerns problems with a case reference, whether that be a fully formed reference, 
	 * or a component part, such as namespace or major version, or related concepts such as type names, 
	 * fully-qualified names and other model references. 
	 */

	public static final CDMErrorData	CDM_REFERENCE_UNKNOWN_NAMESPACE						= new CDMErrorData(
			"CDM_REFERENCE_UNKNOWN_NAMESPACE", "Unknown namespace: {namespace} (major version {majorVersion})",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_UNKNOWN_TYPE							= new CDMErrorData(
			"CDM_REFERENCE_UNKNOWN_TYPE", "Unknown type: {type} (major version {majorVersion})", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_NOT_CASE_TYPE							= new CDMErrorData(
			"CDM_REFERENCE_NOT_CASE_TYPE", "Type is a non-case type: {type}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_INVALID_TYPE							= new CDMErrorData(
			"CDM_REFERENCE_INVALID_TYPE", "Invalid type: {type}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_INVALID_ID							= new CDMErrorData(
			"CDM_REFERENCE_INVALID_ID", "Invalid id in case reference '{caseReference}': {id}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_INVALID_VERSION						= new CDMErrorData(
			"CDM_REFERENCE_INVALID_VERSION", "Invalid type in case reference '{caseReference}': {type}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_INVALID_MAJOR_VERSION					= new CDMErrorData(
			"CDM_REFERENCE_INVALID_MAJOR_VERSION",
			"Invalid major version in case reference '{caseReference}': {majorVersion}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_INVALID_FORMAT						= new CDMErrorData(
			"CDM_REFERENCE_INVALID_FORMAT", "Invalid case reference format: {caseReference}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_VERSION_MISMATCH						= new CDMErrorData(
			"CDM_REFERENCE_VERSION_MISMATCH",
			"Version in case reference ({version}) mismatches actual version ({actualVersion}): {caseReference}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_NOT_EXIST								= new CDMErrorData(
			"CDM_REFERENCE_NOT_EXIST", "Case does not exist: {caseReference}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_CID_NOT_EXIST							= new CDMErrorData(
			"CDM_REFERENCE_CID_NOT_EXIST", "Case with case identifier does not exist: {caseIdentifier}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_WRONG_TYPE						= new CDMErrorData(
			"CDM_REFERENCE_LINK_WRONG_TYPE",
			"Case {caseReference} cannot be linked to {targetCaseReference} as target case is the wrong type for '{linkName}'",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_NAME_NOT_EXIST					= new CDMErrorData(
			"CDM_REFERENCE_LINK_NAME_NOT_EXIST",
			"Type '{typeName}' (major version {majorVersion}) does not have a link called '{linkName}'",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_NOT_ARRAY						= new CDMErrorData(
			"CDM_REFERENCE_LINK_NOT_ARRAY",
			"Type '{typeName}' (major version {majorVersion}) link '{linkName}' is not an array, so cannot link to {size} cases",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_OPPOSITE_NOT_ARRAY				= new CDMErrorData(
			"CDM_REFERENCE_LINK_OPPOSITE_NOT_ARRAY",
			"Type '{typeName}' (major version {majorVersion}) '{linkName}' link's opposite end ('{oppositeLinkName}') is not an "
					+ "array, so cannot be linked from multiple cases",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_ALREADY_LINKED					= new CDMErrorData(
			"CDM_REFERENCE_LINK_ALREADY_LINKED",
			"Case {caseReference} is already linked to {targetCaseReference} via link '{linkName}'", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_LINK_NOT_LINKED						= new CDMErrorData(
			"CDM_REFERENCE_LINK_NOT_LINKED",
			"Case {caseReference} is not linked to {targetCaseReference} via link '{linkName}'", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_DUPLICATE_LINK_TARGET					= new CDMErrorData(
			"CDM_REFERENCE_DUPLICATE_LINK_TARGET",
			"Case '{caseReference}' cannot be linked twice to '{targetCaseReference}' via link '{linkName}'",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_TERMINAL_STATE_PREVENTS_UPDATE		= new CDMErrorData(
			"CDM_REFERENCE_TERMINAL_STATE_PREVENTS_UPDATE",
			"Case is in a terminal state so cannot be updated: {caseReference}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REFERENCE_TERMINAL_STATE_PREVENTS_LINKING		= new CDMErrorData(
			"CDM_REFERENCE_TERMINAL_STATE_PREVENTS_LINKING",
			"Links cannot be created for a case in a terminal state: {caseReference}", BAD_REQUEST_400);
	
	public static final CDMErrorData	CDM_REFERENCE_NOT_CASE_APP									= new CDMErrorData(
			"CDM_REFERENCE_NOT_CASE_APP", "Update on these case(s) of type {caseType} is not permitted.", BAD_REQUEST_400);

	/**
	 * CDM_DEPLOYMENT_* messages concern problems with deployment/undeployment of applications
	 */

	public static final CDMErrorData	CDM_DEPLOYMENT_BAD_RASC								= new CDMErrorData(
			"CDM_DEPLOYMENT_BAD_RASC", "The RASC is invalid: {message}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_DATAMODEL_DESERIALIZATION_FAILED		= new CDMErrorData(
			"CDM_DEPLOYMENT_DATAMODEL_DESERIALIZATION_FAILED", "Data Model deserialization failed", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_INVALID_DATAMODEL					= new CDMErrorData(
			"CDM_DEPLOYMENT_INVALID_DATAMODEL", "The Data Model failed validation", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_INVALID_DATAMODEL_UPGRADE			= new CDMErrorData(
			"CDM_DEPLOYMENT_INVALID_DATAMODEL_UPGRADE",
			"The Data Model is not a valid upgrade from the existing version", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_UNKNOWN_ARTIFACT_TYPE				= new CDMErrorData(
			"CDM_DEPLOYMENT_UNKNOWN_ARTIFACT_TYPE",
			"Case Data Manager does not expect to receive artifacts of type: {type}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_INVALID_VERSION_DEPENDENCY			= new CDMErrorData(
			"CDM_DEPLOYMENT_INVALID_VERSION_DEPENDENCY",
			"Invalid application version dependency range. The lower end must be inclusive and "
					+ "the upper end must be the major version above the lower end (exclusive): {versionRangeExpression} ",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_UNRESOLVABLE_DEPENDENCY				= new CDMErrorData(
			"CDM_DEPLOYMENT_UNRESOLVABLE_DEPENDENCY",
			"Application requires a single existing application with id '{applicationId}' matching version "
					+ "range '{versionRangeExpression}', but found: {found}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_DEPENDENCIES_PREVENT_UNDEPLOYMENT	= new CDMErrorData(
			"CDM_DEPLOYMENT_DEPENDENCIES_PREVENT_UNDEPLOYMENT",
			"Undeployment is not possible as other application(s) depend on this application: {dependencies}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_DEPLOYMENT_DUPLICATE_NAMESPACE					= new CDMErrorData(
			"CDM_DEPLOYMENT_DUPLICATE_NAMESPACE",
			"Another application exists at version {majorVersion}.x containing a data model with namespace '{namespace}'",
			BAD_REQUEST_400);

	/**
	 * CDM_PERSISTENCE_* messages concern problems with database interaction
	 */
	public static final CDMErrorData	CDM_PERSISTENCE_NOT_CONNECTED						= new CDMErrorData(
			"CDM_PERSISTENCE_NOT_CONNECTED", "Service is not connected to the database", INTERNAL_SERVER_ERROR_500);

	public static final CDMErrorData	CDM_PERSISTENCE_TRANSIENT_REPOSITORY_ERROR			= new CDMErrorData(
			"CDM_PERSISTENCE_TRANSIENT_REPOSITORY_ERROR", "A transient repository error occurred",
			INTERNAL_SERVER_ERROR_500);

	public static final CDMErrorData	CDM_PERSISTENCE_REPOSITORY_ERROR					= new CDMErrorData(
			"CDM_PERSISTENCE_REPOSITORY_ERROR", "A repository error occurred", INTERNAL_SERVER_ERROR_500);

	public static final CDMErrorData	CDM_REST_APPLICATION_ID_TOO_LONG					= new CDMErrorData(
			"CDM_REST_APPLICATION_ID_TOO_LONG", "applicationId must not exceed 256 characters: '{value}'",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_NAMESPACE_TOO_LONG							= new CDMErrorData(
			"CDM_REST_NAMESPACE_TOO_LONG", "namespace must not exceed 256 characters: '{value}'", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_APPLICATION_MAJOR_VERSION_INVALID			= new CDMErrorData(
			"CDM_REST_APPLICATION_MAJOR_VERSION_INVALID", "applicationMajorVersion is invalid: '{value}'",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_REF_LIST_INVALID							= new CDMErrorData(
			"CDM_API_REF_LIST_INVALID", "Case reference list must be non-null and contain at least one value",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASEDATA_LIST_INVALID						= new CDMErrorData(
			"CDM_API_CASEDATA_LIST_INVALID", "Casedata list must be non-null and contain at least one value",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_REF_AND_CASEDATA_LISTS_SIZE_MISMATCH		= new CDMErrorData(
			"CDM_API_REF_AND_CASEDATA_LISTS_SIZE_MISMATCH", "Case reference and casedata lists must be the same size",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_SKIP_INVALID								= new CDMErrorData(
			"CDM_API_SKIP_INVALID", "Skip is invalid: '{value}'", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_TOP_INVALID									= new CDMErrorData(
			"CDM_API_TOP_INVALID", "Top is invalid: '{value}'", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_TOP_MANDATORY								= new CDMErrorData(
			"CDM_API_TOP_MANDATORY", "Top is mandatory", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_LINK_NAME_MANDATORY							= new CDMErrorData(
			"CDM_API_LINK_NAME_MANDATORY", "Link name must be specified", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_TYPE_INVALID								= new CDMErrorData(
			"CDM_API_TYPE_INVALID", "Type invalid: {value}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_DQL_NEEDS_LINK_NAME							= new CDMErrorData(
			"CDM_API_DQL_NEEDS_LINK_NAME", "Use of DQL requires a link name", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BADJSON									= new CDMErrorData(
			"CDM_REST_BADJSON", "Bad JSON in request", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_ILLEGAL_ARGUMENT_EXCEPTION					= new CDMErrorData(
			"CDM_REST_ILLEGAL_ARGUMENT_EXCEPTION", "An Illegal Argument Exception occurred", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_INVALID_URL_ENCODING						= new CDMErrorData(
			"CDM_REST_INVALID_URL_ENCODING", "Invalid URL encoding", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BADFILTEREXPRESSIONS						= new CDMErrorData(
			"CM_REST_BADFILTEREXPRESSIONS", "$filter contained invalid expression(s): {expressions}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_INTERNAL									= new CDMErrorData(
			"CDM_REST_INTERNAL", "An error occurred", INTERNAL_SERVER_ERROR_500);

	public static final CDMErrorData	CDM_REST_INVALID_REQUEST_PROPERTY					= new CDMErrorData(
			"CDM_REST_INVALID_REQUEST_PROPERTY", "Invalid request property '{name}' (value: {value})", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BAD_CASE_REFERENCE_LIST					= new CDMErrorData(
			"CDM_REST_BAD_CASE_REFERENCE_LIST",
			"caseReferences in(...) must have a comma-separated list of single-quoted case references, not: "
					+ "{caseReferences}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BAD_TARGET_CASE_REFERENCE_LIST				= new CDMErrorData(
			"CDM_REST_BAD_TARGET_CASE_REFERENCE_LIST",
			"targetCaseReference in(...) must have a comma-separated list of single-quoted case references, not: "
					+ "{targetCaseReferences}",
			BAD_REQUEST_400);

    public static final CDMErrorData CDM_REST_BAD_TARGET_CASE_REFERENCE_LIST_WITH_DUPLICATES =
            new CDMErrorData("CDM_REST_",
                    "targetCaseReference in(...) must have a comma-separated list of single-quoted case references without any duplicates. Duplicates found : "
                            + "{targetCaseReferences}",
                    BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BAD_DELETE_LINKS_FILTER					= new CDMErrorData(
			"CDM_REST_BAD_DELETE_LINKS_FILTER",
			"Filter may contain name and at most one of 'targetCaseReference eq' and 'targetCaseReference in(...)', "
					+ "not: {filter}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BAD_IS_IN_TERMINAL_STATE					= new CDMErrorData(
			"CDM_REST_BAD_IS_IN_TERMINAL_STATE",
			"The only value allowed for isInTerminalState is FALSE, not: {isInTerminalState}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_BAD_IS_REFERENCED_BY_PROCESS				= new CDMErrorData(
			"CDM_REST_BAD_IS_REFERENCED_BY_PROCESS",
			"The only value allowed for isReferencedByProcess is FALSE, not: {isReferencedByProcess}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_CASE_REFERENCE_IN_PREVENTS_PARAMETERS		= new CDMErrorData(
			"CDM_REST_CASE_REFERENCE_IN_PREVENTS_PARAMETERS",
			"When $filter contains caseReference, other $filter expressions are not allowed and neither are "
					+ "$skip, $top, $count, $search or $dql",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_REST_NOT_AUTHORISED								= new CDMErrorData(
			"CDM_REST_NOT_AUTHORISED", "The REST call is not authorized", BAD_REQUEST_400);

	/**
	 * CDM_API_* messages concern problems with arguments passed to APIs (not necessarily REST)
	 */
	public static final CDMErrorData	CDM_API_CASES_BAD_SELECT							= new CDMErrorData(
			"CDM_API_CASES_BAD_SELECT",
			"Bad select expression '{select}'. Expected one or more comma-separated tokens from caseReference (or cr), "
					+ "casedata (or c), summary (or s) and/or metadata (or m).",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASES_BAD_MODIFICATION_TIMESTAMP			= new CDMErrorData(
			"CDM_API_CASES_BAD_MODIFICATION_TIMESTAMP",
			"Modification timestamp '{modificationTimestamp}' is not in a valid format. "
					+ "It must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (and can be partially specified, "
					+ "elements populated left-to-right, but must include 'T' separator and time-zone)",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_TYPES_BAD_SELECT							= new CDMErrorData(
			"CDM_API_TYPES_BAD_SELECT",
			"Bad select expression '{select}'. Expected one or more comma-separated tokens from basic (or b), "
					+ "attributes (or a), summaryAttributes (or sa), states (or s), links (or l) and/or dependencies (or d).",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASES_SAME_TYPE								= new CDMErrorData(
			"CDM_API_CASES_SAME_TYPE", "All cases must be of the same type", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASES_SAME_MAJOR_VERSION					= new CDMErrorData(
			"CDM_API_CASES_SAME_MAJOR_VERSION", "All cases must be of the same application major version",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASES_BAD_MAJOR_VERSION						= new CDMErrorData(
			"CDM_API_CASES_BAD_MAJOR_VERSION", "Application major version must be a non-negative integer",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_LINK_TARGET_WRONG_TYPE						= new CDMErrorData(
			"CDM_API_LINK_TARGET_WRONG_TYPE", "Link '{linkName}' is of type {expectedType} "
					+ "(major version {majorVersion}), so cannot be assigned: {caseReference}",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_LINK_REFS_WITHOUT_NAME						= new CDMErrorData(
			"CDM_API_LINK_REFS_WITHOUT_NAME", "A link name must be given when specifying target case reference(s)",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_MODIFICATION_TIMESTAMP_MANDATORY			= new CDMErrorData(
			"CDM_API_MODIFICATION_TIMESTAMP_MANDATORY", "Modification timestamp must be specified", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASE_TYPE_MANDATORY							= new CDMErrorData(
			"CDM_API_CASE_TYPE_MANDATORY", "Case type must be specified", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASES_BAD_CASE_TYPE							= new CDMErrorData(
			"CDM_API_CASES_BAD_CASE_TYPE", "Bad case type: {caseType}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_DQL_WITH_OTHER_SEARCH_PARAMETERS			= new CDMErrorData(
			"CDM_API_DQL_WITH_OTHER_SEARCH_PARAMETERS",
			"When DQL is used, Search, CID, Case State and Modification Timestamp must not be used", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_DQL_WITH_SEARCH								= new CDMErrorData(
			"CDM_API_DQL_WITH_SEARCH", "When DQL is used, Search must not be used", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_BAD_DQL										= new CDMErrorData(
			"CDM_API_BAD_DQL", "Bad DQL: {issues}", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_CASE_STATE_MANDATORY						= new CDMErrorData(
			"CDM_API_CASE_STATE_MANDATORY", "A case state (state value) must be specified", BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_NO_CURRENT_USER								= new CDMErrorData(
			"CDM_API_NO_CURRENT_USER", "Unable to determine the calling user. RequestContext must be set correctly.",
			BAD_REQUEST_400);

	public static final CDMErrorData	CDM_API_BAD_CURRENT_USER_GUID						= new CDMErrorData(
			"CDM_API_BAD_CURRENT_USER_GUID",
			"RequestContext must contain a user GUID of no more than 36 characters, not: {guid}", BAD_REQUEST_400);

    public static final CDMErrorData CDM_API_FORBIDDEN = new CDMErrorData(
			"CDM_API_FORBIDDEN", "You are not not authorized to call this API", FORBIDDEN_403);

	// Prevent external instantiation.
	private CDMErrorData(String code, String messageTemplate, int httpStatus)
	{
		super(code, messageTemplate, httpStatus);
	}
}
