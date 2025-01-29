package com.tibco.bpm.cdm.core.queue;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConstants;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Base class for JobPerformers that deal with parts of the auto-purge functionality.
 * 
 * @author smorgan
 * @since 2019
 */
public class AbstractAutoPurgeJobPerformer implements AutoPurgeConstants
{
	static CLFClassContext				logCtx	= CloudLoggingFramework.init(AbstractAutoPurgeJobPerformer.class,
			CDMLoggingInfo.instance);

	protected AdminConfigurationService	adminConfigurationService;

	// Called by Spring
	public void setAdminConfigurationService(AdminConfigurationService adminConfigurationService)
	{
		this.adminConfigurationService = adminConfigurationService;
	}

	private Integer getIntegerPropertyFromConfigService(String propertyName, int minAllowedValue, int maxAllowedValue)
	{
		Integer integerValue = null;
		CLFMethodContext clf = logCtx.getMethodContext("getIntegerPropertyFromConfigService");
		if (adminConfigurationService == null)
		{
			clf.local.error("adminConfigurationService is not set.");
		}
		try
		{
			Property property = adminConfigurationService.getProperty(GroupId.cdm, propertyName);
			if (property != null)
			{
				String stringValue = property.getValue();
				try
				{
					integerValue = Integer.parseInt(stringValue);
					if (integerValue < minAllowedValue || integerValue > maxAllowedValue)
					{
						clf.local.error(String.format("Config property %s must have a value between %d and %d, not %d",
								propertyName, minAllowedValue, maxAllowedValue, integerValue));
						integerValue = null;
					}
				}
				catch (NumberFormatException e)
				{
					clf.local.error(
							String.format("Config property %s has a non-integer value: %s", propertyName, stringValue));
				}
			}
		}
		catch (ServiceException e)
		{
			clf.local.error("Failed to get property from config service.");
		}
		return integerValue;
	}

	protected Integer getIntervalInMinutes()
	{
		return getIntegerPropertyFromConfigService(PROPERTY_INTERVAL, MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES);
	}

	protected Integer getPurgeTimeInMinutes()
	{
		return getIntegerPropertyFromConfigService(PROPERTY_PURGE_TIME, MIN_PURGE_TIME_MINUTES, MAX_PURGE_TIME_MINUTES);
	}
}
