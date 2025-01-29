/**	
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.api;

/**
 * 
 * Implementor of the interface will provide extra methods to verify the authentication information.
 * @author ssirsika
 *
 */
public interface BPMAuthVerifier {

	/**
	 * Verify the pass JWT Open ID token.
	 * @param jwtToken JWT Open ID token
	 * @param offline pass <code>true</code> in verification should not consult the IDP's public key
	 * 		  otherwise pass as <code>false</code> which will just verify token offline without contacting IDP
	 * @return <code>true</code> if verification is successful otherwise <code>false</code>
	 * @throws Exception
	 */
	public boolean verify(String jwtToken, boolean offline) throws Exception;
	
	/**
	 * Verify the pass JWT Open ID token and return the 'user key' (key name as defined in OpenId shared resource or default) value from the claims object.
	 * 'User key' value will be used to identify the user in BPME.
	 * @param jwtToken JWT Open ID token
	 * @param offline pass <code>true</code> in verification should not consult the IDP's public key
	 * 		  otherwise pass as <code>false</code> which will just verify token offline without contacting IDP
	 * @return {@link String} 'user key' value from claims.
	 * @throws Exception
	 */
	public String getVerifiedUserKeyValue(String jwtToken, boolean offline) throws Exception ;
}
