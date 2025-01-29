package com.tibco.bpm.cdm.libs.dql.dto;

import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;

public class SortColumn {
	
	boolean isDescending = false;
	private String name;
	private ModelAttribute attr;
	
	public SortColumn(ModelAttribute attribute) {
		attr = attribute;
	}
	
	public SortColumn(ModelAttribute attribute, boolean desc) {
		attr = attribute;;
		isDescending = desc;
	}

	public ModelAttribute getAttribute() {
		return attr;
	}

	public void setAttribute(ModelAttribute attribute) {
		attr = attribute;
	}

	public boolean isDescending() {
		return isDescending;
	}

	public void setDescendingOrder() {
		isDescending = true;
	}

}
