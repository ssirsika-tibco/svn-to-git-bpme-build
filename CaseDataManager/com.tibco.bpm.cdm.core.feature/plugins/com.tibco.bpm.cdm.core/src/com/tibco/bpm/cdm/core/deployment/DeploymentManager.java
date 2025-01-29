package com.tibco.bpm.cdm.core.deployment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO.ApplicationIdAndVersion;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO.CreationResult;
import com.tibco.bpm.cdm.core.dao.CasesTableIndexDAO;
import com.tibco.bpm.cdm.core.dao.DAOFactory;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.DataModelInfo;
import com.tibco.bpm.cdm.core.dao.DataModelDependencyDAO;
import com.tibco.bpm.cdm.core.dao.IdentifierInitialisationInfoDAO;
import com.tibco.bpm.cdm.core.dao.LinkDAO;
import com.tibco.bpm.cdm.core.dao.StateDAO;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.deployment.DeploymentContext.ApplicationDependency;
import com.tibco.bpm.cdm.core.deployment.DeploymentContext.DataModelArtifact;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.da.dm.api.ValidationResult;
import com.tibco.bpm.dt.rasc.Dependency;
import com.tibco.bpm.dt.rasc.MicroService;
import com.tibco.bpm.dt.rasc.MicroServiceContent;
import com.tibco.bpm.dt.rasc.RuntimeApplication;
import com.tibco.bpm.dt.rasc.RuntimeContent;
import com.tibco.bpm.dt.rasc.Version;
import com.tibco.bpm.dt.rasc.VersionRange;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Orchestrates the deployment lifecycle of deployed artifacts
 * @author smorgan
 * @since 2019
 */
public class DeploymentManager
{
	private static CLFClassContext			logCtx						= CloudLoggingFramework
			.init(DeploymentManager.class, CDMLoggingInfo.instance);

	private static final String				ARTIFACT_TYPE_DATA_MODEL	= "dm";

	private static final String				ARTIFACT_TYPE_SCRIPT		= "js";

	private ApplicationDAO					applicationDAO;

	private DataModelDAO					dataModelDAO;

	private TypeDAO							typeDAO;

	private LinkDAO							linkDAO;

	private IdentifierInitialisationInfoDAO	identifierInitialisationInfoDAO;

	private CasesTableIndexDAO				casesTableIndexDAO;

	private DataModelDependencyDAO			dataModelDependencyDAO;

	private StateDAO						stateDAO;

	private Map<String, ArtifactHandler>	artifactHandlers;

    DAOFactory daoFactory;


	public DeploymentManager()
	{
		// Initialise the artifact handler map. If we need to handle additional artifact
		// types in the future, we'll add further entries to this map.

		artifactHandlers = new HashMap<>();
		artifactHandlers.put(ARTIFACT_TYPE_DATA_MODEL, new DataModelArtifactHandler());
		artifactHandlers.put(ARTIFACT_TYPE_SCRIPT, new ScriptArtifactHandler());
	}


	// Called by Spring
	public void setDaoFactory(DAOFactory factory) {
		daoFactory = factory;
		applicationDAO = daoFactory.getApplicationDAOImpl();
		dataModelDAO = daoFactory.getDataModelDAOImpl();
		typeDAO = daoFactory.getTypeDAOImpl();
		linkDAO = daoFactory.getLinkDAOImpl();
		identifierInitialisationInfoDAO = daoFactory.getIdentifierInitialisationInfoDAOImpl();
		casesTableIndexDAO = daoFactory.getCasesTableIndexDAOImpl();
		dataModelDependencyDAO = daoFactory.getDataModelDependencyDAOImpl();
		stateDAO = daoFactory.getStateDAOImpl();	
	}
	
	public DAOFactory getDaoFactory() {
		return daoFactory;		
	}
	

	public String deploy(RuntimeApplication runtimeApplication)
			throws DeploymentException, PersistenceException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("deploy");

		// The is unique for each deployed version and is the number that will
		// be subsequently passed to 'undeploy', 'status' and 'readyForUndeploy'.
		BigInteger deploymentId = runtimeApplication.getID();

		// Unique identifier for the application
		String appId = runtimeApplication.getApplicationInternalName();

		// Cosmetic label for the application
		String appName = runtimeApplication.getApplicationName();

		Version appVersion = runtimeApplication.getAppVersion();

