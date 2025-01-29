package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for indexes on case storage to enhance performance. Manages both the indexes themselves
 * as well as meta-data about those indexes.
 * @author smorgan
 * @since 2019
 */
public interface CasesTableIndexDAO
{
	/**
	 * Creates indexes for all searchable attributes of all case types within the given Data Model
	 * @param dataModelId Is for the Data Model as returned by DataModelDAO.create(...)
	 * @param dataModel The Data Model itself
	 * @param typeNameToIdMap A map from name to id for the types. Obtained when persisting types via TypeDAO.create(...)
	 * @throws PersistenceException
	 */
	public void create(BigInteger dataModelId, DataModel dataModel, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException;

	/**
	 * Creates/removes indexes depending on the changes between oldDataModel and newDataModel.
	 * 
	 * @param dataModelId
	 * @param oldDataModel
	 * @param newDataModel
	 * @param typeNameToIdMap
	 * @throws PersistenceException
	 */
	public void update(BigInteger dataModelId, DataModel oldDataModel, DataModel newDataModel,
			Map<String, BigInteger> typeNameToIdMap) throws PersistenceException;

	/**
	 * Deletes all indexes relating to the given deployment.
	 * @param deploymentId
	 * @throws PersistenceException
	 */
	public void delete(BigInteger deploymentId) throws PersistenceException;
}
