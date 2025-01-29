/*
 * Copyright (c) TIBCO Software Inc 2004 - 2016. All rights reserved.
 *
 */

package com.tibco.bpm.cdm.api.rest.v1.model;

import java.util.ArrayList;
import java.util.*;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfo;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TypesGetResponseBody extends ArrayList<TypeInfo>
{
    

    

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TypesGetResponseBody[");
        sb.append("]");
        return sb.toString();
    }
}