		// Extract the major and full version numbers from the Version object.
		int appMajorVersion = appVersion.getMajor();
		String appVersionString = appVersion.toString();

		// (Avoiding sensitive data in message)
		clf.local.debug("Starting deployment. deploymentId=%s, appVersion=%s, appMajorVersion=%d", deploymentId,
				appVersion, appMajorVersion);

		// We're only interested in the content targeted at Case-Manager
		MicroServiceContent cdmContent = runtimeApplication.getMicroServices().stream()
				.filter(m -> m.getMicroService() == MicroService.CM).findFirst().orElse(null);

		if (cdmContent == null || cdmContent.getContent().size() == 0)
		{
			throw DeploymentException.newBadRASC("No artifacts for CDM", null);
		}
		else
		{
			// Create the top-level application row in the cdm_applications table, which returns a
			// key that persisted datamodels will use to refer to it. If the application already exists, it
			// will be selected for update (thus acting as a mutex for the whole update operation) and we
			// will update the row later.
			
			boolean isCaseApp = false;

			if ("case".equalsIgnoreCase(runtimeApplication.getProjectType()))
			{
				isCaseApp = true;
			}

			CreationResult creationResult = applicationDAO.createOrReadForUpdate(deploymentId, appName, appId,
					appVersionString,isCaseApp);

			BigInteger topLevelId = creationResult.getId();

			// This flag is true if we're replacing an existing application
			boolean isUpdate = creationResult.getIsUpdate();

			// Create a context into which we'll gather information as we get
			// each artifact from the RASC.
			DeploymentContext deploymentContext = new DeploymentContext();

			// Get inter-application depedency information from the RASC and record
			// it in our context.
			Collection<Dependency> dependencies = runtimeApplication.getDependencies();
			if (dependencies != null)
			{
				for (Dependency dependency : dependencies)
				{
					String dAppId = dependency.getApplicationId();
					VersionRange versionRange = dependency.getRange();
					deploymentContext.addApplicationDependency(dAppId, versionRange);
				}
			}

			// Enumerate artifacts, dealing with each depending on its type.
			for (RuntimeContent content : cdmContent.getContent())
			{
				// Attempt to find an appropriate handler for this artifact type.
				String artifactType = content.getArtifactType();
				ArtifactHandler artifactHandler = artifactHandlers.get(artifactType);

				if (artifactHandler == null)
				{
					// No handler found, suggesting this is an artifact that shouldn't
					// be sent to us.
					throw DeploymentException.newUnknownArtifactType(artifactType);

				}
				artifactHandler.handleArtifact(content, deploymentContext);
			}

			// Now the RASC has been fully processed, we can look at what we've gathered
			// in deploymentContext and deal with it accordingly.  We've no dependency on the
			// RASC from the point onwards; Everything we need is in deploymentContext.

			// Before we can begin validating Data Models, we need to resolve inter-application
			// dependencies, such that we have a pool of existing foreign models that we can 
			// use to resolve cross-model references from the models we're deploying
			List<BigInteger> foreignProjectTopLevelIds = new ArrayList<>();
			List<DataModelInfo> modelsInOtherApplications = null;
			if (!isCaseApp) {
			for (ApplicationDependency dependency : deploymentContext.getApplicationDependencies())
			{
				List<BigInteger> matchingApps = applicationDAO.getByVersionRange(dependency.getId(),
						dependency.getVersionRange());
				int matchingAppCount = matchingApps.size();
				if (matchingAppCount != 1)
				{
					// If zero or multiple found...
					throw DeploymentException.newUnresolvableDependency(dependency.getId(),
							dependency.getVersionRange().toString(), matchingApps.toString());
				}
				else
				{
					foreignProjectTopLevelIds.add(matchingApps.get(0));
				}
			}

			if (!deploymentContext.getApplicationDependencies().isEmpty())
			{
				// Get the data models from applications we depend on
				modelsInOtherApplications = dataModelDAO.readForApplications(foreignProjectTopLevelIds);

				// Deserialize those models - Failure is considered an InternalException as these
				// models would have been validated at deployment time.
				for (DataModelInfo modelInOtherApplication : modelsInOtherApplications)
				{
					try
					{
						modelInOtherApplication
								.setDataModel(DataModel.deserialize(modelInOtherApplication.getModelJson()));
					}
					catch (DataModelSerializationException e)
					{
						throw InternalException.newInternalException(e);
					}
				}
			}
		}
			List<DataModelInfo> dataModelInfos = new ArrayList<>();

			// Now all DataModel artifacts are in memory and deserialized, process them.
			for (DataModelArtifact dataModelArtifact : deploymentContext.getDataModelArtifacts())
			{
				String modelJson = null;
				String namespace = null;
				try
				{
					// Check that the model is valid.
					DataModel dm = dataModelArtifact.getDataModel();

					// Ensure foreign models are set, so cross-references can be resolved.
					List<String> namespaceDependencies = dm.getNamespaceDependencies();
					// Get the DataModels from this application that the current DataModel depends on.
					// (i.e. get the DataModels whose namespaces appear in the namespace dependency
					// list for the model we're currently processing).
					List<DataModel> foreignModels = deploymentContext.getDataModelArtifacts().stream()
							.map(DataModelArtifact::getDataModel)
							.filter(otherModel -> namespaceDependencies.contains(otherModel.getNamespace()))
							.collect(Collectors.toList());
					dm.getForeignModels().addAll(foreignModels);

					if (!deploymentContext.getApplicationDependencies().isEmpty() && modelsInOtherApplications != null)
					{
						// Find models in the (already deployed) applications we depend on, filtering to those
						// that have namespaces we're interested in.
						List<DataModelInfo> requiredModelsInOtherApplications = modelsInOtherApplications.stream()
								.filter(dmi -> namespaceDependencies.contains(dmi.getDataModel().getNamespace()))
								.collect(Collectors.toList());
						dm.getForeignModels().addAll(requiredModelsInOtherApplications.stream()
								.map(DataModelInfo::getDataModel).collect(Collectors.toList()));

					}


                    ValidationResult validationResult = dm.validate();
                    if (validationResult.containsErrors()) {
                        String reportMessage =
                                validationResult.toReportMessage();
                        throw DeploymentException
                                .newInvalidDataModel(reportMessage);
                    }

					modelJson = dataModelArtifact.getJson();
					namespace = dm.getNamespace();

					if (dataModelDAO.existsForOtherApplication(topLevelId, namespace, appMajorVersion))
					{
						throw DeploymentException.newDuplicateNamespace(namespace, Integer.toString(appMajorVersion));
					}

					BigInteger dataModelId = null;
					DataModelInfo modelInfo = null;

					// If we're updating an application, we need to check that Data Model changes are compatible.
					if (isUpdate)
					{
						// Get existing data model (if one exists)
						modelInfo = dataModelDAO.read(topLevelId, namespace, appMajorVersion);
						if (modelInfo != null)
						{
							// Data Model already exists, so validate that the new model is a valid upgrade
							dataModelId = modelInfo.getId();
							String existingModelJson = modelInfo.getModelJson();
							DataModel existingDM = DataModel.deserialize(existingModelJson);
							ValidationResult upgradeValidationResult = dm.validateUpgradeFrom(existingDM);
							if (upgradeValidationResult.containsErrors())
							{
								throw DeploymentException
										.newInvalidDataModelUpgrade(upgradeValidationResult.toReportMessage());
							}
							modelInfo.setDataModel(dm);
							modelInfo.setOldDataModel(existingDM);

							// If we get this far, then it's a valid upgrade, so go ahead and 
							// persist the updated model.
							modelInfo.setModelJson(modelJson);
							modelInfo.setScript(dataModelArtifact.getScript());
						}
					}

					// Unless the data model already existed, go ahead and persist it
					if (dataModelId == null)
					{
						modelInfo = new DataModelInfo(null, null, dm, modelJson, dataModelArtifact.getScript(), false);
					}
					dataModelInfos.add(modelInfo);
				}
				catch (DataModelSerializationException e)
				{
					throw DeploymentException.newDataModelDeserializationFailed(e);
				}
			}

			// When we reach here, dataModels contains a list of models: some new and
			// some updated (and already verified to be valid upgrades)
			int size = dataModelInfos.size();
			clf.local.debug("RASC contains %d Data Model%s", size, size != 1 ? "s" : "");

			for (DataModelInfo info : dataModelInfos)
			{
				DataModel dm = info.getDataModel();
				DataModel oldDataModel = info.getOldDataModel();

				if (info.getIsUpdate())
				{
					// Update the existing persisted model with the new JSON
					dataModelDAO.update(info.getId(), info.getModelJson(), info.getScript());

					// Get existing type/id mappings, then persist new types and add them to the map
					// (As type removal is not allowed, this can only be additive)
					Map<String, BigInteger> typeNameToIdMap = typeDAO.read(info.getId());
					typeNameToIdMap.putAll(typeDAO.update(info.getId(), info.getOldDataModel(), dm));

					// TODO Update links
					linkDAO.update(oldDataModel, dm, typeNameToIdMap);

					// III can be added, removed or changed
					identifierInitialisationInfoDAO.update(info.getId(), oldDataModel, dm, typeNameToIdMap);

					// Add/remove indexes
					casesTableIndexDAO.update(info.getId(), oldDataModel, dm, typeNameToIdMap);

					// Update states
					stateDAO.update(info.getId(), oldDataModel, dm, typeNameToIdMap);

				}
				else
				{
					// Persist the new model
					BigInteger id = dataModelDAO.create(topLevelId, appMajorVersion, dm.getNamespace(),
							info.getModelJson(), info.getScript());
					info.setId(id);

					// Create types
					Map<String, BigInteger> typeNameToIdMap = typeDAO.create(id, dm);

					// Create links
					linkDAO.create(dm, typeNameToIdMap);

					// Process III(s)
					identifierInitialisationInfoDAO.create(dm, typeNameToIdMap);

					// Create indexes
					casesTableIndexDAO.create(id, dm, typeNameToIdMap);

					// Create states
					stateDAO.create(dm, typeNameToIdMap);

				}
			}

			// Persist dependencies.
			// Now all models have been persisted (thus have ids), go ahead and determine
			// the ids corresponding to foreign models on which each depends, so that
			// id->id dependencies can be persisted.
			for (DataModelInfo info : dataModelInfos)
			{
				DataModel dm = info.getDataModel();

				// Get the foreign DataModels that were associated with the DataModel itself
				List<DataModel> foreignModels = dm.getForeignModels();

				// Filter the DataModelInfo list to just those models.
				List<DataModelInfo> foreignDataModelInfos = dataModelInfos.stream()
						.filter(dmi -> foreignModels.contains(dmi.getDataModel())).collect(Collectors.toList());

				// Map from DataModelInfos to ids.
				List<BigInteger> foreignDataModelIds = foreignDataModelInfos.stream().map(DataModelInfo::getId)
						.collect(Collectors.toList());

				// If we depend on models in other applications, include them too
				if (modelsInOtherApplications != null)
				{
					List<DataModelInfo> requiredModelsInOtherApplications = modelsInOtherApplications.stream()
							.filter(dmi -> foreignModels.contains(dmi.getDataModel())).collect(Collectors.toList());
					List<BigInteger> requiredModelsInOtherApplicationsIds = requiredModelsInOtherApplications.stream()
							.map(DataModelInfo::getId).collect(Collectors.toList());
					foreignDataModelIds.addAll(requiredModelsInOtherApplicationsIds);
				}

				// Call the DAO to create/update the mappings.
				if (isUpdate)
				{
					dataModelDependencyDAO.update(info.getId(), foreignDataModelIds);
				}
				else
				{
					dataModelDependencyDAO.create(info.getId(), foreignDataModelIds);
				}
			}

			if (isUpdate)
			{
				// Update the top application itself
				applicationDAO.update(topLevelId, deploymentId, appName, appId, appVersion.toString());
			}
		}
		return appId;
	}

	public String undeploy(BigInteger deploymentId) throws PersistenceException, DeploymentException, InternalException
	{
		// Check to make sure no other applications depend on the data models we're
		// about to remove. If they do, fail gracefully.
		List<ApplicationIdAndVersion> applicationsThatDependOnApplication = applicationDAO
				.getApplicationsThatDependOnApplication(deploymentId);
		if (applicationsThatDependOnApplication != null && !applicationsThatDependOnApplication.isEmpty())
		{
			throw DeploymentException
					.newDependenciesPreventUndeployment(applicationsThatDependOnApplication.toString());
		}

		// Remove dependency links
		dataModelDependencyDAO.delete(deploymentId);

		// Drop indexes
		casesTableIndexDAO.delete(deploymentId);

		return applicationDAO.delete(deploymentId);
	}

	public long getDataModelCountByDeploymentId(BigInteger deploymentId) throws PersistenceException
	{
		return dataModelDAO.getCountByDeploymentId(deploymentId);
	}
}
