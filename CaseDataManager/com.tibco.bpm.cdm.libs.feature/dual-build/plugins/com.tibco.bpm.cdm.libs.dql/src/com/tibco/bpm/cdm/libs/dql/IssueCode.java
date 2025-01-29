package com.tibco.bpm.cdm.libs.dql;

/**
 * Represents a type of issue that may occur with a DQL statement
 * @author smorgan
 * @since 2019
 */
public class IssueCode
{
	public enum Type
	{
		ERROR
	};

	private String					code;

	private String					message;

	private Type					type;

	public static final IssueCode	DQL_UNKNOWN_ATTRIBUTE_NAME		= new IssueCode("DQL_UNKNOWN_ATTRIBUTE_NAME",
			"Unknown attribute name: {name}", Type.ERROR);

	public static final IssueCode	DQL_UNPARSABLE					= new IssueCode("DQL_UNPARSABLE",
			"Unparseable DQL string: {string}", Type.ERROR);

	public static final IssueCode	DQL_BAD_EXPRESSION				= new IssueCode("DQL_BAD_EXPRESSION",
			"Bad expression: {expression}", Type.ERROR);

	public static final IssueCode	DQL_ATTRIBUTE_NOT_SEARCHABLE	= new IssueCode("DQL_ATTRIBUTE_NOT_SEARCHABLE",
			"Attribute '{name}' is not searchable: {expression}", Type.ERROR);

	public static final IssueCode	DQL_VALUE_SHOULD_BE_QUOTED		= new IssueCode("DQL_VALUE_SHOULD_BE_QUOTED",
			"Value for {type} attribute should be single-quoted: {expression}", Type.ERROR);

	public static final IssueCode	DQL_VALUE_SHOULD_NOT_BE_QUOTED	= new IssueCode("DQL_VALUE_SHOULD_NOT_BE_QUOTED",
			"Value for {type} attribute should not be quoted: {expression}", Type.ERROR);

	public static final IssueCode	DQL_VALUE_NOT_VALID_FOR_TYPE	= new IssueCode("DQL_VALUE_NOT_VALID_FOR_TYPE",
			"Value is not appropriate for {type} type: {expression}", Type.ERROR);

    public static final IssueCode DQL_LEAF_NODE_STRUCTURED_TYPE =
    new IssueCode("DQL_LEAF_NODE_STRUCTURED_TYPE",
            "Leaf node in the query fragment '{path}' is of structured data type. It needs to be of a simple data type.",
            Type.ERROR);
	
    public static final IssueCode DQL_MULTI_VALUED_ATTRIBUTE_MISSING_INDEX =
            new IssueCode("DQL_MULTI_VALUED_ATTRIBUTE_MISSING_INDEX",
                    "Multi valued attribute '{name}' should be specified with an index: {path}",
                    Type.ERROR);

    public static final IssueCode DQL_SINGLE_VALUED_ATTRIBUTE_INDEX_PRESENT =
            new IssueCode("DQL_SINGLE_VALUED_ATTRIBUTE_INDEX_PRESENT",
                    "Single valued attribute '{name}' should not be specified with an index: {path}",
                    Type.ERROR);

    public static final IssueCode DQL_UNKNOWN_DATA_TYPE = 
			new IssueCode("DQL_UNKNOWN_DATA_TYPE",
					"Unknown data type attribute '{name}' from the data type '{parent}'.", Type.ERROR);
    
    public static final IssueCode DQL_UNKNOWN_ATTRIBUTE = 
			new IssueCode("DQL_UNKNOWN_ATTRIBUTE",
					"Unknown attribute '{name}' from the data type '{parent}'.", Type.ERROR);
    
    public static final IssueCode DQL_BADLY_FORMATTED_PATH_EXPRESSION = 
			new IssueCode("DQL_BADLY_FORMATTED_PATH_EXPRESSION",
					"Badly formatted path expression : {path}", Type.ERROR);
	
	public static final IssueCode DQL_SPURIOUS_TEXT_AFTER_BRACKET = 
			new IssueCode("DQL_SPURIOUS_TEXT_AFTER_BRACKET",
					"Spurious text is present after the end bracket : {path}", Type.ERROR);	
	
	public static final IssueCode DQL_ALL_INDEX_NOT_SUPPORTED = 
			new IssueCode("DQL_ALL_INDEX_NOT_SUPPORTED",
					"Index value ALL is not supported : {path}", Type.ERROR);	
	
