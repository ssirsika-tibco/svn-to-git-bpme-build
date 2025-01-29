package com.tibco.bpm.cdm.api;

import java.util.List;

import com.tibco.bpm.cdm.api.dto.CaseInfo;
import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.ValidationException;

/**
 * Private API for Case Data Manager. This can be obtained via Blueprint with something like:
 * <pre>{@code
 *    <reference id="caseDataManager" interface="com.tibco.bpm.cdm.api.CaseDataManager" />
 * }</pre>
 *
 * All methods have the potential to throw an ArgumentException with a message code 
 * 'CDM_API_FORBIDDEN' if the caller lacks the required System Actions. The message code 
 * can be checked by calling {{@link ArgumentException#getCode()} 
 * 
 * @author smorgan
 * @since 2019
 */
public interface CaseDataManager
{
	/**
	 * Creates a single case.
	 * 
	 * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
	 * @param majorVersion major version of the containing application
	 * @param casedata JSON casedata
	 * @return case reference for the new case
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the casedata is not valid in relation to the Data Model
	 */
	public CaseReference createCase(QualifiedTypeName type, int majorVersion, String casedata)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Creates one or more cases.
	 * 
	 * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
	 * @param majorVersion major version of the containing application
	 * @param casedata list of JSON casedata
	 * @return list of case references for the new cases
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the casedata is not valid in relation to the Data Model
	 */
	public List<CaseReference> createCases(QualifiedTypeName type, int majorVersion, List<String> casedata)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Reads a case
	 * 
	 * @param ref case reference for case to read
	 * @return CaseInfo object containing casedata and case reference (with the version number 
	 * component reflecting the current version number, not necessarily the same as that in 
	 * the supplied case reference).
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public CaseInfo readCase(CaseReference ref) throws ArgumentException, InternalException;

	public CaseInfo readCase(QualifiedTypeName type, int majorVersion, String caseIdentifier)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Reads the given cases(s). The order of the returned list will
	 * match the order of the case references in the supplied list.
	 * 
	 * @param refs case references for cases to read 
	 * @return list of CaseInfo objects containing casedata and case reference (with the version number
	 * component reflecting the current version number, not necessarily the same as that in 
	 * the supplied case reference), in the same order as the supplied references.
	 * 
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public List<CaseInfo> readCases(List<CaseReference> refs) throws ArgumentException, InternalException;

