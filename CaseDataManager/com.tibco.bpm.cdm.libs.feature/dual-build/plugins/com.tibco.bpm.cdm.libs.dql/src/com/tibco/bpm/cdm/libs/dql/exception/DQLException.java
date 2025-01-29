package com.tibco.bpm.cdm.libs.dql.exception;

public class DQLException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String attributePath;
    
    private String attributeName;
    
    private String parentName;
    
    private String dataField;

	public String getAttributePath() {
		return attributePath;
	}

	public void setAttributePath(String attributePath) {
		this.attributePath = attributePath;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String name) {
		this.attributeName = name;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public String getDataField() {
		return dataField;
	}

	public void setDataField(String dataField) {
		this.dataField = dataField;
	}	
}
