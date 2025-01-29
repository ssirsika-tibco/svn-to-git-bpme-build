package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates that a case mutation operation has failed due to the version number in
 * a case reference mismatching the actual version of the case (implying that a change
 * has occurred since the reference was constructed).
 * 20200514: Reinstating the class name CaseOutOfSyncError. See ACE-3559 for more details.
 * @author smorgan
 * @since 2019
 */
public class CaseOutOfSyncError extends ReferenceException
{
	private static final long serialVersionUID = 3575529332787905386L;

	protected CaseOutOfSyncError(ErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	public static CaseOutOfSyncError newVersionMismatch(int version, int actualVersion, String caseReference)
	{
		return new CaseOutOfSyncError(CDMErrorData.CDM_REFERENCE_VERSION_MISMATCH,
				new String[]{"version", Integer.toString(version), "actualVersion", Integer.toString(actualVersion),
						"caseReference", caseReference});
	}
}
