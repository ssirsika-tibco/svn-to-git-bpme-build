/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

public class PersonNameConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static final String[]	FIRST_NAME			= {"Jay", "Lou", "Murph", "Simon", "Howard", "Joshy",
			"Nachiket", "Paddington", "Benjamin", "Vicky", "René", "Pedro", "Martin", "Keith", "Satish", "Michael",
			"Kimble", "Peter", "Tom", "Aaron", "Hannah", "Nestor", "Emily", "Joe", "Gordon", "Cameron", "Bill", "Steve",
			"Jimmy", "Kim", "Chuck", "Saul", "Mike", "Nigel", "David", "Derek", "Wade", "Ogden", "James", "Ernest",
			"Rolo", "Martin", "Lisa", "Adrian", "Dwight", "Jim", "Pam", "Robert", "Hannah", "Clay", "Cisco", "Uwe",
			"Harry", "Ron", "Hermione", "Albus", "Scorpius", "Andreas", "Denmark", "Evan", "Jonas", "Ben", "Joseph",
			"Adam", "Chris", "Freddie", "Roger", "Brian", "John", "Tyrion", "Arya", "Theon", "Daenerys", "Woody",
			"Buzz", "Ham", "Toby", "Jessica", "Alice", "Steven", "Ray", "Jane", "Ed", "Danny", "Clem", "Maurice", "Kim",
			"Joyce", "Eleven", "Dustin", "Lucas", "Nancy", "Jonathan", "Karen", "Will", "Billy", "Bob", "Sam", "Kay",
			"Cocker", "Peanut", "Una", "Dick", "Sal", "Scott", "Basil", "Iqbal", "Nan", "Giuseppe", "Heathcote",
			"Stavros", "Aidy", "Rich", "Cookie", "Rinu", "Amos"};

	public static final String[]	LAST_NAME			= {"Mascis", "Barlow", "Murphy", "Morgan", "Phillips",
			"Augustine", "Ashtaputre", "Bear", "Tenne", "Caines", "Bauer", "Hookstraten", "MacLeish", "Kirkman",
			"Shore", "Wells", "Lozano", "Rhodes", "Macmillan", "Clarke", "Howe", "Gates", "Jobs", "Wozniak", "McGill",
			"Wexler", "Goodman", "Ehrmantraut", "Tufnel", "St. Hubbins", "Smalls", "Watts", "Morrow", "Halliday",
			"Cline", "Haynes", "Patton", "Tweedie", "Price", "Scott", "Shrute", "Halpert", "Beasley", "California",
			"Baker", "Jenson", "Paignton", "Rosenberg", "Potter", "Weasley", "Granger", "Dumbeldore", "Malfoy",
			"Umbridge", "Seyfarth", "Lightman", "Lester", "Doorbell", "Engressia", "Neubauer", "Mullen", "Saelee",
			"Hong", "Cornelius", "Tang", "Mercury", "Taylor", "May", "Deacon", "Lannister", "Stark", "Greyjoy",
			"Targaryen", "Schrödinger", "Clunt", "Toast", "Purchase", "Plough", "Howzer-Black", "Fandango", "Moss",
			"Justice", "Atkins", "Byers", "Hopper", "Wheeler", "Henderson", "Sinclair", "Brenner", "Harrington",
			"Mayfield", "Hargrove", "Newby", "Owens", "Tightneck", "Boo", "Whistle", "Length", "Weerdly", "Commotion",
			"Chestnut", "Watchfair", "Achieve", "Slack", "Race", "Pursuit", "Pilatis", "Killens", "Taal", "Merks",
			"Shortie", "Joseph", "Doll", "Inman", "Vile"};

	private static final String[]	FIRST_NAME_S		= {"जोश्य", "अभिमन्यु", "भीष्‍म", "धृष्टद्युम्न", "कुन्ती",
			"युधिष्ठिर", "नारायण", "श्री हर्षा", "रिनु", "सिमोन्", "नचिकेत", "मर्तिन", "जोबिन जोह्न", "मेर्विन",
			"सतिश"};

	private static final String[]	LAST_NAME_S			= {"आउग्स्तिने", "द्रौपद", "पण्डु", "पवार", "पल्लेति", "जोसेपः",
			"मोर्गान", "अष्टपुत्रे", "पत्तोन", "चेरुपरंबिल्", "फर्नन्दिस्", "गुरुशन्थप्प"};

	public static Option			optionParts			= new ChoiceOption("parts", "Parts",
			Arrays.asList("both", "first", "last"), Arrays.asList("First & Last", "First only", "Last only"));

	public static Option			optionSanskrit		= new Option(OptionType.BOOLEAN, "useSanskit", "Use Sanskrit");

	public static Option			optionMiddleInitial	= new Option(OptionType.BOOLEAN, "includeMiddleInitial",
			"Include middle initial");

	public static Option			optionAlliterate	= new Option(OptionType.BOOLEAN, "alliterate",
			"Always alliterate");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionParts, optionSanskrit, optionMiddleInitial, optionAlliterate);
	}

	@Override
	public String conjure()
	{
		Boolean includeMiddleInitial = (Boolean) getOptionValues().get(optionMiddleInitial);
		boolean middle = includeMiddleInitial != null && includeMiddleInitial;

		// Alliterate
		Boolean alliterateOpt = (Boolean) getOptionValues().get(optionAlliterate);
		boolean alliterate = alliterateOpt != null && alliterateOpt;

		// Sanskit not allowed when middle initial or alliteration selected
		Boolean useSanskit = (Boolean) getOptionValues().get(optionSanskrit);
		boolean us = useSanskit != null && useSanskit && !middle && !alliterate;

		String parts = (String) getOptionValues().get(optionParts);
		if (parts == null)
		{
			parts = "both";
		}

		String[] firsts = us ? FIRST_NAME_S : FIRST_NAME;
		String[] lasts = us ? LAST_NAME_S : LAST_NAME;

		StringBuilder buf = new StringBuilder();
		if ("both".equals(parts) || "first".equals(parts))
		{
			String firstName = ConjuringUtils.randomString(firsts);
			buf.append(firstName);
			if (alliterate && "both".equals(parts))
			{
				String firstChar = firstName.substring(0, 1);
				// Alliteration enables, so filter lastname list down to just
				// those with an initial that matches the first name.
				lasts = Arrays.asList(lasts).stream().filter(s -> s.startsWith(firstChar)).toArray(String[]::new);
			}
		}
		if ("both".equals(parts))

		{
			if (middle)
			{
				buf.append(" ");
				buf.append(alliterate ? buf.substring(0, 1) : ConjuringUtils.randomCapitalLetter());
			}
			buf.append(" ");
		}
		if ("both".equals(parts) || "last".equals(parts))
		{
			buf.append(ConjuringUtils.randomString(lasts));
		}

		String result = buf.toString();
		return result;
	}

	public String getDescription()
	{
		return "From members of the development team, to characters from top TV dramas, this one has 'em all.\n\n"
				+ "Note: Sanskit option is ignored when using middle initials or alliteration.";
	}
}
