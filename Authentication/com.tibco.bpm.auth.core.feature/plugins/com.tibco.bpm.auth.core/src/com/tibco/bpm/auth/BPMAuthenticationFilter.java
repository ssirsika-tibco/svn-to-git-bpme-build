package com.tibco.bpm.auth;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class BPMAuthenticationFilter  implements Filter {

	private static final String ENABLE_AUTHENTICATION="enableAuthentication";
	private boolean isEnableAuthentication= true;

	@Override
	
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		String enableAuthInitParam = filterConfig.getInitParameter(ENABLE_AUTHENTICATION);
		if(null!=enableAuthInitParam && enableAuthInitParam.equalsIgnoreCase("false")){
			isEnableAuthentication=false;
		}
	}

}
