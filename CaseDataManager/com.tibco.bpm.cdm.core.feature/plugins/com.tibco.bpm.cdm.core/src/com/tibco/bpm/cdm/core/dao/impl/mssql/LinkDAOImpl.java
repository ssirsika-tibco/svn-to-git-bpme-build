package com.tibco.bpm.cdm.core.dao.impl.mssql;



import javax.sql.DataSource;

/**
 * MS-SQL implementation of the LinkDAO interface that persists link definitions in 
 * the cdm_links database table.
 * 
 */
public class LinkDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.LinkDAOImpl
{

	public LinkDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
}
