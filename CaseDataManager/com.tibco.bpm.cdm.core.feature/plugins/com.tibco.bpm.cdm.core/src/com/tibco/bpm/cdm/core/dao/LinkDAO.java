package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.sql.Connection;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dto.LinkDTO;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for Link definitions. 
 * @author smorgan
 * @since 2019
 */
public interface LinkDAO
{
	public void create(DataModel dm, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException, InternalException;

	public LinkDTO getLink(Connection conn, BigInteger typeId, String name)
			throws PersistenceException, InternalException;

	public void update(DataModel oldDataModel, DataModel dm, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException, InternalException;;
}
