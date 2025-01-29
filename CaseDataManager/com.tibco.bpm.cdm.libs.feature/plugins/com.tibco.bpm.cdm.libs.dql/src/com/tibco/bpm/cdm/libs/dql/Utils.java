package com.tibco.bpm.cdm.libs.dql;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	
	private static final Pattern $TAG_EXPR =
	    	Pattern.compile("(\\s*(\\$\\w+)\\s*)");
    public static final Pattern DATA_FIELD_EXPR =
    		Pattern.compile("(\\s*(\\$\\{data\\..+\\})\\s*)");    
    public static final Pattern DATA_FIELD_EXPR_ALT =
    		Pattern.compile("(\\s*\\(\\s*(\\$\\{data\\..+\\})\\s*\\)\\s*)");

    public static List<String> getAttrPathSegments(String expr) {
    	
    	ArrayList<String> list = new ArrayList<String>();
    	int index = 0;
    	int noOfLeftBraces = 0;
    	
    	while(expr.indexOf("[", index) != -1) {
    		noOfLeftBraces++;
    		int currentLeftBracketIndex = expr.indexOf("[", index);
    		int nextLeftBracketIndex = expr.indexOf("[", currentLeftBracketIndex + 1);
    		int nextRightBracketIndex = expr.indexOf("]", currentLeftBracketIndex + 1);
    		
    		if (nextRightBracketIndex < nextLeftBracketIndex || nextLeftBracketIndex == -1) {
    			if (noOfLeftBraces == 1) {
	    			String segment = null;
	    			while(expr.indexOf(".", index) != -1) {
	    				int delimiterIntPos = expr.indexOf(".", index);
	    				if (delimiterIntPos < currentLeftBracketIndex) {
	    					segment = expr.substring(index,delimiterIntPos);
	    					list.add(segment);
	    					index = delimiterIntPos+1;
	    				} else {
	    					break;
	    				}    				
	    			}
	    			int delimiterPos = expr.indexOf(".", nextRightBracketIndex+1);
	    			if (delimiterPos != -1) {
		    			if ( delimiterPos == nextRightBracketIndex+1) { 
		    				segment = expr.substring(index, nextRightBracketIndex+1);
		    				index = nextRightBracketIndex+2;
		    			} else {
		    				segment = expr.substring(index, delimiterPos);
		    				index = delimiterPos +1;
		    			}
	    			} else {
	    				segment = expr.substring(index);
	    				index = expr.length();
	    			}
	    			list.add(segment);
	    			noOfLeftBraces = 0;
    			} 
    		} else {
	    			noOfLeftBraces++;
	    			String segment = null;
	    			while(expr.indexOf(".", index) != -1) {
	    				int delimiterIntPos = expr.indexOf(".", index);
	    				if (delimiterIntPos < currentLeftBracketIndex) {
	    					segment = expr.substring(index,delimiterIntPos);
	    					list.add(segment);
	    					index = delimiterIntPos+1;
	    				} else {
	    					break;
	    				}    				
	    			}
	    			while (expr.indexOf("]", nextLeftBracketIndex+1) != -1 && noOfLeftBraces > 0) {
	    				nextRightBracketIndex = expr.indexOf("]", nextLeftBracketIndex+1);
	    				int intermidateLeftBracketIndex = expr.indexOf("[", nextLeftBracketIndex+1);
	    				if (intermidateLeftBracketIndex != -1 && intermidateLeftBracketIndex < nextRightBracketIndex) {
	    					noOfLeftBraces++;
	    				}
	    				nextLeftBracketIndex = nextRightBracketIndex;
	    				noOfLeftBraces--;
	    			}
	    			int delimiterPos = expr.indexOf(".", nextRightBracketIndex+1);
	    			if (delimiterPos != -1) {
		    			if ( delimiterPos == nextRightBracketIndex+1) { 
		    				segment = expr.substring(index, nextRightBracketIndex+1);
		    				index = nextRightBracketIndex+2;
		    			} else {
		    				segment = expr.substring(index, delimiterPos);
		    				index = delimiterPos +1;
		    			}
	    			} else {
	    				segment = expr.substring(index);
	    				index = expr.length();
	    			}
	    			list.add(segment);
	    			noOfLeftBraces = 0;
    		}
    	}
    	if (index ==  expr.length()) {
    		return list;
    	}
    	String remainder = null;
    	if (index > 0 && index < expr.length()) {
    		remainder = expr.substring(index);
    	} else {
    		remainder = expr;
    	}
    	String[] splits = remainder.split("[, . ']+");
	    for (int i = 0; i < splits.length; i++) {
	    	list.add(splits[i]);
	    }
    	
	    return list;
    }

    public static boolean matchBraces(String expr, String left, String right) {
    	
    	int noLeftBrackets = 0, noRightBrackets = 0;
    	int index = 0;
    	
    	while (expr.indexOf(left,index) != -1) {
    		int nextIndex = expr.indexOf(left,index);
    		noLeftBrackets++;
    		index = nextIndex+1;
    	}
    	index = 0;
    	while (expr.indexOf(right,index) != -1) {
    		int nextIndex = expr.indexOf(right,index);
    		noRightBrackets++;
    		index = nextIndex+1;
    	}    	
    	return (noLeftBrackets==noRightBrackets);
    }  
    
    public static boolean containsTag(String str) {
    	Matcher m = $TAG_EXPR.matcher(str);
    	if (m.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public static boolean containsDataField(String str) {
    	if (str.startsWith("$")) {
    		Matcher m = DATA_FIELD_EXPR.matcher(str);
        	if (m.matches()) {
        		return true;
        	} else {
        		m = DATA_FIELD_EXPR_ALT.matcher(str);
        		if (m.matches()) {
        			return true;
        		}
        		return false;
        	}            	
    	} else {
    		return false;
    	} 
    }
}
