package com.tibco.bpm.cdm.core.dao.impl.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.PropertyDAO;

/**
 * Oracle implementation of DAO for arbitrary name/value pairs stored in the database
 */
public class PropertyDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.PropertyDAOImpl implements PropertyDAO
{

	// Get a property's value
	private static final String	SQL_GET	= "SELECT value FROM cdm_properties WHERE name = ?";

	private static final String	SQL_SET	= "INSERT INTO cdm_properties (name, value) VALUES (?, ?)";
	
	private static final String	SQL_UPDATE	= "UPDATE cdm_properties SET value = ? WHERE name = ?";

	public PropertyDAOImpl(DataSource dataSource) {
		super(dataSource);
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

			if (get(name) == null) {
				ps = conn.prepareStatement(SQL_SET);
				ps.setString(1, name);
				ps.setString(2, value);
			} else {
				ps = conn.prepareStatement(SQL_UPDATE);
				ps.setString(1, value);
				ps.setString(2, name);
			}
			
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
