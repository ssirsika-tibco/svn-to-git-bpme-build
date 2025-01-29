package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a condition that is only satisfied when each of its child
 * conditions are satisfied
 * 
 * @author spanse
 * @since April 2024
 */
public class AndOrGroupDTO extends ConditionGroupDTO
{
	public AndOrGroupDTO(List<? extends SearchConditionDTO> children)
	{
		super(children);
	}

	@Override
    public String toString()
	{
        return String.format(" (%s)",
				children.stream().map(SearchConditionDTO::toString).collect(Collectors.joining(", ")));
	}
}
