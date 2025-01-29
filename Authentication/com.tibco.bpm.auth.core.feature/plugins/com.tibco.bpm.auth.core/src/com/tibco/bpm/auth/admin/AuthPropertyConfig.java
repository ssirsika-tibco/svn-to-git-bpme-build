
package com.tibco.bpm.auth.admin;

import java.util.concurrent.atomic.AtomicBoolean;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.auth.core.AuthServiceData;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Authentication property config.
 *
 * @author sajain
 * @since Apr 20, 2020
 */
public class AuthPropertyConfig {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(AuthPropertyConfig.class, AuthLoggingInfo.instance);
	/**
	 * Instance variable which can be used for accessing properties.
	 */
	public static AuthPropertyConfig INSTANCE;
	
	/**
	 * Static string constant for "basicAuthEnabled" property.
	 */
	public static final String IS_BASIC_AUTH_ENABLED = "basicAuthEnabled"; //$NON-NLS-1$

	/**
	 * Static string constant for "openIdAuthSharedResourceName" property.
	 * TODO : Remove if not used.
	 */
	@Deprecated
	public static final String OPEN_ID_AUTH_SHARED_RESOURCE_NAME = "openIdAuthSharedResourceName"; //$NON-NLS-1$

	/**
	 * Static string constant for "successRedirectionUrl" property.
	 */
	public static final String SUCCESS_REDIRECT_URL = "successRedirectionUrl"; //$NON-NLS-1$

	/**
	 * Boolean flag to check if Basic Auth is enabled or not.
	 */
	private boolean isBasicAuthEnabled = true;
	
	/**
	 * Boolean flag to check if Basic Auth is enabled or not.
	 */
	private static final boolean DEFAULT_BASICAUTH_ENABLED = true;

	/**
	 * Shared resource name to be used for OpenId authentication
	 * TODO : Remove if not used.
	 */
	@Deprecated
	private String openIdAuthSharedResourceName;

	/**
	 * Success redirect URL in case of authentication is successful
	 */
	private String successRedirectionUrl;
	
	
	/**
	 * Static string constant for "bpmSessionTimeout" property.
	 */
	public static final String BPM_SESSION_TIMEOUT = "bpmSessionTimeout"; //$NON-NLS-1$


	/**
	 * Static string constant for "openId-auth-header-prefix" property.
	 */
	private static final String OPEN_ID_AUTH_HEADER_PREFIX = "openId-auth-header-prefix";
	
	/**
	 * Static string constant for "openId-auth-header-prefix" default property value .
	 */
	private static final String OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT = "Bearer";
	
	/**
	 * Initialize the default value to 1800 seconds (i.e 30 mins)
	 */
	private int bpmSessionTimeout = 1800;
	
	/**
	 * Set the default value to 1800 seconds (i.e 30 mins)
	 */
	private int DEFAULT_SESSION_TIMEOUT=1800;
	
	
	private AtomicBoolean isDefaultSet= new AtomicBoolean(false);
	
	/**
	 * Boolean flag to check if we need to invalidate the current session on a fresh login.
	 */
	private boolean invalidateCurrentSessionOnLogin = false;
	
	/**
	 * Static string constant for "invalidateCurrentSessionOnLogin" property.
	 */
	public static final String INVALIDATE_CURRENT_SESSION_ON_LOGIN = "invalidateCurrentSessionOnLogin"; //$NON-NLS-1$
	
	
	
	
	public AuthPropertyConfig() {
		INSTANCE = this;
	}

	public void handlePropertyChange(String name, String value) {
		switch (name) {
		case IS_BASIC_AUTH_ENABLED:
			setBasicAuthEnabled(Boolean.parseBoolean(value));
			break;

		case OPEN_ID_AUTH_SHARED_RESOURCE_NAME:
			setOpenIdAuthSharedResourceName(value);
			break;

		case SUCCESS_REDIRECT_URL:
			setSuccessRedirectionUrl(value);
			break;
		
		case BPM_SESSION_TIMEOUT:
			setBpmSessionTimeout(value);
			break;
		case INVALIDATE_CURRENT_SESSION_ON_LOGIN:
			setInvalidateSessionOnLogin(Boolean.parseBoolean(value));
			break;

		default:
			break;
		}
	}

	

