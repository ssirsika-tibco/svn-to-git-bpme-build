package com.tibco.bpm.cdm.core.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import com.tibco.bpm.cdm.api.CaseDataManager;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.ApplicationIdAndMajorVersion;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.se.api.Scope;
import com.tibco.bpm.se.api.ScriptEngineService;
import com.tibco.bpm.se.api.ScriptManager;

/**
 * Implements ScriptEngine Service
 *
 *
 * @author jaugusti
 * @since 12 Apr 2019
 */
public class ScriptEngineServiceImpl implements ScriptEngineService
{

	private static CLFClassContext	logCtx	= CloudLoggingFramework.init(ScriptEngineServiceImpl.class,
			CDMLoggingInfo.instance);

	private DataModelDAO			dataModelDAO;

	private CaseDataManager			cdmAPI;

	private TypeDAO					typeDAO;

    DAOFactory daoFactory;

	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		dataModelDAO = daoFactory.getDataModelDAOImpl();
		typeDAO = daoFactory.getTypeDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}

	public DataModelDAO getDataModelDAO()
	{
		return dataModelDAO;
	}


	//applicationId,majorVersion
	@Override
	public Scope createScope(Map<String, Long> businessDataApplicationInfo, List<String> dataFieldsDescriptors)
			throws ScriptException
	{

		CLFMethodContext clf = logCtx.getMethodContext("createScope");
		List<String> scripts = new ArrayList<String>();
		Map<String, Long> caseTypesInfo = new HashMap<String, Long>();
		//Load the scripts directly and indirectly related to app versions supplied.

		long t1 = System.currentTimeMillis();
		if (businessDataApplicationInfo != null)
		{

			try
			{
				List<ApplicationIdAndMajorVersion> applications = businessDataApplicationInfo.entrySet().stream()
						.map(e -> new ApplicationIdAndMajorVersion(e.getKey(), e.getValue().intValue()))
						.collect(Collectors.toList());
				scripts = dataModelDAO.readScripts(applications);

				for (ApplicationIdAndMajorVersion application : applications)
				{
					List<TypeInfoDTO> types = typeDAO.getTypes(application.getApplicationId(), null,
							application.getMajorVersion(), true, 0, Integer.MAX_VALUE);

					types.stream()
							.forEach(type -> caseTypesInfo.put(
									new QualifiedTypeName(type.getNamespace(), type.getName()).toString(),
									new Long(type.getApplicationMajorVersion())));

				}

			}
			catch (PersistenceException e)
			{
				//TODO: Should this be an error? 
				clf.local.info(e, "Error while loading scripts for " + businessDataApplicationInfo);
				throw new ScriptException(e);

			}
		}

		for (String dataFieldsDescriptor : dataFieldsDescriptors)
		{
			scripts.add(dataFieldsDescriptor);
		}
		long t2 = System.currentTimeMillis();
		clf.local.debug("Time taken to load relevant scripts from DB: " + (t2 - t1));

		t1 = System.currentTimeMillis();
		Scope scope = ScriptManager.createScopeWithScripts(scripts);
		CaseDataScriptAPI caseData = new CaseDataScriptAPIImpl(caseTypesInfo, cdmAPI, scope, dataModelDAO);
		scope.bindObjectToBPM("caseData", caseData);
		t2 = System.currentTimeMillis();
		clf.local.debug("Time taken to create scope and evaluate all generated scripts: " + (t2 - t1));

		return scope;

	}

	public CaseDataManager getCdmAPI()
	{
		return cdmAPI;
	}

	public void setCdmAPI(CaseDataManager cdmAPI)
	{
		this.cdmAPI = cdmAPI;
	}

	public TypeDAO getTypeDAO()
	{
		return typeDAO;
	}


	@Override
	public Scope createScope() throws ScriptException
	{
		return ScriptManager.createScopeWithScripts();
	}

}
