package com.tibco.bpm.cdm.core.logging;

import com.tibco.bpm.logging.cloud.annotations.metadata.CLFComponentAttribute;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.n2.logging.annotations.metadata.N2LFComponent;
import com.tibco.n2.logging.annotations.metadata.definition.N2LFExceptionContextDefinition;
import com.tibco.n2.logging.annotations.metadata.definition.N2LFLogContextDefinition;
import com.tibco.n2.logging.enums.N2LFType;
import com.tibco.n2.logging.metadata.AbstractLoggingMetaData;
import com.tibco.n2.logging.registration.ProbeBinding;

@N2LFComponent(id = "CDM", binding = ProbeBinding.ALL, description = "Case Data Manager", version = "1.0", extension = CloudMetaData.class)
@N2LFExceptionContextDefinition(definitions = CDMExceptionMessages.class)
@N2LFLogContextDefinition(definitions = CDMAuditMessages.class)
public class CDMLoggingInfo extends AbstractLoggingMetaData
{
	public static final CDMLoggingInfo	instance				= new CDMLoggingInfo();

	// length -1 means "unlimited length (although the logging infrastructure may truncate it)", said Keith, 16 Nov 2017
	@CLFComponentAttribute(name = "cdmMetadataSensitive", type = N2LFType.STRING, length = -1, isEncrypted = false, isMultibyteData = true)
	public static final String			CDM_METADATA_SENSITIVE	= "cdmMetadataSensitive";

	@CLFComponentAttribute(name = "timeTaken", type = N2LFType.LONG, length = 50, isEncrypted = false, isMultibyteData = false)
	public static final String			CDM_TIME_TAKEN			= "timeTaken";

	@CLFComponentAttribute(name = "method", type = N2LFType.STRING, length = 50, isEncrypted = false, isMultibyteData = true)
	public static final String			CDM_METHOD				= "method";

}
