package com.tibco.bpm.cdm.core.dao.impl.mssql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DataModelDependencyDAO;

/*
 * Implementation specific to MS-SQL database
 */
public class DataModelDependencyDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.DataModelDependencyDAOImpl
{

	public DataModelDependencyDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
	

}
