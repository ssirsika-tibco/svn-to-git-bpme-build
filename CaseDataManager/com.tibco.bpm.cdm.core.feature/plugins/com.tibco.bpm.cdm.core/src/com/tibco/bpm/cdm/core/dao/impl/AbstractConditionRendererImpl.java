package com.tibco.bpm.cdm.core.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import com.tibco.bpm.cdm.core.dao.ConditionRenderer;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;

/**
 * Abstract class which implements ConditionRenderer
 * @author spanse
 *
 */
public abstract class AbstractConditionRendererImpl implements ConditionRenderer {

	@Override
	public abstract String render(SearchConditionDTO dto);

	@Override
	public abstract String renderOrderBy(SearchConditionDTO dto);

	@Override
	public abstract int setParameters(SearchConditionDTO condition, PreparedStatement ps, int idx) throws SQLException;
	
	protected DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
	
    protected static List<String> splitValues(String value) {
    	List<String> values = new ArrayList<String>();
		if (value.indexOf(" and ") != -1) {
			int index = value.indexOf(" and ");
			values.add(value.substring(0,index).trim());
			values.add(value.substring(index+5).trim());
		} else if (value.startsWith("(") && value.endsWith(")")) {
			int i = 1; 
			while (value.indexOf(',', i) != -1) {
				int index = value.indexOf(",", i);
				values.add(value.substring(i,index).trim());
				i = index+1;
			}
			values.add(value.substring(i,value.length()-1).trim());
		} else {
			values.add(value);
		}
		return values;
    }
    
    protected static boolean isWildcardCharEscaped(String str) {
    	
    	boolean present = false;    	
    	int i = 0;
    	
    	if (str.indexOf("\\") != -1) {
    		i = str.indexOf("\\");
    		if (i < str.length()) {
    			String nextChar = str.substring(i+1, i+2);
    			if (nextChar.equals("%") || nextChar.equals("*") || nextChar.equals("?") || nextChar.equals("_")) {
    				present = true;
    			}
    		}
    	}
 
    	return present;
    }    
}
