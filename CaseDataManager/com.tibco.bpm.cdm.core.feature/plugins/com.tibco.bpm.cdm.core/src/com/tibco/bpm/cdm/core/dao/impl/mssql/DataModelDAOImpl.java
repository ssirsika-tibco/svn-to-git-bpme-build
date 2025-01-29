package com.tibco.bpm.cdm.core.dao.impl.mssql;

import javax.sql.DataSource;

/**
 * Implementation specific to MS-SQL database
 * Reusing the implementation for the oracle database.
 */
public class DataModelDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.DataModelDAOImpl
{

	
	public DataModelDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	

}
