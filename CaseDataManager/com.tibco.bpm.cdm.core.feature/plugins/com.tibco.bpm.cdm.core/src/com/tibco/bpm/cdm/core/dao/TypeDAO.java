package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for types, be they case or non-case.
 * @author smorgan
 * @since 2019
 */
public interface TypeDAO
{
	/**
	 * Creates entries in the types table for each type in the given DataModel.
	 * As a unique id is created for each type, this returns a map from type names to ids, which can be used 
	 * when creating child entities that need to refer to a type id.
	 * @param dataModelId
	 * @param dataModel
	 * @return
	 * @throws PersistenceException
	 */
	public Map<String, BigInteger> create(BigInteger dataModelId, DataModel dataModel) throws PersistenceException;

	/**
	 * Reads entries for the given Data Model, returning a map from type name to id.
	 * 
	 * @param dataModelId
	 * @return
	 * @throws PersistenceException
	 */
	public Map<String, BigInteger> read(BigInteger dataModelId) throws PersistenceException;

	public BigInteger getId(String namespace, int majorVersion, String name) throws PersistenceException;

	/**
	 * Adds, removes and alters types in order to reflect the transition from the old to new model. 
	 * @param dataModelId
	 * @param oldDataModel
	 * @param newDataModel
	 * @return
	 * @throws PersistenceException
	 */
	public Map<String, BigInteger> update(BigInteger dataModelId, DataModel oldDataModel, DataModel newDataModel)
			throws PersistenceException;

	/**
	 * Obtains type info, optionally filtered/paginated.
	 * 
	 * @param applicationId
	 * @param namespace
	 * @param majorVersion
	 * @param isCase
	 * @param skip
	 * @param top
	 * @return
	 * @throws PersistenceException
	 */
	public List<TypeInfoDTO> getTypes(String applicationId, String namespace, Integer majorVersion, Boolean isCase,
			Integer skip, Integer top) throws PersistenceException;

	/**
	 * Obtains case type ids for the given application id
	 * @param applicationId
	 * @return
	 * @throws PersistenceException
	 */
	List<BigInteger> getCaseTypeIdsByApplication(BigInteger applicationId) throws PersistenceException;

	/**
	 * Gets a type by id
	 * @param typeId
	 * @return
	 * @throws PersistenceException
	 */
	public TypeInfoDTO getType(BigInteger typeId) throws PersistenceException;
}
