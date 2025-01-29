/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2017 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NaughtyStringConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{

	private static final String	RESOURCE_BLNS	= "blns.txt";

	private static final String	RESOURCE_CC		= "cc-naughty.txt";

	private List<String>		stringsBLNS;

	private List<String>		stringsCC;

	private List<String>		stringsBoth;

	public static Option		optionBLNS		= new Option(OptionType.BOOLEAN, "blns", "Use blns.txt");

	public static Option		optionCC		= new Option(OptionType.BOOLEAN, "cc", "Use cc-naughty.txt");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionBLNS, optionCC);
	}

	protected String readInputStreamToString(InputStream inputStream) throws IOException
	{
		char[] buffer = new char[1024];
		StringBuilder buf = new StringBuilder();
		Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		int count = 0;
		while (count >= 0)
		{
			count = in.read(buffer, 0, buffer.length);
			if (count > 0)
			{
				buf.append(buffer, 0, count);
			}
		}
		return buf.toString();
	}

	protected String readResource(String resourcePath) throws IOException
	{
		InputStream is = getClass().getResourceAsStream(resourcePath);
		return readInputStreamToString(is);
	}

	private List<String> readStringFile(String resource) throws IOException
	{
		List<String> result = new ArrayList<>();
		String s = readResource(resource);
		String[] lines = s.split("[\r\n]+");
		for (String line : lines)
		{
			if (!line.startsWith("#") && line.length() != 0)
			{
				result.add(line);
			}
		}
		return result;
	}

	public NaughtyStringConjurer() throws IOException
	{
		stringsBLNS = readStringFile(RESOURCE_BLNS);
		stringsCC = readStringFile(RESOURCE_CC);
		stringsBoth = new ArrayList<>();
		stringsBoth.addAll(stringsBLNS);
		stringsBoth.addAll(stringsCC);
	}

	@Override
	public String conjure()
	{
		List<String> source = null;
		Boolean useBLNS = (Boolean) getOptionValues().get(optionBLNS);
		Boolean useCC = (Boolean) getOptionValues().get(optionCC);
		if (useBLNS != null && useBLNS && useCC != null && useCC)
		{
			source = stringsBoth;
		}
		else if (useBLNS != null && useBLNS)
		{
			source = stringsBLNS;
		}
		else if (useCC != null && useCC)
		{
			source = stringsCC;
		}

		String result = source == null ? "(Select some strings!)" : ConjuringUtils.randomString(source);
		return result;
	}

	@Override
	public String getDescription()
	{
		return "Naughty Strings are strings which  have a high probability of causing issues when used as user-input data.\n\n"
				+ "This generator has two lists of strings available:\n\n"
				+ "- blns.txt : The Big List of Naughty Strings is the collective work of the Naughty String community. "
				+ "See: https://github.com/minimaxir/big-list-of-naughty-strings\n(Using blns.txt from 15 Nov 2018)\n\n"
				+ "- cc-naughty.txt : An internally-produced set of evil character sequences from content creators including Howard 'Unicode troublemaker' Phillips."
				+ "\n\nNaughty string counts: blns.txt: " + stringsBLNS.size() + ", cc-naughty.txt: "
				+ stringsCC.size();
	}

}
