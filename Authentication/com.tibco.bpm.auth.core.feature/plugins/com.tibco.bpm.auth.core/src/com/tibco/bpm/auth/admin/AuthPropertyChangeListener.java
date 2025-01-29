package com.tibco.bpm.auth.admin;

import com.tibco.bpm.ace.admin.api.PropertyChangeService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.model.PropertyChange;

public class AuthPropertyChangeListener implements PropertyChangeService {

	protected AuthPropertyConfig configHandler;

	@Override
	public void processPropertyChange(PropertyChange propertyChange) {

		Property property = propertyChange.getProperty();

		if ((property != null) && (property.getGroupId().compareTo(GroupId.auth) == 0)) {
			getConfigHandler().handlePropertyChange(property.getName(), property.getValue());
		}
	}

	public AuthPropertyConfig getConfigHandler() {
		return configHandler;
	}

	public void setConfigHandler(AuthPropertyConfig configHandler) {
		this.configHandler = configHandler;
	}

}
