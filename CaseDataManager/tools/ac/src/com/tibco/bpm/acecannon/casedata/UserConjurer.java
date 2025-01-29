package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

// TODO This is returning a String, which the caller needs to know is to be treated as an object
public class UserConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static Option				optionOmitFirstAndLast	= new Option(OptionType.BOOLEAN, "omitFirstAndLast",
			"Omit first/lastName");

	public static Option				optionOmitType			= new Option(OptionType.BOOLEAN, "omitType",
			"Omit \"type\": \"base:User\"");

	private static final ObjectMapper	om						= new ObjectMapper();

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionOmitFirstAndLast, optionOmitType);
	}

	@Override
	public String conjure()
	{
		JsonNodeFactory fac = JsonNodeFactory.instance;
		ObjectNode obj = fac.objectNode();
		// TODO make this cope with 28 digits
		String id = "" + ConjuringUtils.randomInteger(1, 18);
		String firstName = ConjuringUtils.randomString(PersonNameConjurer.FIRST_NAME);
		String lastName = ConjuringUtils.randomString(PersonNameConjurer.LAST_NAME);
		StringBuilder email = new StringBuilder();
		email.append(firstName.toLowerCase());
		email.append(".");
		email.append(lastName.toLowerCase().replaceAll(" ", ""));
		email.append("@tibco.com");
		obj.set("id", fac.textNode(id));
		obj.set("name", fac.textNode(firstName + " " + lastName));
		Boolean omitFirstAndLast = (Boolean) getOptionValues().get(optionOmitFirstAndLast);
		if (!Boolean.TRUE.equals(omitFirstAndLast))
		{
			obj.set("firstName", fac.textNode(firstName));
			obj.set("lastName", fac.textNode(lastName));
		}
		obj.set("email", fac.textNode(email.toString()));
		Boolean omitType = (Boolean) getOptionValues().get(optionOmitType);
		if (!Boolean.TRUE.equals(omitType))
		{
			obj.set("type", fac.textNode("base:User"));
		}
		String result = "";
		try
		{
			result = om.writeValueAsString(obj);
		}
		catch (JsonProcessingException e)
		{
			result = "ERROR:" + e.getMessage();
		}
		return result;
	}

	@Override
	public String getDescription()
	{
		return "Makes a JSON object suitable for attributes of the base:User type.  These have 'id', 'name', 'email' and, optionally, firstName and lastName properties."
				+ "\n\nNote: If this generator is selected for non-object attributes, the value will be written as a string, "
				+ "rather than an object.";
	}
}
