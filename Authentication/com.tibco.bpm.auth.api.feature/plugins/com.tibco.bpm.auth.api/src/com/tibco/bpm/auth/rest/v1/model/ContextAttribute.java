/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.auth.rest.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * A name/value pair, used within Error, that provides contextual information about the Error.
 *
 * @GENERATED this is generated code; do not edit.
 */
public class ContextAttribute 
{
    
    private String name = null;
    private String value = null;

    
    /**
     * The name of the context attribute.
     */
    @JsonProperty("name")
    public String getName()
    {
        return name;
    }

    public void setName(String aValue)
    {
        name = aValue;
    }
    
    /**
     * The value of the context attribute.
     */
    @JsonProperty("value")
    public String getValue()
    {
        return value;
    }

    public void setValue(String aValue)
    {
        value = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ContextAttribute[");
        sb.append(", name=").append(name);
        sb.append(", value=").append(value);
        sb.append("]");
        return sb.toString();
    }
}
