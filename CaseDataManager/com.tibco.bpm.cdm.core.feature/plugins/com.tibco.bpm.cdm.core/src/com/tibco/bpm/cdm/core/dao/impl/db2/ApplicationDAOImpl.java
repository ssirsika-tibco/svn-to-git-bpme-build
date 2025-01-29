package com.tibco.bpm.cdm.core.dao.impl.db2;



import javax.sql.DataSource;

/**
 * Implementation specific to IBM DB2 database. Reusing the implementation for
 * the oracle database.
 * 
 * @author spanse
 * @since 20-Dec-2021
 * 
 */
public class ApplicationDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.ApplicationDAOImpl 
{

	public ApplicationDAOImpl(DataSource dataSource) {
		super(dataSource);
	}



}
