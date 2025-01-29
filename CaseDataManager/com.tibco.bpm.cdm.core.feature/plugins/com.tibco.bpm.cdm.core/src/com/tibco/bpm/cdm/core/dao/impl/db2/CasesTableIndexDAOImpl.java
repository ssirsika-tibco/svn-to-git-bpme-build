package com.tibco.bpm.cdm.core.dao.impl.db2;

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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * Implementation specific to IBM DB2 database
 * 
 * @author spanse
 * @since 20-Dec-2021
 */
public class CasesTableIndexDAOImpl
        extends com.tibco.bpm.cdm.core.dao.impl.CasesTableIndexDAOImpl
{
	/**
	 * Format for index names. The replaceable portions are:
	 * - Last fragment of namespace
	 * - Type name
	 * - Attribute name
	 * All are abbreviated to ensure the identifier doesn't exceed 63 bytes (the PostgreS limit) or
	 * collide with existing indexes (given that index names are unique at schema level).
	 */
	private static final String	INDEX_NAME_TEMPLATE	= "i_cdm_cases_int_%s_%d";

	/**
	 * DDL to create an index
	 */
	private static final String	DDL_CREATE_TEMPLATE	= "CREATE INDEX %s ON cdm_cases_int ((casedata->'%s')) "
			+ "WHERE type_id = %s";

	/**
	 * DDL to drop an index
	 */
	private static final String	DDL_DELETE_TEMPLATE	= "DROP INDEX %s";

	/**
	 * SQL to store index meta-data in cdm_type_indexes
	 */
	private static final String	SQL_CREATE			= "INSERT INTO cdm_type_indexes (type_id, name, attribute_name) "
			+ "VALUES (?, ?, ?)";

	private static final String	SQL_READ			= "SELECT name, attribute_name FROM cdm_type_indexes WHERE type_id = ?";

	private static final String	SQL_DELETE			= "DELETE FROM cdm_type_indexes WHERE type_id = ? AND attribute_name = ?";

	/**
	 * SQL to deletes the cdm_type_indexes rows for a given deployment id, returning the names
	 * of those indexes (such that the names can be used to drop the actual indexes).
	 */
	private static final String	SQL_DELETE_ALL		= "DELETE FROM cdm_type_indexes WHERE type_id IN "
			+ "(SELECT id FROM cdm_types WHERE datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE application_id IN "
			+ "(SELECT id FROM cdm_applications WHERE deployment_id = ?)))";

	
	public CasesTableIndexDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Runs the given DDL via the given Connection
	 * @param conn
	 * @param ddl
	 * @throws PersistenceException
	 */
	private void runDDL(Connection conn, String ddl) throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{


			ps = conn.prepareStatement(ddl);
			ps.execute();
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
	public void create(BigInteger dataModelId, DataModel dataModel, Map<String, BigInteger> typeNameToIdMap)
			throws PersistenceException
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			for (StructuredType caseType : dataModel.getStructuredTypes().stream().filter(st -> st.getIsCase())
					.collect(Collectors.toList()))
			{
				String typeName = caseType.getName();
				BigInteger typeId = typeNameToIdMap.get(typeName);
				String typeIdString = typeId.toString();
				int idx = 1;
				for (Attribute attribute : caseType.getAttributes().stream().filter(a -> a.getIsSearchable())
						.collect(Collectors.toList()))
				{
					String attributeName = attribute.getName();
					String indexName = String.format(INDEX_NAME_TEMPLATE, typeIdString, idx++);

					// Note that '?' markers can't be included in the DDL when doing CREATE INDEX.
					// See: https://www.postgresql.org/docs/current/plpgsql-statements.html#PLPGSQL-STATEMENTS-EXECUTING-DYN
					// "Another restriction on parameter symbols is that they only work in SELECT, INSERT, 
					// UPDATE, and DELETE commands. In other statement types (generically called utility statements), 
					// you must insert values textually even if they are just data values."
					
					//The index that is getting created here uses the syntax specific to postgres.
					//TBD: Need to figure out what works for oracle and postgres.
					
					//createIndex(conn, indexName, attributeName, typeId);
					createTypeIndexReference(conn, typeId, indexName, attributeName);
				}
			}
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}

	@Override
	public void update(BigInteger dataModelId, DataModel oldDataModel, DataModel newDataModel,
			Map<String, BigInteger> typeNameToIdMap) throws PersistenceException
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Types can only be added, not removed, so we can assume the list of types in the new model
			// includes all those from the old model.
			for (StructuredType type : newDataModel.getStructuredTypes())
			{
				List<String> addList = new ArrayList<>();
				List<String> removeList = new ArrayList<>();

				// Find corresponding type in old model
				String typeName = type.getName();
				StructuredType oldType = oldDataModel.getStructuredTypeByName(typeName);
				BigInteger typeId = typeNameToIdMap.get(typeName);
				Map<String, String> attributeNameToIndexNameMap = readIndexReferences(conn, typeId);

				// Attributes can't be removed, only added, but existing searchable attributes can
				// be made non-searchable, in where case their indexes can be removed (and their names
				// re-used by new indexes)
				for (Attribute attr : type.getAttributes())
				{
					String attrName = attr.getName();
					boolean newSearchable = attr.getIsSearchable();

					// Find corresponding attribute definition in old model (if this type existed)
					Attribute oldAttr = oldType != null ? oldType.getAttributeByName(attrName) : null;
					boolean needToAddIndex = false;
					if (oldAttr != null)
					{
						boolean oldSearcable = oldAttr.getIsSearchable();
						if (newSearchable && !oldSearcable)
						{
							// Existing attribute made searchable
							needToAddIndex = true;
						}
						else if (!newSearchable && oldSearcable)
						{
							// Existing attribute made non-searchable, so remove the index
							removeList.add(attrName);
						}
					}
					else
					{
						// If this new attribute is searchable, an index is required
						needToAddIndex = newSearchable;
					}

					if (needToAddIndex)
					{
						addList.add(attrName);
					}
				}

				// Remove indexes for attributes that are no longer searchable.
				// Note that we do this _before_ adding any new indexes to allow
				// the names to be reused.
				for (String attrName : removeList)
				{
					String indexName = attributeNameToIndexNameMap.get(attrName);
					dropIndex(conn, indexName);
					deleteTypeIndexReference(conn, typeId, attrName);
					attributeNameToIndexNameMap.remove(attrName);

				}

				for (String attrName : addList)
				{
					// Find an available name with the lowest possible numeric suffix
					int num = 1;
					String indexName = null;
					do
					{
						indexName = String.format(INDEX_NAME_TEMPLATE, typeId.toString(), num++);
					}
					while (attributeNameToIndexNameMap.containsValue(indexName));
					
					//The index that is getting created here uses the syntax specific to postgres.
					//TBD: Need to figure out what works for oracle and postgres.
					
					//createIndex(conn, indexName, attrName, typeId);
					createTypeIndexReference(conn, typeId, indexName, attrName);
					attributeNameToIndexNameMap.put(attrName, indexName);
				}
			}
		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}

	private Map<String, String> readIndexReferences(Connection conn, BigInteger typeId) throws PersistenceException
	{
		Map<String, String> attributeNameToIndexNameMap = new HashMap<>();
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{


			ps = conn.prepareStatement(SQL_READ);
			ps.setBigDecimal(1, new BigDecimal(typeId));

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					String indexName = rset.getString(1);
					String attributeName = rset.getString(2);
					attributeNameToIndexNameMap.put(attributeName, indexName);
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

		return attributeNameToIndexNameMap;
	}

	private void createTypeIndexReference(Connection conn, BigInteger typeId, String indexName, String attributeName)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{


			ps = conn.prepareStatement(SQL_CREATE);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(2, indexName);
			ps.setString(3, attributeName);

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

	private void deleteTypeIndexReference(Connection conn, BigInteger typeId, String attributeName)
			throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{


			ps = conn.prepareStatement(SQL_DELETE);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(2, attributeName);

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

	private void createIndex(Connection conn, String indexName, String attributeName, BigInteger typeId)
			throws PersistenceException
	{
		String ddl = String.format(DDL_CREATE_TEMPLATE, indexName, attributeName, typeId);
		runDDL(conn, ddl);

	}

	private void dropIndex(Connection conn, String indexName) throws PersistenceException
	{
		String ddl = String.format(DDL_DELETE_TEMPLATE, indexName);
		runDDL(conn, ddl);
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


				ps = conn.prepareStatement(SQL_DELETE_ALL);
				ps.setBigDecimal(1, new BigDecimal(deploymentId));

				boolean success = ps.execute();

				if (success)
				{
					ResultSet rset = ps.getResultSet();
					while (rset.next())
					{
						String indexName = rset.getString(1);
						dropIndex(conn, indexName);
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

		}
		finally
		{
			cleanUp(null, null, conn);
		}
	}
}
