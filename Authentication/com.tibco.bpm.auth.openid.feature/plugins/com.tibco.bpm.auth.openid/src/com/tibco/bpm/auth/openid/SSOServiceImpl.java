package com.tibco.bpm.auth.openid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.tibco.bpm.ace.admin.api.AdminConfigurationService;
import com.tibco.bpm.ace.admin.model.GroupId;
import com.tibco.bpm.ace.admin.model.HttpClient;
import com.tibco.bpm.ace.admin.model.OpenIdAuthentication;
import com.tibco.bpm.ace.admin.model.Property;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.auth.api.HttpClientSSOBinding;
import com.tibco.bpm.auth.openid.config.OpenIdResourceProvider;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

public class SSOServiceImpl implements HttpClientSSOBinding { 

	private static CLFClassContext logCtx = CloudLoggingFramework.init(SSOServiceImpl.class,
			AuthLoggingInfo.instance);
	
	private Map<String, OAuth2AccessToken> tokenMap = new ConcurrentHashMap<String, OAuth2AccessToken>();
	
	/**
	 * Static string constant for "openId-auth-header-prefix" property.
	 */
	private static final String OPEN_ID_AUTH_HEADER_PREFIX = "openId-auth-header-prefix";
	
	/**
	 * Static string constant for "openId-auth-header-prefix" default property value .
	 */
	private static final String OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT = "Bearer";

	private static final String AUTH_HEADER_NAME = "Authorization";
	
	
	@Override
	public void updateSSOBindingDetails(HttpClientBuilder builder, HttpClient client) throws ServiceException { 
		CLFMethodContext clf = logCtx.getMethodContext("updateSSOBindingDetails");
		clf.local.debug("Inside updateSSOBindingDetails");
		OpenIdAuthentication openIdProviderName = null;
		OAuth2AccessToken accessToken = null;
		Header header = null;
		List<Header>  httpHeaders = new ArrayList<Header>();
		//check whether the Auth type is OPENID or not, if yes, only then proceed
		if(HttpClient.AuthTypeEnum.OPENID.equals(client.getAuthType())) {
			openIdProviderName = client.getOpenIdProviderName();
			String httpSharedResourceName = client.getName();
			if(openIdProviderName == null) {
				throw new ServiceException("Could not find Open ID resource configured with " + httpSharedResourceName);
			}
			if(tokenMap.containsKey(httpSharedResourceName)) {
				clf.local.debug("Clearing the exisiting authentication information for http shared resource '%s'  : ", httpSharedResourceName);
				tokenMap.remove(httpSharedResourceName);
			}
			accessToken = generateNewAccessToken(clf, openIdProviderName, accessToken, httpSharedResourceName);
			
			String authHeaderValue = getOpenIdAuthHeaderPrefixProverty() + " " + accessToken.getValue();
			header = new BasicHeader(AUTH_HEADER_NAME, authHeaderValue);
			if(header != null) {
				httpHeaders.add(header);
				builder.setDefaultHeaders(httpHeaders);
			}
			
		}
	}

	/**
	 * @param clf
	 * @param openIdProviderName
	 * @param accessToken
	 * @param httpSharedResourceName
	 * @return OAuth2AccessToken
	 * @throws ServiceException
	 */
	private OAuth2AccessToken generateNewAccessToken(CLFMethodContext clf, OpenIdAuthentication openIdProviderName,
			OAuth2AccessToken accessToken, String httpSharedResourceName) throws ServiceException {
		clf.local.debug("Generating fresh access token for HTTP client shared resource '%s'  : ", httpSharedResourceName);
		try {
			OAuth2ProtectedResourceDetails clientCredsTokenResource = clientCredsTokenResource(openIdProviderName);
			AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest();
			OAuth2ClientContext auth2ClientContext = new DefaultOAuth2ClientContext(accessTokenRequest);
			OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientCredsTokenResource, auth2ClientContext);
			accessToken = restTemplate.getAccessToken();
			tokenMap.put(httpSharedResourceName, accessToken);
		}
		catch(Exception e) {
			clf.local.error(e, "Exception occurred in aquiring fresh access token");
			throw new ServiceException("Exception occurred in aquiring fresh access token : " + e.getCause());
		}
		return accessToken;
	}
	
	/**
	 * @param openIdResource
	 * @return OAuth2ProtectedResourceDetails
	 */
	private OAuth2ProtectedResourceDetails clientCredsTokenResource(OpenIdAuthentication openIdResource) {
		CLFMethodContext clf = logCtx.getMethodContext("clientCredsTokenResource");
		clf.local.debug("OpenID Bootstrap: Creating clientCredsTokenResource bean");
		ClientCredentialsResourceDetails clientCredTokenResource = new ClientCredentialsResourceDetails();
		clientCredTokenResource.setAccessTokenUri(openIdResource.getAccessTokenUri());
		clf.local.trace("OpenID Bootstrap: Access token URI : '%s'",
				OpenIdResourceProvider.INSTANCE.getAccessTokenUri());
		String clientId = openIdResource.getClientId();
		clientCredTokenResource.setClientId(clientId);
		clf.local.trace("OpenID Bootstrap: Client Id : '%s'", clientId);
		String clientSecret = openIdResource.getClientSecret();
		clientCredTokenResource.setClientSecret(clientSecret);
		clf.local.trace("OpenID Bootstrap: Client Secret : '%s'", clientSecret);
		clientCredTokenResource.setId("1");
		return clientCredTokenResource;
	}
	
	/**
	 * Get the 'openId-auth-header-prefix' property value. If 'value' is already set in the
	 * admin then get that value otherwise return the default value.
	 * 
	 * @return {@link String} property value
	 * @throws ServiceException
	 */
	private String getOpenIdAuthHeaderPrefixProverty() throws ServiceException {
		CLFMethodContext clf = logCtx.getMethodContext("getOpenIdAuthHeaderPrefixProverty");
		AdminConfigurationService service = OpenIdContextHelper.getINSTANCE().getAdminConfigService();
		Property property = service.getProperty(GroupId.auth, OPEN_ID_AUTH_HEADER_PREFIX);
		if (null == property) {
			clf.local.trace("Returning default openId-auth-header-prefix '%s'", OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT);
			return OPEN_ID_AUTH_HEADER_PREFIX_DEFAULT;
		} else {
			String value = property.getValue();
			clf.local.trace("Returning value of 'openId-auth-header-prefix' property as '%s'", value);
			return value;
		}
	}

	@Override
	public boolean isOpenIdTokenValid(String httpClientSharedResourceName) throws ServiceException {
		
		CLFMethodContext clf = logCtx.getMethodContext("isOpenIdTokenValid");
		clf.local.debug("Validating id token is valid for http shared resource : '%s'", httpClientSharedResourceName);
		
		boolean isTokenValid = true;
	
		if(httpClientSharedResourceName != null && tokenMap.containsKey(httpClientSharedResourceName)) {
			OAuth2AccessToken accessToken = tokenMap.get(httpClientSharedResourceName);
			Date expiration = accessToken.getExpiration();
			Date now = new Date();
			if (!expiration.before(now)) {
				clf.local.debug("Existing token is valid");
			}
			else {
				clf.local.debug("Existing token has expired");
				isTokenValid = false;
			}
		}
		else {
			clf.local.debug("No HTTP shared resource with name '%s' esists in cache, returning false", httpClientSharedResourceName);
			isTokenValid = false;
		}
		return isTokenValid;
	}

}
