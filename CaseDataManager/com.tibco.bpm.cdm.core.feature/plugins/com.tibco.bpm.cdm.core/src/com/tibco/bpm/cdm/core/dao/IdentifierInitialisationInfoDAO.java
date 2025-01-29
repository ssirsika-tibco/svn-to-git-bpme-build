package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for IdentifierInitialisationInfo ('III'). These represent the desire to auto-generate ids 
 * for a given case identifier attribute, along with optional prefix, suffix and minimum number length choices.
 * @see com.tibco.bpm.da.dm.api.IdentifierInitialisationInfo
 * 
 * @author smorgan
 */
public interface IdentifierInitialisationInfoDAO
{
	/**
	 * Creates entries for the given Data Model
	 * 
	 * @param dataModel
	 * @param typeNameToIdMap
	 * @throws PersistenceException
	 */
	public void create(DataModel dataModel, Map<String, BigInteger> typeNameToIdMap) throws PersistenceException;

	/**
	 * Creates/updates/removes entries, as required when moving from old to new Data Model versions.
	 * 
	 * @param dataModelId
	 * @param oldDataModel
	 * @param newDataModel
	 * @param typeNameToIdMap
	 * @throws PersistenceException
	 */
	public void update(BigInteger dataModelId, DataModel oldDataModel, DataModel newDataModel,
			Map<String, BigInteger> typeNameToIdMap) throws PersistenceException;
}
