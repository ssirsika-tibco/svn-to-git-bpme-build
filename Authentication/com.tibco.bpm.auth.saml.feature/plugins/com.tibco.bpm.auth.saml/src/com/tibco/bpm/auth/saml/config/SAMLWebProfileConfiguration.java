/**
 * Copyright (c) TIBCO Software Inc 2004 - 2020. All rights reserved.
 */

package com.tibco.bpm.auth.saml.config;

import java.util.List;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.KeyStoreProvider;
import com.tibco.bpm.ace.admin.model.SamlWebProfileAuthentication;
import com.tibco.bpm.ace.admin.model.SamlWebProfileAuthentication.IdpMetadataSourceEnum;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Class reads the {@link AdminConfigurationService} and store the information to be used in 
 * {@link DynamicWebSecurityConfig}.
 * @author ssirsika 
 */
public class SAMLWebProfileConfiguration {

	private static CLFClassContext logCtx = CloudLoggingFramework.init(SAMLWebProfileConfiguration.class,
			AuthLoggingInfo.instance);

	public final static SAMLWebProfileConfiguration INSTANCE = new SAMLWebProfileConfiguration();
	/**
	 * Required
	 */
	private String entityId = "com:ace:spring:sp";
	private String idpHttpMetadataURL;
	private String idpStringMetadata;
	private IdpMetadataSourceEnum idpMetadataSource;
	private String idpLoginURL = "/login";
	private String idpSSOURL = "/SSO";
	private Integer responseSkewTimeInSec;

	/**
	 * Optional fields
	 */

	private Integer maxAuthenticationAge;

	private Boolean signMetadata = false;

	private Boolean signAuthenticationRequest = false;
	private Boolean signAssertions = false;

	private String keyAliasEncryption;

	private Boolean encryptAssertion = false;

	// for load balancer
	private Boolean useLoadBalancer = false;
	private String entityBaseURL;
	private String lbScheme;
	private String lbServerName;
	private Integer lbServerPort;
	private Boolean lbIncludeServerPortInRequestURL;
	private String lbContextPath;

	private KeyStoreProvider keyStoreProvider;
	private String keyAliasToEncrypt;
	private String keyAliasToEncryptPassword;
	private String keyAliasToSign;
	private String keyAliasToSignPassword;
	private String defaultKeyAlias;
	private String defaultKeyAliasPassword;

	SAMLWebProfileConfiguration() {
	}

