/**
 * Copyright (c) TIBCO Software Inc 2004 - 2020. All rights reserved.
 */

package com.tibco.bpm.auth.saml.config;

import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.SAMLWebSSOHoKProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.storage.EmptyStorageFactory;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.AbstractProfileBase;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.tibco.bpm.ace.admin.model.KeyStoreProvider;
import com.tibco.bpm.ace.admin.model.SamlWebProfileAuthentication.IdpMetadataSourceEnum;
import com.tibco.bpm.auth.saml.SAMLSecurityBundleActivator;
import com.tibco.bpm.auth.saml.handler.BPMSAMLAuthenticationFailureHandler;
import com.tibco.bpm.auth.saml.handler.BPMSamlAuthenticationSuccessHandler;
import com.tibco.bpm.auth.saml.logging.AuthLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;

/**
 * Configuration related to SAML security.
 * 
 * @author ssirsika
 */
@Configuration
@EnableWebSecurity(debug = false)
public class DynamicWebSecurityConfig extends WebSecurityConfigurerAdapter {

	private Timer backgroundTaskTimer = new Timer(true);
	private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
	private SAMLUserDetailsServiceImpl samlUserDetailsServiceImpl = new SAMLUserDetailsServiceImpl();
	private static CLFClassContext logCtx = CloudLoggingFramework.init(DynamicWebSecurityConfig.class,
			AuthLoggingInfo.instance);

	public DynamicWebSecurityConfig() {
		CLFMethodContext clf = logCtx.getMethodContext("DynamicWebSecurityConfig");
		clf.local.debug("SAML Bootstrap: Registering DynamicWebSecurityConfig for SAML");
		Security.setProperty("keystore.type", "jks"); // This is required because underline library by default takes
														// .crt files.
	}

	public void shutdown() {
		this.backgroundTaskTimer.purge();
		this.backgroundTaskTimer.cancel();
		this.multiThreadedHttpConnectionManager.shutdown();
	}

	/**
	 * Initialization of the velocity engine to create a XML Parser to read the
	 * SAML.
	 * 
	 * @return {@link VelocityEngine}
	 */
	@Bean
	public VelocityEngine velocityEngine() {
		CLFMethodContext clf = logCtx.getMethodContext("velocityEngine");
		clf.local.debug("SAML Bootstrap: Creating velocityEngine bean");
		return VelocityFactory.getEngine();
	}

	/**
	 * XML parser pool needed for OpenSAML parsing
	 * 
	 * @return {@link StaticBasicParserPool}
	 */
	@Bean(initMethod = "initialize")
	public StaticBasicParserPool parserPool() {
		CLFMethodContext clf = logCtx.getMethodContext("parserPool");
		clf.local.debug("SAML Bootstrap: Creating parserPool bean");
		return new StaticBasicParserPool();
	}

	@Bean(name = "parserPoolHolder")
	public ParserPoolHolder parserPoolHolder() {
		CLFMethodContext clf = logCtx.getMethodContext("parserPoolHolder");
		clf.local.debug("SAML Bootstrap: Creating parserPoolHolder bean");
		return new ParserPoolHolder();
	}

	/**
	 * Bindings, encoders and decoders used for creating and parsing messages
	 * 
	 * @return {@link HttpClient}
	 */
	@Bean
	public HttpClient httpClient() {
		CLFMethodContext clf = logCtx.getMethodContext("httpClient");
		clf.local.debug("SAML Bootstrap: Creating httpClient bean");
		return new HttpClient(this.multiThreadedHttpConnectionManager);
	}

	/**
	 * SAML Authentication Provider responsible for validating of received SAML
	 * messages. In case SAML token is valid then authentication provider creates an
	 * authenticated UsernamePasswordAuthenticationToken
	 * 
	 * @return {@link SAMLAuthenticationProvider}
	 */
	@Bean
	public SAMLAuthenticationProvider samlAuthenticationProvider() {
		CLFMethodContext clf = logCtx.getMethodContext("samlAuthenticationProvider");
		SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
		samlAuthenticationProvider.setUserDetails(samlUserDetailsServiceImpl);
		samlAuthenticationProvider.setForcePrincipalAsString(false);
		clf.local.debug("SAML Bootstrap: Creating samlAuthenticationProvider bean");
		return samlAuthenticationProvider;
	}

