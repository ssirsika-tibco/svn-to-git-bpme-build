/*
 * Copyright (c) TIBCO Software Inc 2004, 2024. All rights reserved.
 */

package com.tibco.bpm.cdm.libs.dql.dto;

/**
 *
 *
 * @author spanse
 * @since 30-Apr-2024
 */
public enum ConditionOperator {

    AND("and"), OR("or");

    private String value;

    ConditionOperator(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