	private void setInvalidateSessionOnLogin(boolean value)  {
	     invalidateCurrentSessionOnLogin = value;
		
	}
	
	public boolean isInvalidateSessionOnLogin() throws ServiceException {
		
		CLFMethodContext clf = logCtx.getMethodContext("isInvalidateSessionOnLogin");
		Property value = getAdminService().getProperty(GroupId.auth, INVALIDATE_CURRENT_SESSION_ON_LOGIN);
		if (null == value) {
			clf.local.trace("Returning default value as false");
			return invalidateCurrentSessionOnLogin;
		} else {
			invalidateCurrentSessionOnLogin= Boolean.parseBoolean(value.getValue());
			return invalidateCurrentSessionOnLogin;
		}
		
	}

	/**
	 * @return isBasicAuthEnabled
	 * @throws ServiceException 
	 */
	public boolean isBasicAuthEnabled() throws ServiceException {
	    boolean isBasicAuthEn = false;
	    Property value = getAdminService().getProperty(GroupId.auth, IS_BASIC_AUTH_ENABLED);
        if (null == value) {
            isBasicAuthEn = DEFAULT_BASICAUTH_ENABLED;
        } else {
            isBasicAuthEn = Boolean.parseBoolean(value.getValue());
        }
		if(isBasicAuthEn == false) {
			/**
			 * DON'T TELL ANYONE : Following is the technique by which one can come out of
			 * the deadlock caused by wrong SSO (Single Sign-On) configuration and disabled
			 * basic authorization. While configuring the authentication, admin user may
			 * want to disable the basic authentication and only rely on SSO with
			 * OpenID/SAML. Basic authentication can be disabled by setting
			 * 'basicAuthEnabled' property to 'false'. In some weird situation, admin user
			 * might disable the basic authentication before properly configuring the SSO.
			 * This can create a deadlock situation where admin can not login using basic
			 * auth as well as SSO.
			 * 
			 * To come out of this deadlock, admin/support can login to karaf console and
			 * set the system property 'basicAuthEnabled' as follows :
			 *  1) Login to karaf console from command prompt: 
			 *  docker exec -it bpm-ace /opt/tibco/tibco-karaf-1.0.0-SNAPSHOT/bin/client 
			 *  2) Set the system property by typing following command on karaf console as:
			 *   system:property basicAuthEnabled true
			 * 
			 * There is no need to restart the docker container.
			 * 
			 * Following code will read the system property and enable the basic
			 * authentication.
			 * 
			 */
			String systemProperty = System.getProperty("basicAuthEnabled");
			boolean parsedValue = Boolean.parseBoolean(systemProperty);
			isBasicAuthEnabled = parsedValue ? parsedValue : isBasicAuthEnabled;
		}
		return isBasicAuthEnabled;
	}

	/**
	 * @param isBasicAuthEnabled
	 */
	public void setBasicAuthEnabled(boolean isBasicAuthEnabled) {
		this.isBasicAuthEnabled = isBasicAuthEnabled;
	}

	/**
	 * @return the openIdAuthSharedResourceName
	 * @throws ServiceException 
	 */
	public String getOpenIdAuthSharedResourceName() throws ServiceException {
		Property value = getAdminService().getProperty(GroupId.auth, OPEN_ID_AUTH_SHARED_RESOURCE_NAME);
        if (null == value) {
            return openIdAuthSharedResourceName;
        } else {
            return value.getValue();
        }
	}

	/**
	 * @param openIdAuthSharedResourceName the openIdAuthSharedResourceName to set
	 */
	public void setOpenIdAuthSharedResourceName(String openIdAuthSharedResourceName) {
		this.openIdAuthSharedResourceName = openIdAuthSharedResourceName;
	}

