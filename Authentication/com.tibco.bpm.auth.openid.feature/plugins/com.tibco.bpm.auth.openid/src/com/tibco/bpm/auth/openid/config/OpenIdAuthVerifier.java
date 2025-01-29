/**
 * Copyright (c) TIBCO Software Inc 2004, 2020. All rights reserved.
 */
package com.tibco.bpm.auth.openid.config;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;

import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibco.bpm.auth.api.BPMAuthVerifier;
import com.tibco.bpm.auth.openid.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Implement method to verify authentication information like JWT token
 * 
 * @author ssirsika
 *
 */
public class OpenIdAuthVerifier implements BPMAuthVerifier {

	public static OpenIdAuthVerifier INSTANCE = new OpenIdAuthVerifier();

	private CLFClassContext logCtx = CloudLoggingFramework.init(OpenIdAuthVerifier.class, AuthLoggingInfo.instance);

	/**
	 * Return {@link Map} of the claim after verifying JWT token.
	 * 
	 * @param jwtToken Open ID token. OpenID Connect always issues ID token along
	 *                with access token to provide compatibility with OAuthand match
	 *                the general tendency for authorizing identity 
	 * @param offline  pass <code>true</code> in verification should not consult the
	 *                 IDP's public key otherwise pass as <code>false</code> which
	 *                 will just verify token offline without contacting IDP
	 * @return {@link Map} of claims if verified otherwise 'null'
	 * @throws Exception
	 */

	public Map<String, Object> getVerifiedClaims(String jwtToken, boolean offline) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("getVerifiedClaims");
		clf.local.debug("Verifying claims with offline mode = %s and jwtToken = %s", offline, jwtToken);