	/**
	 * SAMLContextProvider is responsible for parsing HttpRequest/Response and
	 * determining which local entity (IDP/SP) is responsible for its handling.
	 * 
	 * @return {@link SAMLContextProvider}
	 */
	@Bean
	public SAMLContextProvider contextProvider() {
		CLFMethodContext clf = logCtx.getMethodContext("contextProvider");
		if (SAMLWebProfileConfiguration.INSTANCE.getUseLoadBalancer()) {
			clf.local.info("SAML Bootstrap: Using load balancer in SAML Context Provider");
			SAMLContextProviderLB samlContextProviderLB = new SAMLContextProviderLB();
			String lbScheme = SAMLWebProfileConfiguration.INSTANCE.getLbScheme();
			samlContextProviderLB.setScheme(lbScheme);
			clf.local.trace("SAML Bootstrap: Load Balancer scheme : %s", lbScheme);
			String lbServerName = SAMLWebProfileConfiguration.INSTANCE.getLbServerName();
			samlContextProviderLB.setServerName(lbServerName);
			clf.local.trace("SAML Bootstrap: Load Balancer server name : %s", lbServerName);
			Integer lbServerPort = SAMLWebProfileConfiguration.INSTANCE.getLbServerPort();
			if (lbServerPort != null) {
				samlContextProviderLB.setServerPort(lbServerPort.intValue());
			}
			String lbContextPath = SAMLWebProfileConfiguration.INSTANCE.getLbContextPath();
			samlContextProviderLB.setContextPath(lbContextPath);
			clf.local.trace("SAML Bootstrap: Load Balancer context path : %s", lbContextPath);
			Boolean lbIncludeServerPortInRequestURL = SAMLWebProfileConfiguration.INSTANCE
					.getLbIncludeServerPortInRequestURL();
			samlContextProviderLB.setIncludeServerPortInRequestURL(lbIncludeServerPortInRequestURL);
			if (lbIncludeServerPortInRequestURL) {
				clf.local.trace("SAML Bootstrap: Load Balancer server port : %d", lbServerPort.intValue());
			} else {
				clf.local.trace("SAML Bootstrap: Load Balancer server port is not included");
			}
			samlContextProviderLB.setStorageFactory(new EmptyStorageFactory());
			return samlContextProviderLB;
		} else {
			return new SAMLContextProviderImpl();
		}
	}

	/**
	 * Initialization of OpenSAML library
	 * 
	 * @return {@link SAMLBootstrap}
	 */
	@Bean
	public static SAMLBootstrap samlBootstrap() {
		CLFMethodContext clf = logCtx.getMethodContext("samlBootstrap");
		clf.local.debug("SAML Bootstrap: Creating SAMLBootstrap bean");
		return new SAMLBootstrap();
	}

	/**
	 * Logger for SAML messages and events
	 * 
	 * @return {@link SAMLDefaultLogger}
	 */
	@Bean
	public SAMLDefaultLogger samlLogger() {
		CLFMethodContext clf = logCtx.getMethodContext("samlLogger");
		clf.local.debug("SAML Bootstrap: Creating SAMLDefaultLogger bean");
		return new SAMLDefaultLogger();
	}

	/**
	 * SAML 2.0 WebSSO Assertion Consumer
	 * 
	 * @return {@link WebSSOProfileConsumer}
	 */
	@Bean
	public WebSSOProfileConsumer webSSOprofileConsumer() {
		WebSSOProfileConsumerImpl webSSOProfileConsumerImpl = new WebSSOProfileConsumerImpl();
		setResponceSkewTimeToProfile(webSSOProfileConsumerImpl);
		Integer maxAuthenticationAge = SAMLWebProfileConfiguration.INSTANCE.getMaxAuthenticationAge();
		if (maxAuthenticationAge != null) {
			webSSOProfileConsumerImpl.setMaxAuthenticationAge(maxAuthenticationAge.intValue());
		}
		CLFMethodContext clf = logCtx.getMethodContext("webSSOprofileConsumer");
		clf.local.debug("SAML Bootstrap: Creating WebSSOProfileConsumer bean with maximum authentication age as '%d'",
				webSSOProfileConsumerImpl.getMaxAuthenticationAge());
		return webSSOProfileConsumerImpl;
	}

	// SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
	@Bean
	@Deprecated
	public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	// SAML 2.0 Web SSO profile
	@Bean
	public WebSSOProfile webSSOprofile() {
		WebSSOProfileImpl webSSOProfileImpl = new WebSSOProfileImpl();
		setResponceSkewTimeToProfile(webSSOProfileImpl);
		return webSSOProfileImpl;
	}

	// SAML 2.0 Holder-of-Key Web SSO profile
	@Bean
	@Deprecated
	public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	// SAML 2.0 ECP profile
	@Bean
	public WebSSOProfileECPImpl ecpprofile() {
		return new WebSSOProfileECPImpl();
	}

	@Bean
	public SingleLogoutProfile logoutprofile() {
		SingleLogoutProfileImpl singleLogoutProfileImpl = new SingleLogoutProfileImpl();
		setResponceSkewTimeToProfile(singleLogoutProfileImpl);
		return singleLogoutProfileImpl;
	}

	/**
	 * Central storage of cryptographic keys
	 * 
	 * @return {@link KeyManager}
	 */
	@Bean
	public KeyManager keyManager() {
		CLFMethodContext clf = logCtx.getMethodContext("keyManager");
		clf.local.debug("SAML Bootstrap: Creating keyManager bean");

		SAMLWebProfileConfiguration ri = SAMLWebProfileConfiguration.INSTANCE;
		KeyStoreProvider keyStoreProvider = ri.getKeyStoreProvider();
		if (keyStoreProvider != null) {
			clf.local.trace("Reading KeyStore Provider : '%s'", keyStoreProvider.getName());
			Resource storeFile = new ByteArrayResource(Base64.getDecoder().decode(keyStoreProvider.getKeyStore()));
			Map<String, String> passwords = new HashMap<String, String>();
			if (isValidString(ri.getKeyAliasEncryption())) {
				passwords.put(ri.getKeyAliasEncryption(), ri.getKeyAliasToEncryptPassword());
				clf.local.trace("Added password for Encryption key : '%s'", ri.getKeyAliasEncryption());
			}

			if (isValidString(ri.getKeyAliasToSign())) {
				passwords.put(ri.getKeyAliasToSign(), ri.getKeyAliasToSignPassword());
				clf.local.trace("Added password for signing key : '%s'", ri.getKeyAliasToSign());
			}

			if (isValidString(ri.getDefaultKeyAlias())) {
				passwords.put(ri.getDefaultKeyAlias(), ri.getDefaultKeyAliasPassword());
				clf.local.trace("Added password for default key : '%s'", ri.getDefaultKeyAlias());
			}

			return new JKSKeyManager(storeFile, keyStoreProvider.getPassword(), passwords, ri.getDefaultKeyAlias());
		}
		return new EmptyKeyManager();
	}



	// Setup TLS Socket Factory
	@Bean
	public TLSProtocolConfigurer tlsProtocolConfigurer() {
		CLFMethodContext clf = logCtx.getMethodContext("tlsProtocolConfigurer");
		clf.local.debug("SAML Bootstrap: Creating TLSProtocolConfigurer bean");
		return new TLSProtocolConfigurer();
	}

	@Bean
	public WebSSOProfileOptions defaultWebSSOProfileOptions() {
		CLFMethodContext clf = logCtx.getMethodContext("defaultWebSSOProfileOptions");
		clf.local.debug("SAML Bootstrap: Creating WebSSOProfileOptions bean");
		WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
		webSSOProfileOptions.setIncludeScoping(false);
		return webSSOProfileOptions;
	}

	/**
	 * Entry point to initialize authentication.
	 * 
	 * @return {@link SAMLEntryPoint}
	 */
	@Bean
	public SAMLEntryPoint samlEntryPoint() {
		SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
		samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
		samlEntryPoint.setFilterProcessesUrl(SAMLWebProfileConfiguration.INSTANCE.getIdpLoginURL());
		CLFMethodContext clf = logCtx.getMethodContext("samlEntryPoint");
		clf.local.debug("SAML Bootstrap: Creating SAMLEntryPoint bean with entry point url '%s'",
				samlEntryPoint.getFilterProcessesUrl());
		return samlEntryPoint;
	}

