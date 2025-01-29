package com.tibco.bpm.cdm.core.dao.impl.oracle;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.TypeDAO;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * Oracle implementation of the TypelDAO interface that persists types in the cdm_types database table.
 */
public class TypeDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.TypeDAOImpl implements TypeDAO
{
	/**
	 * Persists a type and returns an id (assigned from a sequence)
	 */
	private static final String	SQL_CREATE							= "INSERT INTO cdm_types (datamodel_id, name, is_case) "
			+ "VALUES (?, ?, ?)";

	private static final String	SQL_GET_CASE_TYPE_IDS_BY_APP		= "SELECT id FROM cdm_types "
			+ "WHERE is_case = 1 AND datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE application_id =  ?)";

	protected String	SQL_GET								= "SELECT * FROM ( " 
			+ "SELECT t.name as name, t.is_case as is_case, d.id as datamodel_id, d.namespace as namespace, " 
			+ "d.major_version as major_version, a.application_id as application_id, "
			+ "ROW_NUMBER() OVER (ORDER BY a.application_id, d.major_version, d.namespace, t.name) row_num "
			+ "FROM cdm_types t "
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "INNER JOIN cdm_applications a ON d.application_id=a.id %s ) "
			+ "WHERE row_num BETWEEN ? and ?";
	
	// Simpler query that isn't concerned with row numbers
	protected  String	SQL_GET_NO_SKIP						= "SELECT t.name as name, t.is_case as is_case, "
			+ "d.id as datamodel_id, d.namespace as namespace, "
			+ "d.major_version as major_version, a.application_id as application_id FROM cdm_types t "
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "INNER JOIN cdm_applications a ON d.application_id=a.id %s "
			+ "ORDER BY a.application_id, d.major_version, d.namespace, t.name FETCH NEXT ? ROWS ONLY";


	
	public TypeDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.TypeDAO#create(java.math.BigInteger, com.tibco.bpm.da.dm.api.DataModel)
	 */
	@Override
	public Map<String, BigInteger> create(BigInteger dataModelId, DataModel dataModel) throws PersistenceException
	{
		Map<String, BigInteger> nameToIdMap = new HashMap<>();
		Connection conn = null;
		try
		{
			conn = getConnection();
			for (StructuredType type : dataModel.getStructuredTypes())
			{
				BigInteger id = createType(conn, dataModelId, type);
				nameToIdMap.put(type.getName(), id);
			}
		}
		finally
		{
			cleanUp(null, null, conn);
		}
		return nameToIdMap;
	}

