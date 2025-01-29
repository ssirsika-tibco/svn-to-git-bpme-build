package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for Data Models. There may be several Data Models for a given application.
 * @author smorgan
 * @since 2019
 */
public interface DataModelDAO
{
	/**
	 * Value object to enable passing of data model info between APIs.
	 * Note that APIs tend to sparsely populate these objects, depending on what they're intending to achieve.
	 */
	public static class DataModelInfo
	{
		private BigInteger	id;

		private String		modelJson;

		private boolean		isUpdate;

		private DataModel	oldDataModel;

		private DataModel	dataModel;

		private String		script;

		public DataModelInfo(BigInteger id, DataModel oldDataModel, DataModel dataModel, String modelJson,
				String script, boolean isUpdate)
		{
			this.id = id;
			this.oldDataModel = oldDataModel;
			this.dataModel = dataModel;
			this.modelJson = modelJson;
			this.isUpdate = isUpdate;
			this.script = script;
		}

		public BigInteger getId()
		{
			return id;
		}

		public void setModelJson(String modelJson)
		{
			this.modelJson = modelJson;
		}

		public String getModelJson()
		{
			return modelJson;
		}

		public boolean getIsUpdate()
		{
			return isUpdate;
		}

		public DataModel getDataModel()
		{
			return dataModel;
		}

		public void setDataModel(DataModel dataModel)
		{
			this.dataModel = dataModel;
		}

		public DataModel getOldDataModel()
		{
			return oldDataModel;
		}

		public void setOldDataModel(DataModel oldDataModel)
		{
			this.oldDataModel = oldDataModel;
		}

		public void setId(BigInteger id)
		{
			this.id = id;

		}

		public void setScript(String script)
		{
			this.script = script;
		}

		public String getScript()
		{
			return script;
		}
	}

	public static class ApplicationIdAndMajorVersion
	{
		private String	applicationId;

		private int		majorVersion;

		public ApplicationIdAndMajorVersion(String applicationId, int majorVersion)
		{
			this.applicationId = applicationId;
			this.majorVersion = majorVersion;
		}

		public String getApplicationId()
		{
			return applicationId;
		}

		public int getMajorVersion()
		{
			return majorVersion;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((applicationId == null) ? 0 : applicationId.hashCode());
			result = prime * result + majorVersion;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ApplicationIdAndMajorVersion other = (ApplicationIdAndMajorVersion) obj;
			if (applicationId == null)
			{
				if (other.applicationId != null) return false;
			}
			else if (!applicationId.equals(other.applicationId)) return false;
			if (majorVersion != other.majorVersion) return false;
			return true;
		}
	}

	/**
	 * Persists a data model entry.
	 * 
	 * @param applicationId id of application in in the cdm_applications table
	 * @param majorVersion derived from the full version number in the corresponding application
	 * @param namespace Matches the 'model name' in the BOM (e.g. 'com.example.ordermodel')
	 * @param model The Data Model itself
	 * @param script JavaScript corresponding to this Datamodel
	 * @return A generated identifier, which can be used to reference this entity from its dependents
	 * @throws PersistenceException
	 */
	public BigInteger create(BigInteger applicationId, int majorVersion, String namespace, String model, String script)
			throws PersistenceException;

	public DataModelInfo read(BigInteger dataModelId) throws PersistenceException;

	/**
	 * Reads a data model matching the given applicationId / namespace / majorVerson combo.
	 * 
	 * @param applicationId
	 * @param namespace
	 * @return
	 * @throws PersistenceException
	 */
	public DataModelInfo read(BigInteger applicationId, String namespace, int majorVersion) throws PersistenceException;

	public List<DataModelInfo> readForApplications(List<BigInteger> applicationIds) throws PersistenceException;

	public DataModelInfo read(String namespace, int majorVersion, boolean loadDependencies)
			throws PersistenceException, InternalException;

	/**
	 * Updates an existing persisted data model entry with the new model JSON.
	 * 
	 * @param dataModelId
	 * @param model
	 * @param script
	 * @throws PersistenceException
	 */
	public void update(BigInteger dataModelId, String model, String script) throws PersistenceException;

	/**
	 * Counts the artifacts for the given deployment
	 * @param deploymentId
	 * @return
	 * @throws PersistenceException
	 */
	public long getCountByDeploymentId(BigInteger deploymentId) throws PersistenceException;

	/**
	 * Returns true if a data model with the given namespace/majorVersion exists for
	 * an application _other than_ the given one.
	 * 
	 * @param applicationId
	 * @param namespace
	 * @param majorVersion
	 * @return
	 * @throws PersistenceException 
	 */
	public boolean existsForOtherApplication(BigInteger applicationId, String namespace, int majorVersion)
			throws PersistenceException;

	/**
	* Reads Scripts correponding to all applications specified
	* @param applications Application(s) for which scripts are required
	* @return List<String> list of Scripts, ordered to avoid forward references when processed sequentially
	* @throws PersistenceException
	*/
	public List<String> readScripts(List<ApplicationIdAndMajorVersion> applications) throws PersistenceException;

	/**
	 * Gets all application ids
	 * @return
	 * @throws PersistenceException
	 */
	public List<BigInteger> getApplicationIds() throws PersistenceException;
}
