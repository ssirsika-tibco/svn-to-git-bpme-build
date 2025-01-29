package com.tibco.bpm.cdm.core.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;


public class DAOFactory {
	
	// Database type = Oracle
	private static final String ORACLE = "oracle";
	// Database type = PostgreSQL
	private static final String POSTGRES = "postgres";	
	// Database type = MS SQL
	private static final String MS_SQL = "microsoft";

    // Database type = IBM DB2
    private static final String DB2 = "db2";

	private String dbtype = null;
		
	protected DataSource dataSource;
	
	protected ApplicationDAO appDAOImpl;
	
	protected CaseDAO caseDAOImpl;
	
	protected CaseLinkDAO caseLinkDAOImpl;
	
	protected DataModelDAO dataModelDAOImpl;

	protected LinkDAO linkDAOImpl;

	protected PropertyDAO	propertyDAOImpl;
	
	protected StateDAO	stateDAOImpl;
	
	protected TypeDAO	typeDAOImpl;
	
	protected IdentifierValueDAO identifierValueDAOImpl;
	
	protected IdentifierInitialisationInfoDAO identifierInitialisationInfoDAOImpl;

	protected DataModelDependencyDAO dataModelDependencyDAOImpl;
	
	protected CasesTableIndexDAO casesTableIndexDAOImpl;
	
	protected SimpleSearchRenderer simpleSearchRenderer;
	
	protected ConditionRenderer conditionRenderer;
	
	public ApplicationDAO getApplicationDAOImpl() {
		return appDAOImpl;
	}

	public CaseDAO getCaseDAOImpl() {
		return caseDAOImpl;
	}

	public DataModelDAO getDataModelDAOImpl() {
		return dataModelDAOImpl;
	}

	public LinkDAO getLinkDAOImpl() {
		return linkDAOImpl;
	}

	public PropertyDAO getPropertyDAOImpl() {
		return propertyDAOImpl;
	}


	public StateDAO getStateDAOImpl() {
		return stateDAOImpl;
	}


	public TypeDAO getTypeDAOImpl() {
		return typeDAOImpl;
	}
	
	public CaseLinkDAO getCaseLinkDAOImpl() {
		return caseLinkDAOImpl;
	}

	public IdentifierValueDAO getIdentifierValueDAOImpl() {
		return identifierValueDAOImpl;
	}

	public IdentifierInitialisationInfoDAO getIdentifierInitialisationInfoDAOImpl() {
		return identifierInitialisationInfoDAOImpl;
	}

	public DataModelDependencyDAO getDataModelDependencyDAOImpl() {
		return dataModelDependencyDAOImpl;
	}

	public CasesTableIndexDAO getCasesTableIndexDAOImpl() {
		return casesTableIndexDAOImpl;
	}

	public SimpleSearchRenderer getSimpleSearchRenderer() {
		return simpleSearchRenderer;
	}

	public ConditionRenderer getConditionRenderer() {
		return conditionRenderer;
	}

