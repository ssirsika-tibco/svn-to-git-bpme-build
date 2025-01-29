/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;


public class CasesPutRequestBodyItem 
{
    
    private String caseReference = null;
    private String casedata = null;

    
    /**
     * In the form {id}-{namespace}.{case type name}-{application major version}-{version}\ne.g. 101-com.example.ordermodel-Order-1-7\n
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
        sb.append("CasesPutRequestBodyItem[");
        sb.append(", caseReference=").append(caseReference);
        sb.append(", casedata=").append(casedata);
        sb.append("]");
        return sb.toString();
    }
}
