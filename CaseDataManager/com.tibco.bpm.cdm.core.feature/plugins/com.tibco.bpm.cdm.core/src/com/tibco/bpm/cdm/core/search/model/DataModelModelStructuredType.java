package com.tibco.bpm.cdm.core.search.model;

import java.util.List;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.libs.dql.Utils;
import com.tibco.bpm.cdm.libs.dql.exception.DQLException;
import com.tibco.bpm.cdm.libs.dql.exception.UnknownAttributeException;
import com.tibco.bpm.cdm.libs.dql.exception.UnknownDataTypeException;
import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;
import com.tibco.bpm.cdm.libs.dql.model.ModelStructuredType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.StructuredType;

public class DataModelModelStructuredType implements ModelStructuredType
{
	
	private StructuredType structuredType;

	public DataModelModelStructuredType(StructuredType structuredType)
	{
		this.structuredType = structuredType;
	}

	@Override
	public String getName()
	{
		return structuredType.getName();
	}

	@Override
    public ModelAttribute getAttribute(String name)
            throws DQLException
	{

        if (isTopLevelAttribute(name)) {
            Attribute attribute =
            		structuredType.getAttributeByName(name);
            if (attribute == null) {
            	UnknownAttributeException uae = new UnknownAttributeException();
            	uae.setAttributeName(name);
            	uae.setAttributePath(name);
            	uae.setParentName(structuredType.getDataModel().getNamespace() + ":" + structuredType.getLabel());
                throw uae;
            }
            return new DataModelModelAttribute(attribute);
       }

		// If the attribute is not available at the top level, then we need to search within the child types.
		// In such cases the name passed here will be qualified name with child structured type.
       QualifiedChildAttribute childAttribute =
             searchForChildAttribute(structuredType, name);
       
       return childAttribute == null ? null : new DataModelModelAttribute(
               childAttribute.getAttribute(),
               childAttribute.getQualifiedName());
	}

	@Override
	public List<ModelAttribute> getAttributes()
	{
		return structuredType.getAttributes().stream().map(a -> new DataModelModelAttribute(a))
				.collect(Collectors.toList());
	}

    private class QualifiedChildAttribute {

        Attribute attribute;

        String qname;

        public QualifiedChildAttribute(Attribute attribute, String qname) {
            this.attribute = attribute;
            this.qname = qname;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public String getQualifiedName() {
            return this.qname;
        }
    }

    private QualifiedChildAttribute searchForChildAttribute(
            StructuredType structuredType, String name)
            throws DQLException{

        StructuredType currentStructureType = structuredType;

        List<String> segs = Utils.getAttrPathSegments(name);
        
        // Traverse the child type hierarchy until its end
        for (int i = 0; i < segs.size() - 1; i++) {

            String childTypeValue = segs.get(i);
            Attribute childType = currentStructureType.getAttributeByName(childTypeValue);
            if (childType == null) {
                // There is no child structured type for this name.
            	UnknownDataTypeException udte = new UnknownDataTypeException();
            	udte.setAttributeName(childTypeValue);
            	udte.setAttributePath(name);
            	udte.setParentName(currentStructureType.getDataModel().getNamespace() + ":" + currentStructureType.getLabel());
                throw udte;
            }

            if (childType.getTypeObject() instanceof StructuredType) {
                currentStructureType = (StructuredType) childType.getTypeObject();
            }
        }
        
        // This is leaf node
        String childAttrValue = segs.get(segs.size()-1);        
        Attribute childAttr = currentStructureType
                .getAttributeByName(childAttrValue);
        if (childAttr == null) {
            // There is no child attribute for this name.
        	UnknownAttributeException uae = new UnknownAttributeException();
        	uae.setAttributeName(childAttrValue);
        	uae.setAttributePath(name);
        	uae.setParentName(currentStructureType.getDataModel().getNamespace() + ":" + currentStructureType.getLabel());
            throw uae;
        }

        return new QualifiedChildAttribute(childAttr, name);

    }





    private boolean isTopLevelAttribute(String attrName) {

        if (attrName.indexOf(".") == -1) //$NON-NLS-1$
            return true;
        else
            return false;

    }
    

}
