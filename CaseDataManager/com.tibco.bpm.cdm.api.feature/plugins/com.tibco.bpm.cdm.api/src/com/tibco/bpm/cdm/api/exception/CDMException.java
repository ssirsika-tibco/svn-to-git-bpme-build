package com.tibco.bpm.cdm.api.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base type for CDM exceptions.
 * Features include:
 * - Error specification (code, message with {tokens}, HTTP equivalent status code) via ErrorData class
 * - Storage of arbitrary metadata (name/value with boolean isSensitive flag) to enable propagation of context
 *   up to a level where it can be appropriately logged.
 *
 * @author smorgan
 * @since 2019
 */
public abstract class CDMException extends Exception
{
	public static class MetadataEntry
	{
		private String	name;

		private String	value;

		private boolean	isSensitive;

		public MetadataEntry(String name, String value, boolean isSensitive)
		{
			this.name = name;
			this.value = value;
			this.isSensitive = isSensitive;
		}

		public String getName()
		{
			return name;
		}

		public String getValue()
		{
			return value;
		}

		public boolean getIsSensitive()
		{
			return isSensitive;
		}

		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("MetadataEntry [name=");
			builder.append(name);
			builder.append(", value=");
			builder.append(value);
			builder.append(", isSensitive=");
			builder.append(isSensitive);
			builder.append("]");
			return builder.toString();
		}
	}

	private static final long	serialVersionUID	= 1L;

	private ErrorData			errorData;

	private Map<String, String>	attributes			= new HashMap<>();

	private List<MetadataEntry>	metadataEntries		= new ArrayList<>();

	private static Pattern		TOKEN_PAT			= Pattern.compile("\\{.+?\\}");

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

	protected CDMException(ErrorData errorData)
	{
		super(errorData.getMessageTemplate());
		this.errorData = errorData;
	}

	protected CDMException(ErrorData errorData, String[] params)
	{
		super(bakeMessage(errorData.getMessageTemplate(), params));
		this.errorData = errorData;
		if (null != params) for (int i = 0; i + 1 < params.length; i += 2)
		{
			attributes.put(params[i], params[i + 1]);
		}
	}

	protected CDMException(ErrorData errorData, Throwable cause)
	{
		super(errorData.getMessageTemplate(), cause);
		this.errorData = errorData;
	}

	protected CDMException(ErrorData errorData, Throwable cause, String[] params)
	{
		super(bakeMessage(errorData.getMessageTemplate(), params), cause);
		this.errorData = errorData;
		for (int i = 0; i + 1 < params.length; i += 2)
		{
			attributes.put(params[i], params[i + 1]);
		}
	}

	protected void setAttribute(String name, String value)
	{
		attributes.put(name, value);
	}

	public Map<String, String> getAttributes()
	{
		return attributes;
	}

	public ErrorData getErrorData()
	{
		return errorData;
	}

	public String getCode()
	{
		return errorData.getCode();
	}

	public void addMetadata(String name, String value, boolean isSensitive)
	{
		MetadataEntry metadataEntry = new MetadataEntry(name, value, isSensitive);
		metadataEntries.add(metadataEntry);
	}

	public List<MetadataEntry> getMetadataEntries()
	{
		return metadataEntries;
	}

	public List<MetadataEntry> getSensitiveMetadataEntries()
	{
		return metadataEntries.stream().filter(me -> me.getIsSensitive()).collect(Collectors.toList());
	}

	public List<MetadataEntry> getNonSensitiveMetadataEntries()
	{
		return metadataEntries.stream().filter(me -> !me.getIsSensitive()).collect(Collectors.toList());
	}

	/**
	 * If true, then attempting the operation that caused this exception again may succeed (such
	 * as problems caused by temporary database problems).
	 * @return
	 */
	public boolean isRetryable()
	{
		return false;
	}
}
