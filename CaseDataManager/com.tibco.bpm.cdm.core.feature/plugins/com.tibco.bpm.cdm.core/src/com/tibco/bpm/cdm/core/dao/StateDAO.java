package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * DAO for States.  There will be one for each state within the state model for each case type.
 * @author smorgan
 * @since 2019
 */
public interface StateDAO
{
	public static class StateInfo
	{
		private BigInteger	id;

		private String		value;

		public StateInfo(BigInteger id, String value)
		{
			this.id = id;
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}

		public BigInteger getId()
		{
			return id;
		}
	}

	public void create(DataModel dm, Map<String, BigInteger> typeNameToIdMap) throws PersistenceException;

	public List<StateInfo> get(String typeName, String namespace, int majorVersion) throws PersistenceException;

	public void update(BigInteger id, DataModel oldDataModel, DataModel dm, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException;
}
