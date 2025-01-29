package com.tibco.bpm.cdm.libs.dql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.bpm.cdm.libs.dql.dto.AndOrGroupDTO;
import com.tibco.bpm.cdm.libs.dql.dto.AttributeSearchConditionDTO;
import com.tibco.bpm.cdm.libs.dql.dto.ConditionGroupDTO;
import com.tibco.bpm.cdm.libs.dql.dto.ConditionOperator;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchOperatorDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SortColumn;
import com.tibco.bpm.cdm.libs.dql.dto.SortOrder;
import com.tibco.bpm.cdm.libs.dql.exception.ALLIndexNotSupportedException;
import com.tibco.bpm.cdm.libs.dql.exception.BadlyFormattedPathExcpetion;
import com.tibco.bpm.cdm.libs.dql.exception.DQLException;
import com.tibco.bpm.cdm.libs.dql.exception.DataFieldIndexNotAnIntegerException;
import com.tibco.bpm.cdm.libs.dql.exception.DataFieldMultiplicityMismatchException;
import com.tibco.bpm.cdm.libs.dql.exception.DataFieldNotArrayTypeException;
import com.tibco.bpm.cdm.libs.dql.exception.DataFieldTypeMismatchException;
import com.tibco.bpm.cdm.libs.dql.exception.IncorrectSyntaxForDataFieldException;
import com.tibco.bpm.cdm.libs.dql.exception.IndexNotSupportedForSizeFunctionException;
import com.tibco.bpm.cdm.libs.dql.exception.InvalidIndexException;
import com.tibco.bpm.cdm.libs.dql.exception.LeafNodeStruturedTypeException;
import com.tibco.bpm.cdm.libs.dql.exception.MultiValuedAttributeMissingIndexException;
import com.tibco.bpm.cdm.libs.dql.exception.SingleValuedAttributeIndexException;
import com.tibco.bpm.cdm.libs.dql.exception.SpuriousTextAfterBracketException;
import com.tibco.bpm.cdm.libs.dql.exception.TagNotSupportedException;
import com.tibco.bpm.cdm.libs.dql.exception.UnknownAttributeException;
import com.tibco.bpm.cdm.libs.dql.exception.UnknownDataFieldException;
import com.tibco.bpm.cdm.libs.dql.exception.UnknownDataTypeException;
import com.tibco.bpm.cdm.libs.dql.model.DataFieldProvider;
import com.tibco.bpm.cdm.libs.dql.model.ModelAbstractType;
import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;
import com.tibco.bpm.cdm.libs.dql.model.ModelBaseType;
import com.tibco.bpm.cdm.libs.dql.model.ModelStructuredType;

/**
 * Parses DQL string into equivalent SearchConditionDTO object representation.
 * @author smorgan
 * @since 2019
 * Updated by spanse 2024
 */
public class DQLParser
{
    private static final Pattern AND_OR_START =
            Pattern.compile("(?i)(\\s+and|or\\s+).*");
    
	private static final Pattern OPERATOR_EXPRESSION	= Pattern
			.compile("(?i)(\\S+?)\\s*(=|!=|<=|>=|<>|<|>|\\s+between\\s+|\\s+not(\\s+)between\\s+|\\s+in\\s+|\\s+not\\s+in\\s+)\\s*('(.+)'|(.+))");

    private static final Pattern IN_NOT_IN_EXPR =
            Pattern.compile("(?i)(\\s+(not\\s+in|in)\\s+).*");

    private static final Pattern FUNCTION_EXPR =
            Pattern.compile("(?i)(upper\\(\\S+\\)|lower\\(\\S+\\)|size\\(\\S+\\)).*");

    private static final Pattern ORDER_BY_EXPR =
            Pattern.compile("(?i)(\\s*(order\\s+by)\\s+).*");

    private static final Pattern TYPE_OF_EXPR =
            Pattern.compile("(?i)(\\s*(type\\s+of)\\s+).*");
    
    private static final String LEFT_PARA = "("; //$NON-NLS-1$

    private static final String RIGHT_PARA = ")"; //$NON-NLS-1$

    private static final int MAX_PARENTHESES_LEVEL = 10;

	// A backslash that is not followed by a second backslash
	private static final String					LONE_BACKSLASH		= "\\\\(?!\\\\)";

	// hh:mm:ss
	private static final String					PAT_TIME			= "(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]";

	private static final String					PAT_MS				= "(\\.\\d{3})";

	private static final String					PAT_TZ				= "(Z|[+|-]\\d{2}(?:\\:?\\d{2})?)";

	private static final String					PAT_DATE			= "(\\d{4})-(\\d{2})-(0[1-9]|[12][0-9]|3[01])";

	private static final String					PAT_DATE_TIME_TZ	= PAT_DATE + "T" + PAT_TIME + PAT_MS + "?" + PAT_TZ;

	// ACE Time is hh:mm:ss
	private static final Pattern				patTime				= Pattern.compile(PAT_TIME);

	// Date is yyyy-MM-dd
	private static final Pattern				patDate				= Pattern.compile(PAT_DATE);

	// Date Time TZ is a full JSON Schema date-time. i.e. including seconds, with optional millis, but always including suffix.
	private static final Pattern				patDataTimeTZ		= Pattern.compile(PAT_DATE_TIME_TZ);

	private static final List<String>			BOOLEAN_VALUES		= Arrays.asList(new String[]{"true", "false"});

	private ModelStructuredType					type;

	private List<AttributeSearchConditionDTO>	ascs				= new ArrayList<>();
	
	private SortOrder							sortOrder			= null;

	private List<Issue>							issues				= new ArrayList<>();
	
	private DataFieldProvider					dfProvider			= null;

	public DQLParser(ModelStructuredType type)
	{
		this.type = type;
	}
	
	public DQLParser(ModelStructuredType type, DataFieldProvider provider)
	{
		this.type = type;
		this.dfProvider = provider;
	}