		Jwt tokenDecoded = getDecodedToken(jwtToken, offline);
		if (tokenDecoded != null) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);
			return verifyClaims(authInfo) ? authInfo : null;
		}
		return null;
	}

	/**
	 * Return the user key value from the claims after verifying the passed ID token.
	 *  @param jwtToken Open ID token. OpenID Connect always issues ID token along
	 *                with access token to provide compatibility with OAuthand match
	 *                the general tendency for authorizing identity.
	 * 
	 * @param offline pass <code>true</code> in verification should not consult the
	 *                IDP's public key otherwise pass as <code>false</code> which
	 *                will just verify token offline without contacting IDP
	 * @return value {@link String} value of user key from claims.
	 *                
	 */
	public String getVerifiedUserKeyValue(String jwtToken, boolean offline) throws Exception {
		Map<String, Object> claims = getVerifiedClaims(jwtToken, offline);
		if(claims != null) {
			final OpenIdConnectUserDetails user = new OpenIdConnectUserDetails(claims, null,
					OpenIdResourceProvider.INSTANCE.getUserKey());
			return user.getUsername();
		}
		return null;
	}
	/**
	 * Verify the pass JWT token.
	 * 
	 * @param jwtToken JWT token
	 * @param offline  pass <code>true</code> in verification should not consult the
	 *                 IDP's public key otherwise pass as <code>false</code> which
	 *                 will just verify token offline without contacting IDP
	 * @return <code>true</code> if verification is successful otherwise
	 *         <code>false</code>
	 * @throws Exception
	 */
	@Override
	public boolean verify(String jwtToken, boolean offline) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("verify");
		clf.local.debug("Verifying claims for offline= %s and jwtToken = %s", offline, jwtToken);

		Jwt tokenDecoded = getDecodedToken(jwtToken, offline);
		if (tokenDecoded != null) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);
			return verifyClaims(authInfo);
		}
		return false;
	}

	/**
	 * Return the decoded JWT token. If 'offline' is passed as <code>false</code>
	 * then token will be verified using public key provided by the IDP. If
	 * 'offline' is passed as <code>true</code> then token will be only decoded.
	 * 
	 * @param jwtToken {@link String} JWT token
	 * @param offline  boolean
	 * @return {@link Jwt} token
	 * @throws Exception
	 */
	private Jwt getDecodedToken(String jwtToken, boolean offline) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("getDecodedToken");
		clf.local.debug("Decoding jwt token for offline= %s and jwtToken = %s", offline, jwtToken);

		if (jwtToken != null) {
			Jwt tokenDecoded;
			if (offline) {
				tokenDecoded = JwtHelper.decode(jwtToken);
			} else {
				String kid = JwtHelper.headers(jwtToken).get("kid");
				clf.local.trace("kid from the token = %s", kid);
				String algHeader = JwtHelper.headers(jwtToken).get("alg");
				clf.local.trace("alg from the token = %s", algHeader);
				CryptoAlgorithm algorithm = CryptoAlgorithm.fromHeaderParamValue(algHeader);
				tokenDecoded = JwtHelper.decodeAndVerify(jwtToken, verifier(kid, algorithm.standardName()));
			}
			return tokenDecoded;
		}
		return null;
	}

	/**
	 * Verify the claims data to check if it's valid or not. If claims are not valid
	 * then throw an exception.
	 * 
	 * @param claims
	 */
	private boolean verifyClaims(@SuppressWarnings("rawtypes") Map claims) {
		CLFMethodContext clf = logCtx.getMethodContext("verifyClaims");
		clf.local.debug("Verifying claims : %s", claims.toString());

		Integer exp = (Integer) claims.get("exp");
		Date expireDate = new Date(exp * 1000L);
		Date now = new Date();
		if (expireDate.before(now)) {
			clf.local.debug("Claims are expired");
			return false;
		}
		clf.local.debug("Claims are valid");
		return true;
	}

	/**
	 * Create the new RSA signature verifier for passed 'kid'. It verifies the
	 * signature using RSA public key. Public key will be accessed using URL defined
	 * in 'jwkUrl' which then will be passed to {@link RsaVerifier}.
	 * 
	 * @param kid is the unique identifier for the key
	 * @param algorithmName SHA algorithm standard name
	 * @return {@link RsaVerifier}
	 * @throws Exception
	 */
	private RsaVerifier verifier(String kid, String algorithmName) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("verifier");
		JwkProvider provider = new UrlJwkProvider(new URL(OpenIdResourceProvider.INSTANCE.getJwkUrl()));
		clf.local.debug("Getting RSA verifier for kid = '%s' and public key url = '%s'", kid, OpenIdResourceProvider.INSTANCE.getJwkUrl());
		Jwk jwk = provider.get(kid);
		clf.local.debug("Creating RsaVerifier for algorithm = '%s'", algorithmName);
		return new RsaVerifier((RSAPublicKey) jwk.getPublicKey(), algorithmName);
	}
	
	/**
	 * Private enum for defined Algorithm (&quot;alg&quot;) values present in id_token header.
	 */
	enum CryptoAlgorithm {
		RS256("SHA256withRSA", "RS256"),
		RS384("SHA384withRSA", "RS384"),
		RS512("SHA512withRSA", "RS512"),
		ES256("SHA256withECDSA", "ES256"),
		ES384("SHA384withECDSA", "ES384"),
		ES512("SHA512withECDSA", "ES512");

		private final String standardName;		// JCA Standard Name
		private final String headerParamValue;

		CryptoAlgorithm(String standardName, String headerParamValue) {
			this.standardName = standardName;
			this.headerParamValue = headerParamValue;
		}

		String standardName() {
			return this.standardName;
		}

		String headerParamValue() {
			return this.headerParamValue;
		}

		static CryptoAlgorithm fromHeaderParamValue(String headerParamValue) {
			CryptoAlgorithm result = null;
			for (CryptoAlgorithm algorithm : values()) {
				if (algorithm.headerParamValue().equals(headerParamValue)) {
					result = algorithm;
					break;
				}
			}
			return result != null ? result : RS256;
		}
	}
}
