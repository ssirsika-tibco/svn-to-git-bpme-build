package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates a validation error relating to case data (as opposed to non-case data)
 * 
 * @author smorgan
 * @since 2019
 */
public class CasedataException extends ValidationException
{
	private static final long serialVersionUID = 1L;

	private CasedataException(ErrorData errorData)
	{
		super(errorData);
	}

	protected CasedataException(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	private CasedataException(ErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	public static CasedataException newUnknownStateValue(String value)
	{
		return new CasedataException(CDMErrorData.CDM_CASEDATA_UNKNOWN_STATE_VALUE, new String[]{"value", value}); //V
	}

	public static CasedataException newIdentifierNotSet(String name)
	{
		return new CasedataException(CDMErrorData.CDM_CASEDATA_IDENTIFIER_NOT_SET, new String[]{"name", name});
	}

	public static CasedataException newCIDChanged(String oldCID, String newCID)
	{
		return new CasedataException(CDMErrorData.CDM_CASEDATA_CID_CHANGED,
				new String[]{"oldCID", oldCID, "newCID", newCID});
	}

	public static CasedataException newCIDWhenAuto(String cid)
	{
		return new CasedataException(CDMErrorData.CDM_CASEDATA_CID_WHEN_AUTO, new String[]{"cid", cid});
	}
}
