package com.tibco.bpm.cdm.core.dao.impl.mssql;



import javax.sql.DataSource;


/**
 * MS-SQL implementation of the StateDAO interface that persists states in the cdm_states database table.
 * Reusing the implementation for the oracle database.
 */
public class StateDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.StateDAOImpl 
{

	public StateDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	

}
