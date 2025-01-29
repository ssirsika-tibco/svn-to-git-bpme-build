/*
 * Copyright (c) TIBCO Software Inc 2004, 2024. All rights reserved.
 */

package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 * @author spanse
 * @since 29-Apr-2024
 */
public class OrGroupDTO extends ConditionGroupDTO {

    public OrGroupDTO(List<? extends SearchConditionDTO> children)
    {
        super(children);
    }

    @Override
    public String toString()
    {
        return String.format("OR (%s)",
                children.stream().map(SearchConditionDTO::toString).collect(Collectors.joining(", ")));
    }
}
