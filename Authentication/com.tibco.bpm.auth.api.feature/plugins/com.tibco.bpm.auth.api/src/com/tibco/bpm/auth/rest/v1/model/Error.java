/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.auth.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * The definition of an error, providing a suitable message, error code and context information.
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
     * The textual error message.
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
     * The following are the possible error codes in the Client Configuration service (note that the description shown is not part of the error code):\n- SS_UNKNOWN_ERROR - Internal unexpected error.\n
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
     * Stack trace details (only provided in a debug environment).
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
     * A name/value pair that provides contextual information about the Error.
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
