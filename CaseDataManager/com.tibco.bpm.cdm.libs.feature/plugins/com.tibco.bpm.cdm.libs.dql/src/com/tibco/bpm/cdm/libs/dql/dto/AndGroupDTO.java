package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a condition that is only satisfied when each of its child conditions are satisfied
 * @author smorgan
 * @since 2019
 */
public class AndGroupDTO extends ConditionGroupDTO
{
	public AndGroupDTO(List<? extends SearchConditionDTO> children)
	{
		super(children);
	}

	public String toString()
	{
		return String.format("AND(%s)",
				children.stream().map(SearchConditionDTO::toString).collect(Collectors.joining(", ")));
	}
}
