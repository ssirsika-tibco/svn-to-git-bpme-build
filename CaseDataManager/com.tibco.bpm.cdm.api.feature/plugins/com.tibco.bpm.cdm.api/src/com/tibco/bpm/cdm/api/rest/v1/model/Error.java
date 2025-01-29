/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import java.util.*;
import com.tibco.bpm.cdm.api.rest.v1.model.ContextAttribute;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Provides details when an error occurs\n
 *
 * @GENERATED this is generated code; do not edit.
 */
public class Error 
{
    
    private String errorMsg = null;
    private String errorCode = null;
    private String stackTrace = null;
    private List<ContextAttribute> contextAttributes = new ArrayList<ContextAttribute>() ;

    
    /**
     * Verbose error message
     */
    @JsonProperty("errorMsg")
    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String aValue)
    {
        errorMsg = aValue;
    }
    
    /**
     */
    @JsonProperty("errorCode")
    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(String aValue)
    {
        errorCode = aValue;
    }
    
    /**
     * Added if available
     */
    @JsonProperty("stackTrace")
    public String getStackTrace()
    {
        return stackTrace;
    }

    public void setStackTrace(String aValue)
    {
        stackTrace = aValue;
    }
    
    /**
     * Error Attributes
     */
    @JsonProperty("contextAttributes")
    public List<ContextAttribute> getContextAttributes()
    {
        return contextAttributes;
    }

    public void setContextAttributes(List<ContextAttribute> aValue)
    {
        contextAttributes = aValue;
    }
    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Error[");
        sb.append(", errorMsg=").append(errorMsg);
        sb.append(", errorCode=").append(errorCode);
        sb.append(", stackTrace=").append(stackTrace);
        sb.append(", contextAttributes=").append(contextAttributes);
        sb.append("]");
        return sb.toString();
    }
}