	public DataSource getDataSource()
	{
		return dataSource;
	}

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
		findOutDatabaseType();
		setDAOImpl();
	}
	
	public String getDatabaseType() {
		if (dbtype == null) {
			findOutDatabaseType();
		}
		return dbtype;
	}
	
	private void findOutDatabaseType() {
		Connection conn = DataSourceUtils.getConnection(dataSource);
		if (conn == null)
		{
			return;
		}
		if (dbtype == null) {
			try {
			dbtype = conn.getMetaData().getDatabaseProductName();
			DataSourceUtils.releaseConnection(conn, dataSource);
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}			
		

	}
	
	
	private void setDAOImpl() {
		
		if (dbtype.toLowerCase().contains(ORACLE)) {
			appDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.ApplicationDAOImpl(getDataSource());
			caseDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.CaseDAOImpl(getDataSource());
			caseLinkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.CaseLinkDAOImpl(getDataSource());
			dataModelDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.DataModelDAOImpl(getDataSource());
			linkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.LinkDAOImpl(getDataSource());
			propertyDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.PropertyDAOImpl(getDataSource());
			stateDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.StateDAOImpl(getDataSource());
			typeDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.TypeDAOImpl(getDataSource());
			identifierValueDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.IdentifierValueDAOImpl(getDataSource());
			identifierInitialisationInfoDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.IdentifierInitialisationInfoDAOImpl(getDataSource());
			dataModelDependencyDAOImpl =  new com.tibco.bpm.cdm.core.dao.impl.oracle.DataModelDependencyDAOImpl(getDataSource());
			casesTableIndexDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.oracle.CasesTableIndexDAOImpl(getDataSource());
			simpleSearchRenderer = new com.tibco.bpm.cdm.core.dao.impl.oracle.SimpleSearchRendererImpl();
			conditionRenderer = new com.tibco.bpm.cdm.core.dao.impl.oracle.ConditionRendererImpl();
			caseDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseLinkDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setLinkDAO(linkDAOImpl);
		} else if (dbtype.toLowerCase().contains(POSTGRES)) {
			appDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.ApplicationDAOImpl(getDataSource());
			caseDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CaseDAOImpl(getDataSource());
			caseLinkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CaseLinkDAOImpl(getDataSource());
			dataModelDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.DataModelDAOImpl(getDataSource());
			linkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.LinkDAOImpl(getDataSource());
			propertyDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.PropertyDAOImpl(getDataSource());
			stateDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.StateDAOImpl(getDataSource());
			typeDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.TypeDAOImpl(getDataSource());
			identifierValueDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl(getDataSource());
			identifierInitialisationInfoDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.IdentifierInitialisationInfoDAOImpl(getDataSource());
			dataModelDependencyDAOImpl =  new com.tibco.bpm.cdm.core.dao.impl.DataModelDependencyDAOImpl(getDataSource());
			casesTableIndexDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CasesTableIndexDAOImpl(getDataSource());
			simpleSearchRenderer = new com.tibco.bpm.cdm.core.dao.impl.SimpleSearchRendererImpl();
			conditionRenderer = new com.tibco.bpm.cdm.core.dao.impl.ConditionRendererImpl();
			caseDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseLinkDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setLinkDAO(linkDAOImpl);			
		} else if (dbtype.toLowerCase().contains(MS_SQL)) {
			appDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.ApplicationDAOImpl(getDataSource());
			caseDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.CaseDAOImpl(getDataSource());
			caseLinkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.CaseLinkDAOImpl(getDataSource());
			dataModelDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.DataModelDAOImpl(getDataSource());
			linkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.LinkDAOImpl(getDataSource());
			propertyDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.PropertyDAOImpl(getDataSource());
			stateDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.StateDAOImpl(getDataSource());
			typeDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.TypeDAOImpl(getDataSource());
			identifierValueDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.IdentifierValueDAOImpl(getDataSource());
			identifierInitialisationInfoDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.IdentifierInitialisationInfoDAOImpl(getDataSource());
			dataModelDependencyDAOImpl =  new com.tibco.bpm.cdm.core.dao.impl.mssql.DataModelDependencyDAOImpl(getDataSource());
			casesTableIndexDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.mssql.CasesTableIndexDAOImpl(getDataSource());
			simpleSearchRenderer = new com.tibco.bpm.cdm.core.dao.impl.mssql.SimpleSearchRendererImpl();
			conditionRenderer = new com.tibco.bpm.cdm.core.dao.impl.mssql.ConditionRendererImpl();
			caseDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseLinkDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setLinkDAO(linkDAOImpl);			
        } else if (dbtype.toLowerCase().contains(DB2)) {
            appDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.ApplicationDAOImpl(
                            getDataSource());
            caseDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.db2.CaseDAOImpl(
                    getDataSource());
            caseLinkDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.CaseLinkDAOImpl(
                            getDataSource());
            dataModelDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.DataModelDAOImpl(
                            getDataSource());
            linkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.db2.LinkDAOImpl(
                    getDataSource());
            propertyDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.PropertyDAOImpl(
                            getDataSource());
            stateDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.db2.StateDAOImpl(
                    getDataSource());
            typeDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.db2.TypeDAOImpl(
                    getDataSource());
            identifierValueDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.IdentifierValueDAOImpl(
                            getDataSource());
            identifierInitialisationInfoDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.IdentifierInitialisationInfoDAOImpl(
                            getDataSource());
            dataModelDependencyDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.DataModelDependencyDAOImpl(
                            getDataSource());
            casesTableIndexDAOImpl =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.CasesTableIndexDAOImpl(
                            getDataSource());
            simpleSearchRenderer =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.SimpleSearchRendererImpl();
            conditionRenderer =
                    new com.tibco.bpm.cdm.core.dao.impl.db2.ConditionRendererImpl();
            caseDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
            caseDAOImpl.setConditionRenderer(conditionRenderer);
            caseLinkDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
            caseLinkDAOImpl.setConditionRenderer(conditionRenderer);
            caseLinkDAOImpl.setLinkDAO(linkDAOImpl);
        } else {
			appDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.ApplicationDAOImpl(getDataSource());
			caseDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CaseDAOImpl(getDataSource());
			caseLinkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CaseLinkDAOImpl(getDataSource());
			dataModelDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.DataModelDAOImpl(getDataSource());
			linkDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.LinkDAOImpl(getDataSource());
			propertyDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.PropertyDAOImpl(getDataSource());
			stateDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.StateDAOImpl(getDataSource());
			typeDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.TypeDAOImpl(getDataSource());
			identifierValueDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl(getDataSource());
			identifierInitialisationInfoDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.IdentifierInitialisationInfoDAOImpl(getDataSource());
			dataModelDependencyDAOImpl =  new com.tibco.bpm.cdm.core.dao.impl.DataModelDependencyDAOImpl(getDataSource());
			casesTableIndexDAOImpl = new com.tibco.bpm.cdm.core.dao.impl.CasesTableIndexDAOImpl(getDataSource());
			simpleSearchRenderer = new com.tibco.bpm.cdm.core.dao.impl.SimpleSearchRendererImpl();
			conditionRenderer = new com.tibco.bpm.cdm.core.dao.impl.ConditionRendererImpl();
			caseDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setSimpleSearchRenderer(simpleSearchRenderer);
			caseLinkDAOImpl.setConditionRenderer(conditionRenderer);
			caseLinkDAOImpl.setLinkDAO(linkDAOImpl);			
		}			
	}


}