	/**
	 * Setup advanced info about metadata
	 * 
	 * @return ExtendedMetadata
	 */
	@Bean
	public ExtendedMetadata extendedMetadata() {
		CLFMethodContext clf = logCtx.getMethodContext("extendedMetadata");
		ExtendedMetadata extendedMetadata = new ExtendedMetadata();
		extendedMetadata.setIdpDiscoveryEnabled(false);

		// Check for signing requirement
		Boolean signMetadata = SAMLWebProfileConfiguration.INSTANCE.getSignMetadata();
		extendedMetadata.setSignMetadata(signMetadata);
		clf.local.debug("SAML Bootstrap: Creating ExtendedMetadata bean");
		if (signMetadata) {
			clf.local.debug("SAML Bootstrap: Metadata signing enabled.");
		}

		extendedMetadata.setEcpEnabled(true);

		// Check for encryption requirement
		if (SAMLWebProfileConfiguration.INSTANCE.getEncryptAssertion()) {
			String keyAliasEncryption = SAMLWebProfileConfiguration.INSTANCE.getKeyAliasEncryption();
			if (keyAliasEncryption != null && !"".equals(keyAliasEncryption.trim())) {
				extendedMetadata.setEncryptionKey(keyAliasEncryption);
				clf.local.debug("SAML Bootstrap: Assertion encryption enabled with key = '%s'", keyAliasEncryption);
			}
		}
		return extendedMetadata;
	}

	@Bean
	public ExtendedMetadataDelegate extendedMetadataProvider() throws MetadataProviderException {
		CLFMethodContext clf = logCtx.getMethodContext("extendedMetadataProvider");
		clf.local.debug("SAML Bootstrap: Creating ExtendedMetadataDelegate bean");
		AbstractReloadingMetadataProvider resourseMetadataProvider = null;
		if (SAMLWebProfileConfiguration.INSTANCE.getIdpMetadataSource() == IdpMetadataSourceEnum.IDP_STRING_META_DATA) {
			String idpStringMetadata = SAMLWebProfileConfiguration.INSTANCE.getIdpStringMetadata();
			if (idpStringMetadata != null && !idpStringMetadata.trim().equals("")) {
				idpStringMetadata = StringEscapeUtils.unescapeXml(idpStringMetadata);
				StringMetadataResourceWrapper stringMetadataProviderResource = new StringMetadataResourceWrapper(
						idpStringMetadata);
				resourseMetadataProvider = new ResourceBackedMetadataProvider(this.backgroundTaskTimer,
						stringMetadataProviderResource);
				clf.local.debug("SAML Bootstrap: Using metadata string");
			}
		} else if (SAMLWebProfileConfiguration.INSTANCE
				.getIdpMetadataSource() == IdpMetadataSourceEnum.IDP_HTTP_META_DATA_URL) {
			String idpHttpMetadataURL = SAMLWebProfileConfiguration.INSTANCE.getIdpHttpMetadataURL();
			resourseMetadataProvider = new HTTPMetadataProvider(this.backgroundTaskTimer, httpClient(),
					idpHttpMetadataURL);
			clf.local.debug("SAML Bootstrap: Using metadata url '%s'", idpHttpMetadataURL);
		}

		if (resourseMetadataProvider == null) {
			clf.local.error("IDP metadata is not provided.");
			throw new MetadataProviderException("IDP metadata is not provided.");
		}

		resourseMetadataProvider.setParserPool(parserPool());
		ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(resourseMetadataProvider,
				extendedMetadata());
		extendedMetadataDelegate.setMetadataTrustCheck(false);
		extendedMetadataDelegate.setMetadataRequireSignature(false);
		backgroundTaskTimer.purge();
		return extendedMetadataDelegate;
	}

	@Bean
	@Qualifier("metadata")
	public CachingMetadataManager metadata() throws MetadataProviderException {
		CLFMethodContext clf = logCtx.getMethodContext("extendedMetadataProvider");
		clf.local.debug("SAML Bootstrap: Creating CachingMetadataManager bean");
		List<MetadataProvider> providers = new ArrayList<MetadataProvider>();
		providers.add(extendedMetadataProvider());
		return new CachingMetadataManager(providers);
	}

