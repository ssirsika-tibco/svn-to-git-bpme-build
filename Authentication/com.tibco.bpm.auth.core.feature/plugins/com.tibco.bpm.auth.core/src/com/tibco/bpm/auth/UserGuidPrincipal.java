package com.tibco.bpm.auth;

import java.io.Serializable;
import java.security.Principal;

public class UserGuidPrincipal implements Principal,Serializable{

	private String guid;
	
	public UserGuidPrincipal(String guid) {
		super();
		this.guid = guid;
	}

	@Override
	public String getName() {
		return guid;
	}

}
