package com.tibco.bpm.cdm.core.rest.v1;

import java.util.List;

import javax.ws.rs.core.Response;

import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.rest.v1.api.TypesService;
import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfo;
import com.tibco.bpm.cdm.core.AbstractService;
import com.tibco.bpm.cdm.core.TypesPrivateService;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Implementation of the TypesService interface for the .../types REST resource
 * @author smorgan
 * @since 2019
 */
public class TypesServiceImpl extends AbstractService implements TypesService
{
	static CLFClassContext		logCtx						= CloudLoggingFramework.init(TypesServiceImpl.class,
			CDMLoggingInfo.instance);

	private TypesPrivateService typesServiceLocal;
	
	
	@Override
	public Response typesGet(String filter, String select, Integer skip, Integer top) throws Exception
	{
		CLFMethodContext clf = logCtx.getMethodContext("typesGet");
		try {
			List<TypeInfo> typeBeans = getTypesServiceLocal().typesGet(filter, select, skip, top);
				return Response.ok().entity(typeBeans).build();
		}
		catch (CDMException e)
		{
			// Log and re-throw
			logException(clf, e);
			throw e;
		}
	}

	public TypesPrivateService getTypesServiceLocal() {
		return typesServiceLocal;
	}
	// Called by Spring
	public void setTypesServiceLocal(TypesPrivateService typesServiceLocal) {
		this.typesServiceLocal = typesServiceLocal;
	}
}