	/**
	 * Filter automatically generates default SP metadata
	 * 
	 * @return {@link MetadataGenerator}
	 */
	@Bean
	public MetadataGenerator metadataGenerator() {
		CLFMethodContext clf = logCtx.getMethodContext("metadataGenerator");
		clf.local.debug("SAML Bootstrap: Creating MetadataGenerator bean");
		MetadataGenerator metadataGenerator = new MetadataGenerator();
		String entityId = SAMLWebProfileConfiguration.INSTANCE.getEntityId();
		metadataGenerator.setEntityId(entityId);
		clf.local.trace("SAML Bootstrap: entityId = '%s'", entityId);
		metadataGenerator.setExtendedMetadata(extendedMetadata());
		metadataGenerator.setIncludeDiscoveryExtension(false);
		metadataGenerator.setKeyManager(keyManager());

		// Check for signing requirements.
		Boolean signAuthenticationRequest = SAMLWebProfileConfiguration.INSTANCE.getSignAuthenticationRequest();
		metadataGenerator.setRequestSigned(signAuthenticationRequest);
		clf.local.trace("SAML Bootstrap: signed request = '%b'", signAuthenticationRequest);
		Boolean signAssertions = SAMLWebProfileConfiguration.INSTANCE.getSignAssertions();
		metadataGenerator.setWantAssertionSigned(signAssertions);
		clf.local.trace("SAML Bootstrap: sign assertions = '%b'", signAssertions);

		if (SAMLWebProfileConfiguration.INSTANCE.getUseLoadBalancer()) {
//        	metadataGenerator.setEntityBaseURL("https://localhost:8443");
			String entityBaseURL = SAMLWebProfileConfiguration.INSTANCE.getEntityBaseURL();
			metadataGenerator.setEntityBaseURL(entityBaseURL);
			clf.local.trace("SAML Bootstrap: entity base URL = '%s'", entityBaseURL);
		}
		return metadataGenerator;
	}

	// The filter is waiting for connections on URL suffixed with filterSuffix
	// and presents SP metadata there
	@Bean
	@Deprecated
	public MetadataDisplayFilter metadataDisplayFilter() {
		return new MetadataDisplayFilter();
	}

	/**
	 * Handler deciding where to redirect user after successful login
	 * 
	 * @return {@link AuthenticationSuccessHandler}
	 */
	@Bean
	public AuthenticationSuccessHandler successRedirectHandler() {
		CLFMethodContext clf = logCtx.getMethodContext("successRedirectHandler");
		clf.local.debug("SAML Bootstrap: Creating AuthenticationSuccessHandler bean");
		BPMSamlAuthenticationSuccessHandler successHandler = new BPMSamlAuthenticationSuccessHandler();
		return successHandler;
	}

	/**
	 * Handler deciding where to redirect user after failed login
	 * 
	 * @return {@link SimpleUrlAuthenticationFailureHandler}
	 */
	@Bean
	public AuthenticationFailureHandler authenticationFailureHandler() {
		CLFMethodContext clf = logCtx.getMethodContext("authenticationFailureHandler");
		clf.local.debug("SAML Bootstrap: Creating SimpleUrlAuthenticationFailureHandler bean");
		return new BPMSAMLAuthenticationFailureHandler();
	}

