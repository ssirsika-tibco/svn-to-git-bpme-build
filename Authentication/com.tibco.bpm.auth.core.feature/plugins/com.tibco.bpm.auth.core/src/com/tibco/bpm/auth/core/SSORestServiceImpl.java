/**
 * 
 */
package com.tibco.bpm.auth.core;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.tibco.bpm.ace.admin.model.OpenIdAuthentication;
import com.tibco.bpm.ace.admin.model.SamlWebProfileAuthentication;
import com.tibco.bpm.ace.admin.service.exception.ServiceException;
import com.tibco.bpm.auth.exception.AuthBaseException;
import com.tibco.bpm.auth.exception.AuthMessages.ErrorCode;
import com.tibco.bpm.auth.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Common REST service which will redirect to specific SSO (SAML or Open ID)
 * endpoint, depending on what is configured.
 * 
 * @author ssirsika
 *
 */
public class SSORestServiceImpl implements SSORestService {

	@Context
	private HttpServletRequest httpServletRequest;

	static CLFClassContext logCtx = CloudLoggingFramework.init(SSORestServiceImpl.class, AuthLoggingInfo.instance);
	/**
	 * @return Redirect response depending upon the configured SSO resource instance
	 */
	public Response redirect() throws Exception {
		String redirectUrl = getRedirectUrl();
		if (redirectUrl != null) {
			URI uri = UriBuilder.fromPath(redirectUrl).build();
			return Response.temporaryRedirect(uri).build();
		} else {
			throw new AuthBaseException(ErrorCode.AUTH_SSO_UNAVAILABLE, null, null);
		}
	}

	/**
	 * Return the redirect URL from SSO Resource Template.
	 * 
	 * @param
	 * @return redirectUrl otherwise null in case of missing/invalid configuration
	 * @throws ServiceException 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private String getRedirectUrl() throws ServiceException, ServletException, IOException {
		OpenIdAuthentication openIdAuthRI = AuthServiceData.INSTANCE.getOpenIdAuthRI();
		CLFMethodContext clf = logCtx.getMethodContext("getRedirectUrl");
		if (openIdAuthRI != null) {
			String result = openIdAuthRI.getRedirectUri();
			clf.local.debug("OpenId : redirect URI : '%s'", result);
			return result;
		} else {
			SamlWebProfileAuthentication samlAuthRI = AuthServiceData.INSTANCE.getSAMLAuthRI();
			if(samlAuthRI != null) {
				URL url = new URL(httpServletRequest.getRequestURL().toString());
				String hostPath = String.format("%s://%s", url.getProtocol(), url.getAuthority());
				String result =  hostPath + samlAuthRI.getIdpLoginUrl();
				clf.local.debug("SAML : redirect URI : '%s'", result);
				return result;
			}
		}
		return null;
	}
}
