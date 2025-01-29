package com.tibco.bpm.auth.core;

public class UserInfo {
	private String name;
	private String guid;

	public UserInfo(String name, String guid) {
		super();
		this.name = name;
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

}
