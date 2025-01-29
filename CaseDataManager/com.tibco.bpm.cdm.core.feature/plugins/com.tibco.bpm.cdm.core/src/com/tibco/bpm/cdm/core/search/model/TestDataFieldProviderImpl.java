package com.tibco.bpm.cdm.core.search.model;

import com.tibco.bpm.cdm.libs.dql.exception.DQLException;
import com.tibco.bpm.cdm.libs.dql.model.DataFieldProvider;
import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;

public class TestDataFieldProviderImpl implements DataFieldProvider {

	DataModelModelStructuredType type;
	
	public TestDataFieldProviderImpl(DataModelModelStructuredType type) {
		this.type = type;
	}
	@Override
	public ModelAttribute getDataField(String parameterPath) {
		
		//System.out.println("getDataField: " + parameterPath);
		if (parameterPath == null) {
			return null;
		}
		if (parameterPath.startsWith("data.")) {
			String param = parameterPath.substring(5);
			if (param.indexOf(".") == -1) {
				try {
					ModelAttribute attribute = type.getAttribute(param);
					//System.out.println("getDataField: param: " + param + "attr: " + attribute.getName() + " : " + attribute.getQualifiedName());
					return attribute;
				} catch (DQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				int index = param.indexOf(".");
				param = param.substring(index+1);
				try {
					ModelAttribute attribute = type.getAttribute(param);
					//System.out.println("getDataField: param: " + param + " attr: " + attribute.getName() + " : " + attribute.getQualifiedName());
					return attribute;
				} catch (DQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
