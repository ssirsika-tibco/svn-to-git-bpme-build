package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.PersistenceException;

/**
 * DAO for persisting from/to dependency mappings between DataModels.
 * @author smorgan
 */
public interface DataModelDependencyDAO
{
	public static class Dependency
	{
		private BigInteger	from;

		private BigInteger	to;

		public Dependency(BigInteger from, BigInteger to)
		{
			this.from = from;
			this.to = to;
		}

		public BigInteger getFrom()
		{
			return from;
		}

		public BigInteger getTo()
		{
			return to;
		}
	}

	/**
	 * Creates dependency mappings from the given data model to the given foreign data models
	 * @param dataModelId
	 * @param foreignDataModelIds
	 * @throws PersistenceException
	 */
	public void create(BigInteger dataModelId, List<BigInteger> foreignDataModelIds) throws PersistenceException;

	/**
	 * Updates dependency mappings from the given data model to match the given foreign data model list.
	 * @param dataModelId
	 * @param foreignDataModelIds
	 * @throws PersistenceException
	 */
	public void update(BigInteger dataModelId, List<BigInteger> foreignDataModelIds) throws PersistenceException;

	/**
	 * Deletes all dependency mappings relating to the given deployment.
	 * @param deploymentId
	 * @throws PersistenceException
	 */
	public void delete(BigInteger deploymentId) throws PersistenceException;
}
