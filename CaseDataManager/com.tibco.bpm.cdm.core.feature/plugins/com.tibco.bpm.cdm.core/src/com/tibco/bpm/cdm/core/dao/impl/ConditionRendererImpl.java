package com.tibco.bpm.cdm.core.dao.impl;

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

/**
 * PostgreS implementation of ConditionRenderer.  Assumes that the column name 'casedata' corresponds
 * to cdm_cases_int.casedata
 * 
 * @author smorgan
 * @since 2019
 */
public class ConditionRendererImpl extends  AbstractConditionRendererImpl
{
    /*
     * This implementation has been updated using the postgress support for
     * query the json data for release 5.4.0. SELECT * FROM CDM_CASES_INT c
     * where casedata::json #>> '{customer,accounts,0,IBAN}' = 'iban 001'
     */
    private static final String SQL_GET_CONDITION_TEMPLATE =
            "casedata::json #>> '{%s}' %s ?"; //$NON-NLS-1$
    
    private static final String SQL_GET_CONDITION_NUMERIC_TEMPLATE =
            "(casedata::json #>> '{%s}')::float %s ?"; //$NON-NLS-1$
    
    private static final String SQL_GET_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE =
            "(casedata::json #>> '{%s}')::float %s ?"; //$NON-NLS-1$
    
    private static final String SQL_GET_CONDITION_FUNCTION_TEMPLATE = 
    		 "%s(casedata::json #>> '{%s}') %s ?"; //$NON-NLS-1$;
    
    protected static final String SQL_GET_BETN_CONDITION_TEMPLATE =
    		 "casedata::json #>> '{%s}' %s ? and ?"; //$NON-NLS-1$
    
    protected static final String SQL_GET_BETN_CONDITION_NUMERIC_TEMPLATE =
   		 "(casedata::json #>> '{%s}')::float %s ? and ?"; //$NON-NLS-1$
    
    protected static final String SQL_GET_BETN_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE =
   		 "(casedata::json #>> '{%s}')::float %s ? and ?"; //$NON-NLS-1$
    
    protected static final String SQL_GET_IN_CONDITION_TEMPLATE =
            "casedata::json #>> '{%s}' %s %s  "; //$NON-NLS-1$

    protected static final String SQL_GET_ESCAPE_CONDITION_TEMPLATE =
            "casedata::json #>> '{%s}' %s ? escape ('\\') "; //$NON-NLS-1$

    protected static final String SQL_GET_ESCAPE_CONDITION_FUNCTION_TEMPLATE =
            "%s(casedata::json #>> '{%s}') %s ? escape ('\\') "; //$NON-NLS-1$

	// i.e. is absent from the JSON (wouldn't match properties set 'null', but such properties are removed on the way in)
    private static final String SQL_GET_CONDITION_NULL_TEMPLATE =
            "casedata::json #>> '{%s}' IS NULL"; //$NON-NLS-1$

    private static final String SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE = 
    		"json_array_length(casedata::json #> '{%s}') %s ? ";
    
    private static final String SQL_ORDER_BY_CLAUSE = " order by ";
    
    private static final String SQL_ORDER_BY_TEMPLATE = "casedata::json #>> '{%s}' ";
    
    private static final String SQL_ORDER_BY_NUMERIC_TEMPLATE = "(casedata::json #>> '{%s}')::float ";
    
