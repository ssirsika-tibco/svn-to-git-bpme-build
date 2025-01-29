/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth.exception;

import java.util.Map;

import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;

/**
 * Exception used to handle Unauthorized use exception.
 *
 * @author sajain
 * @since Apr 16, 2020
 */
public class UnauthorizedUserException extends AuthBaseException{
    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = 1L;

    public UnauthorizedUserException(ErrorCode code, Throwable throwable, Map<String, String> attributes)
    {
        super(code, throwable, attributes);
    }

    public UnauthorizedUserException(ErrorCode code, Map<String, String> attributes)
    {
        super(code, attributes);
    }
}
