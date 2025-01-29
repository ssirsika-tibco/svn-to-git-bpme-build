package com.tibco.bpm.cdm.core.dao.impl.oracle;


import java.math.BigInteger;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO;

/**
 * Implementation specific to Oracle database
 */
public class ApplicationDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.ApplicationDAOImpl implements ApplicationDAO
{

	// Insert into cdm_datamodels, auto-populating id and deployment_timestamp, returning id
	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	private static final String		SQL_CREATE								= "INSERT INTO cdm_applications (application_name, application_id, "
			+ "deployment_id, major_version, minor_version, micro_version, qualifier,is_case_app) VALUES "
			+ "(?, ?, ?, ?, ?, ?, ?,?)";


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
		super.SQL_CREATE = SQL_CREATE;
		return super.createOrReadForUpdate(deploymentId, applicationName, applicationId, applicationVersion,isCaseApp);
	}

}
