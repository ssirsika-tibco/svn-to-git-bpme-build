package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.List;

/**
 * Represents a container condition which is considered true depending
 * on whether the child conditions it contains are true 
 * @author smorgan
 * @since 2019
 */
public abstract class ConditionGroupDTO extends SearchConditionDTO
{
	protected List< ? extends SearchConditionDTO> children;
	
	protected SortOrder sortOrder;

	public ConditionGroupDTO(List< ? extends SearchConditionDTO> children)
	{
		this.children = children;
	}

	public List<? extends SearchConditionDTO> getChildren()
	{
		return children;
	}
	
	public SortOrder getSortOrder() {
		return sortOrder;
	}
	
	public void setSortOrder(SortOrder sort) {
		sortOrder = sort;
	}
}
