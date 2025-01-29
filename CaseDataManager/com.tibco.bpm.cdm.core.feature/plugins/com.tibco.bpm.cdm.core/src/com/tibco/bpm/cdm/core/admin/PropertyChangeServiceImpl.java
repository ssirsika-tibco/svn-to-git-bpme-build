package com.tibco.bpm.cdm.core.admin;

import com.tibco.bpm.ace.admin.api.PropertyChangeService;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.model.PropertyChange;
import com.tibco.bpm.cdm.core.CaseSequenceCache;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConfigChangeHandler;
import com.tibco.bpm.cdm.core.autopurge.AutoPurgeConstants;

/**
 * Implementation of ACE Admin's PropertyChangeService interface.
 * This is called when any config properties in the 'cdm' group are changed.
 * 
 * @author smorgan
 * @since 2019
 */
public class PropertyChangeServiceImpl implements PropertyChangeService, AutoPurgeConstants
{
	private static final String PROPERTY_CACHE_SIZE = "caseIdsCacheSize";
	
	private AutoPurgeConfigChangeHandler autoPurgeConfigChangeHandler;
	
	private CaseSequenceCache					caseSequenceCache;
	
	public void setCaseSequenceCache(CaseSequenceCache caseSequenceCache) {
		this.caseSequenceCache = caseSequenceCache;
	}
	
	// Called by Spring
	public void setAutoPurgeConfigChangeHandler(AutoPurgeConfigChangeHandler autoPurgeConfigChangeHandler)
	{
		this.autoPurgeConfigChangeHandler = autoPurgeConfigChangeHandler;
	}
	
	@Override
	public void processPropertyChange(PropertyChange propertyChange)
	{
		Property property = propertyChange.getProperty();

		// The only property change we care about responding to immediately
		// is autopurge.interval.  At the time of writing, there is one other
		// property (autopurge.purgeTime) which we fetch at the time of use.

		if (property != null)
		{
			String name = property.getName();
			if (PROPERTY_INTERVAL.equals(name))
			{
				// If property was deleted, value will be null. There is no
				// need for us to explicitly check the change type.
				String value = property.getValue();
				autoPurgeConfigChangeHandler.handleIntervalChange(value);
			}
			
			if (PROPERTY_CACHE_SIZE.equals(name))
			{
				// If property was deleted, value will be null. There is no
				// need for us to explicitly check the change type.
				String value = property.getValue();
				caseSequenceCache.setCacheSize(Integer.parseInt(value));
			}
		}
	}
}