	public static final IssueCode DQL_$TAG_INDEX_NOT_SUPPORTED = 
			new IssueCode("DQL_$TAG_INDEX_NOT_SUPPORTED",
					"$ tag for index value is not supported : {path}", Type.ERROR);
	
	public static final IssueCode DQL_INDEX_MUST_BE_AN_INTEGER = 
			new IssueCode("DQL_INDEX_MUST_BE_AN_INTEGER",
					"Index must be an integer value : {name}", Type.ERROR);	
	
	public static final IssueCode DQL_INDEX_NOT_SUPPORTED_FOR_SIZE_FUNCTION = 
			new IssueCode("DQL_INDEX_NOT_SUPPORTED_FOR_SIZE_FUNCTION",
					"Index should not be provided for the multi valued attribute '{name}' within the size() function. : {path}", 
					Type.ERROR);	
	
	public static final IssueCode DQL_PARANTHESES_MATCH_ERROR =
            new IssueCode("DQL_PARANTHESES_MATCH_ERROR",
                    "Parantheses are not matching in the given DQL string: {dql}",
                    Type.ERROR);

	public static final IssueCode DQL_SQ_BRACKETS_MATCH_ERROR =
            new IssueCode("DQL_SQ_BRACKETS_MATCH_ERROR",
                    "Right and left side square brackets used for array index are not matching in the given DQL string: {dql}",
                    Type.ERROR);
	
	public static final IssueCode DQL_BRACES_MATCH_ERROR =
            new IssueCode("DQL_BRACES_MATCH_ERROR",
                    "Right and left side braces used for the data fields are not matching in the given DQL string: {dql}",
                    Type.ERROR);
	
	public static final IssueCode DQL_TYPE_DOES_NOT_SUPPORT_OPERATOR =
            new IssueCode("DQL_TYPE_DOES_NOT_SUPPORT_OPERATOR",
                    "The data type for the attribute '{name}' does not support the operator: {opr}",
                    Type.ERROR);
 
    public static final IssueCode DQL_TYPE_DOES_NOT_SUPPORT_FUNCTION =
            new IssueCode("DQL_TYPE_DOES_NOT_SUPPORT_FUNCTION",
                    "The data type for the attribute '{name}' does not support the function: {func}",
                    Type.ERROR);

    public static final IssueCode DQL_SINGLE_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_SIZE_FUNCTION =
            new IssueCode("DQL_SINGLE_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_SIZE_FUNCTION",
                    "The attribute '{name}' is specified as single valued attribute which does not support the size function.",
                    Type.ERROR);
    
    public static final IssueCode DQL_NON_SUPPORTED_OPR_FOR_SIZE_FUNCTION =
            new IssueCode("DQL_NON_SUPPORTED_OPR_FOR_SIZE_FUNCTION",
                    "The size function does not support '{opr}'. : {expr}",
                    Type.ERROR);
    
    public static final IssueCode DQL_MULTI_VALUED_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION =
            new IssueCode("DQL_MULTI_VALUED_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION",
                    "The size function does not accept multi valued data field '{datafield}' as value.",
                    Type.ERROR);
    
    public static final IssueCode DQL_NON_INTEGER_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION =
    		new IssueCode("DQL_NON_INTEGER_DATA_FIELD_VALUE_NOT_SUPPORTED_FOR_SIZE_FUNCTION",
    				"Data field {datafield} used as value for the size function must be an Integer (Number with zero decimal places).",
    				Type.ERROR);

    public static final IssueCode DQL_DATA_SIZE_FUNCTION =
            new IssueCode("DQL_SINGLE_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_SIZE_FUNCTION",
                    "The attribute '{name}' is specified as single valued attribute which does not support the size function.",
                    Type.ERROR);
    
    public static final IssueCode DQL_SIZE_FUNCTION_NEEDS_INT_VALUE =
    		new IssueCode("DQL_SIZE_FUNCTION_NEEDS_INT_VALUE",
            "The size function accepts only integer value : {expr}",
            Type.ERROR);
    
    public static final IssueCode DQL_MULTI_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_ORDER_BY =
            new IssueCode("DQL_MULTI_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_ORDER_BY",
                    "The attribute '{name}' is specified as multi valued attribute which does not support the order by clause.",
                    Type.ERROR);
    
