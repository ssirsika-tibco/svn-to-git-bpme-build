/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

public class AddressFirstLineConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static final String[]	NAMES				= {"Brandon", "Bosworth", "Apple", "Klondike", "Sunmagic",
			"Potato", "Green", "Vicky", "Caines", "Ariel", "Banana", "Sudeley", "Mead", "High", "Low", "Swindon",
			"Kembrey", "Simon", "Joshy", "Howard", "Nachiket", "Caesar", "Swindon", "Flapjack", "Agricola", "Bärenpark",
			"Neom", "Windmill"};

	private static final String[]	ENDINGS				= {"Street", "Walk", "Way", "Avenue", "Road", "Mews",
			"Cottages", "Drive", "Place", "Close", "Boulevard", "Causeway", "Crescent", "Square", "Circle", "Grove"};

	private static final String[]	PRETENTIOUS_FULL	= {"The Olive Grove", "Little Orchard", "The Stables",
			"Excaliber Cottage", "Splendid House", "Cannon HQ", "Casa Bevron", "The White House", "Fawlty Towers"};

	private static final String[]	PRETENTIOUS_FIRST	= {"Morning Dew", "Oak Valley", "Excaliber", "Caines",
			"Peace Valley", "Morgan", "Phillips", "Augustine", "Ashtaputre", "Patton", "Massive"};

	private static final String[]	PRETENTIOUS_SECOND	= {"Chateau", "Estate", "House", "Cottage", "Mansion",
			"Residence", "Manor", "Penthouse", "Dwellings", "Crib"};

	public static Option			optionExcludeNamed	= new Option(OptionType.BOOLEAN, "excludeNamed",
			"Exclude named properties");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionExcludeNamed);
	}

	@Override
	public String conjure()
	{
		// Work out whether to include named properties
		Boolean excludeNamed = (Boolean) getOptionValues().get(optionExcludeNamed);
		if (excludeNamed == null)
		{
			excludeNamed = false;
		}

		// If named properties should be included, do so 20% 
		// of the time
		boolean useNamed = !excludeNamed && Math.random() <= 0.2;

		StringBuilder buf = new StringBuilder();
		if (useNamed)
		{
			// Use a pretentious name, 50/50 chance of a single name
			// or composite name
			if (Math.random() >= 0.5)
			{
				buf.append(ConjuringUtils.randomString(PRETENTIOUS_FULL));
			}
			else
			{
				buf.append(ConjuringUtils.randomString(PRETENTIOUS_FIRST));
				buf.append(" ");
				buf.append(ConjuringUtils.randomString(PRETENTIOUS_SECOND));
			}
		}
		else
		{
			// Use a standard first line
			buf.append(ConjuringUtils.randomInteger(1, 2));
			buf.append(" ");
			buf.append(ConjuringUtils.randomString(NAMES));
			buf.append(" ");
			buf.append(ConjuringUtils.randomString(ENDINGS));
		}
		return buf.toString();
	}
}