    private void validateExpressions(List<String> exprs)
    {
        
        for (int k = 0; k < exprs.size(); k++) {
            String expr = exprs.get(k).replaceAll("\\s+"," ");
            Matcher m = OPERATOR_EXPRESSION.matcher(expr);
            boolean operatorExpressionMatchFailed = false;
            boolean orderbyMatchFailed = false;
            if (m.matches())
            {
            	boolean functionUpperPresent = false, functionLowerPresent = false, functionSizePresent = false;
                String attrName = m.group(1).trim();
                String operatorSymbol = m.group(2).trim();
                operatorSymbol = operatorSymbol.replaceAll("\\s+"," ").toLowerCase();
                SearchOperatorDTO operator = SearchOperatorDTO.fromDQLSymbol(operatorSymbol);
                
               	if (isFunctionUpperPresent(attrName)) {
                	functionUpperPresent = true;
                	attrName = retrieveAttrName(attrName);
                } else if (isFunctionLowerPresent(attrName)) {
                	functionLowerPresent = true;
                	attrName = retrieveAttrName(attrName);
                } else if (isFunctionSizePresent(attrName)) {
                	if (operatorSymbol.equalsIgnoreCase("between") || operatorSymbol.equalsIgnoreCase("not between")
                			|| operatorSymbol.equalsIgnoreCase("in") || operatorSymbol.equalsIgnoreCase("not in")) {
                		issues.add(new Issue(IssueCode.DQL_NON_SUPPORTED_OPR_FOR_SIZE_FUNCTION, 
                				new String[]{"expr", expr, "opr", operatorSymbol}));
                		return;
                	}
                	functionSizePresent = true;
                	attrName = retrieveAttrName(attrName);
                }
                	
                String attrValue = m.group(6);
                if (attrValue == null) {
                	attrValue = m.group(5);
	            }
                if (attrValue != null) {
                	attrValue = attrValue.trim();
                }
                
                if (isUnspportedQueryParam(attrValue)) {
                	issues.add(new Issue(IssueCode.DQL_NO_SUPPORT_FOR_QUERY_PARAM, 
                		new String[]{"expr", expr, "param" , attrValue}));
                	return;
                } 
                
                
                if (operatorSymbol.equalsIgnoreCase("between") || operatorSymbol.equalsIgnoreCase("not between")) {
                	//TODO attrValue can be data field.
                	// If the data field parameter is present for between and not between operators,
                	// it needs to be of the number data type.
                	if (!containsDataField(attrValue)) {
	                	if (attrValue.indexOf(",") != -1) {
	                		int i = attrValue.indexOf(",");
	                		if (attrValue.indexOf(",", i+1) == -1) {
	                			attrValue = attrValue.substring(0,i).trim() + " and " + attrValue.substring(i+1).trim();
	                		} else {
	                			issues.add(new Issue(IssueCode.DQL_BETWEEN_OPR_SUPPORTS_TWO_VALUES, 
	                            		new String[]{"opr", operatorSymbol, "attrValue" , attrValue}));
	                            return;
	                		}
	                	} else if (exprs.get(k+1).trim().equalsIgnoreCase("and")) {
	                		attrValue = attrValue + " and " + exprs.get(k+2);
	                		k += 2;
	                	} else {
	                		attrValue = attrValue + " and " + exprs.get(k+1);
	                		k += 1;
	                	}
                	}
                } else if (operatorSymbol.equalsIgnoreCase("in") || operatorSymbol.equalsIgnoreCase("not in")) {
                	//TODO attrValue can be data field.
                }
                
                boolean isQuoted = false;
                if ("null".equals(attrValue))
                {
                    // The literal 'null' means that attribute value is compared to nothing.
                    attrValue = null;
                } else {
                	 // Not found in group 6 (unquoted value), so check group 5 (quoted value)
                	if ((m.group(6) == null) && (m.group(5) != null))
                    {
                        isQuoted = true;
                    }
                	
                	// There is specific condition for string value provided for the in operator.
                	// You can have one more strings in quotes here.
                	if ( m.group(6) != null) {
                		if (isValueQuoted (m.group(6).trim())) {
                			isQuoted = true;
                		}
                	}
	
	                // Prevent single quotes that are not escaped
	                // Although this could be handled in the regex, it's easier to follow this way.
	                boolean foundIllegalQuote = false;
	                // attrValue may have comma separated strings which are quoted.
	                // Need to check individual values for non escaped single quotes.
	                List<String> values = splitValues(attrValue);
	                for (String cvalue : values) {
		                for (int i = 1; i < cvalue.length()-1 && !foundIllegalQuote; i++)
		                {
		                    if (cvalue.charAt(i) == '\'')
		                    {
		                        if (cvalue.charAt(i - 1) != '\\')
		                        {
		                            // Illegal non-escaped single quote was matched by regex
		                            issues.add(new Issue(IssueCode.DQL_BAD_EXPRESSION, new String[]{"expression", expr}));
		                            foundIllegalQuote = true;
		                        }
		                    }
		                }
	                }
	
	                if (attrValue != null)
	                {
	                    attrValue = unescape(attrValue);
	                }
                }

            ModelAttribute attr = null;
            try {
            	attr = validateAttrPath(attrName, functionSizePresent);
            	if (attr != null) {
            		attr.setReferenceName(attrName);  
            	}
            } catch (UnknownDataFieldException e) {
                issues.add(new Issue(
                        IssueCode.DQL_UNKNOWN_DATA_FIELD,
                        new String[] { "datafield", e.getDataField() } ));
                    return;
            } catch (DataFieldIndexNotAnIntegerException e) {
                issues.add(new Issue(
                        IssueCode.DQL_DATA_FIELD_INDEX_NUMERIC_TYPE,
                        new String[] { "datafield", e.getDataField(), "path", expr } ));
                    return;
            } catch (DataFieldTypeMismatchException e) {
                issues.add(new Issue(
                        IssueCode.DQL_DATA_FIELD_INDEX_NUMERIC_TYPE,
                        new String[] { "datafield", e.getDataField(), "path", expr } ));
                    return;
            } catch (DataFieldMultiplicityMismatchException e) {
            	issues.add(new Issue(IssueCode.DQL_DATA_FIELD_INDEX_SINGLE_VALUED,
                        new String[] { "datafield", e.getDataField(), "path", expr } ));
            		return;	
            }  catch (LeafNodeStruturedTypeException e) {
            	if (!functionSizePresent) {
            		issues.add(new Issue(IssueCode.DQL_LEAF_NODE_STRUCTURED_TYPE,
                        new String[] {"path", e.getAttributePath() } ));
                    return;
            	}
            } catch (MultiValuedAttributeMissingIndexException e) {
                issues.add(new Issue(
                        IssueCode.DQL_MULTI_VALUED_ATTRIBUTE_MISSING_INDEX,
                        new String[] { "name", e.getAttributeName(), "path", e.getAttributePath() } ));
                    return;
            } catch (SingleValuedAttributeIndexException e) {
                issues.add(new Issue(
                        IssueCode.DQL_SINGLE_VALUED_ATTRIBUTE_INDEX_PRESENT,
                        new String[] { "name", e.getAttributeName(), "path", e.getAttributePath() } ));
                    return;
            } catch (UnknownDataTypeException e) {
                issues.add(new Issue(
                        IssueCode.DQL_UNKNOWN_DATA_TYPE,
                        new String[] { "name", e.getAttributeName(), "parent", e.getParentName() }));
                    return;
            } catch (UnknownAttributeException e) {
                issues.add(new Issue(
                        IssueCode.DQL_UNKNOWN_ATTRIBUTE,
                        new String[]{ "name", e.getAttributeName(), "parent", e.getParentName() }));
                    return;
            } catch (InvalidIndexException iie) {
            	if (iie instanceof BadlyFormattedPathExcpetion) {
            		issues.add(new Issue(
                            IssueCode.DQL_BADLY_FORMATTED_PATH_EXPRESSION,
                            new String[]{"path", iie.getAttributePath()}));
                        return;
            	} else if (iie instanceof IncorrectSyntaxForDataFieldException ) { 
                	issues.add(new Issue(
                            IssueCode.INCORRECT_SYNTAX_FOR_DATA_FIELD,
                            new String[] {"datafield", iie.getAttributePath(), "expr", expr}));
                        return;
                }else if (iie instanceof ALLIndexNotSupportedException) {
            		issues.add(new Issue(
                            IssueCode.DQL_ALL_INDEX_NOT_SUPPORTED,
                            new String[]{"path", iie.getAttributePath()}));
                        return; 
            	} else if (iie instanceof SpuriousTextAfterBracketException) {
            		issues.add(new Issue(
                            IssueCode.DQL_SPURIOUS_TEXT_AFTER_BRACKET,
                            new String[]{"path", iie.getAttributePath()}));
                        return;            		
            	} else if (iie instanceof TagNotSupportedException) {
            		issues.add(new Issue(
                            IssueCode.DQL_$TAG_INDEX_NOT_SUPPORTED,
                            new String[]{"path", iie.getAttributePath()}));
                        return;            		
            	} else if (iie instanceof IndexNotSupportedForSizeFunctionException) {
            		issues.add(new Issue(
                            IssueCode.DQL_INDEX_NOT_SUPPORTED_FOR_SIZE_FUNCTION,
                            new String[]{"name", iie.getAttributeName(), "path", iie.getAttributePath()}));
                        return;            		
            	} else {
            		issues.add(new Issue(
                            IssueCode.DQL_INDEX_MUST_BE_AN_INTEGER,
                            new String[]{"name", iie.getAttributeName()}));
                        return; 
            	}
            } catch (DQLException e) {
    				issues.add(new Issue(IssueCode.DQL_BAD_EXPRESSION, 
    					new String[] {"expression" , e.getAttributePath()}));
			}

                // Now check for parentheses preceding this expression up to
                // MAX_PARANTHESES_LEVEL levels. You should go further only if
                // you find first
                // one.
                int precedingParentheses = 0, succedingParentheses = 0;
                if (k > 0) {
                    for (int level =
                            1; level <= MAX_PARENTHESES_LEVEL; level++) {
                        if (k - level >= 0) {
                            String prevExpr = exprs.get(k - level).trim();
                            if (prevExpr.equalsIgnoreCase("(")) { //$NON-NLS-1$
                                precedingParentheses++;
                            } else {
                                break;
                            }
                        }
                        if (precedingParentheses == 0) {
                            break;
                        }
                    }
                }

                if (k < (exprs.size() - 1)) {
                    for (int level =
                            1; level <= MAX_PARENTHESES_LEVEL; level++) {
                        if (k + level < exprs.size()) {
                            String nextExpr = exprs.get(k + level).trim();
                            if (nextExpr.equalsIgnoreCase(")")) { //$NON-NLS-1$
                                succedingParentheses++;
                            } else {
                                break;
                            }
                        }
                        if (succedingParentheses == 0) {
                            break;
                        }
                    }
                }

                // Check the presence of the condition operator and/or which
                // follows the current expression.
                ConditionOperator conditionOpr = null;
                if (k + 1 + succedingParentheses < exprs.size()) {
                    String nextExpr =
                            exprs.get(k + 1 + succedingParentheses).trim();
                    if (nextExpr.equalsIgnoreCase("and")) { //$NON-NLS-1$
                    	if (!operatorSymbol.equalsIgnoreCase("not between") && !operatorSymbol.equalsIgnoreCase("between"))
                    		conditionOpr = ConditionOperator.AND;
                        k++;
                    } else if (nextExpr.equalsIgnoreCase("or")) { //$NON-NLS-1$
                        conditionOpr = ConditionOperator.OR;
                        k++;
                    }
                }

            if (attr != null)
            {
                ModelAbstractType attrType = attr.getType();
                boolean shouldBeQuoted = attrType == ModelBaseType.TEXT;
                if (!attr.isSearchable() && attrType instanceof ModelBaseType)
                {
                    issues.add(new Issue(IssueCode.DQL_ATTRIBUTE_NOT_SEARCHABLE,
                            new String[]{"expression", expr, "name", attr.getName()}));
                } else if (!attr.isArray() && functionSizePresent)
            	{
                	issues.add(new Issue(IssueCode.DQL_SINGLE_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_SIZE_FUNCTION,
                			new String[] {"name",attr.getName()}));
                } 
                else if (attrValue != null)
                {
                	if (shouldBeQuoted && !isQuoted)
                    {
                		if (containsDataField(attrValue)) {
                			try {
								if (!validateDataField(attrValue, attr, true, true, operatorSymbol) ) {
									issues.add(new Issue(IssueCode.DQL_INVALID_DATA_FIELD,
								            new String[]{"datafield", attrValue}));
								}
							} catch (DQLException e) {
								if (e instanceof DataFieldTypeMismatchException) {
									issues.add(new Issue(IssueCode.DQL_DATA_FIELD_TYPE_MISMATCH,
								            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
								} else if (e instanceof DataFieldNotArrayTypeException) {
									issues.add(new Issue(IssueCode.DQL_DATA_FIELD_NOT_ARRAY_TYPE, 
											new String[]{"datafield", attrValue, "opr", operatorSymbol}));
								} else if (e instanceof DataFieldMultiplicityMismatchException) {
									issues.add(new Issue(IssueCode.DQL_DATA_FIELD_MULTIPLICITY_MISMATCH,
								            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
								}							
							}
                		} else {
                			issues.add(new Issue(IssueCode.DQL_VALUE_SHOULD_BE_QUOTED,
                                new String[]{"expression", expr, "type", attr.getType().getName()}));
                		}
                    }
                    else if (!shouldBeQuoted && isQuoted)
                    {
                        issues.add(new Issue(IssueCode.DQL_VALUE_SHOULD_NOT_BE_QUOTED,
                                new String[]{"expression", expr, "type", attr.getType().getName()}));
                    }
                    // Note: Final false argument means we don't want constraints (minValue etc) to be validated.
                    // e.g. There's no harm in allowing 'number > 0' when number has a min value of 1.
                    else if ((attr.getType() instanceof ModelBaseType) && !isStringType(attrValue, (ModelBaseType) attr.getType()))
                    {
                    	if (attrType == ModelBaseType.NUMBER || attrType == ModelBaseType.FIXED_POINT_NUMBER
                    		|| attrType == ModelBaseType.DATE || attrType == ModelBaseType.TIME || attrType == ModelBaseType.DATE_TIME_TZ) {
                    		if (containsDataField(attrValue)) {
                    			try {
                    				
									if (!validateDataField(attrValue, attr, true, true, operatorSymbol) ) {
										issues.add(new Issue(IssueCode.DQL_INVALID_DATA_FIELD,
									            new String[]{"datafield", attrValue}));
									}
								} catch (DQLException e) {
									if (e instanceof DataFieldTypeMismatchException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_TYPE_MISMATCH,
									            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
									} else if (e instanceof DataFieldNotArrayTypeException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_NOT_ARRAY_TYPE, 
												new String[]{"datafield", attrValue, "opr", operatorSymbol}));
									} else if (e instanceof DataFieldMultiplicityMismatchException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_MULTIPLICITY_MISMATCH,
									            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
									}
								}
                    		} else {
                    			issues.add(new Issue(IssueCode.DQL_VALUE_NOT_VALID_FOR_TYPE,
                                        new String[]{"expression", expr, "type", attr.getType().getName()}));
                    		}
                    	} else {
                    		issues.add(new Issue(IssueCode.DQL_VALUE_NOT_VALID_FOR_TYPE,
                                new String[]{"expression", expr, "type", attr.getType().getName()}));
                    	}
                    }
                    else
                    {
                    	if (attrType == ModelBaseType.TEXT || attrType == ModelBaseType.URI || attrType == ModelBaseType.BOOLEAN) {
                        	if (operatorSymbol.equalsIgnoreCase("not between") || operatorSymbol.equalsIgnoreCase("between") 
                        			|| operatorSymbol.equals("<") || operatorSymbol.equals(">") || operatorSymbol.equals("<=")
                        			|| operatorSymbol.equals("<=")) {
                        		issues.add(new Issue(IssueCode.DQL_TYPE_DOES_NOT_SUPPORT_OPERATOR,
                                        new String[]{"opr", operatorSymbol, "name", attr.getName()}));
                        	}                    	
                        }
                    	if (attrType == ModelBaseType.NUMBER || attrType == ModelBaseType.FIXED_POINT_NUMBER 
                        		|| attrType == ModelBaseType.DATE || attrType == ModelBaseType.TIME
                        		|| attrType == ModelBaseType.DATE_TIME_TZ || attrType == ModelBaseType.BOOLEAN) {
                        	if (functionLowerPresent || functionUpperPresent) {
                        		String func = "lower";
                        		if (functionUpperPresent)
                        			func = "upper";
                        		issues.add(new Issue(IssueCode.DQL_TYPE_DOES_NOT_SUPPORT_FUNCTION,
                                        new String[]{"func", func, "name", attr.getName()}));
                        	}
                        }
                    	if (functionSizePresent && !isInteger(attrValue)) {
                    		
                    		if (containsDataField(attrValue)) {
								try {
									if (!isDataFieldAnIntegerType(attrValue)) {
										issues.add(new Issue(IssueCode.DQL_SIZE_FUNCTION_NEEDS_INT_VALUE,
											new String[]{"expr", attrValue}));
									}
								} catch (DQLException e) {
									if (e instanceof UnknownDataFieldException) {
										issues.add(new Issue(IssueCode.DQL_UNKNOWN_DATA_FIELD,
						                        new String[] { "datafield", e.getDataField() } ));
									} else if (e instanceof DataFieldTypeMismatchException) {
										issues.add(new Issue(IssueCode.DQL_NON_INTEGER_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION,
									            new String[]{"datafield", attrValue}));
									} else if (e instanceof DataFieldMultiplicityMismatchException) {
										issues.add(new Issue(IssueCode.DQL_MULTI_VALUED_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION,
									            new String[]{"datafield", attrValue}));
									} else {
										issues.add(new Issue(IssueCode.DQL_UNKNOWN_DATA_FIELD,
						                        new String[] { "datafield", e.getDataField() } ));
									}
								}
                    		} else {
                    			issues.add(new Issue(IssueCode.DQL_SIZE_FUNCTION_NEEDS_INT_VALUE,
                                    new String[]{"expr", expr }));
                    		}
                    	}
                    	
                        if (attrType == ModelBaseType.NUMBER || attrType == ModelBaseType.FIXED_POINT_NUMBER)
                        {
                            // Strip insignificant leading zeros (otherwise PostgreS will complain
                            // about a value such as 0199)
                        	//TODO: attrValue can be data field here.
                        	if (containsDataField(attrValue)) {
                    			try {
									if (!validateDataField(attrValue, attr, true, true, operatorSymbol) ) {
										issues.add(new Issue(IssueCode.DQL_INVALID_DATA_FIELD,
									            new String[]{"datafield", attrValue}));
									}
								} catch (DQLException e) {
									if (e instanceof DataFieldTypeMismatchException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_TYPE_MISMATCH,
									            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
									} else if (e instanceof DataFieldNotArrayTypeException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_NOT_ARRAY_TYPE, 
												new String[]{"datafield", attrValue, "opr", operatorSymbol}));
									} else if (e instanceof DataFieldMultiplicityMismatchException) {
										issues.add(new Issue(IssueCode.DQL_DATA_FIELD_MULTIPLICITY_MISMATCH,
									            new String[]{"datafield", attrValue, "attribute", attr.getReferenceName()}));
									}
								}
                    		} else {
                    			attrValue = updateNumberValue(attrValue);
                    		}
                        }
                        if (attrType == ModelBaseType.TEXT || attrType == ModelBaseType.URI) {
                        	
                            if (isWildcardCharPresent(attrValue, "%")) {
                            	operator = SearchOperatorDTO.fromDQLSymbol("like");
                            }
                            if (isWildcardCharPresent(attrValue, "_")) {
                            	operator = SearchOperatorDTO.fromDQLSymbol("like");
                            }
                            if (isWildcardCharPresent(attrValue, "*")) {
                            	operator = SearchOperatorDTO.fromDQLSymbol("like");
                            	attrValue = replaceWildcardChars(attrValue, "*", "%");
                            }                        		
                            if (isWildcardCharPresent(attrValue, "?")) {
                        		operator = SearchOperatorDTO.fromDQLSymbol("like");
                        		attrValue = replaceWildcardChars(attrValue, "?", "_");
                        	}
                            if (isWildcardCharEscaped(attrValue)) {
                            	// The attribute value contains an escape character meant for escaping wildcard characters.
                            	// In this case also we will have to use "like" operator instead of "=".  
                            	operator = SearchOperatorDTO.fromDQLSymbol("like");                           	
                            }
                        }
                            AttributeSearchConditionDTO ascDTO =
                                    new AttributeSearchConditionDTO(attr,
                                            operator, attrValue, conditionOpr);
                            ascDTO.setPreceedingParentheses(
                                    precedingParentheses);
                            ascDTO.setSucceedingParentheses(
                                    succedingParentheses);
                            if (functionUpperPresent) {
                            	ascDTO.setFunctionUpperPresent(true);
                            }
                            if (functionLowerPresent) {
                            	ascDTO.setFunctionLowerPresent(true);
                            }
                            if (functionSizePresent) {
                            	ascDTO.setFunctionSizePresent(true);
                            }
                            ascs.add(ascDTO);
                    }
                }
                else
                {
                        AttributeSearchConditionDTO ascDTO =
                                new AttributeSearchConditionDTO(attr, operator,
                                        null, conditionOpr);
                        ascDTO.setPreceedingParentheses(precedingParentheses);
                        ascDTO.setSucceedingParentheses(succedingParentheses);
                        ascs.add(ascDTO);
                }
            }
            else
            {
                issues.add(new Issue(IssueCode.DQL_UNKNOWN_ATTRIBUTE_NAME, new String[]{"name", attrName}));
            }
        } else {
        	if (!isValidExpression(expr)) {
        		operatorExpressionMatchFailed = true;
        	}
        }
	        // Check for the order by clause
	        m = ORDER_BY_EXPR.matcher(expr);
	        if (m.matches()) {
	        	sortOrder = new SortOrder();
	        	String columnsStr = expr.trim().substring(m.group(1).trim().length());
            	String[] columns = columnsStr.split(",");    
            	
            	for (int l=0; l<columns.length; l++) {
            		
            		String attrName;
            		ModelAttribute attr = null;
            		boolean isDescending = false;
            		String colstr = columns[l].trim();
            		String[] segs = colstr.split(" ");
            		if (segs.length > 2) {
            			orderbyMatchFailed = true;
            			continue;
            		} else if (segs.length == 2) {
            			attrName = segs[0].trim();
            			if (segs[1].equalsIgnoreCase("asc")) {
            				
            			} else if (segs[1].equalsIgnoreCase("desc")) {
            				isDescending = true;
            			} else {
            				orderbyMatchFailed = true;
            				continue;
            			}
            		} else {
            			attrName = segs[0].trim();
            		}
            		
            		attr = getValidAttributeForSort(attrName);
        			if (attr != null) {
        				SortColumn sc = new SortColumn(attr);
        				if (isDescending) {
        					sc.setDescendingOrder();
        				}
        				sortOrder.addSortColumn(sc);
        			}        			
            	}
            	
	        } else {
            	if (!isValidExpression(expr)) {
            		orderbyMatchFailed = true;
            	}	        
            }
	        if (operatorExpressionMatchFailed && orderbyMatchFailed) {
	        	issues.add(new Issue(IssueCode.DQL_BAD_EXPRESSION, new String[]{"expression", expr}));
	        }
    
        }
    }

