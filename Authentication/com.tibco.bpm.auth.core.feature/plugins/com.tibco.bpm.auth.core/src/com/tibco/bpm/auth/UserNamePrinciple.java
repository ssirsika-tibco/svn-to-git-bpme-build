package com.tibco.bpm.auth;

import java.io.Serializable;
import java.security.Principal;

public class UserNamePrinciple implements Principal,Serializable{

	private String name;
	
	public UserNamePrinciple(String name) {
		super();
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
