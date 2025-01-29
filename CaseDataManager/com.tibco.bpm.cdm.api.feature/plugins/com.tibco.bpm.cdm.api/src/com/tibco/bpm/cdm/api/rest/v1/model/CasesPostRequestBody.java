/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CasesPostRequestBody 
{
    
    private String caseType = null;
    private Integer applicationMajorVersion = null;
    private List<String> casedata = new ArrayList<String>() ;

    
    /**
     * Type is expressed in the form \{namespace}.\{case type name}.\ne.g. com.example.ordermodel.Order\n
     */
    @JsonProperty("caseType")
    public String getCaseType()
    {
        return caseType;
    }

    public void setCaseType(String aValue)
    {
        caseType = aValue;
    }
    
    /**
     * Major version of the application in which the type is defined\n
     */
    @JsonProperty("applicationMajorVersion")
    public Integer getApplicationMajorVersion()
    {
        return applicationMajorVersion;
    }

    public void setApplicationMajorVersion(Integer aValue)
    {
        applicationMajorVersion = aValue;
    }
    
    /**
     */
    @JsonProperty("casedata")
    public List<String> getCasedata()
    {
        return casedata;
    }

    public void setCasedata(List<String> aValue)
    {
        casedata = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CasesPostRequestBody[");
        sb.append(", caseType=").append(caseType);
        sb.append(", applicationMajorVersion=").append(applicationMajorVersion);
        sb.append(", casedata=").append(casedata);
        sb.append("]");
        return sb.toString();
    }
}
