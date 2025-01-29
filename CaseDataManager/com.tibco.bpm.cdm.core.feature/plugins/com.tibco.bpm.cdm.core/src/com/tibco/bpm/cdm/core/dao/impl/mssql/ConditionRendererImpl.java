package com.tibco.bpm.cdm.core.dao.impl.mssql;

import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;

/**
 * ConditionRenderer implementation to support MS SQL database.
 */
public class ConditionRendererImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.ConditionRendererImpl
{
    protected static final String SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE =
            "(SELECT COUNT(*) FROM OPENJSON(casedata, '$.%s')) %s ? "; //$NON-NLS-1$
    
    protected static final String SQL_ORDER_BY_NUMERIC_TEMPLATE = "cast(json_value(casedata, '$.%s') as real) "; 
    
    protected static final String SQL_ORDER_BY_FIXED_POINT_NUMERIC_TEMPLATE = "cast(json_value(casedata, '$.%s') as decimal(%s,%s)) "; 

    protected String getSizeFunctionTemplate( ) {
    	return SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE;
    }
    
    public String renderOrderBy(SearchConditionDTO condition) {
    	
    	super.SQL_ORDER_BY_NUMERIC_TEMPLATE = SQL_ORDER_BY_NUMERIC_TEMPLATE;
    	super.SQL_ORDER_BY_FIXED_POINT_NUMERIC_TEMPLATE = SQL_ORDER_BY_FIXED_POINT_NUMERIC_TEMPLATE;
    	return super.renderOrderBy(condition);
    }
}