	/**
	 * @return the successRedirectionUrl
	 * @throws ServiceException 
	 */
	public String getSuccessRedirectionUrl() throws ServiceException {
		Property value = getAdminService().getProperty(GroupId.auth, SUCCESS_REDIRECT_URL);
        if (null == value) {
            return successRedirectionUrl;
        } else {
            return value.getValue();
        }
	}

	/**
	 * Setter for BpmSessionTimeout value
	 * 
	 * @param bpmSessionTimeout
	 */
	private void setBpmSessionTimeout(String bpmSessionTimeout) {
		CLFMethodContext clf = logCtx.getMethodContext("setBpmSessionTimeout");
		if (bpmSessionTimeout != null) {
			try {
				int parsedInt = Integer.parseInt(bpmSessionTimeout);
				if (parsedInt > 0) {
					if (parsedInt < 5) {
						parsedInt = 5;
						clf.local.debug("Setting bpmSessionTimeout less than 5 mins is not allowed, so making it as 5 mins");
					}
					this.bpmSessionTimeout = parsedInt * 60;
					clf.local.trace("Capturing the session timeout value in the Property change listener");
				}
			} catch (NumberFormatException e) {
				clf.local.debug(e, "Trying to set non-integer as a session timeout value");
			}
		}
	}

	/**
	 * Get the session timeout property value. If 'value' is already set in the
	 * admin then get that value otherwise return the default value.
	 * 
	 * @return
	 * @throws ServiceException
	 */
	public int getBpmSessionTimeout() throws ServiceException {
		CLFMethodContext clf = logCtx.getMethodContext("getBpmSessionTimeout");
		Property value = getAdminService().getProperty(GroupId.auth, BPM_SESSION_TIMEOUT);
		if (null == value) {
			clf.local.trace("Returning default/already set session timeout '%d'", bpmSessionTimeout);
			return DEFAULT_SESSION_TIMEOUT;
		} else {
			try {
				int parsedInt = Integer.parseInt(value.getValue());
				if (parsedInt > 0) {
					if (parsedInt < 5) {
						parsedInt = 5;
						clf.local.debug("bpmSessionTimeout less than 5 mins is not allowed, so returning 5 mins");
					}
					return parsedInt * 60;
				} else {
					return bpmSessionTimeout;
				}
			} catch (NumberFormatException e) {
				clf.local.debug(e,
						"Non-integer value is set in the session timeout property, so returning the older/default value.");
				return bpmSessionTimeout;
			}
		}
	}

	/**
	 * Get the 'openId-auth-header-prefix' property value. If 'value' is already set in the
	 * admin then get that value otherwise return the default value.
	 * 
	 * @return {@link String} property value
	 * @throws ServiceException
	 */
	public String getOpenIdAuthHeaderPrefixProverty() throws ServiceException {
		CLFMethodContext clf = logCtx.getMethodContext("getOpenIdAuthHeaderPrefixProverty");
		Property property = getAdminService().getProperty(GroupId.auth, OPEN_ID_AUTH_HEADER_PREFIX);
		if (null == property) {
			clf.local.trace("Returning default openId-auth-header-prefix '%s'", OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT);
			return OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT;
		} else {
			String value = property.getValue();
			clf.local.trace("Returning value of 'openId-auth-header-prefix' property as '%s'", value);
			return value;
		}
	}
	
	/**
	 * @param successRedirectionUrl the successRedirectionUrl to set
	 */
	public void setSuccessRedirectionUrl(String successRedirectionUrl) {
		this.successRedirectionUrl = successRedirectionUrl;
	}
	
	/**
     * @return the adminService
     */
    public AdminConfigurationService getAdminService() {
        return AuthServiceData.INSTANCE.getAdminConfigurationService();
    }

	public int getDefaultSessionTimeout() {
		return DEFAULT_SESSION_TIMEOUT;
	}

	public void setDefaultSessionTimeout(int dEFAULT_SESSION_TIMEOUT) {
		if(isDefaultSet.compareAndSet(false, true)) {
			DEFAULT_SESSION_TIMEOUT = dEFAULT_SESSION_TIMEOUT;
		}
		
	}

}
