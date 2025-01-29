package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.IdentifierInitialisationInfoDAO;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.IdentifierInitialisationInfo;
import com.tibco.bpm.da.dm.api.StructuredType;

public class IdentifierInitialisationInfoDAOImpl extends AbstractDAOImpl implements IdentifierInitialisationInfoDAO
{
	private static final String	SQL_CREATE	= "INSERT INTO cdm_identifier_infos (type_id, prefix, suffix, min_num_length, next_num) "
			+ "VALUES (?, ?, ?, ?, 0)";

	private static final String	SQL_UPDATE	= "UPDATE cdm_identifier_infos SET prefix = ?, suffix = ?, min_num_length = ? "
			+ "WHERE type_id = ?";

	private static final String	SQL_DELETE	= "DELETE FROM cdm_identifier_infos WHERE type_id = ?";

	public IdentifierInitialisationInfoDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}

	@Override
	public void create(DataModel dataModel, Map<String, BigInteger> typeNameToIdMap) throws PersistenceException
	{
		Connection conn = null;
		Statement ts = null;
		try
		{
			conn = getConnection();
			
			List<BigInteger> iiiTypeIds = new ArrayList<>();
			for (StructuredType type : dataModel.getStructuredTypes())
			{
				if (type.getIsCase())
				{
					IdentifierInitialisationInfo iii = type.getIdentifierInitialisationInfo();
					if (iii != null)
					{
						iiiTypeIds.add(typeNameToIdMap.get(type.getName()));
						createIII(conn, typeNameToIdMap.get(type.getName()), iii.getPrefix(), iii.getSuffix(),
								iii.getMinNumLength());
					}
				}
			}
		}
		catch (Exception e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, null, conn);
		}
	}

	private void createIII(Connection conn, BigInteger typeId, String prefix, String suffix, Integer minNumLength)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		try
		{
			ps = conn.prepareStatement(SQL_CREATE);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			if (prefix != null)
			{
				ps.setString(2, prefix);
			}
			else
			{
				ps.setNull(2, Types.CHAR);
			}
			if (suffix != null)
			{
				ps.setString(3, suffix);
			}
			else
			{
				ps.setNull(3, Types.CHAR);
			}
			if (minNumLength != null)
			{
				ps.setInt(4, minNumLength);
			}
			else
			{
				ps.setNull(4, Types.INTEGER);
			}

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(null, ps, null);
		}
	}

	private void updateIII(Connection conn, BigInteger typeId, String prefix, String suffix, Integer minNumLength)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		try
		{
			ps = conn.prepareStatement(SQL_UPDATE);
			if (prefix != null)
			{
				ps.setString(1, prefix);
			}
			else
			{
				ps.setNull(1, Types.CHAR);
			}
			if (suffix != null)
			{
				ps.setString(2, suffix);
			}
			else
			{
				ps.setNull(2, Types.CHAR);
			}
			if (minNumLength != null)
			{
				ps.setInt(3, minNumLength);
			}
			else
			{
				ps.setNull(3, Types.INTEGER);
			}
			ps.setBigDecimal(4, new BigDecimal(typeId));

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(null, ps, null);
		}
	}

	private void deleteIII(Connection conn, BigInteger typeId) throws PersistenceException
	{
		PreparedStatement ps = null;
		try
		{
			ps = conn.prepareStatement(SQL_DELETE);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(null, ps, null);
		}
	}

	private boolean areIIIsTheSame(IdentifierInitialisationInfo iii1, IdentifierInitialisationInfo iii2)
	{
		boolean result = true;

		if (iii1 == null)
		{
			if (iii2 != null)
			{
				result = false;
			}
		}

		if (iii2 == null)
		{
			if (iii1 != null)
			{
				result = false;
			}
		}

		if (iii1 != null && iii2 != null)
		{
			String prefix1 = iii1.getPrefix();
			String suffix1 = iii1.getSuffix();
			Integer minNumLength1 = iii1.getMinNumLength();
			String prefix2 = iii2.getPrefix();
			String suffix2 = iii2.getSuffix();
			Integer minNumLength2 = iii2.getMinNumLength();

			if (prefix1 == null)
			{
				if (prefix2 != null)
				{
					result = false;
				}
			}
			else
			{
				if (!prefix1.equals(prefix2))
				{
					result = false;
				}
			}

			if (result)
			{
				if (suffix1 == null)
				{
					if (suffix2 != null)
					{
						result = false;
					}
				}
				else
				{
					if (!suffix1.equals(suffix2))
					{
						result = false;
					}
				}
			}

			if (result)
			{
				if (minNumLength1 == null)
				{
					if (minNumLength2 != null)
					{
						result = false;
					}
				}
				else
				{
					if (!minNumLength1.equals(minNumLength2))
					{
						result = false;
					}
				}
			}

		}
		return result;
	}

	@Override
	public void update(BigInteger dataModelId, DataModel oldDataModel, DataModel newDataModel,
			Map<String, BigInteger> typeNameToIdMap) throws PersistenceException
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			for (StructuredType type : newDataModel.getStructuredTypes())
			{
				if (type.getIsCase())
				{
					BigInteger typeId = typeNameToIdMap.get(type.getName());
					IdentifierInitialisationInfo iii = type.getIdentifierInitialisationInfo();
					StructuredType oldType = oldDataModel.getStructuredTypeByName(type.getName());
					if (oldType != null)
					{
						// Type already existed
						IdentifierInitialisationInfo oldIII = oldType.getIdentifierInitialisationInfo();
						if (iii == null)
						{
							// Has no III, so if it had one before, remove it
							if (oldIII != null)
							{
								// Remove III
								deleteIII(conn, typeId);
							}
						}
						else
						{
							// Has an III, so if it had one before, update it (if it's changed)
							// If it didn't have one before, create it.
							if (oldIII != null)
							{
								// Have any elements of the III changed?
								if (!areIIIsTheSame(oldIII, iii))
								{
									// Update III
									updateIII(conn, typeId, iii.getPrefix(), iii.getSuffix(), iii.getMinNumLength());
								}
							}
							else
							{
								// Add III
								createIII(conn, typeId, iii.getPrefix(), iii.getSuffix(), iii.getMinNumLength());
							}
						}
					}
					else
					{
						// New type - Add III, if it has one
						if (iii != null)
						{
							createIII(conn, typeId, iii.getPrefix(), iii.getSuffix(), iii.getMinNumLength());
						}
					}
				}
			}
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}

}
