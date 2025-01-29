/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.cdm.core.aspect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMErrorData;

/**
 * Represents a parsed 'select' expression for selecting from a number of 'aspects' of an entity.
 * Concreate sub-classes should ensure they populate allAspects with a complete catalogue of Aspect objects
 * that they recognise and then call populateFromSelectExpression to populate themselves from a select string.
 *
 * @author smorgan
 * @since 2019
 */
public abstract class AbstractAspectSelection
{
	private static final String	SEPARATOR	= ",";

	public List<Aspect>			allAvailableAspects;

	protected List<Aspect>		aspects		= new ArrayList<Aspect>();

	public AbstractAspectSelection(List<Aspect> allAvailableAspects)
	{
		this.allAvailableAspects = allAvailableAspects;
	}

	public Aspect findAspectByPath(String path)
	{
		// The token can either be a top-level aspect name, or a dot-separated
		// path to a sub-aspect.  In both cases, abbreviations can be used.
		String[] fragments = path.split("\\.");

		// Attempt to find a top-level path with the first fragment as
		// its name or abbreviation.
		List<Aspect> available = allAvailableAspects;

		Aspect aspect = null;
		for (String fragment : fragments)
		{
			if (available == null)
			{
				// No aspects are available which suggests the path has extraneous fragment(s).
				// Error - no such aspect. Indicate by returning null
				return null;
			}
			aspect = available.stream()
					.filter(a -> fragment.equals(a.getName()) || fragment.equals(a.getAbbreviation())).findFirst()
					.orElse(null);
			if (aspect == null)
			{
				// Error - no such aspect. Indicate by returning null
				return null;
			}
			else
			{
				// To cope with sub-aspect selection, continue the search using the sub-aspects of the chosen aspect.
				available = aspect.getSubAspects();
			}
		}
		return aspect;
	}

	/**
	 * Returns true if the selection includes the given aspect.
	 */
	public boolean includes(Aspect aspect)
	{
		return aspects.contains(aspect);
	}

	/**
	 * Returns true if the selection includes the given aspect
	 * or is nothing (i.e. is implicitly selecting everything)
	 */
	public boolean includesOrIsNothing(Aspect aspect)
	{
		return includes(aspect) || isNothing();
	}

	/**
	 * Returns true if the selection exactly matches the aspect(s) passed in
	 */
	public boolean includesOnly(Aspect... someAspects)
	{
		return aspects.size() == someAspects.length && aspects.containsAll(Arrays.asList(someAspects));
	}

	/**
	 * Returns true if the selection includes any aspect(s) EXCEPT the one specified.
	 */
	public boolean includesAnythingExcept(Aspect aspect)
	{
		boolean found = false;
		for (int i = 0; !found && i < aspects.size(); i++)
		{
			// Found an aspect that isn't the one specified?
			found = !aspects.get(i).equals(aspect);
		}
		return found;
	}

	/**
	 * Returns true if the selections includes one or more of the given choices.
	 */
	public boolean includesAny(List<Aspect> choices)
	{
		return choices.stream().anyMatch(c -> includes(c));
	}

	/**
	 * Returns true if the selections includes one or more of the given choices.
	 */
	public boolean includesAny(Aspect... choices)
	{
		return includesAny(Arrays.asList(choices));
	}

	public boolean includesAnyOrIsNothing(Aspect... choices)
	{
		return isNothing() || includesAny(choices);
	}
	
	/**
	 * Return true if the selection includes the given aspect or any of its sub-aspects.
	 */
	public boolean includesOrIncludesSubAspectsOf(Aspect aspect)
	{
		boolean result = false;
		if (includes(aspect))
		{
			result = true;
		}
		else
		{
			List<Aspect> subAspects = aspect.getSubAspects();
			if (subAspects != null)
			{
				result = includesAny(subAspects);
			}
		}
		return result;
	}

	public boolean includesOrIncludesSubAspectsOfOrIsNothing(Aspect aspect)
	{
		boolean result = false;
		if (isNothing())
		{
			result = true;
		}
		else
		{
			if (includes(aspect))
			{
				result = true;
			}
			else
			{
				List<Aspect> subAspects = aspect.getSubAspects();
				if (subAspects != null)
				{
					result = includesAny(subAspects);
				}
			}
		}
		return result;
	}

	/**
	 * Populates object based on the aspect(s) references in the given select string.  If the select
	 * string is invalid, an exception is throw using the provided ErrorDate (so concrete sub-classes
	 * can choose an appropriate error to match their requirements).
	 * @throws ArgumentException 
	 */
	protected void populateFromSelectExpression(String select, CDMErrorData errorData) throws ArgumentException 
	{
		if (select != null)
		{
			for (String tokenFull : select.split(SEPARATOR))
			{
				String token = tokenFull.trim();
				if (token.length() != 0)
				{
					Aspect aspect = findAspectByPath(token);
					if (aspect == null)
					{
						throw ArgumentException.newBadSelect(select, errorData);
					}
					aspects.add(aspect);
				}
			}
		}
	}

	public boolean isNothing()
	{
		return aspects.isEmpty();
	}

	public String toString()
	{
		return aspects.toString();
	}
}