	/**
	 * Initialize the configuration from resource template and return the
	 * <code>true</code> if initialization is done otherwise return false.
	 * 
	 * @return
	 * @return
	 */
	public boolean init(AdminConfigurationService adminConfigService) {
		CLFMethodContext clf = logCtx.getMethodContext("init");
		clf.local.debug("Initializing from Resource Instance");
		try {
			List<String> samlAuthRINames = adminConfigService.getSamlWebProfileAuthenticationNames();
			String newRiName = samlAuthRINames.isEmpty() ? null : samlAuthRINames.get(0);
			// if new RI is available then only set the configuration.
			if (newRiName != null) {
				SamlWebProfileAuthentication samlAuthRI = adminConfigService
						.getSamlWebProfileAuthenticationByName(newRiName);
				setEntityId(samlAuthRI.getEntityId());
				setIdpSSOURL(samlAuthRI.getIdpSsoUrl());
				setIdpLoginURL(samlAuthRI.getIdpLoginUrl());
				setResponseSkewTimeInSec(samlAuthRI.getResponseSkewTime());
				setMaxAuthenticationAge(samlAuthRI.getMaxAuthenticationAge());
				setIdpMetadataSource(samlAuthRI.getIdpMetadataSource());
				setIdpHttpMetadataURL(samlAuthRI.getIdpMetadataUrl());
				setIdpStringMetadata(samlAuthRI.getIdpStringMetadata());
				setEntityBaseURL(samlAuthRI.getEntityBaseUrl());
				setUseLoadBalancer(samlAuthRI.getUseLoadBalancer());
				setSignAuthenticationRequest(samlAuthRI.getSignAuthenticationRequest());
				setSignAssertions(samlAuthRI.getSignAssertions());
				setSignMetadata(samlAuthRI.getSignMetadata());
				setKeyAliasEncryption(samlAuthRI.getKeyAliasToEncrypt());
				setEncryptAssertion(samlAuthRI.getEncryptAssertion());
				if (getUseLoadBalancer()) {
					setLbScheme(samlAuthRI.getScheme());
					setLbServerName(samlAuthRI.getServerName());
					setLbServerPort(samlAuthRI.getServerPort());
					setLbContextPath(samlAuthRI.getContextPath());
					setLbIncludeServerPortInRequestURL(samlAuthRI.getIncludeServerPortInRequestUrl());
				}
				setKeyStoreProvider(samlAuthRI.getKeyStoreProvider());
				setKeyAliasToEncrypt(samlAuthRI.getKeyAliasToEncrypt());
				setKeyAliasToEncryptPassword(samlAuthRI.getKeyAliasToEncryptPassword());
				setKeyAliasToSign(samlAuthRI.getKeyAliasToSign());
				setKeyAliasToSignPassword(samlAuthRI.getKeyAliasToSignPassword());
				setDefaultKeyAlias(samlAuthRI.getDefaultKeyAlias());
				setDefaultKeyAliasPassword(samlAuthRI.getDefaultKeyAliasPassword());
				clf.local.trace("Configuration details : %s", toString());
				return true;
			}
		} catch (Exception e) {
			clf.local.error(e, "Error while reading the Resource Instance details from admin");
			return false;
		}
		clf.local.debug("Could not found valid resource instance for SAML");
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public Integer getResponseSkewTimeInSec() {
		return responseSkewTimeInSec;
	}

	/**
	 * 
	 * @param responseSkewTimeInSec
	 */
	public void setResponseSkewTimeInSec(Integer responseSkewTimeInSec) {
		this.responseSkewTimeInSec = responseSkewTimeInSec;
	}

	/**
	 * 
	 * @return
	 */
	public String getKeyAliasEncryption() {
		return keyAliasEncryption;
	}

	/**
	 * 
	 * @param keyAliasEncryption
	 */
	public void setKeyAliasEncryption(String keyAliasEncryption) {
		this.keyAliasEncryption = keyAliasEncryption;
	}

	/**
	 * 
	 * @return
	 */
	public String getIdpLoginURL() {
		return idpLoginURL;
	}

	/**
	 * 
	 * @return
	 */
	public String getIdpSSOURL() {
		return idpSSOURL;
	}

	/**
	 * 
	 * @return
	 */
	public String getIdpHttpMetadataURL() {
		return idpHttpMetadataURL;
	}

	/**
	 * 
	 * @return
	 */
	public Boolean getSignMetadata() {
		return signMetadata;
	}

	/**
	 * 
	 * @return
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * 
	 * @param idpLoginURL
	 */
	public void setIdpLoginURL(String idpLoginURL) {
		if (idpLoginURL == null)
			return;
		this.idpLoginURL = idpLoginURL;
	}

	/**
	 * 
	 * @param idpSSOURL
	 */
	public void setIdpSSOURL(String idpSSOURL) {
		if (idpSSOURL == null)
			return;
		this.idpSSOURL = idpSSOURL;
	}

	/**
	 * 
	 * @param signMetadata
	 */
	public void setSignMetadata(Boolean signMetadata) {
		if (signMetadata == null)
			return;
		this.signMetadata = signMetadata;
	}

	/**
	 * 
	 * @param idpHttpMetadataURL
	 */
	public void setIdpHttpMetadataURL(String idpHttpMetadataURL) {
		this.idpHttpMetadataURL = idpHttpMetadataURL;
	}

	/**
	 * @return the idpStringMetadata
	 */
	public String getIdpStringMetadata() {
		return idpStringMetadata;
	}

	/**
	 * @param idpStringMetadata the idpStringMetadata to set
	 */
	public void setIdpStringMetadata(String idpStringMetadata) {
		this.idpStringMetadata = idpStringMetadata;
	}

	/**
	 * 
	 * @param entityId
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	/**
	 * 
	 * @return
	 */
	public Boolean getEncryptAssertion() {
		return encryptAssertion;
	}

	/**
	 * 
	 * @param encryptAssertion
	 */
	public void setEncryptAssertion(Boolean encryptAssertion) {
		this.encryptAssertion = encryptAssertion;
	}

	/**
	 * @return the maxAuthenticationAge
	 */
	public Integer getMaxAuthenticationAge() {
		return maxAuthenticationAge;
	}

	/**
	 * @param maxAuthenticationAge the maxAuthenticationAge to set
	 */
	public void setMaxAuthenticationAge(Integer maxAuthenticationAge) {
		this.maxAuthenticationAge = maxAuthenticationAge;
	}

	/**
	 * @return the useLoadBalancer
	 */
	public Boolean getUseLoadBalancer() {
		return useLoadBalancer;
	}

	/**
	 * @param useLoadBalancer the useLoadBalancer to set
	 */
	public void setUseLoadBalancer(Boolean useLoadBalancer) {
		this.useLoadBalancer = useLoadBalancer;
	}

	/**
	 * @return the entityBaseURL
	 */
	public String getEntityBaseURL() {
		return entityBaseURL;
	}

	/**
	 * @param entityBaseURL the entityBaseURL to set
	 */
	public void setEntityBaseURL(String entityBaseURL) {
		this.entityBaseURL = entityBaseURL;
	}

	/**
	 * @return the lbScheme
	 */
	public String getLbScheme() {
		return lbScheme;
	}

	/**
	 * @param lbScheme the lbScheme to set
	 */
	public void setLbScheme(String lbScheme) {
		this.lbScheme = lbScheme;
	}

	/**
	 * @return the lbServerName
	 */
	public String getLbServerName() {
		return lbServerName;
	}

	/**
	 * @param lbServerName the lbServerName to set
	 */
	public void setLbServerName(String lbServerName) {
		this.lbServerName = lbServerName;
	}

	/**
	 * @return the lbServerPort
	 */
	public Integer getLbServerPort() {
		return lbServerPort;
	}

	/**
	 * @param lbServerPort the lbServerPort to set
	 */
	public void setLbServerPort(Integer lbServerPort) {
		this.lbServerPort = lbServerPort;
	}

	/**
	 * @return the lbIncludeServerPortInRequestURL
	 */
	public Boolean getLbIncludeServerPortInRequestURL() {
		return lbIncludeServerPortInRequestURL;
	}

	/**
	 * @param lbIncludeServerPortInRequestURL the lbIncludeServerPortInRequestURL to
	 *                                        set
	 */
	public void setLbIncludeServerPortInRequestURL(Boolean lbIncludeServerPortInRequestURL) {
		this.lbIncludeServerPortInRequestURL = lbIncludeServerPortInRequestURL;
	}

	/**
	 * @return the lbContextPath
	 */
	public String getLbContextPath() {
		return lbContextPath;
	}

	/**
	 * @param lbContextPath the lbContextPath to set
	 */
	public void setLbContextPath(String lbContextPath) {
		this.lbContextPath = lbContextPath;
	}

	/**
	 * @return the idpMetadataSource
	 */
	public IdpMetadataSourceEnum getIdpMetadataSource() {
		return idpMetadataSource;
	}

	/**
	 * @param idpMetadataSource the idpMetadataSource to set
	 */
	public void setIdpMetadataSource(IdpMetadataSourceEnum idpMetadataSource) {
		this.idpMetadataSource = idpMetadataSource;
	}

	/**
	 * @return the signAuthenticationRequest
	 */
	public Boolean getSignAuthenticationRequest() {
		return signAuthenticationRequest;
	}

	/**
	 * @param signAuthenticationRequest the signAuthenticationRequest to set
	 */
	public void setSignAuthenticationRequest(Boolean signAuthenticationRequest) {
		this.signAuthenticationRequest = signAuthenticationRequest;
	}

	/**
	 * @return the signAssertions
	 */
	public Boolean getSignAssertions() {
		return signAssertions;
	}

	/**
	 * @param signAssertions the signAssertions to set
	 */
	public void setSignAssertions(Boolean signAssertions) {
		this.signAssertions = signAssertions;
	}

	/**
	 * @return the keyStoreProvider
	 */
	public KeyStoreProvider getKeyStoreProvider() {
		return keyStoreProvider;
	}

	/**
	 * @param keyStoreProvider the keyStoreProvider to set
	 */
	public void setKeyStoreProvider(KeyStoreProvider keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
	}
	
	/**
	 * @return the keyAliasToSignPassword
	 */
	public String getKeyAliasToSignPassword() {
		return keyAliasToSignPassword;
	}

	/**
	 * @param keyAliasToSignPassword the keyAliasToSignPassword to set
	 */
	public void setKeyAliasToSignPassword(String keyAliasToSignPassword) {
		this.keyAliasToSignPassword = keyAliasToSignPassword;
	}

	/**
	 * @return the keyAliasToSign
	 */
	public String getKeyAliasToSign() {
		return keyAliasToSign;
	}

	/**
	 * @param keyAliasToSign the keyAliasToSign to set
	 */
	public void setKeyAliasToSign(String keyAliasToSign) {
		this.keyAliasToSign = keyAliasToSign;
	}

	/**
	 * @return the keyAliasToEncrypt
	 */
	public String getKeyAliasToEncrypt() {
		return keyAliasToEncrypt;
	}

	/**
	 * @param keyAliasToEncrypt the keyAliasToEncrypt to set
	 */
	public void setKeyAliasToEncrypt(String keyAliasToEncrypt) {
		this.keyAliasToEncrypt = keyAliasToEncrypt;
	}

	/**
	 * @return the keyAliasToEncryptPassword
	 */
	public String getKeyAliasToEncryptPassword() {
		return keyAliasToEncryptPassword;
	}

	/**
	 * @param keyAliasToEncryptPassword the keyAliasToEncryptPassword to set
	 */
	public void setKeyAliasToEncryptPassword(String keyAliasToEncryptPassword) {
		this.keyAliasToEncryptPassword = keyAliasToEncryptPassword;
	}
	
	/**
	 * @return the defaultKeyAliasPassword
	 */
	public String getDefaultKeyAliasPassword() {
		return defaultKeyAliasPassword;
	}

	/**
	 * @param defaultKeyAliasPassword the defaultKeyAliasPassword to set
	 */
	public void setDefaultKeyAliasPassword(String defaultKeyAliasPassword) {
		this.defaultKeyAliasPassword = defaultKeyAliasPassword;
	}

	/**
	 * @return the defaultKeyAlias
	 */
	public String getDefaultKeyAlias() {
		return defaultKeyAlias;
	}

	/**
	 * @param defaultKeyAlias the defaultKeyAlias to set
	 */
	public void setDefaultKeyAlias(String defaultKeyAlias) {
		this.defaultKeyAlias = defaultKeyAlias;
	}

	@Override
	public String toString() {
		return "SAMLWebProfileConfiguration [entityId=" + entityId + ", idpHttpMetadataURL=" + idpHttpMetadataURL
				+ ", idpStringMetadata=" + idpStringMetadata + ", idpMetadataSource=" + idpMetadataSource
				+ ", idpLoginURL=" + idpLoginURL + ", idpSSOURL=" + idpSSOURL + ", responseSkewTimeInSec="
				+ responseSkewTimeInSec + ", maxAuthenticationAge=" + maxAuthenticationAge + ", signMetadata="
				+ signMetadata + ", signAuthenticationRequest=" + signAuthenticationRequest + ", signAssertions="
				+ signAssertions + ", keyAliasEncryption=" + keyAliasEncryption + ", encryptAssertion="
				+ encryptAssertion + ", useLoadBalancer=" + useLoadBalancer + ", entityBaseURL=" + entityBaseURL
				+ ", lbScheme=" + lbScheme + ", lbServerName=" + lbServerName + ", lbServerPort=" + lbServerPort
				+ ", lbIncludeServerPortInRequestURL=" + lbIncludeServerPortInRequestURL + ", lbContextPath="
				+ lbContextPath + ", keyStoreProvider=" + keyStoreProvider + ", keyAliasToEncrypt=" + keyAliasToEncrypt
				+ ", keyAliasToEncryptPassword=" + keyAliasToEncryptPassword + ", keyAliasToSign=" + keyAliasToSign
				+ ", keyAliasToSignPassword=" + keyAliasToSignPassword + ", defaultKeyAlias=" + defaultKeyAlias
				+ ", defaultKeyAliasPassword=" + defaultKeyAliasPassword + "]";
	}
}
