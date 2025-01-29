package com.tibco.bpm.acecannon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.ds.PGPoolingDataSource;

import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.Database;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;

public class ACEDAO
{
	private static final String	DATAMODEL_GET			= "SELECT model FROM cdm_datamodels WHERE namespace = ? AND major_version = ?";

	private static final String	LINK_NAMES_BY_TYPE_1	= "SELECT end1_name FROM cdm_links WHERE end1_owner_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN (SELECT id FROM cdm_datamodels WHERE namespace = ?))";

	private static final String	LINK_NAMES_BY_TYPE_2	= "SELECT end2_name FROM cdm_links WHERE end2_owner_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN (SELECT id FROM cdm_datamodels WHERE namespace = ?))";

	// Exclusive lock wait time (milliseconds)
	private static final int	WAIT					= 300000;

	private static DataSource	dataSource;

	private Connection			connection;

	public ACEDAO()
	{
		try
		{
			dataSource = makeDataSource();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static DataSource makeDataSource() throws SQLException
	{
		Database dbConfig = ConfigManager.INSTANCE.getActiveProfile().getDatabase();
		String url = "jdbc:postgresql://" + dbConfig.getHost() + ":5432/" + dbConfig.getName();
		if (dataSource != null)
		{
			((PGPoolingDataSource) dataSource).close();
		}
		PGPoolingDataSource dataSource = new PGPoolingDataSource();
		dataSource.setUrl(url);
		dataSource.setDataSourceName("Data Source for AC");
		dataSource.setUser(dbConfig.getUser());
		dataSource.setPassword(dbConfig.getPassword());
		dataSource.setPortNumber(5432);
		dataSource.setMaxConnections(10);
		return dataSource;
	}

	public Connection requestConnection() throws SQLException
	{
		Connection connection = dataSource.getConnection();
		return connection;
	}

	public List<String> getLinkNamesByType(String namespace, String name) throws SQLException
	{
		List<String> result = new ArrayList<>();
		Statement ts = null;
		PreparedStatement ps = null;

		if (connection == null)
		{
			connection = requestConnection();
		}

		try
		{
			ts = connection.createStatement();
			ts.execute("SET statement_timeout TO " + String.valueOf(WAIT));
			ps = connection.prepareStatement(LINK_NAMES_BY_TYPE_1);
			ps.setString(1, name);
			ps.setString(2, namespace);

			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					result.add(rset.getString(1));
				}
			}
			ps.close();
			ps = connection.prepareStatement(LINK_NAMES_BY_TYPE_2);
			ps.setString(1, name);
			ps.setString(2, namespace);

			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					result.add(rset.getString(1));
				}
			}
		}
		finally
		{
			if (ps != null)
			{
				ps.close();
			}
			if (ts != null)
			{
				ts.close();
			}
		}
		return result;
	}

	public DataModel getDataModel(String namespace, long modelMajorVersion) throws SQLException
	{
		Statement ts = null;

		if (connection == null)
		{
			connection = requestConnection();
		}

		ts = connection.createStatement();
		ts.execute("SET statement_timeout TO " + String.valueOf(WAIT));
		PreparedStatement ps = connection.prepareStatement(DATAMODEL_GET);
		ps.setString(1, namespace);
		ps.setLong(2, modelMajorVersion);

		boolean success = ps.execute(); // ordinarily we would use executeUpdate() but we want a ResultSet not just the #updated

		DataModel dm = null;
		if (success)
		{
			ResultSet rset = ps.getResultSet();
			while (rset.next())
			{
				String model = rset.getString(1);
				try
				{
					dm = DataModel.deserialize(model);
				}
				catch (DataModelSerializationException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return dm;
	}
}
