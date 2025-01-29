/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.cdm.core.rest;

import java.util.List;

import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.CDMErrorData;

/**
 * Indicates that arguments passed to a REST request were invalid or that the
 * context was not appropriately set.
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class RESTRequestException extends CDMException
{
	private static final long serialVersionUID = 1L;

	private RESTRequestException(CDMErrorData errorData)
	{
		super(errorData);
	}

	private RESTRequestException(CDMErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	public static RESTRequestException newBadFilterExpressions(List<String> expressions)
	{
		StringBuilder expressionsBuf = new StringBuilder();
		for (int i = 0, size = expressions.size(); i < size; i++)
		{
			if (i != 0)
			{
				expressionsBuf.append(", ");
			}
			expressionsBuf.append(expressions.get(i));
		}
		RESTRequestException e = new RESTRequestException(CDMErrorData.CDM_REST_BADFILTEREXPRESSIONS,
				new String[]{"expressions", expressionsBuf.toString()});
		return e;
	}

	public static RESTRequestException newApplicationIdInvalid(String value)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_APPLICATION_ID_TOO_LONG, new String[]{"value", value});
	}

	public static RESTRequestException newNamespaceInvalid(String value)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_NAMESPACE_TOO_LONG, new String[]{"value", value});
	}

	public static RESTRequestException newApplicationMajorVersionInvalid(String value)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_APPLICATION_MAJOR_VERSION_INVALID,
				new String[]{"value", value});
	}

	public static RESTRequestException newInvalidRequestProperty(String name, String value)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_INVALID_REQUEST_PROPERTY,
				new String[]{"name", name, "value", value});
	}

	public static RESTRequestException newBadCaseReferences(String caseReferences)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_BAD_CASE_REFERENCE_LIST,
				new String[]{"caseReferences", caseReferences});
	}

	public static RESTRequestException newBadTargetCaseReferences(String targetCaseReferences)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_BAD_TARGET_CASE_REFERENCE_LIST,
				new String[]{"targetCaseReferences", targetCaseReferences});
	}

	public static RESTRequestException newCaseReferencePreventsOthers()
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_CASE_REFERENCE_IN_PREVENTS_PARAMETERS);
	}

	public static RESTRequestException newBadDeleteLinksFilter(String filter)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_BAD_DELETE_LINKS_FILTER, new String[]{"filter", filter});
	}

	public static RESTRequestException newNotAuthorised()
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_NOT_AUTHORISED);
	}

	public static RESTRequestException newBadIsInTerminalState(String isInTerminalState)
	{
		return new RESTRequestException(CDMErrorData.CDM_REST_BAD_IS_IN_TERMINAL_STATE,
				new String[]{"isInTerminalState", isInTerminalState});
	}

    public static RESTRequestException newBadTagetCaseReferencesWithDuplicates(
            String refs) {
        return new RESTRequestException(
                CDMErrorData.CDM_REST_BAD_TARGET_CASE_REFERENCE_LIST_WITH_DUPLICATES,
                new String[] { "targetCaseReferences", refs });
    }

}
