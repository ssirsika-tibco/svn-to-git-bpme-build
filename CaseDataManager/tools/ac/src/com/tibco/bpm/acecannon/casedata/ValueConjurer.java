/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.List;
import java.util.Map;

public interface ValueConjurer<T>
{
	public static class Option
	{
		private OptionType	type;

		private String		name;

		private String		label;

		public Option(OptionType type, String name, String label)
		{
			this.type = type;
			this.name = name;
			this.label = label;
		}

		public OptionType getType()
		{
			return type;
		}

		public String getName()
		{
			return name;
		}

		public String getLabel()
		{
			return label;
		}

		public String toString()
		{
			return "Option(" + name + ": " + type + ")";
		}
	}

	public static class ChoiceOption extends Option
	{
		private List<String>	optionNames;

		private List<String>	optionLabels;

		public ChoiceOption(String name, String label, List<String> optionNames, List<String> optionLabels)
		{
			super(OptionType.SINGLE_CHOICE, name, label);
			this.optionNames = optionNames;
			this.optionLabels = optionLabels;
		}

		public List<String> getOptionNames()
		{
			return optionNames;
		}

		public List<String> getOptionLabels()
		{
			return optionLabels;
		}
	}

	public static enum OptionType
	{
		BOOLEAN,
		INTEGER,
		BIG_DECIMAL,
		TEXT,
		TEXT_LIST,
		SINGLE_CHOICE
	}

	public T conjure();

	public List<T> conjureMany(int quantity);

	public List<Option> getOptions();

	public Map<Option, Object> getOptionValues();

	public String getDescription();
}
