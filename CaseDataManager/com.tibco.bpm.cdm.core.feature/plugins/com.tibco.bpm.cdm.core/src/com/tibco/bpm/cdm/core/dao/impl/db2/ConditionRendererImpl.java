package com.tibco.bpm.cdm.core.dao.impl.db2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.tibco.bpm.cdm.libs.dql.dto.AttributeSearchConditionDTO;
import com.tibco.bpm.cdm.libs.dql.dto.ConditionGroupDTO;
import com.tibco.bpm.cdm.libs.dql.dto.ConditionOperator;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SortColumn;
import com.tibco.bpm.cdm.libs.dql.dto.SortOrder;
import com.tibco.bpm.cdm.libs.dql.model.ModelAbstractType;
import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;
import com.tibco.bpm.cdm.libs.dql.model.ModelBaseType;
import com.tibco.bpm.da.dm.api.Constraint;

/**
 * ConditionRenderer implementation to support IBM DB2 database.
 * 
 * @author spanse
 * @since 01-Dec-2021
 */
public class ConditionRendererImpl extends com.tibco.bpm.cdm.core.dao.impl.oracle.ConditionRendererImpl
{

    protected static final String SQL_GET_CONDITION_NUMERIC_TEMPLATE =
            "json_value(casedata, '$.%s' RETURNING REAL) %s ? "; //$NON-NLS-1$
    
    protected static final String SQL_GET_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE =
            "json_value(casedata, '$.%s' RETURNING DECIMAL(%s,%s)) %s ? "; //$NON-NLS-1$

    protected static final String SQL_GET_BETN_CONDITION_NUMERIC_TEMPLATE =
            "json_value(casedata, '$.%s' RETURNING REAL) %s ? and ? "; //$NON-NLS-1$

    protected static final String SQL_GET_BETN_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE =
            "json_value(casedata, '$.%s' RETURNING DECIMAL(%s,%s)) %s ? and ? "; //$NON-NLS-1$
    
    protected static final String SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE =
            "(SYSTOOLS.JSON_LEN(SYSTOOLS.JSON2BSON(casedata), '%s')) %s ? "; //$NON-NLS-1$

    private static final String SQL_ORDER_BY_CLAUSE = " order by ";
    
    private static final String SQL_ORDER_BY_TEMPLATE = "json_value(casedata, '$.%s') as xx";
    
    private static final String SQL_ORDER_BY_NUMERIC_TEMPLATE = "json_value(casedata, '$.%s' RETURNING REAL) as xx";
    
    private static final String SQL_ORDER_BY_FIXED_POINT_TEMPLATE = "json_value(casedata, '$.%s' RETURNING DECIMAL(%s,%s)) as xx";

    
	protected String getBetweenConditionTemplate(AttributeSearchConditionDTO asc) {
		
		String template;
        String attrName = asc.getAttribute().getReferenceName();
        String operator = asc.getOperator().toString();
        ModelAbstractType type = asc.getAttribute().getType();
        
    	if (type == ModelBaseType.NUMBER ) {
    		template = String.format(SQL_GET_BETN_CONDITION_NUMERIC_TEMPLATE, attrName, operator);;
    	} else if (type == ModelBaseType.FIXED_POINT_NUMBER){
    		String precision = asc.getAttribute().getConstraint(Constraint.NAME_LENGTH);
    		String scale = asc.getAttribute().getConstraint(Constraint.NAME_DECIMAL_PLACES);
    		template = String.format(
        			SQL_GET_BETN_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE, attrName, precision, scale, operator);
    		
    	} else {
    		template = String.format(SQL_GET_BETN_CONDITION_TEMPLATE, attrName, operator);
    	}
    	return template;
	}
	
