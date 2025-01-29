package com.tibco.bpm.cdm.core.aspect;

import java.util.Arrays;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMErrorData;

/**
 * select parser for /cases API
 *
 * @author smorgan
 * @since 2019
 */
public class CaseAspectSelection extends AbstractAspectSelection
{
	public static final Aspect			ASPECT_CASE_REFERENCE					= new Aspect("caseReference", "cr");

	public static final Aspect			ASPECT_CASEDATA							= new Aspect("casedata", "c");

	public static final Aspect			ASPECT_SUMMARY							= new Aspect("summary", "s");

	// Sub-aspects of the 'metadata' aspect
	public static final Aspect			ASPECT_METADATA_CREATED_BY				= new Aspect("createdBy", "cb");

	public static final Aspect			ASPECT_METADATA_CREATION_TIMESTAMP		= new Aspect("creationTimestamp", "ct");

	public static final Aspect			ASPECT_METADATA_MODIFIED_BY				= new Aspect("modifiedBy", "mb");

	public static final Aspect			ASPECT_METADATA_MODIFICATION_TIMESTAMP	= new Aspect("modificationTimestamp",
			"mt");

	// The metadata aspect has various sub-aspects
	public static final Aspect			ASPECT_METADATA							= new Aspect("metadata", "m",
			Arrays.asList(ASPECT_METADATA_CREATED_BY, ASPECT_METADATA_CREATION_TIMESTAMP, ASPECT_METADATA_MODIFIED_BY,
					ASPECT_METADATA_MODIFICATION_TIMESTAMP));

	private static final List<Aspect>	allAspects								= Arrays.asList(ASPECT_CASE_REFERENCE,
			ASPECT_CASEDATA, ASPECT_SUMMARY, ASPECT_METADATA);

	protected CaseAspectSelection()
	{
		super(allAspects);
	}

	/**
	 * Parses the supplied '$select' expression and returns a CaseAspectSelection,
	 * throwing an exception if the expression is invalid.
	 * @throws ArgumentException 
	 */
	public static CaseAspectSelection fromSelectExpression(String select) throws ArgumentException
	{
		CaseAspectSelection cas = new CaseAspectSelection();
		// Use the appropriate error message for public/private API on failure
		cas.populateFromSelectExpression(select, CDMErrorData.CDM_API_CASES_BAD_SELECT);
		return cas;
	}

	public static CaseAspectSelection fromAspects(Aspect... aspects)
	{
		CaseAspectSelection cas = new CaseAspectSelection();
		cas.aspects.addAll(Arrays.asList(aspects));
		return cas;
	}
}
