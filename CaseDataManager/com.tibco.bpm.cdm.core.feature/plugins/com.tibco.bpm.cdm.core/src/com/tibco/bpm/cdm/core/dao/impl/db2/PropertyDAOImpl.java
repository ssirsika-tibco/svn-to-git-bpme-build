package com.tibco.bpm.cdm.core.dao.impl.db2;


import javax.sql.DataSource;


/**
 * IBM DB2 implementation of DAO for arbitrary name/value pairs stored in the
 * database Reusing Oracle implementation.
 * 
 * @author spanse
 * @since 20-Dec-2021
 */
public class PropertyDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.PropertyDAOImpl 
{

	public PropertyDAOImpl(DataSource dataSource) {
		super(dataSource);
	}


}
