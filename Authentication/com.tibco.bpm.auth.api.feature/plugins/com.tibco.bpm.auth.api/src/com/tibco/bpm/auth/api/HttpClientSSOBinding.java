package com.tibco.bpm.auth.api;

import org.apache.http.impl.client.HttpClientBuilder;

import com.tibco.bpm.ace.admin.model.HttpClient;

public interface HttpClientSSOBinding {
	
	public void updateSSOBindingDetails(HttpClientBuilder builder ,HttpClient client) throws Exception ;
	
	public boolean isOpenIdTokenValid(String httpClientSharedResourceName) throws Exception ;

}
