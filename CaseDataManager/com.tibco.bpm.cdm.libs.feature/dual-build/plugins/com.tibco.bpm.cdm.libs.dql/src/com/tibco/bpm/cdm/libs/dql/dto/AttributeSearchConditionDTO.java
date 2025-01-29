package com.tibco.bpm.cdm.libs.dql.dto;

import com.tibco.bpm.cdm.libs.dql.model.ModelAttribute;

/**
 * Represents a condition based on an attribute value
 * @author smorgan
 * @since 2019
 */
public class AttributeSearchConditionDTO extends SearchConditionDTO
{
	private ModelAttribute			attribute;

	private SearchOperatorDTO	operator;

	private String				value;

    private ConditionOperator successorConditionOperator;

    private int preceedingParentheses = 0;

    private int succeedingParentheses = 0;
    
    private boolean functionUpperPresent = false;
    
    private boolean functionLowerPresent = false;
    
    private boolean functionSizePresent = false;

    public AttributeSearchConditionDTO(ModelAttribute attribute,
            SearchOperatorDTO operator, String value,
            ConditionOperator conditionOpr)
	{
		this.attribute = attribute;
		this.operator = operator;
		this.value = value;
        this.successorConditionOperator = conditionOpr;
	}

	public ModelAttribute getAttribute()
	{
		return attribute;
	}

	public String getValue()
	{
		return value;
	}

    public ConditionOperator getSuccessorConditionOperator() {
        return successorConditionOperator;
    }

    public int getPreceedingParentheses() {
        return preceedingParentheses;
    }

    public void setPreceedingParentheses(int preceedingParentheses) {
        this.preceedingParentheses = preceedingParentheses;
    }

    public int getSucceedingParentheses() {
        return succeedingParentheses;
    }

    public void setSucceedingParentheses(int succeedingParentheses) {
        this.succeedingParentheses = succeedingParentheses;
    }
    
    public SearchOperatorDTO getOperator() {
    	return this.operator;
    }

    public void setOperator(SearchOperatorDTO opr) {
    	this.operator = opr;
    }
    
    public boolean isFunctionUpperPresent() {
		return functionUpperPresent;
	}

	public void setFunctionUpperPresent(boolean functionUpperPresent) {
		this.functionUpperPresent = functionUpperPresent;
	}

	public boolean isFunctionLowerPresent() {
		return functionLowerPresent;
	}

	public void setFunctionLowerPresent(boolean functionLowerPresent) {
		this.functionLowerPresent = functionLowerPresent;
	}

	public boolean isFunctionSizePresent() {
		return functionSizePresent;
	}

	public void setFunctionSizePresent(boolean functionSizePresent) {
		this.functionSizePresent = functionSizePresent;
	}

	@Override
    public String toString()
	{
		return String.format("[%s][%s][%s]", attribute.getName(), operator, value);
	}
}