	/**
	 * Retrieves cases of a given type.
	 * 
	 * - type and majorVersion are mandatory.
	 * - skip, top, search and dql are optional.
	 * 
	 * (note: search and dql must not be used together)
	 * 
	 * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
	 * @param majorVersion major version of the containing application
	 * @param skip Number of cases to skip
	 * @param top (mandatory) Maximum number of cases to return
	 * @param search Simple search expression
	 * @param dql DQL query
	 * @return list of CaseInfo objects containing casedata and case reference (with the version number
	 * component reflecting the current version number, not necessarily the same as that in 
	 * the supplied case reference).
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public List<CaseInfo> readCases(QualifiedTypeName type, int majorVersion, Integer skip, Integer top, String search, String dql)
			throws ArgumentException, InternalException;

	/**
	 * Updates a case 
	 * 
	 * @param ref case reference for the case to update. The version number must match
	 * the current version. 
	 * @param casedata new JSON casedata
	 * @return case reference with version number incremented
	 * @throws ArgumentException if any of the supplied arguments are invalid. If the version
	 * number in the reference mismatches the actual version number, an ArgumentException will
	 * be thrown with a message code 'CDM_REFERENCE_VERSION_MISMATCH'. The message code can be
	 * checked by calling {{@link ArgumentException#getCode()}
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the casedata is not valid in relation to the Data Model
	 */
	public CaseReference updateCase(CaseReference ref, String casedata)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Update one or more cases.  The supplied ref/casedata lists must be of the same size,
	 * containing corresponding pairs of case reference and updated casedata.
	 * 
	 * @param refs list of case references to update
	 * @param casedataList list of new casedata values
	 * @return List of updated case references in the same order as the supplied list.
	 * @throws ArgumentException if any of the supplied arguments are invalid. If the version
	 * number in the reference mismatches the actual version number, an ArgumentException will
	 * be thrown with a message code 'CDM_REFERENCE_VERSION_MISMATCH'. The message code can be
	 * checked by calling {{@link ArgumentException#getCode()}
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the casedata is not valid in relation to the Data Model
	 */
	public List<CaseReference> updateCases(List<CaseReference> refs, List<String> casedataList)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Deletes a case by reference
	 * 
	 * @param ref case reference for case to delete
	 * @throws ArgumentException if any of the supplied arguments are invalid. If the version
	 * number in the reference mismatches the actual version number, an ArgumentException will
	 * be thrown with a message code 'CDM_REFERENCE_VERSION_MISMATCH'. The message code can be
	 * checked by calling {{@link ArgumentException#getCode()}
	 * @throws InternalException if a server error occurs
	 */
	public void deleteCase(CaseReference ref) throws ArgumentException, InternalException;

	/**
	 * Links the given case to the given target case via the given link name.
	 * Note that link APIs are not version-specific, so the version portion of supplied case reference
	 * is not checked.
	 * 
	 * @param ref case reference for case from which to create a link
	 * @param linkName name of the link, as defined in the model
	 * @param targetRef case reference to link to
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public void linkCase(CaseReference ref, String linkName, CaseReference targetRef) throws ArgumentException, InternalException;

	/**
	 * Links the given case to the given target case(s) via the given link name.
	 * Note that link APIs are not version-specific, so the version portion of supplied case references
	 * is not checked.
	 * 
	 * @param ref case reference for the case from which the link is created
	 * @param linkName name of the link, as defined in the model
	 * @param targetRefs case reference(s) to link to
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public void linkCases(CaseReference ref, String linkName, List<CaseReference> targetRefs)
			throws ArgumentException, InternalException;

	/**
	 * Unlinks the given case from the given target case via the given link name.
	 * Note that link APIs are not version-specific, so the version portion of supplied case references
	 * is not checked.
	 * 
	 * @param ref case reference for the case from which the link is removed
	 * @param linkName name of the link, as defined in the model
	 * @param targetRef case reference(s) to link to
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public void unlinkCase(CaseReference ref, String linkName, CaseReference targetRef) throws ArgumentException, InternalException;

	/**
	 * Unlinks the given case from the given target case(s) via the given link name.
	 * Note that link APIs are not version-specific, so the version portion of supplied case references
	 * is not checked.
	 * 
	 * @param ref case reference for the case from which the links are removed
	 * @param linkName name of the link, as defined in the model
	 * @param targetRef case references to unlink from
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public void unlinkCases(CaseReference ref, String linkName, List<CaseReference> targetRefs)
			throws ArgumentException, InternalException;

	/**
	 * Unlinks the given case from all case(s) to which it is linked via the given link name.
	 * Note that link APIs are not version-specific, so the version portion of supplied case references
	 * is not checked.
	 * 
	 * @param ref case reference for the case from which the links are removed
	 * @param linkName name of the link, as defined in the model
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public void unlinkCases(CaseReference ref, String linkName) throws ArgumentException, InternalException;

	/**
	 * Obtains case references for cases that a given case is linked to (via a given link name).
	 * Cases can be filtered by simple search ('search') or DQL query ('dql'). 
	 * (note: search and dql must not be used together)
	 * 
	 * @param ref case reference from which links originate
	 * @param linkName name of link, as defined in the model
	 * @param skip Number of cases to skip
	 * @param top (mandatory) Maximum number of cases to return
	 * @param search Simple search expression
	 * @param dql DQL query
	 * @return
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 */
	public List<CaseReference> navigateLinks(CaseReference ref, String linkName, Integer skip, Integer top, String search, String dql)
			throws ArgumentException, InternalException;

	/**
     * Validates the given data against the model for the given type (case or
     * non-case) It involves strict type checking.
     * 
	 * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
	 * @param majorVersion major version of the containing application
	 * @param casedata casedata JSON to validate
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the data is invalid. The message will contain an explanation of
	 * validation errors.
     */
	public void validate(QualifiedTypeName type, int majorVersion, String data)
			throws ArgumentException, ValidationException, InternalException;

    /**
     * Validates the given data against the model for the given type (case or
     * non-case) with or without strict type check. 
     * 
     * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
     * @param majorVersion major version of the containing application
     * @param casedata casedata JSON to validate
     * @param strictTypeCheck if true checks for type check validations
     * @throws ArgumentException if any of the supplied arguments are invalid
     * @throws InternalException if a server error occurs
     * @throws ValidationException if the data is invalid. The message will contain an
     *             explanation of validation errors.
     */
    public void validate(QualifiedTypeName type, int majorVersion, String data, boolean strictTypeCheck)
            throws ArgumentException, ValidationException, InternalException;

	/**
	 * Validates the given data against the model for the given type (case or non-case),
	 * optionally removing null value properties and null array entries.
	 * 
	 * @param type fully-qualified type name (e.g. 'org.example.bom.Policy')
	 * @param majorVersion major version of the containing application
	 * @param data data JSON to validate
	 * @param removeNulls if true, the returned string will be the same as the data argument, which
	 * any null value properties and null array entries removed
	 * @throws ArgumentException if any of the supplied arguments are invalid
	 * @throws InternalException if a server error occurs
	 * @throws ValidationException if the data is invalid. The message will contain an explanation of
	 * validation errors.
	 */
	public String processData(QualifiedTypeName type, int majorVersion, String data, boolean removeNulls)
			throws ArgumentException, InternalException, ValidationException;

	/**
	 * Returns true if the given case exists.
	 * 
	 * If the supplied ref is syntactically valid and refers to an existing type, a boolean will
	 * be returned to indicate whether the case exists. Otherwise, an ArgumentException will be thrown.
	 * 
	 * @param ref case reference to check the existence of
	 * @return true if exists
	 * @throws ArgumentException if any of the supplied arguments are invalid. 
	 * @throws InternalException if a server error occurs
	 */
	public boolean exists(CaseReference ref) throws ArgumentException, InternalException;
	
	/**
	 * TODO Remove once WRP has been changed to call the one that takes a CaseReference
	 * @deprecated
	 */
    public boolean exists(String ref) throws ArgumentException, InternalException;

	/**
	 * Returns true if the given case is in an active state or false if it is in a terminal state.
	 * 
	 * @param ref case reference to check the state of
	 * @return true if the case is in an active state
	 * @throws ArgumentException if any of the supplied arguments are invalid. 
	 * @throws InternalException if a server error occurs
	 */
	public boolean isActive(CaseReference ref) throws ArgumentException, InternalException;
}
