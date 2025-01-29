package com.tibco.bpm.cdm.core.deployment;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.core.dao.CaseDAO;
import com.tibco.bpm.cdm.core.logging.CDMAuditMessages;
import com.tibco.bpm.cdm.core.logging.CDMExceptionMessages;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.logging.LoggingHelper;
import com.tibco.bpm.dem.api.DeploymentComponentService;
import com.tibco.bpm.dem.api.exception.DeploymentFailedFault;
import com.tibco.bpm.dt.rasc.RuntimeApplication;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * Implements DEM's interface, enabling CDM to be a deployment target.
 * This is advertised via Blueprint, such that DEM's reference listener will pick it up
 * and direct applications containing artifacts targeted for 'Case-Manager' to it.
 * @author smorgan
 * @since 2019
 */
public class DeploymentComponentServiceImpl implements DeploymentComponentService
{
	static CLFClassContext		logCtx	= CloudLoggingFramework.init(DeploymentComponentServiceImpl.class,
			CDMLoggingInfo.instance);

	private DeploymentManager	deploymentManager;

	private CaseDAO				caseDAO;

    DAOFactory daoFactory;
    
	// Called by Spring
	public void setDeploymentManager(DeploymentManager deploymentManager)
	{
		this.deploymentManager = deploymentManager;
	}
	
	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		caseDAO = daoFactory.getCaseDAOImpl();
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	

	
	private static DeploymentFailedFault toDeploymentFailedFault(String messagePrefix, Exception e)
	{
		String message = null;
		String errorCode = null;
		final Map<String, String> attributes = new HashMap<>();

		// If the cause is a CDMException (which it generally will be), propagate
		// the message, context attributes and error code.
		if (e instanceof CDMException)
		{
			CDMException cdmException = (CDMException) e;
			message = cdmException.getMessage();
			attributes.putAll(cdmException.getAttributes());
			// Put the message as a context attribute, as DEM doesn't otherwise propagate the
			// message, replacing it with its own.
			attributes.put("cdm_message", message);
			// Convert metadata into attributes too
			cdmException.getMetadataEntries().forEach(me -> attributes.put("metadata_" + me.getName(), me.getValue()));
			errorCode = cdmException.getErrorData().getCode();
		}
		DeploymentFailedFault fault = new DeploymentFailedFault(String.format("%s: %s", messagePrefix, message),
				attributes, errorCode, e);
		return fault;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.dem.api.DeploymentComponentService#deploy(com.tibco.bpm.dt.rasc.RuntimeApplication)
	 */
	@Override
	public void deploy(RuntimeApplication runtimeApplication) throws DeploymentFailedFault
	{

		CLFMethodContext clf = logCtx.getMethodContext("deploy");
		clf.local.debug("CDM deploy called");
		
		
		
		try
		{
			// Deploy the application. This returns the application id, extracted from the RASC,
			// which we can use for auditing purposes.
			String applicationId = deploymentManager.deploy(runtimeApplication);
			if (applicationId != null)
			{
				//CommonMetaData.APPLICATION_ID - com.example.carapplication
				//CommonMetaData.MANAGED_OBJECT_ID - deploymentId;
				//CommonMetaData.MANAGED_OBJECT_NAME -- applicationName(CarApplication)
				//CommonMetaData.MANAGED_OBJECT_VERSION -- applicationVersion( 1.0.0.20190815151210018)

				clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
				clf.addMethodAttribute(CommonMetaData.MANAGED_OBJECT_ID, runtimeApplication.getID());
				clf.addMethodAttribute(CommonMetaData.MANAGED_OBJECT_NAME, runtimeApplication.getApplicationName());
				clf.addMethodAttribute(CommonMetaData.MANAGED_OBJECT_VERSION,
						runtimeApplication.getAppVersion().toString());

			}

			// Audit success
			clf.audit.audit(CDMAuditMessages.CDM_APPLICATION_DEPLOYED);
		}
		catch (Exception e)
		{
			// Create the exception DEM expects, audit it, then throw it.
			DeploymentFailedFault dff = toDeploymentFailedFault("CDM deployment failed: " + e.getMessage(), e);
			LoggingHelper.addLoggingAttributesFromExceptionMetadata(clf.audit, e)
					.error(CDMExceptionMessages.CDM_APPLICATION_DEPLOYMENT_FAILED, dff);
			throw dff;
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.dem.api.DeploymentComponentService#status(java.math.BigInteger)
	 */
	@Override
	public long status(BigInteger deploymentId) throws DeploymentFailedFault
	{
		// "requires a component to provide a 'count(*)' for all model artefacts in the application"
		// (see Deployment FP: http://confluence.tibco.com/pages/viewpage.action?pageId=167068815)
		// The interface states: 
		//    "Return the total count of all artefacts that exist for the referenced application"

		CLFMethodContext clf = logCtx.getMethodContext("status");
		clf.local.debug("CDM status called with deploymentId = %s", deploymentId);

		long count = 0L;
		try
		{
			count = deploymentManager.getDataModelCountByDeploymentId(deploymentId);
		}
		catch (Exception e)
		{
			DeploymentFailedFault dff = toDeploymentFailedFault("CDM status failed", e);
			clf.local.error(dff, "Status failed");
			throw dff;
		}
		return count;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.dem.api.DeploymentComponentService#readyForUndeploy(java.math.BigInteger)
	 */
	@Override
	public boolean readyForUndeploy(BigInteger deploymentId) throws DeploymentFailedFault
	{
		CLFMethodContext clf = logCtx.getMethodContext("readyForUndeploy");
		clf.local.debug("CDM readyForUndeploy called with deploymentId = %s", deploymentId);

		// "can return false as soon as it finds a model artefact with one or more instances associated with it"
		// (see Deployment FP: http://confluence.tibco.com/pages/viewpage.action?pageId=167068815)

		// The interface states: 
		//    "Is the referenced application ready to be undeployed"

		try
		{
			// Return true if any cases exist for this deployment
			long count = caseDAO.countByDeploymentId(deploymentId);

			boolean result = (count == 0);
			if (!result)
			{
				clf.local.debug(String.format("readyForUndeploy returning false as %d cases exist for deployment id %s",
						count, deploymentId));
			}
			return result;
		}
		catch (Exception e)
		{
			DeploymentFailedFault dff = toDeploymentFailedFault("CDM readyForUndeploy failed", e);
			clf.local.error(dff, "ReadyForUndeploy failed");
			throw dff;
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.dem.api.DeploymentComponentService#undeploy(java.math.BigInteger)
	 */
	@Override
	public void undeploy(BigInteger deploymentId) throws DeploymentFailedFault
	{
		CLFMethodContext clf = logCtx.getMethodContext("undeploy");
		clf.local.debug("CDM undeploy called with deploymentId = %s", deploymentId);

		try
		{
			// Deploy the application. This returns the application id (or null if the
			// application didn't exist) which we can use for auditing purposes.
			String applicationId = deploymentManager.undeploy(deploymentId);
			if (applicationId != null)
			{
				clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
				clf.addMethodAttribute(CommonMetaData.MANAGED_OBJECT_ID, deploymentId);

				// Audit success
				clf.audit.audit(CDMAuditMessages.CDM_APPLICATION_UNDEPLOYED);
			}
			else
			{
				clf.local.debug("Undeploy had no effect of for non-existant deployment id: " + deploymentId);
			}
		}
		catch (Exception e)
		{
			// Create the exception DEM expects, audit it, then throw it.

			DeploymentFailedFault dff = toDeploymentFailedFault("CDM undeployment failed", e);
			clf.audit.error(CDMExceptionMessages.CDM_APPLICATION_UNDEPLOYMENT_FAILED, dff);
			throw dff;
		}
	}
}