	private static boolean isText(String value)
	{
		return true;
	}

	private static boolean isInteger(String value) {
		boolean result = false;
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {				
		}
		return result;
	}
	
    private static boolean isNumber(String value)
	{
		boolean result = false;
		try
		{
			new BigDecimal(value);
			// If we got this far, it's a number
			result = true;
		} catch (NumberFormatException e) {			
			// Check for following formats - 1000 and 2000; (1000,2000) used for in and between clauses.
			List<String> values = splitValues(value);
			try
			{
				for (String value1 : values) {
					new BigDecimal(value1);
				}
				result = true;
			} catch (NumberFormatException e1) {
			}		
		}
		return result;
	}


	private static boolean isTime(String value)
	{
		boolean result = patTime.matcher(value).matches();
		if (result) {
			return result;
		}
		
		List<String> values = splitValues(value);
		for (String value1 : values) {
			result = patTime.matcher(value1).matches();
			if (!result) {
				return result;
			}			
		}
		return true;
	}
	
	private static boolean isDate(String value) {
		boolean result = isDateValid(value);
		if (result) {
			return result;
		}
		List<String> values = splitValues(value);
		for (String value1 : values) {
			result = isDateValid(value1);
			if (!result) {
				return result;
			}			
		}
		return true;
	}

	private static boolean isDateValid(String value)
	{
		// Elastic Search performs leap year validation, so we need that level of
		// strictness here, or we'll run into problems on createCase.
		Matcher matcher = patDate.matcher(value);
		boolean result = true;
		if (matcher.matches())
		{
			int year = Integer.valueOf(matcher.group(1));
			int month = Integer.valueOf(matcher.group(2));
			int day = Integer.valueOf(matcher.group(3));

			// Thirty days hath September,
			// April, June, and November.
			if ((month == 9 || month == 4 || month == 6 || month == 11) && day == 31)
			{
				// (Not allowed 31st in those months)
				result = false;
			}
			// All the rest have thirty-one...
			else if (month == 2)
			{
				// Except for February alone,
				// Which hath but twenty-eight days clear,
				// And twenty-nine in each leap year. 
				boolean isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
				int maxDay = isLeapYear ? 29 : 28;
				if (day > maxDay)
				{
					result = false;
				}
			}
			// https://en.wikipedia.org/wiki/Thirty_days_hath_September
		}
		else
		{
			result = false;
		}
		return result;
	}

