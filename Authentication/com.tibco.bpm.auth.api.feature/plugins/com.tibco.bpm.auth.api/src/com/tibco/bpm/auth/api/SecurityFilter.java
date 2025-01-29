package com.tibco.bpm.auth.api;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SecurityFilter implements Filter {

	protected PathExclusions exclusions;
	
	private BPMSecurityService securityService;
	
	public SecurityFilter() {
		AuthLogger.debug("Security Filter Started");
		exclusions = new PathExclusions();
		exclusions.addExclusion("/apps/login");
		exclusions.addExclusion("/bpm/auth/v1/sso");
	}
	

	public BPMSecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(BPMSecurityService securityService) {
		AuthLogger.debug("setSecurityService "+ securityService);
		this.securityService = securityService;
	}

	@Override
	public void destroy() {
		AuthLogger.debug("Security filer destroyed");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		
		try{
			if(getSecurityService().authenticate(req, res,exclusions)){
				chain.doFilter(req, res);
			}else{
				sendForbiddenResponse(request, res);
			}
			
		}catch(Throwable t){
			request.getServletContext().log(t.getMessage());
			sendForbiddenResponse(request, res);
		}
		


	}

	private void sendForbiddenResponse(ServletRequest request, HttpServletResponse res) throws IOException {
		res.setHeader("WWW-Authenticate", "Basic realm=\"Insert credentials\"");
		res.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		AuthLogger.debug("filer intialized" + config.getServletContext());
		
	}



}
