package com.tibco.bpm.cdm.core.dao.impl.oracle;


import java.math.BigInteger;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;

/**
 * Implementation specific to Oracle database
 */
public class DataModelDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.DataModelDAOImpl implements DataModelDAO
{

	// Insert into cdm_datamodels, auto-populating id and deployment_timestamp, returning id
	private static final String		SQL_CREATE				= "INSERT INTO cdm_datamodels (application_id, major_version, namespace, model,script) VALUES "
			+ "(?, ?, ?, ?, ?)";

	
	public DataModelDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#create(java.math.BigInteger, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public BigInteger create(BigInteger applicationId, int majorVersion, String namespace, String model, String script)
			throws PersistenceException
	{
		
		super.SQL_CREATE = SQL_CREATE;
		return super.create(applicationId, majorVersion, namespace, model, script);

	}
}
