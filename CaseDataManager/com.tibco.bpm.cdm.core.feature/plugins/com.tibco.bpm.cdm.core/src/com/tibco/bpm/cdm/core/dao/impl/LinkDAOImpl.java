package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.LinkDAO;
import com.tibco.bpm.cdm.core.dto.LinkDTO;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.Link;
import com.tibco.bpm.da.dm.api.LinkEnd;

/**
 * PostgreS implementation of the LinkDAO interface that persists link definitions in the cdm_links database table.
 * 
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class LinkDAOImpl extends AbstractDAOImpl implements LinkDAO
{
	// Persists a Link definition
    protected String SQL_CREATE =
            "INSERT INTO cdm_links (id, end1_owner_id, end1_name, end1_is_array, end2_owner_id, end2_name, end2_is_array) "
			+ "VALUES (NEXTVAL('cdm_links_seq'), ?, ?, ?, ?, ?, ?)";

	public static final String	SQL_UPDATE_ARRAYNESS	= "UPDATE cdm_links SET end1_is_array = ?, end2_is_array = ? "
			+ "WHERE end1_owner_id = ? AND end1_name = ? AND end2_owner_id = ? AND end2_name = ?";

	// Gets a Link definition where either end matches a given type id / link name combo
	private static final String	SQL_GET_LINK			= "SELECT l.id, l.end1_owner_id, l.end1_name, l.end1_is_array, d1.namespace, t1.name, d1.major_version, "
			+ "l.end2_owner_id, l.end2_name, l.end2_is_array, d2.namespace, t2.name, d2.major_version "
			+ "FROM cdm_links l INNER JOIN cdm_types t1 ON l.end1_owner_id=t1.id INNER JOIN cdm_datamodels d1 ON t1.datamodel_id=d1.id "
			+ " INNER JOIN cdm_types t2 ON l.end2_owner_id=t2.id INNER JOIN cdm_datamodels d2 ON t2.datamodel_id=d2.id "
			+ "WHERE (l.end1_owner_id = ? AND l.end1_name = ?) OR (l.end2_owner_id = ? AND l.end2_name = ?)";

	public LinkDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
    public LinkDTO getLink(Connection conn, BigInteger typeId, String name)
			throws PersistenceException, InternalException
	{
		boolean connected = false;
		PreparedStatement ps = null;
		Statement ts = null;
		LinkDTO result = null;

		try
		{
			if (conn == null)
			{
				conn = getConnection();
				connected = true;
			}
			ps = conn.prepareStatement(SQL_GET_LINK);

			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(2, name);
			ps.setBigDecimal(3, new BigDecimal(typeId));
			ps.setString(4, name);

			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					result = new LinkDTO();
					result.setId(rset.getBigDecimal(1).toBigInteger());
					result.setEnd1TypeId(rset.getBigDecimal(2).toBigInteger());
					result.setEnd1Name(rset.getString(3));
					result.setEnd1IsArray(rset.getBoolean(4));
					result.setEnd1TypeQTN(new QualifiedTypeName(rset.getString(5), rset.getString(6)));
					result.setEnd1TypeMajorVersion(rset.getInt(7));

					result.setEnd2TypeId(rset.getBigDecimal(8).toBigInteger());
					result.setEnd2Name(rset.getString(9));
					result.setEnd2IsArray(rset.getBoolean(10));
					result.setEnd2TypeQTN(new QualifiedTypeName(rset.getString(11), rset.getString(12)));
					result.setEnd2TypeMajorVersion(rset.getInt(13));
				}
			}
			return result;
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, connected ? conn : null);
		}
	}

	@Override
	public void create(DataModel dm, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException, InternalException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;

		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_CREATE);

			// Reuse prepared statement to create an entry for each Link definition.
			for (Link link : dm.getLinks())
			{
				LinkEnd end1 = link.getEnd1();
				LinkEnd end2 = link.getEnd2();
				BigInteger end1TypeId = typeNameToIdMap.get(end1.getOwner());
				BigInteger end2TypeId = typeNameToIdMap.get(end2.getOwner());

				ps.setBigDecimal(1, new BigDecimal(end1TypeId));
				ps.setString(2, end1.getName());
				ps.setBoolean(3, end1.getIsArray());
				ps.setBigDecimal(4, new BigDecimal(end2TypeId));
				ps.setString(5, end2.getName());
				ps.setBoolean(6, end2.getIsArray());

				ps.executeUpdate();
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

	}

	@Override
	public void update(DataModel oldDataModel, DataModel dm, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException, InternalException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement psUpdate = null;
		Statement ts = null;

		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_CREATE);

			// Reuse prepared statement to create an entry for each Link definition.
			for (Link link : dm.getLinks())
			{
				Link oldLink = oldDataModel
						.getLinks().stream().filter(
								l -> (l.getEnd1().getName().equals(link.getEnd1().getName())
										&& l.getEnd1().getOwnerObject().getName()
												.equals(link.getEnd1().getOwnerObject().getName())
										&& l.getEnd2().getName().equals(link.getEnd2().getName())
										&& l.getEnd2().getOwnerObject().getName()
												.equals(link.getEnd2().getOwnerObject().getName())))
						.findFirst().orElse(null);

				// Only create if it didn't exist in the old model
				if (oldLink == null)
				{
					LinkEnd end1 = link.getEnd1();
					LinkEnd end2 = link.getEnd2();
					BigInteger end1TypeId = typeNameToIdMap.get(end1.getOwner());
					BigInteger end2TypeId = typeNameToIdMap.get(end2.getOwner());

					ps.setBigDecimal(1, new BigDecimal(end1TypeId));
					ps.setString(2, end1.getName());
					ps.setBoolean(3, end1.getIsArray());
					ps.setBigDecimal(4, new BigDecimal(end2TypeId));
					ps.setString(5, end2.getName());
					ps.setBoolean(6, end2.getIsArray());

					ps.executeUpdate();
				}
				else
				{
					// Existing link, so only update arrayness if the arrayness of either end has changed.
					if (link.getEnd1().getIsArray() != oldLink.getEnd1().getIsArray()
							|| link.getEnd2().getIsArray() != oldLink.getEnd2().getIsArray())
					{
						// Lazily create PS for update on first use
						if (psUpdate == null)
						{
							psUpdate = conn.prepareStatement(SQL_UPDATE_ARRAYNESS);
						}

						// Set new arrayness of each end
						psUpdate.setBoolean(1, link.getEnd1().getIsArray());
						psUpdate.setBoolean(2, link.getEnd2().getIsArray());
						
						// Set type ids and end names for WHERE clause
						BigInteger end1TypeId = typeNameToIdMap.get(oldLink.getEnd1().getOwnerObject().getName());
						BigInteger end2TypeId = typeNameToIdMap.get(oldLink.getEnd2().getOwnerObject().getName());

						psUpdate.setBigDecimal(3, new BigDecimal(end1TypeId));
						psUpdate.setString(4, oldLink.getEnd1().getName());
						psUpdate.setBigDecimal(5, new BigDecimal(end2TypeId));
						psUpdate.setString(6, oldLink.getEnd2().getName());
						int count = psUpdate.executeUpdate();
						if (count != 1)
						{
							// Theoretically impossible if model has been validated
							throw InternalException.newInternalException("Update of link didn't find a match");
						}
					}
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

	}
}