    public static final IssueCode DQL_HIERARCHICAL_ATTRIBUTE_DOES_NOT_SUPPORT_ORDER_BY =
            new IssueCode("DQL_MULTI_VALUED_ATTRIBUTE_DOES_NOT_SUPPORT_ORDER_BY",
                    "Order by clause is not supported for the hierarchical attribute path '{name}'. ",
                    Type.ERROR);
        
    public static final IssueCode DQL_NO_SUPPORT_FOR_QUERY_PARAM =
    		new IssueCode("DQL_NO_SUPPORT_QUERY_PARAM",
    				"DQL query parameter replacement '{param}' is no longer supported. " +
    				"You can use the data field directly using ${data.field}. : {expr}",
    				Type.ERROR);

    public static final IssueCode DQL_NO_SUPPORT_FOR_TYPE_OF =
    		new IssueCode("DQL_NO_SUPPORT_FOR_TYPE_OF",
    				"DQL query no longer supports 'type of' operator.   : {dql}",
    				Type.ERROR);
    
    public static final IssueCode DQL_INVALID_DATA_FIELD =
    		new IssueCode("DQL_INVALID_DATA_FIELD",
    				"Data field specified is not valid.   : {datafield}",
    				Type.ERROR);

    public static final IssueCode DQL_UNKNOWN_DATA_FIELD =
    		new IssueCode("DQL_UNKNOWN_DATA_FIELD",
    				"Data field specified '{datafield}' is not valid.",
    				Type.ERROR);
    
    public static final IssueCode INCORRECT_SYNTAX_FOR_DATA_FIELD =
    		new IssueCode("INCORRECT_SYNTAX_FOR_DATA_FIELD",
    				"Syntax for the data field '{datafield}' is not valid in the expression : {expr}",
    				Type.ERROR);
    
    public static final IssueCode DQL_DATA_FIELD_NUMERIC_TYPE =
    		new IssueCode("DQL_DATA_FIELD_NUMERIC_TYPE",
    				"Data field '{datafield}' needs to be of numeric data type.",
    				Type.ERROR);
    
    public static final IssueCode DQL_DATA_FIELD_INDEX_NUMERIC_TYPE =
    		new IssueCode("DQL_DATA_FIELD_INDEX_NUMERIC_TYPE",
    				"Data field '{datafield}' used as array index must be an Integer (Number with zero decimal places) in expression : {path}",
    				Type.ERROR);

    public static final IssueCode DQL_DATA_FIELD_INDEX_SINGLE_VALUED =
    		new IssueCode("DQL_DATA_FIELD_INDEX_SINGLE_VALUED",
    				"Data field '{datafield}' used as array index must be single valued in expression : {path}",
    				Type.ERROR);
    
    public static final IssueCode DQL_DATA_FIELD_NOT_ARRAY_TYPE =
    		new IssueCode("DQL_DATA_FIELD_NOT_ARRAY_TYPE",
    				"Data field '{datafield}' used with an operator '{opr}' needs to be of an array type.",
    				Type.ERROR);
    
    public static final IssueCode DQL_DATA_FIELD_TYPE_MISMATCH =
    		new IssueCode("DQL_DATA_FIELD_TYPE_MISMATCH",
    				"The data type of the data field '{datafield}' does not match with the data type of the attribute '{attribute}'.",
    				Type.ERROR);
    
    public static final IssueCode DQL_DATA_FIELD_MULTIPLICITY_MISMATCH =
    		new IssueCode("DQL_DATA_FIELD_MULTIPLICITY_MISMATCH",
    				"The multiplicity of the data field '{datafield}' does not match with the multiplicity of the attribute '{attribute}'.",
    				Type.ERROR);
    public static final IssueCode DQL_BETWEEN_OPR_SUPPORTS_TWO_VALUES =
    		new IssueCode("DQL_BETWEEN_OPR_SUPPORTS_TWO_VALUES",
    				"The operator '{opr}' accpets only two values for its range. Invalid value: {attrValue}",
    				Type.ERROR);
    
	private IssueCode(String code, String message, Type type)
	{
		this.code = code;
		this.message = message;
		this.type = type;
	}

	public String getCode()
	{
		return code;
	}

	public String getMessage()
	{
		return message;
	}

	public Type getType()
	{
		return type;
	}

	@Override
    public String toString()
	{
		return code;
	}
}