	private static boolean isDateTimeTZ(String value)
	{
		// Note that the Elastic Search concerns in isDate(...) don't apply here, 
		// as Date Time TZ is an ACE exclusive, so ES is irrelevant.
		boolean result = false;
		if (value == null) {
			return result;
		}
		result = patDataTimeTZ.matcher(value).matches();
		if (result) {
			return result;
		}
		
		List<String> values = splitValues(value);
		for (String value1 : values) {
			result = patDataTimeTZ.matcher(value1).matches();
			if (!result) {
				return result;
			}			
		}
		return true;
	}

	private static boolean isBoolean(String value)
	{
		return BOOLEAN_VALUES.contains(value);
	}
	
	private static boolean isURI(String value)
	{
		return true;
	}

	private boolean isStringType(String value, ModelBaseType type)
	{
		boolean result = false;
		if (value != null && type != null)
		{
			if (type == ModelBaseType.TEXT)
			{
				result = isText(value);
			}
			if (type == ModelBaseType.NUMBER)
			{
				result = isNumber(value);
			}
			if (type == ModelBaseType.FIXED_POINT_NUMBER)
			{
				result = isNumber(value);
			}
			else if (type == ModelBaseType.TIME)
			{
				result = isTime(value);
			}
			else if (type == ModelBaseType.DATE)
			{
				result = isDate(value);
			}
			else if (type == ModelBaseType.DATE_TIME_TZ)
			{
				result = isDateTimeTZ(value);
			}
			else if (type == ModelBaseType.BOOLEAN)
			{
				result = isBoolean(value);
			}
			else if (type == ModelBaseType.URI)
			{
				result = isURI(value);
			}
		}
		return result;
	}

