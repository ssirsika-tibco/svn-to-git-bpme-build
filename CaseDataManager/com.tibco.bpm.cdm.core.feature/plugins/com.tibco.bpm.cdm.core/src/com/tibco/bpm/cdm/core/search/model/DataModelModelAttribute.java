package com.tibco.bpm.cdm.core.search.model;

import com.tibco.bpm.cdm.libs.dql.model.ModelAbstractType;
import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;
import com.tibco.bpm.cdm.libs.dql.model.ModelBaseType;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.StructuredType;

public class DataModelModelAttribute implements ModelAttribute
{
	private Attribute attribute;
    private String qname;
    private String referenceName;

	public DataModelModelAttribute(Attribute attribute)
	{
		this.attribute = attribute;
        this.qname = attribute.getName();
	}

    public DataModelModelAttribute(Attribute attribute, String qname) {
        this.attribute = attribute;
        this.qname = qname;
    }

	@Override
	public String getName()
	{
		return attribute.getName();
	}
	
	@Override
	public boolean isSearchable()
	{
		return attribute.getIsSearchable();
	}

	@Override
	public boolean isArray()
	{
		return attribute.getIsArray();
	}
	
	// TODO allowed values
	
	private ModelBaseType toModelBaseType(BaseType type)
	{
		ModelBaseType result = null;
		if (type == BaseType.TEXT)
		{
			result = ModelBaseType.TEXT;
		}
		else if (type == BaseType.NUMBER)
		{
			result = ModelBaseType.NUMBER;
		}
		else if (type == BaseType.FIXED_POINT_NUMBER)
		{
			result = ModelBaseType.FIXED_POINT_NUMBER;
		}
		else if (type == BaseType.URI)
		{
			result = ModelBaseType.URI;
		}
		else if (type == BaseType.BOOLEAN)
		{
			result = ModelBaseType.BOOLEAN;
		}
		else if (type == BaseType.DATE)
		{
			result = ModelBaseType.DATE;
		}
		else if (type == BaseType.TIME)
		{
			result = ModelBaseType.TIME;
		}
		else if (type == BaseType.DATE_TIME_TZ)
		{
			result = ModelBaseType.DATE_TIME_TZ;
		}
		return result;
	}

	@Override
	public ModelAbstractType getType()
	{
		ModelAbstractType result = null;
		AbstractType type = attribute.getTypeObject();
		if (type instanceof StructuredType)
		{
			result = new DataModelModelStructuredType((StructuredType) type);
		}
		else if (type instanceof BaseType)
		{
			result = toModelBaseType((BaseType) type);
		}
		return result;
	}

    @Override
    public String getQualifiedName() {
        return qname;
    }

	@Override
	public String getConstraint(String constraintName) {
		return attribute.getConstraintValue(constraintName);
	}

	@Override
	public void setReferenceName(String ref) {
		referenceName = ref;
		
	}

	@Override
	public String getReferenceName() {
		return referenceName;
	}



}
