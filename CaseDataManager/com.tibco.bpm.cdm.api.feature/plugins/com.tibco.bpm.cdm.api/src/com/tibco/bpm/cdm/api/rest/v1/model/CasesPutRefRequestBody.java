/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;


public class CasesPutRefRequestBody 
{
    
    private String casedata = null;

    
    /**
     */
    @JsonProperty("casedata")
    public String getCasedata()
    {
        return casedata;
    }

    public void setCasedata(String aValue)
    {
        casedata = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CasesPutRefRequestBody[");
        sb.append(", casedata=").append(casedata);
        sb.append("]");
        return sb.toString();
    }
}
