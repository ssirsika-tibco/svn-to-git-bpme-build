/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.cdm.core.aspect;

import java.util.List;

/**
 * See AbstractAspectSelection.
 * An aspect has a name (e.g. 'caseReference'), an abbreviation (e.g. 'cr' - or something sensible if
 * clashes exist between aspects) and may optionally have sub-aspects that can be referenced through
 * dot notation (e.g. metadata.createdBy).
 *
 * @author smorgan
 * @since 2019
 */
public class Aspect
{
	private String			name;

	private String			abbreviation;

	private List<Aspect>	subAspects;

	protected Aspect(String name, String abbreviation, List<Aspect> subAspects)
	{
		this.name = name;
		this.abbreviation = abbreviation;
		this.subAspects = subAspects;
	}

	protected Aspect(String name, String abbreviation)
	{
		this(name, abbreviation, null);
	}

	public String getName()
	{
		return name;
	}

	public String getAbbreviation()
	{
		return abbreviation;
	}

	public List<Aspect> getSubAspects()
	{
		return subAspects;
	}

	public String toString()
	{
		return name + " (" + abbreviation + ")";
	}

}