    private static final String SQL_ORDER_BY_FIXED_POINT_TEMPLATE = "(casedata::json #>> '{%s}')::float ";
    
    
	@Override
	public String render(SearchConditionDTO condition)
	{
		String result = null;
		if (condition != null)
		{
			StringBuilder conditionBuf = new StringBuilder();

			for (SearchConditionDTO cond : ((ConditionGroupDTO) condition).getChildren()) {
					if (cond instanceof AttributeSearchConditionDTO)
					{
						AttributeSearchConditionDTO asc = (AttributeSearchConditionDTO) cond;
						String condSQL = null;
                        String attrName =
                                updateDelimeters(
                                        asc.getAttribute().getReferenceName());
                        String operator = asc.getOperator().toString();
						if (asc.getValue() == null)
						{
							condSQL = String.format(SQL_GET_CONDITION_NULL_TEMPLATE, attrName);
						} else {
	                        ModelAbstractType type = asc.getAttribute().getType();
	                        if (operator.equalsIgnoreCase("between") || operator.equalsIgnoreCase("not between")) {
	                        	if (type == ModelBaseType.NUMBER) {
	                        		condSQL = String.format(
	                            		SQL_GET_BETN_CONDITION_NUMERIC_TEMPLATE, attrName, operator);
	                        	} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
	                        		condSQL = String.format(
	                            		SQL_GET_BETN_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE, attrName, operator);
	                        	} else {
	                        		condSQL = String.format(
	                        			SQL_GET_BETN_CONDITION_TEMPLATE, attrName, operator);
	                        	}
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
	                        	if (type == ModelBaseType.NUMBER) {
	                        		condSQL = String.format(
	                            		SQL_GET_CONDITION_NUMERIC_TEMPLATE, attrName, operator);
	                        	} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
	                        		condSQL = String.format(
	                            		SQL_GET_CONDITION_FIXED_POINT_NUMERIC_TEMPLATE, attrName, operator);
	                        	} else {
	                        		if(asc.isFunctionUpperPresent()) {	
	                        			condSQL = String.format(
	                            			SQL_GET_CONDITION_FUNCTION_TEMPLATE, "upper", attrName, operator);
	                        		} else if (asc.isFunctionLowerPresent()) {
	                        			condSQL = String.format(
	                    					SQL_GET_CONDITION_FUNCTION_TEMPLATE, "lower", attrName, operator);
	                        		} else if (asc.isFunctionSizePresent()) {
	                        			condSQL = String.format(getSizeFunctionTemplate(), attrName, operator);
	                        		} else {
	                        			condSQL = String.format(SQL_GET_CONDITION_TEMPLATE, attrName, operator);
	                        		}
	                        	}
							}
	                    }
                        for (int i = 0; i < asc
                                .getPreceedingParentheses(); i++) {
                            conditionBuf.append(PRECEED_PARENTHESES);
                        }
                        conditionBuf.append(condSQL);
                        for (int i = 0; i < asc
                                .getSucceedingParentheses(); i++) {
                            conditionBuf.append(SUCCEED_PARENTHESES);
                        }
                        if (asc.getSuccessorConditionOperator() != null) {
                            if (asc.getSuccessorConditionOperator() == ConditionOperator.AND) {
                                conditionBuf
                                        .append(SQL_GET_CONDITION_AND_DELIM);
                            } else if (asc
                                    .getSuccessorConditionOperator() == ConditionOperator.OR) {
                                conditionBuf.append(SQL_GET_CONDITION_OR_DELIM);
                            }
                        }
					}
				}
			result = conditionBuf.toString();
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
					ModelAbstractType type = attr.getType();
					String attrName =
                            updateDelimeters(
                                    attr.getReferenceName());
					if (type == ModelBaseType.NUMBER) {
						buff.append(String.format(SQL_ORDER_BY_NUMERIC_TEMPLATE, attrName)); 
					} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
						buff.append(String.format(SQL_ORDER_BY_FIXED_POINT_TEMPLATE, attrName)); 							
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
						// If ascValue is null, SQL is of the form "casedata -> '<attrName>' IS NULL", 
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
									} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
										ps.setFloat(idx++, Float.parseFloat(value1));
										ps.setFloat(idx++, Float.parseFloat(value2));
									} else {
										ps.setString(idx++, value1);
										ps.setString(idx++, value2);
									}
								}
							} else if (operator.equalsIgnoreCase("in") || operator.equalsIgnoreCase("not in")) {
								List<String> values = splitValues(ascValue);
								for(String value : values) {
									
									if (value.startsWith("'") && value.endsWith("'")) {
										//The values provided for in operator are quoted.
										value = value.substring(1,value.length()-1);
									}
									ps.setString(idx++, value);
								}
							} else {
								if (asc.isFunctionSizePresent()) {
									ps.setInt(idx++, Integer.parseInt(ascValue));
								} else if (type == ModelBaseType.NUMBER) {
									if (ascValue.indexOf(dfs.getDecimalSeparator()) == -1) {
										ps.setLong(idx++, Long.valueOf(ascValue));
									} else {
										ps.setFloat(idx++, Float.valueOf(ascValue));
									}
								} else if (type == ModelBaseType.FIXED_POINT_NUMBER) {
									ps.setFloat(idx++, Float.parseFloat(ascValue));
								} else {								
									ps.setString(idx++, ascValue);
								}
							}
						}
					}
				}
		}
		return idx;
	}

    private static String updateDelimeters(String attrName) {
        attrName = attrName.replace(".", ",");
        attrName = attrName.replace("[", ",");
        attrName = attrName.replaceAll("]", "");
        return attrName;
    }
    
    protected String getSizeFunctionTemplate( ) {
    	return SQL_GET_CONDITION_SIZE_FUNCTION_TEMPLATE;
    }
    

}