	@Bean
	public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("samlWebSSOHoKProcessingFilter");
		clf.local.debug("SAML Bootstrap: Creating SAMLWebSSOHoKProcessingFilter bean");
		SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
		samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
		samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
		samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
		return samlWebSSOHoKProcessingFilter;
	}

	// Processing filter for WebSSO profile messages
	@Bean
	public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("samlWebSSOProcessingFilter");
		clf.local.debug("SAML Bootstrap: Creating SAMLProcessingFilter bean");
		SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
		samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
		samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
		samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
		String idpSSOURL = SAMLWebProfileConfiguration.INSTANCE.getIdpSSOURL();
		samlWebSSOProcessingFilter.setFilterProcessesUrl(idpSSOURL);
		clf.local.trace("SAML Bootstrap: Filter Processes Url = '%s'", idpSSOURL);
		return samlWebSSOProcessingFilter;
	}

	@Bean
	public MetadataGeneratorFilter metadataGeneratorFilter() {
		CLFMethodContext clf = logCtx.getMethodContext("metadataGeneratorFilter");
		clf.local.debug("SAML Bootstrap: Creating MetadataGeneratorFilter bean");
		return new MetadataGeneratorFilter(metadataGenerator());
	}

	/**
	 * Logout handler terminating local session
	 * 
	 * @return SecurityContextLogoutHandler
	 */
	@Bean
	public SecurityContextLogoutHandler logoutHandler() {
		CLFMethodContext clf = logCtx.getMethodContext("logoutHandler");
		clf.local.debug("SAML Bootstrap: Creating SecurityContextLogoutHandler bean");
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.setInvalidateHttpSession(true);
		logoutHandler.setClearAuthentication(true);
		return logoutHandler;
	}

	// Bindings
	private ArtifactResolutionProfile artifactResolutionProfile() {
		CLFMethodContext clf = logCtx.getMethodContext("artifactResolutionProfile");
		clf.local.debug("SAML Bootstrap: Creating ArtifactResolutionProfile bean");
		final ArtifactResolutionProfileImpl artifactResolutionProfile = new ArtifactResolutionProfileImpl(httpClient());
		artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
		return artifactResolutionProfile;
	}

	@Bean
	public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) {
		CLFMethodContext clf = logCtx.getMethodContext("artifactBinding");
		clf.local.debug("SAML Bootstrap: Creating HTTPArtifactBinding bean");
		return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
	}

	@Bean
	public HTTPSOAP11Binding soapBinding() {
		CLFMethodContext clf = logCtx.getMethodContext("soapBinding");
		clf.local.debug("SAML Bootstrap: Creating HTTPSOAP11Binding bean");
		return new HTTPSOAP11Binding(parserPool());
	}

	@Bean
	public HTTPPostBinding httpPostBinding() {
		CLFMethodContext clf = logCtx.getMethodContext("httpPostBinding");
		clf.local.debug("SAML Bootstrap: Creating HTTPPostBinding bean");
		return new HTTPPostBinding(parserPool(), velocityEngine());
	}

	@Bean
	public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
		CLFMethodContext clf = logCtx.getMethodContext("httpRedirectDeflateBinding");
		clf.local.debug("SAML Bootstrap: Creating HTTPRedirectDeflateBinding bean");
		return new HTTPRedirectDeflateBinding(parserPool());
	}

	@Bean
	public HTTPSOAP11Binding httpSOAP11Binding() {
		CLFMethodContext clf = logCtx.getMethodContext("httpSOAP11Binding");
		clf.local.debug("SAML Bootstrap: Creating HTTPSOAP11Binding bean");
		return new HTTPSOAP11Binding(parserPool());
	}

	@Bean
	public HTTPPAOS11Binding httpPAOS11Binding() {
		CLFMethodContext clf = logCtx.getMethodContext("httpPAOS11Binding");
		clf.local.debug("SAML Bootstrap: Creating HTTPPAOS11Binding bean");
		return new HTTPPAOS11Binding(parserPool());
	}

	// Processor
	@Bean
	public SAMLProcessorImpl processor() {
		CLFMethodContext clf = logCtx.getMethodContext("processor");
		clf.local.debug("SAML Bootstrap: Creating SAMLProcessorImpl bean");
		Collection<SAMLBinding> bindings = new ArrayList<SAMLBinding>();
		bindings.add(httpRedirectDeflateBinding());
		bindings.add(httpPostBinding());
		bindings.add(artifactBinding(parserPool(), velocityEngine()));
		bindings.add(httpSOAP11Binding());
		bindings.add(httpPAOS11Binding());
		return new SAMLProcessorImpl(bindings);
	}

	/**
	 * Define the security filter chain in order to support SSO Auth by using SAML
	 * 2.0
	 * 
	 * @return Filter chain proxy
	 * @throws Exception
	 */
	@Bean
	public FilterChainProxy samlFilter() throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("samlFilter");
		clf.local.debug("SAML Bootstrap: Creating FilterChainProxy bean");
		List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>();
		String idpLoginURL = SAMLWebProfileConfiguration.INSTANCE.getIdpLoginURL();
		chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher(idpLoginURL), samlEntryPoint()));
		clf.local.trace("SAML Bootstrap: Entry Point URL = '%s'", idpLoginURL);

		// Disabled following filter for not exposing the metadata
		/*
		 * chains.add(new DefaultSecurityFilterChain(new
		 * AntPathRequestMatcher("/saml/metadata/**"), metadataDisplayFilter()));
		 */

		String idpSSOURL = SAMLWebProfileConfiguration.INSTANCE.getIdpSSOURL();
		chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher(idpSSOURL), samlWebSSOProcessingFilter()));
		clf.local.trace("SAML Bootstrap: Web SSO Processing URL = '%s'", idpSSOURL);
		return new FilterChainProxy(chains);
	}

	/*
	 * @Bean public FilterChainProxy samlFilter() throws Exception {
	 * List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>();
	 * chains.add(new DefaultSecurityFilterChain(new
	 * AntPathRequestMatcher("/saml/login/**"), samlEntryPoint())); chains.add(new
	 * DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"),
	 * samlLogoutFilter())); chains.add(new DefaultSecurityFilterChain(new
	 * AntPathRequestMatcher("/saml/metadata/**"), metadataDisplayFilter()));
	 * chains.add(new DefaultSecurityFilterChain(new
	 * AntPathRequestMatcher("/saml/SSO/**"), samlWebSSOProcessingFilter()));
	 * chains.add(new DefaultSecurityFilterChain(new
	 * AntPathRequestMatcher("/saml/SSOHoK/**"), samlWebSSOHoKProcessingFilter()));
	 * chains.add(new DefaultSecurityFilterChain(new
	 * AntPathRequestMatcher("/saml/SingleLogout/**"),
	 * samlLogoutProcessingFilter())); // chains.add(new
	 * DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"),
	 * samlIDPDiscovery())); return new FilterChainProxy(chains); }
	 */

	/**
	 * Returns the authentication manager currently used by Spring. It represents a
	 * bean definition with the aim allow wiring from other classes performing the
	 * Inversion of Control (IoC).
	 * 
	 * @throws Exception
	 */
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("authenticationManagerBean");
		clf.local.debug("SAML Bootstrap: Creating AuthenticationManager bean");
		return super.authenticationManagerBean();
	}

	/**
	 * Defines the web based security configuration.
	 * 
	 * @param http It allows configuring web based security for specific http
	 *             requests.
	 * @throws Exception
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("configure");
		clf.local.debug("SAML Bootstrap: starting security configuration");
		SAMLSecurityBundleActivator.setApplicationContext(getApplicationContext());
		http.httpBasic().authenticationEntryPoint(samlEntryPoint());
		http.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
				.addFilterAfter(samlFilter(), BasicAuthenticationFilter.class)
				.addFilterBefore(samlFilter(), CsrfFilter.class);
		http.csrf().disable();
		http.authorizeRequests().antMatchers("/apps/**").authenticated();
		http.logout().disable(); // The logout procedure is already handled by SAML filters.
		clf.local.info("SAML Bootstrap: Configured SAML Security.");
	}

	/**
	 * Sets a custom authentication provider.
	 * 
	 * @param auth SecurityBuilder used to create an AuthenticationManager.
	 * @throws Exception
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("configure(AuthenticationManagerBuilder)");
		clf.local.debug("SAML Bootstrap: configuring authentication provider");
		auth.authenticationProvider(samlAuthenticationProvider());
	}

	public void destroy() throws Exception {
		CLFMethodContext clf = logCtx.getMethodContext("destroy");
		clf.local.debug("SAML : Relinquishing  resources");
		shutdown();
	}

	/**
	 * @param profile
	 */
	private void setResponceSkewTimeToProfile(AbstractProfileBase profile) {
		Integer responseSkewTimeInSec = SAMLWebProfileConfiguration.INSTANCE.getResponseSkewTimeInSec();
		if (responseSkewTimeInSec != null) {
			CLFMethodContext clf = logCtx.getMethodContext("setResponceSkewTimeToProfile");
			clf.local.debug("SAML Bootstrap: setting skew time = '%d'", responseSkewTimeInSec.intValue());
			profile.setResponseSkew(responseSkewTimeInSec.intValue());
		}
	}
	
	/**
	 * Returns <code>true</code> if string is not null and not empty,
	 * otherwise return <code>false</code>.
	 * @param str {@link String} to be tested.
	 * @return
	 */
	private boolean isValidString(String str) {
		return (str != null) && !("".equals(str.trim()));
	}
}