    private List<String> splitOnAndOr(String str)
	{
		List<String> expressions = new ArrayList<String>();
		boolean inQuotedPortion = false;
		boolean escaped = false;
		boolean inClausePresent = false;
		boolean functionPresent = false;
		int start = 0;
        int endParanthesesIndex = 0;
        int noOfEndParantheses = 0;
        int orderByIndex = 0;
        int consumedIndex = 0;

		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);

			if (inQuotedPortion)
			{
				// When we're in a quoted portion, we're looking for the end of it (i.e. the closing quote)
				// During this, we need to be aware of escaped quotes that are part of the literal value
				// and not intended to end it.
				if (escaped)
				{
					// We're escaped, so this must be a \ or ' to be valid
					if (c != '\\' && c != '\'')
					{
						// An illegal escape sequence was found, so return an empty list.
						// The calling method interprets this as meaning the filter can't be parsed.
						return Collections.emptyList();
					}
					// We've found the character that was escaped, so escaping is no longer in effect.
					escaped = false;
				}
				else if (c == '\\')
				{
					// Escape character found within quoted portion
					// (Next character will need to be a \ or ')
					escaped = true;
				}
				// At the end of the quoted portion?
				else if (c == '\'')
				{
					// We've left the quoted portion, so we're interested in
					// looking for 'ands' again
					inQuotedPortion = false;
				}
			}
			else
			{
				if (c == '\'')
				{
					// Found a quote, so ignore everything until we leave the
					// quoted portion
					inQuotedPortion = true;
				}
                else if (c == '(')  {
                	if (!inClausePresent && !functionPresent) {
                		if (consumedIndex < i) {
                			String str1 = str.substring(consumedIndex,i);
                			if (str1.length() == 1 && str1.trim().isEmpty()) {
                			} else {
                				expressions.add(str.substring(consumedIndex,i));
                			}
                		}
                		expressions.add("("); //$NON-NLS-1$
	                    endParanthesesIndex = 0;
	                    noOfEndParantheses = 0;
	                    start = i + 1;
	                    consumedIndex = i+1;
                	}
                } else if (c == ')') {
                	if (!inClausePresent && !functionPresent) {
	                    endParanthesesIndex = i;
	                    noOfEndParantheses++;
                	} else {
                		if (inClausePresent)
                			inClausePresent = false;
                		if (functionPresent) 
                			functionPresent = false;
                	}
                } else
				{

                	Matcher mOrder = ORDER_BY_EXPR.matcher(str.substring(i));
                	if (mOrder.matches()){
                		orderByIndex = i;
                	}
                	
                	Matcher mIn = IN_NOT_IN_EXPR.matcher(str.substring(i));
                	if (mIn.matches()) {
                		inClausePresent = true;
                	}               	
                	Matcher mfunc = FUNCTION_EXPR.matcher(str.substring(i));
                	if (mfunc.matches()) {
                		functionPresent = true;
                	}
                	// Not in a quoted expression, so check for an ' and '
                    Matcher m = AND_OR_START.matcher(str.substring(i));
					if (m.matches())
					{
						// Found an 'and', so consume everything up to this
						// point and skip over the 'and'.
                        String expr = null;
                        if (endParanthesesIndex == 0) {
                            expr = str.substring(start, i).trim();
                            expressions.add(expr);
                            consumedIndex = i;
                        } else {
                            expr = str
                                    .substring(start,
                                            endParanthesesIndex
                                                    - noOfEndParantheses + 1)
                                    .trim();
                            expressions.add(expr);
                            consumedIndex = endParanthesesIndex
                                    - noOfEndParantheses + 1;
                            for (int k = 0; k < noOfEndParantheses; k++) {
                                expressions.add(")"); //$NON-NLS-1$
                            }
                            endParanthesesIndex = 0;
                            noOfEndParantheses = 0;
                        }
                        expressions.add(m.group(1));
                        if (m.group(1).equalsIgnoreCase("or ")) {
                            start = i + m.end(1) - 1;
                        } else {
                            start = i + m.end(1);
                        }
                        i = start;
                        consumedIndex = i;
					}
				}
			}
		}

		// Consume whatever is left after the final and
		if (start < str.length())
		{
            String expr = null;
            if (endParanthesesIndex == 0) {
            	if (orderByIndex == 0) {
            		expr = str.substring(start).trim();
            		expressions.add(expr);
            	} else {
            		if (orderByIndex > start) {
            			expr = str.substring(start, orderByIndex).trim();
            			expressions.add(expr);
            		}
            		expr = str.substring(orderByIndex);
            		expressions.add(expr);
            	}                
            } else {
                expr = str
                        .substring(start,
                                endParanthesesIndex - noOfEndParantheses + 1)
                        .trim();
                expressions.add(expr);
                for (int i = 0; i < noOfEndParantheses; i++) {
                    expressions.add(")"); //$NON-NLS-1$
                }
                consumedIndex = endParanthesesIndex + 1;
                if (orderByIndex != 0) {
                	expr = str.substring(orderByIndex).trim();
            		expressions.add(expr);
                } else {
                	if (consumedIndex < str.length()) {
                		expressions.add(str.substring(consumedIndex));
                	}
                }               
            }

		}

		return expressions;
	}

	// Removes backslashes from the value, except where followed by a second backslash
	// (which is an intentional literal backslash, preceded by the escape character)
	private String unescape(String value)
	{
		String result = value.replaceAll(LONE_BACKSLASH, "");
		return result;
	}

	/**
	 * Parsed the given DQL string, returning an object representation that can subsequently be
	 * converted to SQL by {@link ConditionRenderer}
	 * @param dql
	 * @return
	 */
	public SearchConditionDTO parse(String dql)
	{
        if (!Utils.matchBraces(dql, LEFT_PARA, RIGHT_PARA)) {
            issues.add(new Issue(IssueCode.DQL_PARANTHESES_MATCH_ERROR,
                    new String[] { "dql", dql }));
            return null;
        }
        if (containsTypeOf(dql)) {
            issues.add(new Issue(IssueCode.DQL_NO_SUPPORT_FOR_TYPE_OF,
                    new String[] { "dql", dql }));
            return null;        	
        }
        
        if (!Utils.matchBraces(dql, "[", "]")) {        	
        	issues.add(new Issue(IssueCode.DQL_SQ_BRACKETS_MATCH_ERROR,
                    new String[] { "dql", dql }));
            return null; 
		}
        if (!Utils.matchBraces(dql, "${", "}")) {
        	issues.add(new Issue(IssueCode.DQL_BRACES_MATCH_ERROR,
                    new String[] { "dql", dql }));
		}
        
        List<String> expressions = splitOnAndOr(dql);

		if (expressions.isEmpty())
		{
			issues.add(new Issue(IssueCode.DQL_UNPARSABLE, new String[]{"string", dql}));
		}

        validateExpressions(expressions);

        // As the parser is splitting on 'and' and 'or', create the group
        // accordingly.
        ConditionGroupDTO groupDTO = new AndOrGroupDTO(ascs);
        if (sortOrder != null) {
        	groupDTO.setSortOrder(sortOrder);
        }
        return groupDTO;
	}


    /**
     * Strips any leading zeros from the number value
     * @param value
     * @return
     */
    private static String updateNumberValue(String value)
	{
    	List<String> updatedValues = new ArrayList<String>();
    	
		if (value.indexOf("and") != -1) {
			List<String> values = splitValues(value);
			for (String value1 : values) {
				try
				{
		            BigDecimal bd = new BigDecimal(value1);
		            value1 = bd.toPlainString();
					updatedValues.add(value1);
				} catch (NumberFormatException e1) {
				}					
			}
			value = updatedValues.get(0) + " and " + updatedValues.get(1);
			
		} else if (value.startsWith("(") && value.endsWith(")") && value.indexOf(",") != -1) {
			List<String> values = splitValues(value);
			try {
				for (String value1 : values) {
		            BigDecimal bd = new BigDecimal(value1);
		            value1 = bd.toPlainString();
					updatedValues.add(value1);
				}
				StringBuffer buff = new StringBuffer();
				buff.append("(");
				for (int i=0; i < updatedValues.size(); i++) {
					buff.append(updatedValues.get(i));
					if (i < updatedValues.size()-1) {
						buff.append(",");
					}
				}
				buff.append(")");
				value = buff.toString();	
			} catch (NumberFormatException e){
			}
		} else {
			try
			{
	            BigDecimal bd = new BigDecimal(value);
	            value = bd.toPlainString();
			}
			catch (NumberFormatException e)
			{
			}			
			
		}
		return value;
	} 
    
    private static List<String> splitValues(String value) {
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
    
    private static boolean isValueQuoted(String value) {
    	// The value can be a single value or comma separated multiple values.
    	if (value == null) 
    		return false;
		List<String> values = splitValues(value);
		for (String cvalue : values) {
			if (cvalue.startsWith("'") && cvalue.startsWith("'") ) {
				continue;
			} else {
				return false;
			}
		}
		return true;
    }
    
    /**
     * The SQL query syntax recognizes % as multi-char wildcard and _ as single char wildcard.
     * Our DQL syntax can also have * as multi-char wildcard and ? as single char wildcard
     * This function can be used replace the given wildcard char to suit SQL query syntax.
     * @param str
     * @param wildchar - wildcard character given in the string
     * @param rep - wildcar character will be replaced with the one supported by SQL.
     * @return - updated string
     */
    private static String replaceWildcardChars(String str, String wildchar, String rep) {   	
    	StringBuffer buff = new StringBuffer();   	
    	int i = 0, index = 0;
    	while (str.indexOf(wildchar,i) != -1) {
    		index = str.indexOf(wildchar,i);
    		if (index > 0) {
    			if (str.substring(index-1, index).equalsIgnoreCase("\\")) {
    				buff.append(str.substring(i,index+1));
    			} else {
    				buff.append(str.substring(i,index));
    				buff.append(rep);
    			}
    		} else {
    			buff.append(rep);
    		}
    		i = index+1;
    	}
    	if (i == index+1) {
    		buff.append(str.substring(i));
    	}
    	return buff.toString();  	
    }
    
    /**
     * Checks the presence of given wildcard char in the attribute value provided in the DQL.
     * Supported characters are % _ * ?
     * @param str
     * @param wildchar
     * @return true for the presence of wildcard character.
     */
    private static boolean isWildcardCharPresent(String str, String wildchar) {
    	
    	boolean present = false;    	
    	int i = 0;
    	while (str.indexOf(wildchar,i) != -1) {
    		int index = str.indexOf(wildchar,i);
    		if (index > 0) {
    			if (!str.substring(index-1, index).equalsIgnoreCase("\\")) {
    				present = true;
    			} 
    		} else {
    			present = true;
    		}
    		i = index+1;
    	}	
    	return present;
    }
    
    private static boolean isWildcardCharEscaped(String str) {
    	
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
    
    private boolean isFunctionUpperPresent(String attr) {
    	Pattern FUNCTION_UPPER_EXPR =
                Pattern.compile("(?i)(upper\\(\\S+\\)).*");
    	Matcher mfunc = FUNCTION_UPPER_EXPR.matcher(attr);
    	if (mfunc.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    private boolean isFunctionLowerPresent(String attr) {
    	Pattern FUNCTION_LOWER_EXPR =
                Pattern.compile("(?i)(lower\\(\\S+\\)).*");
    	Matcher mfunc = FUNCTION_LOWER_EXPR.matcher(attr);
    	if (mfunc.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private boolean isFunctionSizePresent(String attr) {
    	Pattern FUNCTION_SIZE_EXPR =
                Pattern.compile("(?i)(size\\(\\S+\\)).*");	
    	Matcher mfunc = FUNCTION_SIZE_EXPR.matcher(attr);
    	if (mfunc.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private static boolean containsDataField(String str) {
    	return Utils.containsDataField(str);
    }
    
    /**
     * Checks if data field is present in the attribute path segment.
     * @param path
     * @return true in case data field is present.
     * @throws DQLException
     */
    private static boolean isDataFieldPresentInAttrPathSegment(String path) throws DQLException {
    	String indexStr = getIndexFromAttrPathSegment(path);
    	if (indexStr == null) {
    		return false;
    	}

	    if (indexStr.startsWith("$")) {
	    	Matcher m = Utils.DATA_FIELD_EXPR.matcher(indexStr);
	        if (m.matches()) {
	            return true;
	        } else {
	            m = Utils.DATA_FIELD_EXPR_ALT.matcher(indexStr);
	            if (m.matches()) {
	            	return true;
	            }
	            IncorrectSyntaxForDataFieldException idfe = new IncorrectSyntaxForDataFieldException();
	            idfe.setAttributePath(path);
	            throw idfe;
	        }            	
	    } else {
	        return false;
	    }
    }
    
    /**
     * Checks if valid integer value is present in the index of the path segement.
     * @param path
     * @return true if valid integer value is present as the index
     * @throws DQLException
     */
    private static boolean isIntegerValuePresentInAttrPathSegment(String path) throws DQLException {
    	String indexstr = getIndexFromAttrPathSegment(path);
    	if (indexstr == null) {
    		return false;
    	}
    	if (indexstr.startsWith("$")) {
    		return false;
    	}
    	try {
    		Integer.valueOf(indexstr);
    		return true;
    	} catch(NumberFormatException e) {
    		// throw new exception Index must be an Integer value
    		InvalidIndexException ie = new InvalidIndexException();
    		ie.setAttributeName(path);
    		throw ie;
    	}
    }
    
    private static String getIndexFromAttrPathSegment(String segment) throws DQLException {
    	String indexstr = null;
      	int startIndex = segment.indexOf("["); //$NON-NLS-1$
	    int endIndex = -1;
	    if (startIndex != -1) {
	        endIndex = segment.lastIndexOf( "]");
	        if (endIndex == -1) {
		        BadlyFormattedPathExcpetion e = new BadlyFormattedPathExcpetion();
		        e.setAttributePath(segment);
		        throw e;
	        }
	        if (endIndex < segment.length()-1) {
	        	SpuriousTextAfterBracketException e = new SpuriousTextAfterBracketException();
	        	e.setAttributePath(segment);
	        	throw e;
	        }
	        indexstr = segment.substring(startIndex + 1, endIndex);
	        if (Utils.containsTag(indexstr)) {
	        	TagNotSupportedException e = new TagNotSupportedException();
	        	e.setAttributePath(segment);
	        	throw e;
	    	} else if (indexstr.equalsIgnoreCase("all")) {
	    		ALLIndexNotSupportedException e = new ALLIndexNotSupportedException();
        		e.setAttributePath(segment);
        		throw e;
        	}
	       
	    }
	    return indexstr;
    }
    
    private String retrieveAttrName(String attrStr) {
    	int length = attrStr.length();
    	if (isFunctionUpperPresent(attrStr)) {
    		return attrStr.substring(6, length-1);
    	} else if (isFunctionLowerPresent(attrStr)) {
    		return attrStr.substring(6, length-1);
    	} else if (isFunctionSizePresent(attrStr)) {
    		return attrStr.substring(5, length-1);
    	}
    	return attrStr;
    		
    }
    
    private ModelAttribute getValidAttributeForSort(String name) {
        ModelAttribute attr = null;
        try {
            attr = type.getAttribute(name);
            if (attr != null) {
            	if (attr.isArray()) {
            		issues.add(new Issue(
                            IssueCode.DQL_MULTI_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_ORDER_BY,
                            new String[] { "name", name }));
            		return null;
            	}
            	attr.setReferenceName(name);
            	return attr;
            } else {
            	issues.add(new Issue(
                        IssueCode.DQL_UNKNOWN_ATTRIBUTE_NAME,
                        new String[] { "name", name }));
            }
        } catch (MultiValuedAttributeMissingIndexException e) {
            issues.add(new Issue(
                    IssueCode.DQL_MULTI_VALUED_ATTRIBUTE_MISSING_INDEX,
                    new String[] { "name", e.getAttributeName(), "path", e.getAttributePath() } ));
        } catch (SingleValuedAttributeIndexException e) {
            issues.add(new Issue(
                    IssueCode.DQL_SINGLE_VALUED_ATTRIBUTE_INDEX_PRESENT,
                    new String[] { "name", e.getAttributeName(), "path", e.getAttributePath() } ));
        } catch (UnknownDataTypeException e) {
            issues.add(new Issue(
                    IssueCode.DQL_UNKNOWN_DATA_TYPE,
                    new String[] { "name", e.getAttributeName(), "parent", e.getParentName() }));
        } catch (UnknownAttributeException e) {
            issues.add(new Issue(
                    IssueCode.DQL_UNKNOWN_ATTRIBUTE,
                    new String[]{ "name", e.getAttributeName(), "parent", e.getParentName() }));
        } catch (InvalidIndexException iie) {
        	if (iie instanceof BadlyFormattedPathExcpetion) {
        		issues.add(new Issue(
                        IssueCode.DQL_BADLY_FORMATTED_PATH_EXPRESSION,
                        new String[]{"path", iie.getAttributePath()}));
        	} else if (iie instanceof ALLIndexNotSupportedException) {
        		issues.add(new Issue(
                        IssueCode.DQL_ALL_INDEX_NOT_SUPPORTED,
                        new String[]{"path", iie.getAttributePath()}));
        	} else if (iie instanceof SpuriousTextAfterBracketException) {
        		issues.add(new Issue(
                        IssueCode.DQL_SPURIOUS_TEXT_AFTER_BRACKET,
                        new String[]{"path", iie.getAttributePath()}));
        	} else {
        		issues.add(new Issue(
                        IssueCode.DQL_INDEX_MUST_BE_AN_INTEGER,
                        new String[]{"name", iie.getAttributeName()}));
        	}
        
        } catch (DQLException e) {
			issues.add(new Issue(IssueCode.DQL_BAD_EXPRESSION, new String[] {"expression" , e.getAttributePath()}));
		}
        return null;
    }
    
    private boolean isValidExpression(String expr) {    	
    	if (expr != null && !expr.isEmpty()) {
    		expr = expr.trim();
    		if (expr.equalsIgnoreCase("and") || expr.equalsIgnoreCase("or") || expr.equalsIgnoreCase("(") || expr.equalsIgnoreCase(")")) {
    			return true;
    		} else {
    			return false;
    		}
    	}
    	return false;
    }
    
    private boolean isUnspportedQueryParam(String value) {
    	if (value != null && value.startsWith(":")) {
    		return true;
    	} else 
    		return false;
    }
    
    private boolean containsTypeOf(String str) {
    	if (str.toLowerCase().indexOf(" type ") == -1 ) {
    		return false;
    	}
    	int index = str.toLowerCase().indexOf(" type ");
    	Matcher m = TYPE_OF_EXPR.matcher(str.substring(index));
    	if (m.matches()) {
    		return true;
    	} else {
    		return false;
    	}
    } 
    
    private boolean validateDataField(String field, ModelAttribute attribute, boolean checkAttributeType, boolean checkMultiplicity, String operator) throws DQLException{
    	
    	if (dfProvider == null || field == null) {
    		return true;
    	}
    	
    	int startIndex = field.indexOf("${");
    	int endIndex = field.lastIndexOf("}");
    	String dfname = field.substring(startIndex+2,endIndex);
    	ModelAttribute dataField = dfProvider.getDataField(dfname);
    	if (dataField == null) {
    		return false;
    	}
				
    	if ( checkAttributeType && (dataField.getType() != attribute.getType() )) {
    		// Check if the data type for the data field and DQL attribute matches.
    		DataFieldTypeMismatchException dfe = new DataFieldTypeMismatchException();
    		dfe.setAttributeName(attribute.getName());
    		dfe.setAttributePath(attribute.getReferenceName());
    		throw dfe;
    	}
    	if (operator != null && (operator.equalsIgnoreCase("between") || operator.equalsIgnoreCase("not between") 
    			|| operator.equalsIgnoreCase("in") || operator.equalsIgnoreCase("not in") )) {
    		// For the operators - in, not in, between, not between
    		// The datafield should be of the same type as the attribute in the DQL and should be of an array type.
    		if (!dataField.isArray()) {
    			DataFieldNotArrayTypeException dfe = new DataFieldNotArrayTypeException();
    			dfe.setAttributeName(attribute.getName());
        		dfe.setAttributePath(attribute.getReferenceName());
        		dfe.setDataField(field);
        		throw dfe;
    		} else {
    			return true;
    		}
    	}
    	if (checkMultiplicity && (dataField.isArray() != attribute.isArray() )) {
    		// Check if the multiplicity for the data field and DQL attribute matches.
    		DataFieldMultiplicityMismatchException dfe = new DataFieldMultiplicityMismatchException();
    		dfe.setAttributeName(attribute.getName());
    		dfe.setAttributePath(attribute.getReferenceName());
    		throw dfe;
    	}
    	return true;	
    }
    
    private boolean isDataFieldAnIntegerType(String field) throws DQLException{
    	
    	if (dfProvider == null || field == null) {
    		return true;
    	}
    	
    	if (field.startsWith("${data.")) {
    		String dfname = field.substring(2,field.length()-1);
    			ModelAttribute dataField = dfProvider.getDataField(dfname);
    			if (dataField == null) { 
    				UnknownDataFieldException ue = new UnknownDataFieldException();
    	        	ue.setDataField(field);
    	        	throw ue;    				
    			}
				if (dataField.isArray()) {
					DataFieldMultiplicityMismatchException dfe = new DataFieldMultiplicityMismatchException();
					dfe.setDataField(field);
					throw dfe;
				}
				if (dataField.getType() != ModelBaseType.FIXED_POINT_NUMBER ) {
					DataFieldTypeMismatchException dfe = new DataFieldTypeMismatchException();
					dfe.setDataField(field);
					throw dfe;
				}
     			if (ModelBaseType.FIXED_POINT_NUMBER == dataField.getType()){
    				String decimalPlaces = dataField.getConstraint("decimalPlaces");
    				if (decimalPlaces != null && !decimalPlaces.isEmpty()) {
	    				try {
	    					if (Integer.valueOf(decimalPlaces) == 0) {
	    						return true;
	    					}
	    				} catch (NumberFormatException e) {
	    					// not an integer if it hasn't got a valid '0' spec for decimals.
	    				}
    				}
    			}
     			return false;
    	} else {
    		return false;
    	}
    	
    }
    
    /**
     * This method validates the attribute path and returns the attribute if it exsits.
     * It also validates the index provided for the multi valued attributes.
     * The index can be a valid integer or a data field type for fixed point number with zero decimal points. 
     * @param attrPath Qualified path for given attribute
     * @return the attribute if it exists.
     * @throws DQLException
     */
    		
    public ModelAttribute validateAttrPath(String attrPath, boolean isSizeFunction) throws DQLException{
    	
    	ModelAttribute attr = null;
    	List<String> segs = Utils.getAttrPathSegments(attrPath);
    	String qualifiedPath = null;
    	for (int i = 0; i < segs.size(); i++) {
    		
    		boolean isLastSegment = (i < segs.size()-1) ? false : true;
    		String segment = segs.get(i);
    		boolean isValidIntegerInIndex =  isIntegerValuePresentInAttrPathSegment(segment);
    		boolean isDataFieldInIndex = isDataFieldPresentInAttrPathSegment(segment);
    		String attrName = segment;
    		
    		int startIndex = segment.indexOf("["); //$NON-NLS-1$
    	    int endIndex = segment.lastIndexOf( "]");
    	    if (startIndex != -1 && endIndex != -1) {
    	    	attrName = segment.substring(0,startIndex);
    	    }

    	    if (qualifiedPath == null) {
    	    	qualifiedPath = attrName;
    		} else {
    			qualifiedPath = qualifiedPath + "." + attrName;
    		}
    	    
    	    // Check if attribute is multi valued attribute or structured type.
    	    // Check if the data field value is of Number data type.
    	    attr = type.getAttribute(qualifiedPath);
    	    if (attr == null) {
    	    	return null;
    	    }
    	    
    	    if (isLastSegment && attr.getType() instanceof ModelStructuredType) {
    	    	if (isSizeFunction && isValidIntegerInIndex) {
    	    		IndexNotSupportedForSizeFunctionException ise = new IndexNotSupportedForSizeFunctionException();
    	    		ise.setAttributeName(attr.getName());
    	    		ise.setAttributePath(attrPath);
    	    		throw ise;
    	    	}
    	    	if (!isSizeFunction) {
    	    		LeafNodeStruturedTypeException le = new LeafNodeStruturedTypeException();
            	    le.setAttributePath(attrPath);
            	    throw le;
    	    	} else {
    	    		return attr;
    	    	}
    	    }

    	    if (attr.isArray() && !(isValidIntegerInIndex || isDataFieldInIndex)) {
    	    	MultiValuedAttributeMissingIndexException me = new MultiValuedAttributeMissingIndexException();
    	    	me.setAttributeName(attrName);
    	        me.setAttributePath(attrPath);
    	        throw me;    	    	
    	    }
    	    
    	    if (!attr.isArray() && (isValidIntegerInIndex || isDataFieldInIndex)) {
    	        SingleValuedAttributeIndexException se = new SingleValuedAttributeIndexException();
    	        se.setAttributeName(attrName);
    	        se.setAttributePath(attrPath);
    	        throw se;
    	    }
    	    if (isValidIntegerInIndex) {
        	    continue;
    	    }
    	    
    	    if (isDataFieldInIndex) {
    	        String dataField = segment.substring(startIndex + 1, endIndex);
    	        String dftemp = segment.substring(startIndex + 3, endIndex-1);
    	        ModelAttribute dfAttr = dfProvider.getDataField(dftemp);
    	        if (dfAttr == null) {
    	        	UnknownDataFieldException ue = new UnknownDataFieldException();
    	        	ue.setDataField(dataField);
    	        	ue.setAttributePath(attrPath);
    	        	throw ue;
    	        }
    	        
    	        if (!isDataFieldAnIntegerType(dataField)) {
    	        	DataFieldIndexNotAnIntegerException e = new DataFieldIndexNotAnIntegerException();
    	        	e.setAttributePath(attrPath);
    	        	e.setDataField(dataField);
    	        	throw e;
    	        }   
    	    }    		
    	}
    	return attr;
    }
    
    public boolean hasIssues()
	{
		return !issues.isEmpty();
	}

	public List<Issue> getIssues()
	{
		return issues;
	}
	
}
