package com.tibco.bpm.auth.filters;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticateServlet extends HttpServlet {

	/**
	 *
	 **/
	private static final long serialVersionUID = 1861037384364913913L;

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException {
		final PrintWriter writer = response.getWriter();
		writer.println("true");
		
	}

}