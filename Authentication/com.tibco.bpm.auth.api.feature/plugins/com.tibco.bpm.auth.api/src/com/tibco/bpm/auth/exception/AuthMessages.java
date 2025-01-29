/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2018 TIBCO Software Inc
*/
package com.tibco.bpm.auth.exception;

/*
 * Message codes and HTTP status for all the Auth messages returned via REST API.
 * Each message needs a matching entry in auth_messages.properties as this loads 
 * the textual message returned auth_messages.properties can be used by a client 
 * to allow translations with parameter substitution
 */

public class AuthMessages
{
	public enum ErrorCode
	{
		// @formatter:off
        // All these codes should have message in auth_messages.properties
        AUTH_AUTHERROR(403),
	    AUTH_UNKNOWN_ERROR(500),
		AUTH_SSO_UNAVAILABLE(500),
		AUTH_INVALID_AUTHENTICATION(403),;
		// @formatter:on

		private final int httpStatus;

		ErrorCode(int httpStatus)
		{
			this.httpStatus = httpStatus;
		}

		public int getHttpStatus()
		{
			return httpStatus;
		}
	}
}
