package com.tibco.bpm.cdm.api.exception;

/**
 * Indicates a problem during deployment of an application
 * @author smorgan
 * @since 2019
 */
public class DeploymentException extends CDMException
{
	private static final long serialVersionUID = -8836439777821013792L;

	private DeploymentException(CDMErrorData errorData)
	{
		super(errorData);
	}

	private DeploymentException(CDMErrorData errorData, Throwable cause, String[] params)
	{
		super(errorData, cause, params);
	}

	private DeploymentException(CDMErrorData errorData, String[] params)
	{
		super(errorData, params);
	}

	private DeploymentException(CDMErrorData errorData, Throwable cause)
	{
		super(errorData, cause);
	}

	public static DeploymentException newBadRASC(String message, Throwable cause)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_BAD_RASC, cause,
				new String[]{"message", message});
		return result;
	}

	public static DeploymentException newDataModelDeserializationFailed(Throwable cause)
	{
		DeploymentException result = new DeploymentException(
				CDMErrorData.CDM_DEPLOYMENT_DATAMODEL_DESERIALIZATION_FAILED, cause);
		return result;
	}

	public static DeploymentException newInvalidDataModel(String reportMessage)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_INVALID_DATAMODEL);
		// Assume the report is sensitive (as it refers to names within the model)
		result.addMetadata("reportMessage", reportMessage, true);
		return result;
	}

	public static DeploymentException newInvalidDataModelUpgrade(String reportMessage)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_INVALID_DATAMODEL_UPGRADE);
		// Assume the report is sensitive (as it refers to names within the model)
		result.addMetadata("reportMessage", reportMessage, true);
		return result;
	}

	public static DeploymentException newUnknownArtifactType(String type)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_UNKNOWN_ARTIFACT_TYPE,
				new String[]{"type", type});
		return result;
	}

	public static DeploymentException newInvalidVersionDependency(String versionRangeExpression)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_INVALID_VERSION_DEPENDENCY,
				new String[]{"versionRangeExpression", versionRangeExpression});
		return result;
	}

	public static DeploymentException newUnresolvableDependency(String applicationId, String versionRangeExpression,
			String found)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_UNRESOLVABLE_DEPENDENCY,
				new String[]{"applicationId", applicationId, "versionRangeExpression", versionRangeExpression, "found",
						found});
		return result;
	}

	public static DeploymentException newDependenciesPreventUndeployment(String dependencies)
	{
		DeploymentException result = new DeploymentException(
				CDMErrorData.CDM_DEPLOYMENT_DEPENDENCIES_PREVENT_UNDEPLOYMENT,
				new String[]{"dependencies", dependencies});
		return result;
	}

	public static DeploymentException newDuplicateNamespace(String namespace, String majorVersion)
	{
		DeploymentException result = new DeploymentException(CDMErrorData.CDM_DEPLOYMENT_DUPLICATE_NAMESPACE,
				new String[]{"namespace", namespace, "majorVersion", majorVersion});
		return result;
	}

}
