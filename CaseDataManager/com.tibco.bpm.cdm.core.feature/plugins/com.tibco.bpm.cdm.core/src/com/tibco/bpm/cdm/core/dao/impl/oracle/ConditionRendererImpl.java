package com.tibco.bpm.cdm.core.dao.impl.oracle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.tibco.bpm.cdm.core.dao.ConditionRenderer;
import com.tibco.bpm.cdm.core.dao.impl.AbstractConditionRendererImpl;
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
 * ConditionRenderer implementation to support oracle database.
 */
public class ConditionRendererImpl extends  AbstractConditionRendererImpl
{

    protected static final String SQL_GET_CONDITION_TEMPLATE =
            "json_value(casedata, '$.%s') %s ? "; //$NON-NLS-1$
    
    protected static final String SQL_GET_CONDITION_FUNCTION_TEMPLATE =
            "%s(json_value(casedata, '$.%s')) %s ? "; //$NON-NLS-1$
    
    protected static final String SQL_GET_BETN_CONDITION_TEMPLATE =
            "json_value(casedata, '$.%s') %s ? and ? "; //$NON-NLS-1$
    
    protected static final String SQL_GET_IN_CONDITION_TEMPLATE =
            "json_value(casedata, '$.%s') %s %s  "; //$NON-NLS-1$

    protected static final String SQL_GET_ESCAPE_CONDITION_TEMPLATE =
            "json_value(casedata, '$.%s') %s ? escape ('\\') "; //$NON-NLS-1$
 
    protected static final String SQL_GET_ESCAPE_CONDITION_FUNCTION_TEMPLATE =
            "%s(json_value(casedata, '$.%s')) %s ? escape ('\\') "; //$NON-NLS-1$
    
	// i.e. is absent from the JSON (wouldn't match properties set 'null', but such properties are removed on the way in)
    protected static final String SQL_GET_CONDITION_NULL_TEMPLATE =
            "json_value(casedata, '$.%s') = null "; //$NON-NLS-1$ ";
    
    protected static final String SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE =
            "json_value(casedata, '$.%s.size()') %s ? "; //$NON-NLS-1$  
    
    protected static final String SQL_ORDER_BY_CLAUSE = " order by ";
    
    protected static final String SQL_ORDER_BY_TEMPLATE = "json_value(casedata, '$.%s') "; 
    
    protected String SQL_ORDER_BY_NUMERIC_TEMPLATE = "json_value(casedata, '$.%s' RETURNING NUMBER) "; 
    
    protected String SQL_ORDER_BY_FIXED_POINT_NUMERIC_TEMPLATE = "json_value(casedata, '$.%s' RETURNING NUMBER(%s,%s)) "; 

	@Override
	public String render(SearchConditionDTO condition)
	{
		String result = null;
		if (condition != null)
		{
			StringBuilder conditionBuf = new StringBuilder();
            for (SearchConditionDTO cond : ((ConditionGroupDTO) condition)
                    .getChildren())
            {
                if (cond instanceof AttributeSearchConditionDTO)
                {
					AttributeSearchConditionDTO asc = (AttributeSearchConditionDTO) cond;
                    String condSQL = null;
                    String attrName = asc.getAttribute().getReferenceName();
                    String operator = asc.getOperator().toString();
                    if (asc.getValue() == null)
                    {
                    	condSQL = String.format(SQL_GET_CONDITION_NULL_TEMPLATE, attrName);
					} else {
                        ModelAbstractType type = asc.getAttribute().getType();
                        if (operator.equalsIgnoreCase("between") || operator.equalsIgnoreCase("not between")) {
                        	condSQL = getBetweenConditionTemplate(asc);
                        } else if (operator.equalsIgnoreCase("in") || operator.equalsIgnoreCase("not in")) {
                        	List<String> values = splitValues(asc.getValue());
                        	StringBuffer buff = new StringBuffer();
                        	buff.append("(");
                        	for (int i=0; i < values.size(); i++) {
            					buff.append("?");
            					if (i < values.size()-1) {
            						buff.append(",");
            					}
            				}
                        	buff.append(")");
                        	condSQL = String.format(
                        			SQL_GET_IN_CONDITION_TEMPLATE, attrName, operator, buff.toString());
                        } else if (operator.equalsIgnoreCase("like") && isWildcardCharEscaped(asc.getValue())) {
                        	if(asc.isFunctionUpperPresent()) {
                        		condSQL = String.format(SQL_GET_ESCAPE_CONDITION_FUNCTION_TEMPLATE, "upper", attrName, operator);
                        	} else if (asc.isFunctionLowerPresent()) {
                        		condSQL = String.format(SQL_GET_ESCAPE_CONDITION_FUNCTION_TEMPLATE, "lower", attrName, operator);
                        	} else {
                        		condSQL = String.format(SQL_GET_ESCAPE_CONDITION_TEMPLATE, attrName, operator);
                        	}
                        } else {                       	
                        	condSQL = getGetConditionTemplate(asc);
                    	}						
                    }
                    for (int i = 0; i < asc.getPreceedingParentheses(); i++) {
                        conditionBuf.append(PRECEED_PARENTHESES);
                    }
                    conditionBuf.append(condSQL);
                    for (int i = 0; i < asc.getSucceedingParentheses(); i++) {
                        conditionBuf.append(SUCCEED_PARENTHESES);
                    }
                    if (asc.getSuccessorConditionOperator() != null) {
                        if (asc.getSuccessorConditionOperator() == ConditionOperator.AND) {
                            conditionBuf.append(SQL_GET_CONDITION_AND_DELIM);
                        } else if (asc
                                .getSuccessorConditionOperator() == ConditionOperator.OR) {
                            conditionBuf.append(SQL_GET_CONDITION_OR_DELIM);
                        }
                    }
                }
                result = conditionBuf.toString();
			}
		}
		return result;	
	}

