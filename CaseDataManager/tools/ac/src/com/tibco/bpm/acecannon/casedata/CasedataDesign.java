package com.tibco.bpm.acecannon.casedata;

import com.tibco.bpm.da.dm.api.StructuredType;

public class CasedataDesign
{
	private StructuredType	type;

	private ConjuringModel	model;

	public CasedataDesign(StructuredType type, ConjuringModel model)
	{
		this.type = type;
		this.model = model;
	}

	public StructuredType getType()
	{
		return type;
	}

	public ConjuringModel getModel()
	{
		return model;
	}

	public CaseProvider makeProvider() throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
			return new ConjuringCaseProvider(model, type);
	}

	public String toString()
	{
		String name = type != null ? type.getName() : null;
		if (name != null && name.length() > 25)
		{
			name = name.substring(0, 22) + "...";
		}
		return name;
	}
}
