package com.tibco.bpm.cdm.core.dao.impl.mssql;



import java.math.BigInteger;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;

/**
 * Implementation specific to MS-SQL database
 * Reusing the implementation for the oracle database.
 * 
 */
public class ApplicationDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.ApplicationDAOImpl 
{

	private static final String  SQL_SELECT_FOR_UPDATE	= "SELECT id FROM cdm_applications WHERE "
			+ "application_id = ? AND major_version = ? ";

	public ApplicationDAOImpl(DataSource dataSource) {
		super(dataSource);
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#create(java.math.BigInteger, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public CreationResult createOrReadForUpdate(BigInteger deploymentId, String applicationName, String applicationId,
			String applicationVersion,boolean isCaseApp) throws PersistenceException
	{
		super.SQL_SELECT_FOR_UPDATE = SQL_SELECT_FOR_UPDATE;
		return super.createOrReadForUpdate(deploymentId, applicationName, applicationId, applicationVersion,isCaseApp);
	}	

}
