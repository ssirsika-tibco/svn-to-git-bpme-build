package com.tibco.bpm.cdm.core.dao.impl.oracle;


import javax.sql.DataSource;

import com.tibco.bpm.cdm.core.dao.IdentifierInitialisationInfoDAO;



/*
 * Implementation specific to Oracle database
 */
public class IdentifierInitialisationInfoDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.IdentifierInitialisationInfoDAOImpl implements IdentifierInitialisationInfoDAO
{
	public IdentifierInitialisationInfoDAOImpl(DataSource dataSource) {
		super(dataSource);
	}

	
}
