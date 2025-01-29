package com.tibco.bpm.auth.openid.logging;

import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.n2.logging.annotations.metadata.N2LFComponent;
import com.tibco.n2.logging.annotations.metadata.definition.N2LFExceptionContextDefinition;
import com.tibco.n2.logging.annotations.metadata.definition.N2LFLogContextDefinition;
import com.tibco.n2.logging.metadata.AbstractLoggingMetaData;
import com.tibco.n2.logging.registration.ProbeBinding;

/**
 * Class representation to facilitate logging information.
 * 
 * @author sajain
 * @since May 5, 2020
 */
@N2LFComponent(id = "AUTH", binding = ProbeBinding.ALL, description = "Authentication Manager", version = "1.0", extension = CloudMetaData.class)
@N2LFExceptionContextDefinition(definitions = AuthExceptionMessages.class)
@N2LFLogContextDefinition(definitions = AuthAuditMessages.class)
public class AuthLoggingInfo extends AbstractLoggingMetaData {

	public static final AuthLoggingInfo instance = new AuthLoggingInfo();

	public AuthLoggingInfo() {
	}

}
