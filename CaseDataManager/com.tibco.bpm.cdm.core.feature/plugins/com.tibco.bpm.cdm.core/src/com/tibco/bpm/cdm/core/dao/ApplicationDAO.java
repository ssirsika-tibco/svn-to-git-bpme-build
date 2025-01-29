package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.ApplicationIdAndMajorVersion;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoDependencyDTO;
import com.tibco.bpm.dt.rasc.VersionRange;

/**
 * DAO for Applications. There may be several Data Models for a given application.
 * @author smorgan
 * @since 2019
 */
public interface ApplicationDAO
{
	/**
	 * Return type for the createOrReadForUpdate method. Contains an id and flag to indicate 
	 * whether the creation resulted in a new entry (false) or updating an existing one (true).
	 * @author smorgan
	 *
	 */
	public class CreationResult
	{
		private BigInteger	id;

		private boolean		isUpdate;

		public CreationResult(BigInteger id, boolean isUpdate)
		{
			this.id = id;
			this.isUpdate = isUpdate;
		}

		public BigInteger getId()
		{
			return id;
		}

		public boolean getIsUpdate()
		{
			return isUpdate;
		}
	}

	/**
	 * Represents the combination of an application id and application version
	 * @author smorgan
	 */
	public class ApplicationIdAndVersion
	{
		private String	applicationId;

		private String	version;

		public ApplicationIdAndVersion(String applicationId, String version)
		{
			this.applicationId = applicationId;
			this.version = version;
		}

		public String getApplicationId()
		{
			return applicationId;
		}

		public String getVersion()
		{
			return version;
		}

		public String toString()
		{
			return String.format("%s (version %s)", applicationId, version);
		}
	}

	/**
	 * Either persists the given application, _or_ (if the application already exists at the same major version)
	 * does a 'select for update' such that it is locked, allowing changes to be safely made to child entities.
	 * In the latter case, a subsequent call to update(...) must be made to perform the actual update. This is 
	 * typically done as the final step, having performed all upgrade validation and updated child entities.
	 * 
	 * The object returned combines the entry id, along with a flag to indicate whether this is a new or updated 
	 * application, such that deployment logic can act accordingly.
	 * 
	 * @param deploymentId assigned by DEM. Used when subsequently undeploying or querying the application
	 * @param applicationName
	 * @param applicationId
	 * @param applicationVersion full 4 part version number
	 * @return assigned id that child entities can reference and flag to indicate if existing app exists
	 * @throws PersistenceException
	 */
	public CreationResult createOrReadForUpdate(BigInteger deploymentId, String applicationName, String applicationId,
			String applicationVersion,boolean isCaseApp) throws PersistenceException;

	/**
	 * Used after a prior call to createOrReadForUpdate has determined that an app already exists.
	 * Performs the actual update.
	 * 
	 * @param id
	 * @param deploymentId
	 * @param applicationName
	 * @param applicationId
	 * @param applicationVersion
	 * @throws PersistenceException
	 * @throws InternalException 
	 */
	public void update(BigInteger id, BigInteger deploymentId, String applicationName, String applicationId,
			String applicationVersion) throws PersistenceException, InternalException;

	/**
	 * Returns the application(s) that match the given id / version range combo.
	 * 
	 * @param applicationId
	 * @param versionRange
	 * @return
	 * @throws InternalException
	 * @throws DeploymentException
	 * @throws PersistenceException
	 */
	public List<BigInteger> getByVersionRange(String applicationId, VersionRange versionRange)
			throws DeploymentException, PersistenceException, InternalException;

	/**
	 * Deletes the given application.
	 * 
	 * @param deploymentId
	 * @return the application id of the deleted application
	 * @throws PersistenceException
	 */
	public String delete(BigInteger deploymentId) throws PersistenceException, InternalException;

	/**
	 * Given a deployment id, returns applications (id and version) that depend on it.
	 * @param deploymentId
	 * @return
	 * @throws PersistenceException
	 */
	public List<ApplicationIdAndVersion> getApplicationsThatDependOnApplication(BigInteger deploymentId)
			throws PersistenceException, InternalException;

	/**
	 * Given a list of applications (id/version), returns a map from each application to 
	 * a list of foreign namespaces (and containing applications) that it depends on.
	 * @param applications
	 * @return
	 * @throws PersistenceException
	 */
	public Map<ApplicationIdAndMajorVersion, List<TypeInfoDependencyDTO>> getForeignNamespaceDependencies(
			List<ApplicationIdAndMajorVersion> applications) throws PersistenceException, InternalException;
	
	/**
	 * Given a case id returns boolean flag for whether the app is a caseApp
	 * @param caseId
	 * @return
	 * @throws PersistenceException
	 */
	public boolean isCaseApp(BigInteger caseId) throws PersistenceException;
}
