package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.ArrayList;
import java.util.List;

public class SortOrder {
	
	private List<SortColumn> columns= new ArrayList<SortColumn>();
	
	public List<SortColumn> getColumns() {
		return columns;
	}
	
	public void addSortColumn(SortColumn column) {
		columns.add(column);
	}
}
