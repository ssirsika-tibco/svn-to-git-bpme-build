/**
 * 
 */
package com.tibco.bpm.auth.api;

/**
 * @author ssirsika
 * Helper interface used to set the context information
 */
public interface ContextHelper {

	
	/**
	 * Set the default target URL for authentication success
	 * @param defaultTargetUrl
	 */
	public void setDefaultTargetUrl(String defaultTargetUrl);

	/**
	 * Set the authentication listener which will be notified about the success or failure.
	 * @param authenticationListner
	 */
	public void setAuthenticationListener(BPMAuthenticationListener authenticationListner);
}
