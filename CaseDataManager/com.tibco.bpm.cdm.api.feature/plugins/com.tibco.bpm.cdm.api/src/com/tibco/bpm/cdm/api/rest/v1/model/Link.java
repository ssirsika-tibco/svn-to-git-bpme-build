/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;


public class Link 
{
    
    private String name = null;
    private String caseReference = null;

    
    /**
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
     */
    @JsonProperty("caseReference")
    public String getCaseReference()
    {
        return caseReference;
    }

    public void setCaseReference(String aValue)
    {
        caseReference = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Link[");
        sb.append(", name=").append(name);
        sb.append(", caseReference=").append(caseReference);
        sb.append("]");
        return sb.toString();
    }
}
