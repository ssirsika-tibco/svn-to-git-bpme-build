package com.tibco.bpm.acecannon.casedata;

public class EmailAddressConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static final String[]	FIRST_NAME	= {"Jay", "Lou", "Murph", "Simon", "Howard", "Joshy", "Nachiket",
			"Paddington", "Benjamin", "Vicky", "René", "Pedro", "Martin", "Keith", "Satish", "Michael", "Kimble",
			"Peter", "Tom", "Aaron", "Hannah", "Nestor", "Emily", "Joe", "Gordon", "Cameron", "Bill", "Steve", "Jimmy",
			"Kim", "Chuck", "Saul", "Mike", "Nigel", "David", "Derek", "Wade", "Ogden", "James", "Ernest", "Rolo",
			"Martin", "Lisa", "Adrian", "Michael", "Dwight", "Jim", "Pam", "Robert", "Hannah", "Clay", "Cisco", "Uwe",
			"Harry", "Ron", "Hermione", "Albus", "Scorpius", "Andreas", "Denmark", "Evan", "Jonas", "Ben", "Joseph",
			"Adam", "Chris", "Freddie", "Roger", "Brian", "John"};

	private static final String[]	DOMAINS		= {"tibco", "vistaequitypartners", "windmillhillbusinesspark",
			"example", "casecannon", "canondecaso", "fallkanone", "casocannone", "affairecanon", "gevalkanon",
			"kazokanono", "dozadoze", "tapaustykki"};

	private static final String[]	TLDS		= {"com", "org", "co.uk", "biz", "gov", "edu", "ac.uk"};

	@Override
	public String conjure()
	{
		return String.format("%s@%s.%s", ConjuringUtils.randomString(FIRST_NAME).toLowerCase(),
				ConjuringUtils.randomString(DOMAINS), ConjuringUtils.randomString(TLDS));
	}

	@Override
	public String getDescription()
	{
		return "Makes an email address (e.g. bob@casecannon.org)";
	}

}
