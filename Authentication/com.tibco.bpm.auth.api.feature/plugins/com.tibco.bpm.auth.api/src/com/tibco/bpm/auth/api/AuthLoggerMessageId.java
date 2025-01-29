/**
 * 
 */
package com.tibco.bpm.auth.api;

import com.tibco.bpm.logging.cloud.context.CLFIMessageId;

/**
 * Enumeration provides audit/logging code which should be logged on specific
 * error conditions.
 * 
 * @author ssirsika
 */
public enum AuthLoggerMessageId implements CLFIMessageId {

	/**
	 * Code is used for authentication failure when authentication is successful
	 * with SSO provider using SAML or OpenID, but corresponding user entry is not
	 * available in the local LDAP.
	 */
	AUTH_CHECK_FAIL_00001,
	/**
	 * Code is used for when authentication is failed in the SSO providers. That
	 * means, user can not be authorized by the IDP.
	 */
	AUTH_CHECK_FAIL_00002
}