	@Override
	public String renderOrderBy(SearchConditionDTO condition) {

		StringBuffer buff = new StringBuffer();
		// Check for sort order
		if (((ConditionGroupDTO) condition).getSortOrder() != null) {
			SortOrder order = ((ConditionGroupDTO) condition).getSortOrder();
			List<SortColumn> columns = order.getColumns();
			
			if (!columns.isEmpty()) {
				buff.append(SQL_ORDER_BY_CLAUSE);
				int index = 0;
				for (SortColumn column : columns) {
					index++;
					ModelAttribute attr = column.getAttribute();
					String attrName = column.getAttribute().getReferenceName();
					ModelAbstractType type = attr.getType();
					if (type == ModelBaseType.NUMBER ) {
						buff.append(String.format(SQL_ORDER_BY_NUMERIC_TEMPLATE, attrName));
					} else if (type == ModelBaseType.FIXED_POINT_NUMBER){
			    		String precision = column.getAttribute().getConstraint(Constraint.NAME_LENGTH);
			    		String scale = column.getAttribute().getConstraint(Constraint.NAME_DECIMAL_PLACES);
			    		buff.append(String.format(
			    				SQL_ORDER_BY_FIXED_POINT_NUMERIC_TEMPLATE, attrName, precision, scale));
					} else {	
						buff.append(String.format(SQL_ORDER_BY_TEMPLATE, attrName));
					}
					if (column.isDescending()) {
						buff.append("desc");
					}
					if (index < columns.size()) {
						buff.append(",");
					}
				}
				return buff.toString();
			}
		}
		return buff.toString();

	}
	
	@Override
	public int setParameters(SearchConditionDTO condition, PreparedStatement ps, int idx) throws SQLException
	{
		if (condition != null)
		{
            for (SearchConditionDTO cond : ((ConditionGroupDTO) condition)
                    .getChildren())
				{
					if (cond instanceof AttributeSearchConditionDTO)
					{
						AttributeSearchConditionDTO asc = (AttributeSearchConditionDTO) cond;
						String ascValue = asc.getValue();
						String operator = asc.getOperator().toString();
						// If ascValue is null, SQL is of the form "casedata -> "<attrName>":null 
						// so has no '?' to replace.
						if (ascValue != null)
						{
							// if (asc.getAttribute().getTypeObject().getJsonType() == JsonType.STRING)
							ModelAbstractType type = asc.getAttribute().getType();
							// TODO Have function to determine if a given type is text
							if (type == ModelBaseType.TEXT || type == ModelBaseType.DATE || type == ModelBaseType.TIME
									|| type == ModelBaseType.DATE_TIME_TZ || type == ModelBaseType.URI)
							{
								// String type, so wrap in double quotes
								ascValue = String.format("%s", ascValue);
							}
							if (operator.equalsIgnoreCase("between") || operator.equalsIgnoreCase("not between")) {
								idx = setParametersBetweenCondition(ps, idx, type, ascValue);
							} else if (operator.equalsIgnoreCase("in") || operator.equalsIgnoreCase("not in")) {
								idx = setParamtersInCondition(ps, idx, type, ascValue);
							} else {
								idx = setParametersCommon(ps, idx, type, ascValue);
							}
						}
					}
				}
		}
		return idx;
	}
	
