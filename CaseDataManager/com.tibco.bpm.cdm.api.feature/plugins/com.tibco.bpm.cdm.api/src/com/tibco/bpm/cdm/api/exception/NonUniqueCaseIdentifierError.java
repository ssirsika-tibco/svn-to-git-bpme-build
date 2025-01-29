package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates that an attempt was made to create or update a case, such that its
 * case identifier attribute has the same value as that of another case of
 * the same type.
 * 20200514: Reinstating the class name NonUniqueCaseIdentifierError. See ACE-3559 for more details.
 * @author smorgan
 * @since 2019
 */
public class NonUniqueCaseIdentifierError extends CasedataException
{
	private static final long serialVersionUID = 7615040276193362279L;

	protected NonUniqueCaseIdentifierError(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	public static NonUniqueCaseIdentifierError newNonUniqueCID(String cid)
	{
		return new NonUniqueCaseIdentifierError(CDMErrorData.CDM_CASEDATA_NON_UNIQUE_CID, new String[]{"cid", cid});
	}

}
