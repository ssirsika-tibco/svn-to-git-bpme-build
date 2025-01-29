package com.tibco.bpm.cdm.core.dao.impl.oracle;



import javax.sql.DataSource;

import com.tibco.bpm.cdm.core.dao.IdentifierValueDAO;

/*
 * Implementation for DAO for obtaining values for auto-generated case identifiers
 *
 * Implementation specific to Oracle database
 */
public class IdentifierValueDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl implements IdentifierValueDAO
{
	
	public IdentifierValueDAOImpl(DataSource dataSource) {
		super(dataSource);
	}


}
