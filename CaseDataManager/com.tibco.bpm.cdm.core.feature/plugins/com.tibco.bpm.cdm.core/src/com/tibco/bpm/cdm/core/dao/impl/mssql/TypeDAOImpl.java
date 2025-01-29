package com.tibco.bpm.cdm.core.dao.impl.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;

/**
 * MS-SQL implementation of the TypelDAO interface that persists types in the cdm_types database table.
 * Reusing the implementation for the oracle database.
 */
public class TypeDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.TypeDAOImpl
{


	private static final String	SQL_GET	= "WITH types AS( " 
			+ "SELECT t.name as name, t.is_case as is_case, d.id as datamodel_id, d.namespace as namespace, " 
			+ "d.major_version as major_version, a.application_id as application_id, "
			+ "ROW_NUMBER() OVER (ORDER BY a.application_id, d.major_version, d.namespace, t.name) row_num "
			+ "FROM cdm_types t "
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "INNER JOIN cdm_applications a ON d.application_id=a.id %s ) "
			+ "SELECT * from types "
			+ "WHERE row_num >= ? and row_num <= ?";
	
	// Simpler query that isn't concerned with row numbers
	private static final String	SQL_GET_NO_SKIP	= "SELECT t.name as name, t.is_case as is_case, "
			+ "d.id as datamodel_id, d.namespace as namespace, "
			+ "d.major_version as major_version, a.application_id as application_id FROM cdm_types t "
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "INNER JOIN cdm_applications a ON d.application_id=a.id %s "
			+ "ORDER BY a.application_id, d.major_version, d.namespace, t.name "
			+ "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";


	
	public TypeDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.TypeDAO#getTypes(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Boolean, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public List<TypeInfoDTO> getTypes(String applicationId, String namespace, Integer majorVersion, Boolean isCase,
			Integer skip, Integer top) throws PersistenceException
	{
		
		super.SQL_GET = SQL_GET;
		super.SQL_GET_NO_SKIP = SQL_GET_NO_SKIP;
		return super.getTypes(applicationId, namespace, majorVersion, isCase,skip,top);
	}


}
