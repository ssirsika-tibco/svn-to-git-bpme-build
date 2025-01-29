/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import com.tibco.bpm.cdm.api.rest.v1.model.CasesPutRequestBodyItem;
import java.util.ArrayList;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CasesPutRequestBody extends ArrayList<CasesPutRequestBodyItem>
{
    

    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CasesPutRequestBody[");
        sb.append("]");
        return sb.toString();
    }
}
