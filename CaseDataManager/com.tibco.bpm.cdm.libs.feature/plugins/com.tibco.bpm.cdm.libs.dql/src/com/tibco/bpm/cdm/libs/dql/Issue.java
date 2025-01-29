package com.tibco.bpm.cdm.libs.dql;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an issue arising during validation of a DQL statement
 * @author smorgan
 * @since 2019
 */
public class Issue
{
	private static Pattern	TOKEN_PAT	= Pattern.compile("\\{.+?\\}");

	private IssueCode		code;

	private String			message;

	public Issue(IssueCode data)
	{
		this(data, null);
	}

	public Issue(IssueCode code, String[] params)
	{
		this.code = code;
		message = bakeMessage(code.getMessage(), params);
	}

	private static String bakeMessage(String messageTemplate, String[] params)
	{
		Map<String, String> map = null;

		// Build a map of name->value for parameters
		if (params != null && params.length != 0)
		{
			map = new HashMap<String, String>();
			for (int i = 0; i + 1 < params.length; i += 2)
			{
				map.put(params[i], params[i + 1]);
			}
		}

		// Replace {tokens} in the template with corresponding called-supplied parameter values
		Matcher m = TOKEN_PAT.matcher(messageTemplate);
		int pos = 0;
		StringBuilder buf = new StringBuilder();
		while (m.find())
		{
			String token = messageTemplate.substring(m.start() + 1, m.end() - 1);
			if (map == null || !map.containsKey(token))
			{
				// Unrecognised token, so leave as-is
				buf.append(messageTemplate.substring(pos, m.end()));
			}
			else
			{
				buf.append(messageTemplate.substring(pos, m.start()));
				buf.append(map.get(token));
			}
			pos = m.end();
		}

		buf.append(messageTemplate.substring(pos));
		return buf.toString();
	}

	public String getMessage()
	{
		return message;
	}

	public IssueCode getCode()
	{
		return code;
	}

	public String toString()
	{
		return code + ": " + message;
	}
}