	private BigInteger createType(Connection conn, BigInteger dataModelId, StructuredType type)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		try
		{
	
			ps = conn.prepareStatement(SQL_CREATE, new String[]{"id"});
			ps.setBigDecimal(1, new BigDecimal(dataModelId));
			ps.setString(2, type.getName());
			ps.setInt(3, type.getIsCase() ? 1 : 0);

			int success = ps.executeUpdate();

			{
				ResultSet rset = ps.getGeneratedKeys();
				if (rset.next())
				{
					id = rset.getBigDecimal(1).toBigInteger();
				}
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
		return id;
	}

    @Override
    public Map<String, BigInteger> update(BigInteger dataModelId,
            DataModel oldDataModel, DataModel newDataModel)
            throws PersistenceException {
        Map<String, BigInteger> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = getConnection();

            // New types can be added, but existing types can't be removed or
            // have their caseness changed.
            // Therefore, all we have to cope with is the addition of new types.
            for (StructuredType type : newDataModel.getStructuredTypes()) {
                String typeName = type.getName();
                StructuredType oldType =
                        oldDataModel.getStructuredTypeByName(typeName);
                if (oldType == null) {
                    // It's a new type, so add it
                    BigInteger typeId = createType(conn, dataModelId, type);
                    result.put(typeName, typeId);
                }
            }
        } finally {
            cleanUp(null, null, conn);
        }

        return result;
    }

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.TypeDAO#getTypes(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Boolean, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public List<TypeInfoDTO> getTypes(String applicationId, String namespace, Integer majorVersion, Boolean isCase,
			Integer skip, Integer top) throws PersistenceException
	{
		List<TypeInfoDTO> result = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();

			// Build the where clause, based on which properties are being filtered on
			StringBuilder whereClause = new StringBuilder();
			if (applicationId != null)
			{
				if (whereClause.length() != 0)
				{
					whereClause.append(AND);
				}
				else
				{
					whereClause.append(WHERE);
				}
				whereClause.append(SQL_GET_FRAG_WHERE_APPLICATION_ID);
			}
			if (namespace != null)
			{
				if (whereClause.length() != 0)
				{
					whereClause.append(AND);
				}
				else
				{
					whereClause.append(WHERE);
				}
				whereClause.append(SQL_GET_FRAG_WHERE_NAMESPACE);
			}
			if (majorVersion != null)
			{
				if (whereClause.length() != 0)
				{
					whereClause.append(AND);
				}
				else
				{
					whereClause.append(WHERE);
				}
				whereClause.append(SQL_GET_FRAG_WHERE_MAJOR_VERSION);
			}
			if (isCase != null)
			{
				if (whereClause.length() != 0)
				{
					whereClause.append(AND);
				}
				else
				{
					whereClause.append(WHERE);
				}
				whereClause.append(SQL_GET_FRAG_WHERE_IS_CASE);
			}
						
			// Choose the appropriate template (depending on whether skip is used) and plug
			// in the where clause.
			String sql = String.format(skip != null ? SQL_GET : SQL_GET_NO_SKIP, whereClause.toString());

			ps = conn.prepareStatement(sql);

			int pos = 1;

			// Set optional parameter values for where clause
			if (applicationId != null)
			{
				ps.setString(pos++, applicationId);
			}
			if (namespace != null)
			{
				ps.setString(pos++, namespace);
			}
			if (majorVersion != null)
			{
				ps.setInt(pos++, majorVersion);
			}
			if (isCase != null)
			{
				ps.setInt(pos++, isCase ? 1: 0);
			}

			if (skip != null)
			{
				ps.setInt(pos++, skip);
			}

			// Top is mandatory
			ps.setInt(pos++, top);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					TypeInfoDTO ti = new TypeInfoDTO();
					ti.setName(rset.getString("name"));
					// Note: Label is not set here - It is added from the Data Model later
					// We _could_ store it in the DB, but it seems inevitable that the /types
					// API will be enhanced in other ways in the near future to require the
					// Data Model to be loaded anyway...
					ti.setDataModelId(rset.getBigDecimal("datamodel_id").toBigInteger());
					boolean isCaseValue = false;
					if (rset.getInt("is_case") == 1) {
						isCaseValue = true;
					}
					ti.setIsCase(isCaseValue);
					ti.setNamespace(rset.getString("namespace"));
					ti.setApplicationMajorVersion(rset.getInt("major_version"));
					ti.setApplicationId(rset.getString("application_id"));
					result.add(ti);
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
	public List<BigInteger> getCaseTypeIdsByApplication(BigInteger applicationId) throws PersistenceException
	{
		
		super.SQL_GET_CASE_TYPE_IDS_BY_APP = SQL_GET_CASE_TYPE_IDS_BY_APP;
		return super.getCaseTypeIdsByApplication(applicationId);
		
	}

	@Override
	public TypeInfoDTO getType(BigInteger typeId) throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		TypeInfoDTO info = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_GET_CASE_TYPE_BY_ID);
			ps.setBigDecimal(1, new BigDecimal(typeId));

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					// TODO reading from result set is exactly like method that reads many. Share code.
					info = new TypeInfoDTO();
					info.setName(rset.getString("name"));
					info.setDataModelId(rset.getBigDecimal("datamodel_id").toBigInteger());
					boolean isCaseValue = false;
					if (rset.getInt("is_case") == 1) {
						isCaseValue = true;
					}
					info.setIsCase(isCaseValue);
					info.setNamespace(rset.getString("namespace"));
					info.setApplicationMajorVersion(rset.getInt("major_version"));
					info.setApplicationId(rset.getString("application_id"));
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
		return info;		

	}

}
