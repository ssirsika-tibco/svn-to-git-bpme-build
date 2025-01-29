package com.tibco.bpm.cdm.core.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.TransientPersistenceException;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * An abstract DAO implementation that provides the capabilities to get and release connections
 * from a Spring-injected DataSource.
 * 
 * @author smorgan
 * @since 2019
 */
public abstract class AbstractDAOImpl
{
	static CLFClassContext		logCtx		= CloudLoggingFramework.init(AbstractDAOImpl.class,
			CDMLoggingInfo.instance);

	// Exclusive lock wait time (seconds)
	protected static final int	WAIT		= 300000;											// mS for 5 minutes

	private static final String	QUESTION	= "?";

	private static final String	COMMA		= ",";

	protected DataSource		dataSource;

	// Called by Spring
	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	protected Connection getConnection() throws PersistenceException
	{
		try
		{
			Connection conn = DataSourceUtils.getConnection(dataSource);
			if (conn == null)
			{
				throw TransientPersistenceException.newNotConnected();
			}
			return conn;
		}
		catch (CannotGetJdbcConnectionException e)
		{
			throw TransientPersistenceException.newNotConnected(e);
		}
	}

	protected void releaseConnection(Connection conn)
	{
		DataSourceUtils.releaseConnection(conn, dataSource);
	}

	/**
	 * Performs generic clean-up of the given resources. Called following a database interaction.
	 * @param ts
	 * @param ps
	 * @param conn
	 */
	protected void cleanUp(Statement ts, PreparedStatement ps, Connection conn)
	{
		CLFMethodContext clf = logCtx.getMethodContext("cleanUp");

		if (ps != null)
		{
			try
			{
				ps.close();
			}
			catch (SQLException e)
			{
				clf.local.debug(e, "Failed to close prepared statement");
			}
		}

		if (conn != null)
		{
			releaseConnection(conn);
		}
	}

	protected void cleanUp(PreparedStatement ps)
	{
		CLFMethodContext clf = logCtx.getMethodContext("cleanUp");
		if (ps != null)
		{
			try
			{
				ps.close();
			}
			catch (SQLException e)
			{
				clf.local.debug(e, "Failed to close prepared statement");
			}
		}
	}

	protected String renderCommaSeparatedQuestionMarks(int quantity)
	{
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < quantity; i++)
		{
			if (i != 0)
			{
				buf.append(COMMA);
			}
			buf.append(QUESTION);
		}
		return buf.toString();
	}
}
