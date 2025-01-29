package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

//TODO This is returning a String, which the caller needs to know is to be treated as an object
public class WebLinkConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static final ObjectMapper	om					= new ObjectMapper();

	private static final String[]		PROTOCOLS			= {"http", "https"};

	private static final String[]		TLDS				= {"com", "org", "co.uk", "biz", "gov", "edu", "ac.uk"};

	private static final String[]		DOMAINS				= {"tibco", "vistaequitypartners",
			"windmillhillbusinesspark", "example", "casecannon", "canondecaso", "fallkanone", "casocannone",
			"affairecanon", "gevalkanon", "kazokanono", "dozadoze", "tapaustykki"};

	private static final String[]		PATHS				= {"index.html", "index.aspx", "hello.txt", "jumbo.jpg",
			"legal/contract.html", "archived/april2017.txt", "archived/may2017", "archived/june2017",
			"cdn/files_0001/DEADBEEF000100020003/1234/5678/9012/3456/7890/1234/5678/9012/3456/7890/doc.html"};

	private static final String[]		LINK_TEXTS			= {"The report", "The home page", "Terms & conditions",
			"Contract", "Explanatory notes", "Ordering portal", "Further info", "User guide", "Documentation",
			"A rather interesting web site that you are strongly encouraged to check out",
			"A web site that is essential reading prior to working on this case",
			"Documentation, notes, discussions and other content relevant to any person working on this case in any capacity whatsoever",
			"A <i>naughty</i> string to check for script-injection vulnerabilities. <script>alert('This app got more holes than Swiss cheese!');</script>"};

	public static Option				optionOmitLinkText	= new Option(OptionType.BOOLEAN, "omitLinkText",
			"Omit linkText");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionOmitLinkText);
	}

	@Override
	public String conjure()
	{
		JsonNodeFactory fac = JsonNodeFactory.instance;
		StringBuilder buf = new StringBuilder();
		buf.append(ConjuringUtils.randomString(PROTOCOLS));
		buf.append("://");
		if (Math.random() >= 0.3)
		{
			buf.append("www.");
		}
		buf.append(ConjuringUtils.randomString(DOMAINS));
		buf.append(".");
		buf.append(ConjuringUtils.randomString(TLDS));
		if (Math.random() >= 0.8)
		{
			buf.append("/");
			buf.append(ConjuringUtils.randomString(PATHS));
		}
		ObjectNode resultNode = fac.objectNode();
		resultNode.set("address", fac.textNode(buf.toString()));
		Boolean omitLinkText = (Boolean) getOptionValues().get(optionOmitLinkText);
		if (!Boolean.TRUE.equals(omitLinkText))
		{
			resultNode.set("linkText", fac.textNode(ConjuringUtils.randomString(LINK_TEXTS)));
		}
		String json = null;

		try
		{
			json = om.writeValueAsString(resultNode);
		}
		catch (JsonProcessingException e)
		{
			json = "Error: " + e.getMessage();
		}
		return json;
	}

	@Override
	public String getDescription()
	{
		return "Makes a JSON object suitable for attributes of the base:WebLink type.  These have 'address' and, optionally, 'linkText' properties."
				+ "\n\nNote: If this generator is selected for non-object attributes, the value will be written as a string, "
				+ "rather than an object.";
	}
}