package com.tibco.bpm.auth.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import com.tibco.bpm.ace.admin.model.HttpClient;

import org.apache.http.impl.client.HttpClientBuilder;

import com.tibco.bpm.ace.admin.model.HttpClient;

public interface BPMSecurityService {
	
	public  boolean  authenticate(HttpServletRequest req,HttpServletResponse resp, PathExclusions exclusions) throws IOException, Exception;
	public boolean authenticate(HttpServletRequest req, PathExclusions exclusions) throws IOException, Exception;
	public void updateSSOBindingDetails(HttpClientBuilder builder ,HttpClient client) throws Exception ;
	public boolean isOpenIdTokenValid(String httpClientSharedResourceName) throws Exception ;

}