    protected String getSizeFunctionTemplate( ) {
    	return SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE;
    }
    
	protected String getBetweenConditionTemplate(AttributeSearchConditionDTO asc) {
        String attrName = asc.getAttribute().getReferenceName();
        String operator = asc.getOperator().toString();
        String template = String.format(
    			SQL_GET_BETN_CONDITION_TEMPLATE, attrName, operator);
		return template;
	}
	
	protected String getGetConditionTemplate(AttributeSearchConditionDTO asc) {
		String template;
        String attrName = asc.getAttribute().getReferenceName();
        String operator = asc.getOperator().toString();
    		if(asc.isFunctionUpperPresent()) {
    			template = String.format(
        			SQL_GET_CONDITION_FUNCTION_TEMPLATE, "upper", attrName, operator);
    		} else if (asc.isFunctionLowerPresent()) {
    			template = String.format(
					SQL_GET_CONDITION_FUNCTION_TEMPLATE, "lower", attrName, operator);
    		} else if (asc.isFunctionSizePresent()) {
    			template = String.format(getSizeFunctionTemplate(), attrName, operator);
    		} else {
    			template = String.format(SQL_GET_CONDITION_TEMPLATE, attrName, operator);
    		}
		return template;
	}
	
	protected int setParametersBetweenCondition(PreparedStatement ps, int idx, ModelAbstractType type, String ascValue) throws SQLException {		
		if (ascValue.indexOf("and") != -1) {
			int index = ascValue.indexOf("and");
			String value1 = ascValue.substring(0,index).trim();
			String value2 = ascValue.substring(index+3).trim();
			if (type == ModelBaseType.NUMBER) {
				if (value1.indexOf(dfs.getDecimalSeparator()) == -1) {
					ps.setLong(idx++, Long.valueOf(value1));
				} else {
					ps.setFloat(idx++, Float.valueOf(value1));
				}
				if (value2.indexOf(dfs.getDecimalSeparator()) == -1) {
					ps.setLong(idx++, Long.valueOf(value2));
				} else {
					ps.setFloat(idx++, Float.valueOf(value2));
				}
			} else if (type == ModelBaseType.FIXED_POINT_NUMBER ) {
				ps.setFloat(idx++, Float.valueOf(value1));
				ps.setFloat(idx++, Float.valueOf(value2));
			} else {
				ps.setString(idx++, value1);
				ps.setString(idx++, value2);
			}
		}
		return idx;
	}
	
	protected int setParamtersInCondition(PreparedStatement ps, int idx, ModelAbstractType type, String ascValue) throws SQLException {
		List<String> values = splitValues(ascValue);
		for(String value : values) {
			if (type == ModelBaseType.NUMBER) {
				if (value.indexOf(dfs.getDecimalSeparator()) == -1) {
					ps.setLong(idx++, Long.valueOf(value));
				} else {
					ps.setFloat(idx++, Float.valueOf(value));
				}
			} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
				ps.setFloat(idx++, Float.valueOf(value));
			} else {
				if (value.startsWith("'") && value.endsWith("'")) {
					//The values provided for in operator are quoted.
					value = value.substring(1,value.length()-1);
				}
				ps.setString(idx++, value);
			}
		}
		return idx;
	}
	
	protected int setParametersCommon(PreparedStatement ps, int idx, ModelAbstractType type, String value) throws SQLException {
		if (type == ModelBaseType.NUMBER) {
			if (value.indexOf(dfs.getDecimalSeparator()) == -1) {
				ps.setLong(idx++, Long.valueOf(value));
			} else {
				ps.setFloat(idx++, Float.valueOf(value));
			}			
		} else if (type == ModelBaseType.FIXED_POINT_NUMBER ) {
			ps.setFloat(idx++, Float.valueOf(value));
		} else {
			ps.setString(idx++, value);
		}
		return idx;
	}
}
