package com.tibco.bpm.cdm.core.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.PropertyDAO;

/**
 * PostgreS implementation of DAO for arbitrary name/value pairs stored in the database
 * @author smorgan
 * @since 2019
 */
public class PropertyDAOImpl extends AbstractDAOImpl implements PropertyDAO
{

	// Get a property's value
	private static final String	SQL_GET	= "SELECT value FROM cdm_properties WHERE name = ?";

	// Create or update a property using the 'upsert' pattern
	private static final String	SQL_SET	= "INSERT INTO cdm_properties (name, value) VALUES (?, ?) "
			+ "ON CONFLICT (name) DO UPDATE SET value = ?";

	public PropertyDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	@Override
	public String get(String name) throws PersistenceException
	{
		String result = null;
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			ts = conn.createStatement();
			ts.execute("SET statement_timeout TO " + String.valueOf(WAIT));

			ps = conn.prepareStatement(SQL_GET);
			ps.setString(1, name);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					result = rset.getString(1);
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
		return result;
	}

	@Override
	public void set(String name, String value) throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			ts = conn.createStatement();
			ts.execute("SET statement_timeout TO " + String.valueOf(WAIT));

			ps = conn.prepareStatement(SQL_SET);
			ps.setString(1, name);
			ps.setString(2, value);
			ps.setString(3, value);

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}

}
