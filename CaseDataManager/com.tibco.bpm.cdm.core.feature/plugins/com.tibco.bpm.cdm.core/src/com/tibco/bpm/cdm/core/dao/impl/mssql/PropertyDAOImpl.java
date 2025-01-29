package com.tibco.bpm.cdm.core.dao.impl.mssql;


import javax.sql.DataSource;


/**
 * MS-SQL implementation of DAO for arbitrary name/value pairs stored in the database
 */
public class PropertyDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.PropertyDAOImpl 
{

	public PropertyDAOImpl(DataSource dataSource) {
		super(dataSource);
	}


}
