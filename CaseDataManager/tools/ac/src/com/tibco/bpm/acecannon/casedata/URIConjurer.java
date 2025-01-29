package com.tibco.bpm.acecannon.casedata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

//TODO This is returning a String, which the caller needs to know is to be treated as an object
public class URIConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
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

	@Override
	public String conjure()
	{
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
		return buf.toString();
	}

	@Override
	public String getDescription()
	{
		return "Makes a URI. Only supports absolute web URLs.";
	}
}