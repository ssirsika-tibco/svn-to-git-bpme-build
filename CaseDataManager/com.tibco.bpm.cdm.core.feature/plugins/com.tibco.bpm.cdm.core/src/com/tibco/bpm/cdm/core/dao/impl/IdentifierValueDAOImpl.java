package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.IdentifierValueDAO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Implementation for DAO for obtaining values for auto-generated case identifiers
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class IdentifierValueDAOImpl extends AbstractDAOImpl implements IdentifierValueDAO
{
	static CLFClassContext		logCtx						= CloudLoggingFramework.init(IdentifierValueDAOImpl.class,
			CDMLoggingInfo.instance);

	// Exclusive lock wait time (seconds)
    private static final int WAIT = 300000; // mS for 5 minutes

	protected String	SQL_UPDATE_IDENTIFIER		= "SELECT next_num, prefix, suffix, min_num_length "
			+ "FROM cdm_identifier_infos WHERE type_id=? FOR UPDATE";

	public static final String	SQL_UPDATE_IDENTIFIER_SET	= "UPDATE cdm_identifier_infos SET next_num=? "
			+ "WHERE type_id=?";

	// Meta-pattern for generated identifiers (i.e. applying the minimum number of digits to this will
	// result in a new pattern ready to take prefix, number and suffix).
	public static final String	PATTERN_TEMPLATE			= "%%s%%0%dd%%s";

	public static final String	PATTERN_NO_LENGTH			= "%s%d%s";
	
	public IdentifierValueDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}

    public String bakeIdentifier(String prefix, BigInteger number,
            String suffix, Integer minNumLength)
	{
    	CLFMethodContext clf = logCtx.getMethodContext("bakeIdentifier");
    	clf.local.debug("Baking identifier for ID " + number.intValue());
		// Generate a pattern with an appropriate minimum number length portion 
		String pattern = null;
		if (minNumLength != null && minNumLength > 0)
		{
			pattern = String.format(PATTERN_TEMPLATE, minNumLength);
		}
		else
		{
			pattern = PATTERN_NO_LENGTH;
		}

		// Use the pattern to generate an identifier
		String result = String.format(pattern, prefix != null ? prefix : "", number, suffix != null ? suffix : "");
		return result;
	}

	@Override
    public List<String> getIdentifierValues(BigInteger typeId,
            int quantity) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getIdentifierValue");
		clf.local.trace("enter");

		PreparedStatement ps = null;
		PreparedStatement psForSeparateUpdate = null;

		Connection conn = null;

		List<String> result = new ArrayList<>();

		Statement ts = null;

		try
		{
			conn = getConnection();


			ps = conn.prepareStatement(SQL_UPDATE_IDENTIFIER, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

			psForSeparateUpdate = conn.prepareStatement(SQL_UPDATE_IDENTIFIER_SET);

			// type_id
			BigDecimal bdTypeId = new BigDecimal(typeId);
			ps.setBigDecimal(1, bdTypeId);
			psForSeparateUpdate.setBigDecimal(2, bdTypeId);

			ResultSet resultSet = ps.executeQuery();

			// Read first (and theoretically only) row
			// Note: If there are no rows, we won't enter this block and end up returning null.
			if (resultSet.next())
			{
				// Construct the identifier string
				BigInteger firstIdNumber = resultSet.getBigDecimal(1).toBigInteger();
				String prefix = resultSet.getString(2);
				String suffix = resultSet.getString(3);
				Integer minNumLength = resultSet.getInt(4);

				// Generate ids starting at the first number and continuing to
				// produce the required quantity.
				BigInteger currentIdNumber = firstIdNumber;
				for (int i=0; i<quantity; i++)
				{
					result.add(bakeIdentifier(prefix, currentIdNumber, suffix, minNumLength));
					currentIdNumber = currentIdNumber.add(BigInteger.ONE);
				}

				// Increment the number in the database to 'claim' the quantity of identifiers we need
				BigInteger nextIdNumber = firstIdNumber.add(BigInteger.valueOf(quantity));
				psForSeparateUpdate.setBigDecimal(1, new BigDecimal(nextIdNumber));
				psForSeparateUpdate.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			if (psForSeparateUpdate != null)
			{
				try
				{
					psForSeparateUpdate.close();
				}
				catch (SQLException e)
				{
					clf.local.warn(e, "Failed to close prepared statement psForSeparateUpdate");
				}
			}
			cleanUp(ts, ps, conn);
		}
		return result;
	}

	@Override
	public long cacheID(Integer type, int count) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BigInteger cacheBigIntegerID(Integer type, int count) {
		return null;
	}
	
	public String[] cacheIdentifierRowForType(Integer type, int count) throws PersistenceException {
		CLFMethodContext clf = logCtx.getMethodContext("cacheIdentifierRowForType");
		clf.local.trace("enter");

		PreparedStatement ps = null;
		PreparedStatement psForSeparateUpdate = null;

		Connection conn = null;

		List<String> result = new ArrayList<>();

		Statement ts = null;

		ResultSet resultSet = null;
		String[] response = null;
		
		try
		{
			conn = getConnection();


			ps = conn.prepareStatement(SQL_UPDATE_IDENTIFIER, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

			psForSeparateUpdate = conn.prepareStatement(SQL_UPDATE_IDENTIFIER_SET);

			// type_id
			BigDecimal bdTypeId = new BigDecimal(type);
			ps.setBigDecimal(1, bdTypeId);
			psForSeparateUpdate.setBigDecimal(2, bdTypeId);

			resultSet  = ps.executeQuery();
	
			// Read first (and theoretically only) row
			// Note: If there are no rows, we won't enter this block and end up returning null.
			if (resultSet.next())
			{
				response = new String[4];
				// Construct the identifier string
				BigInteger firstIdNumber = resultSet.getBigDecimal(1).toBigInteger();
				response[0] = firstIdNumber.toString();
				response[1] = resultSet.getString(2);
				response[2] = resultSet.getString(3);
				response[3] =Integer.toString(resultSet.getInt(4));
				// Increment the number in the database to 'claim' the quantity of identifiers we need
				BigInteger nextIdNumber = firstIdNumber.add(BigInteger.valueOf(count));
				psForSeparateUpdate.setBigDecimal(1, new BigDecimal(nextIdNumber));
				psForSeparateUpdate.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			if (psForSeparateUpdate != null)
			{
				try
				{
					psForSeparateUpdate.close();
				}
				catch (SQLException e)
				{
					clf.local.warn(e, "Failed to close prepared statement psForSeparateUpdate");
				}
			}
			cleanUp(ts, ps, conn);
		}
		return response;
	}

}
