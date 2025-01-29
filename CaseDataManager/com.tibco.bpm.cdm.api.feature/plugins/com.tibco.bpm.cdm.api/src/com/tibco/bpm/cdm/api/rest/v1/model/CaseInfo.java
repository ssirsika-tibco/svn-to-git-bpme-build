/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


//2 ANNOTATIONS ADDED MANUALLY!:
@JsonPropertyOrder({"caseReference", "casedata", "summary", "metadata"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CaseInfo 
{
    
    private String caseReference = null;
    private String casedata = null;
    private String summary = null;
    private CaseMetadata metadata = null;

    
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
    
    /**
     */
    @JsonProperty("summary")
    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String aValue)
    {
        summary = aValue;
    }
    
    /**
     */
    @JsonProperty("metadata")
    public CaseMetadata getMetadata()
    {
        return metadata;
    }

    public void setMetadata(CaseMetadata aValue)
    {
        metadata = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CaseInfo[");
        sb.append(", caseReference=").append(caseReference);
        sb.append(", casedata=").append(casedata);
        sb.append(", summary=").append(summary);
        sb.append(", metadata=").append(metadata);
        sb.append("]");
        return sb.toString();
    }
}
