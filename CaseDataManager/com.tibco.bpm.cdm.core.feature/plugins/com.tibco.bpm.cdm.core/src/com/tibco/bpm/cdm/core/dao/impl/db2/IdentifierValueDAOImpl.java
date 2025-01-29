package com.tibco.bpm.cdm.core.dao.impl.db2;



import javax.sql.DataSource;

/*
 * Implementation for DAO for obtaining values for auto-generated case identifiers
 *
 * Implementation specific to IBM DB2 database
 * 
 * @author spanse
 * @since 20-Dec-2021
 */
public class IdentifierValueDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl 
{

	public IdentifierValueDAOImpl(DataSource dataSource) {
		super(dataSource);
	}



}
