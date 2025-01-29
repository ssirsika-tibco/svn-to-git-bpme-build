Put 'build.sh' to '/ace/branches/Authentication' folder. Script will create a 'Kar' file and replace it in the running container and restart the docker container.

Steps to enable the SAML authentication
1) com.tibco.bpm.auth.handler.SAMLAuthenticationHandler can be called from BPMSecurityServiceImpl. In SAMLAuthenticationHandler, one can set the 
	defaultTargetUrl and authentication listener. Authentication listener can be used as a callback to get notifications related to authentications.
	
2) As Google IDP only supports ACS URL hosted on Https, so we need to configure the Apache proxy to transfer request from https to http. Copy 'ace.conf' 
   to 'site-available' folder of Apache and enable the same.     