/*
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */

package com.tibco.bpm.auth;

/**
 * Java representation for BPM session.
 *
 * @author sajain
 * @since Mar 31, 2020
 */
public class BPMSession {
    /**
     * Session last accessed time.
     */
    private long sessionLastAccessedTime;
    
    /**
     * Session time out.
     */
    private long sessionTimeOut;
    
    /**
     * @param sessionLastAccessedTime
     * @param sessionTimeOut
     */
    public BPMSession(long sessionLastAccessedTime, long sessionTimeOut) {
        this.sessionLastAccessedTime = sessionLastAccessedTime;
        this.sessionTimeOut = sessionTimeOut;
    }

    /**
     * @return the sessionLastAccessedTime
     */
    public long getSessionLastAccessedTime() {
        return sessionLastAccessedTime;
    }

    /**
     * @param sessionLastAccessedTime the sessionLastAccessedTime to set
     */
    public void setSessionLastAccessedTime(long sessionLastAccessedTime) {
        this.sessionLastAccessedTime = sessionLastAccessedTime;
    }

    /**
     * @return the sessionTimeOut
     */
    public long getSessionTimeOut() {
        return sessionTimeOut;
    }

    /**
     * @param sessionTimeOut the sessionTimeOut to set
     */
    public void setSessionTimeOut(long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }
}