	protected String getGetConditionTemplate(AttributeSearchConditionDTO asc) {
		
		String template;
        String attrName = asc.getAttribute().getReferenceName();
        String operator = asc.getOperator().toString();
        ModelAbstractType type = asc.getAttribute().getType();
 
    	if (type == ModelBaseType.NUMBER) {
    		template = String.format(
        			SQL_GET_CONDITION_NUMERIC_TEMPLATE, attrName, operator);
    	} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
    		String precision = asc.getAttribute().getConstraint(Constraint.NAME_LENGTH);
    		String scale = asc.getAttribute().getConstraint(Constraint.NAME_DECIMAL_PLACES);
    		template = String.format(
        			SQL_GET_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE, attrName, precision, scale, operator);
    	} else {
    		if(asc.isFunctionUpperPresent()) {
    			template = String.format(
            			SQL_GET_CONDITION_FUNCTION_TEMPLATE, "upper", attrName, operator);
    		} else if (asc.isFunctionLowerPresent()) {
    			template = String.format(
    					SQL_GET_CONDITION_FUNCTION_TEMPLATE, "lower", attrName, operator);
    		} else if (asc.isFunctionSizePresent()) {
    			template = String.format(SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE, attrName, operator);
    		} else {
    			template = String.format(SQL_GET_CONDITION_TEMPLATE, attrName, operator);
    		}
    	}        
        return template;
	}
	
    protected String getSizeFunctionTemplate( ) {
    	return SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE;
    }

	protected int setParametersBetweenCondition(PreparedStatement ps, int idx, ModelAbstractType type, String ascValue) throws SQLException {		
		if (ascValue.indexOf("and") != -1) {
			int index = ascValue.indexOf("and");
			String value1 = ascValue.substring(0,index).trim();
			String value2 = ascValue.substring(index+3).trim();
			ps.setString(idx++, value1);
			ps.setString(idx++, value2);
		}
		return idx;
	}
	
	protected int setParamtersInCondition(PreparedStatement ps, int idx, ModelAbstractType type, String ascValue) throws SQLException {
		List<String> values = splitValues(ascValue);
		for(String value : values) {
			if (value.startsWith("'") && value.endsWith("'")) {
				//The values provided for in operator are quoted.
				value = value.substring(1,value.length()-1);
			}
			ps.setString(idx++, value);
		}
		return idx;
	}
	
	protected int setParametersCommon(PreparedStatement ps, int idx, ModelAbstractType type, String value) throws SQLException {
		ps.setString(idx++, value);
		return idx;
	}
	
	@Override
	public String renderOrderBy(SearchConditionDTO condition) {

		StringBuffer buff = new StringBuffer();
		StringBuffer orderbyBuff = new StringBuffer();
		// Check for sort order
		if (((ConditionGroupDTO) condition).getSortOrder() != null) {
			SortOrder order = ((ConditionGroupDTO) condition).getSortOrder();
			List<SortColumn> columns = order.getColumns();
			
			if (!columns.isEmpty()) {
				orderbyBuff.append(SQL_ORDER_BY_CLAUSE);
				int index = 0;
				for (SortColumn column : columns) {
					index++;
					ModelAttribute attr = column.getAttribute();
					String attrName = column.getAttribute().getReferenceName();
					ModelAbstractType type = attr.getType();
					if (type == ModelBaseType.NUMBER ) {
						buff.append(String.format(SQL_ORDER_BY_NUMERIC_TEMPLATE, attrName));
						orderbyBuff.append("xx" + index);
					} else if (type == ModelBaseType.FIXED_POINT_NUMBER){
			    		String precision = column.getAttribute().getConstraint(Constraint.NAME_LENGTH);
			    		String scale = column.getAttribute().getConstraint(Constraint.NAME_DECIMAL_PLACES);
			    		buff.append(String.format(
			    				SQL_ORDER_BY_FIXED_POINT_TEMPLATE, attrName, precision, scale));
			    		orderbyBuff.append("xx" + index);
					} else {	
						buff.append(String.format(SQL_ORDER_BY_TEMPLATE, attrName));
						orderbyBuff.append("cast(xx" + index + " as char)");
					}
					buff.append(index + " ");
					if (column.isDescending()) {
						orderbyBuff.append(" desc");
					}
					if (index < columns.size()) {
						orderbyBuff.append(",");
						buff.append(",");
					}
				}
				return buff.toString() + " *** " + orderbyBuff.toString();
			}
		}
		return buff.toString();

	}

}
