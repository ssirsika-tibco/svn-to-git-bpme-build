package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DataModelDependencyDAO;

public class DataModelDependencyDAOImpl extends AbstractDAOImpl implements DataModelDependencyDAO
{
	private static final String	SQL_CREATE		= "INSERT INTO cdm_datamodel_deps (datamodel_id_from, datamodel_id_to) VALUES (?, ?)";

	private static final String	SQL_DELETE		= "DELETE FROM cdm_datamodel_deps WHERE datamodel_id_from=?";

	private static final String	SQL_DELETE_APP	= "DELETE FROM cdm_datamodel_deps WHERE datamodel_id_from IN "
			+ "(SELECT id FROM cdm_datamodels WHERE application_id IN "
			+ "(SELECT id FROM cdm_applications WHERE deployment_id = ?))";

	public DataModelDependencyDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public void create(BigInteger dataModelId, List<BigInteger> foreignDataModelIds) throws PersistenceException
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			createMappings(conn, dataModelId, foreignDataModelIds);
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}

	private void createMappings(Connection conn, BigInteger dataModelId, List<BigInteger> foreignDataModelIds)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{

			ps = conn.prepareStatement(SQL_CREATE);
			for (BigInteger foreignDataModelId : foreignDataModelIds)
			{
				ps.setBigDecimal(1, new BigDecimal(dataModelId));
				ps.setBigDecimal(2, new BigDecimal(foreignDataModelId));
				ps.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, null);
		}
	}

	private void deleteMappings(Connection conn, BigInteger dataModelId) throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{

			ps = conn.prepareStatement(SQL_DELETE);
			ps.setBigDecimal(1, new BigDecimal(dataModelId));
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, null);
		}
	}

	@Override
	public void update(BigInteger dataModelId, List<BigInteger> foreignDataModelIds) throws PersistenceException
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Delete existing mappings
			deleteMappings(conn, dataModelId);
			// ...and create new ones.
			createMappings(conn, dataModelId, foreignDataModelIds);
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}

	@Override
	public void delete(BigInteger deploymentId) throws PersistenceException
	{
		Connection conn = null;

		try
		{
			conn = getConnection();
			PreparedStatement ps = null;
			Statement ts = null;
			try
			{

				ps = conn.prepareStatement(SQL_DELETE_APP);
				ps.setBigDecimal(1, new BigDecimal(deploymentId));
				ps.executeUpdate();
			}
			catch (SQLException e)
			{
				throw PersistenceException.newRepositoryProblem(e);
			}
			finally
			{
				cleanUp(ts, ps, null);
			}
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}
}
