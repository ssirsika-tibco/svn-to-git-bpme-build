package com.tibco.bpm.cdm.core.aspect;

import java.util.Arrays;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMErrorData;

/**
 * select parser for /types API
 *
 * @author smorgan
 * @since 2019
 */
public class TypeAspectSelection extends AbstractAspectSelection
{
	public static final Aspect			ASPECT_BASIC				= new Aspect("basic", "b");

	public static final Aspect			ASPECT_ATTRIBUTES			= new Aspect("attributes", "a");

	public static final Aspect			ASPECT_SUMMARY_ATTRIBUTES	= new Aspect("summaryAttributes", "sa");

	public static final Aspect			ASPECT_STATES				= new Aspect("states", "s");

	public static final Aspect			ASPECT_LINKS				= new Aspect("links", "l");

	public static final Aspect			ASPECT_DEPENDENCIES			= new Aspect("dependencies", "d");

	private static final List<Aspect>	allAspects					= Arrays.asList(ASPECT_BASIC, ASPECT_ATTRIBUTES,
			ASPECT_SUMMARY_ATTRIBUTES, ASPECT_STATES, ASPECT_LINKS, ASPECT_DEPENDENCIES);

	protected TypeAspectSelection()
	{
		super(allAspects);
	}

	/**
	 * Parses the supplied '$select' expression and returns a CaseAspectSelection,
	 * throwing an exception if the expression is invalid.
	 * @throws ArgumentException 
	 */
	public static TypeAspectSelection fromSelectExpression(String select) throws ArgumentException
	{
		TypeAspectSelection cas = new TypeAspectSelection();
		// Use the appropriate error message for public/private API on failure
		cas.populateFromSelectExpression(select, CDMErrorData.CDM_API_TYPES_BAD_SELECT);
		return cas;
	}
}
