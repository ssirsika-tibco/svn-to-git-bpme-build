package com.tibco.bpm.auth.api;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

public class PathExclusions {
	ArrayList<String> paths=new ArrayList<String>();

	public PathExclusions(ArrayList<String> paths) {
		super();
		if(null!=paths){
			this.paths.addAll(paths);
		}
	}
	
	public PathExclusions() {
		super();
	}
	
	public boolean isExcludePath(HttpServletRequest request){
		//remove this if block only temporary
		/*String isAuthenitcate = System.getProperty("com.tibco.authenticate");
		if(null!=isAuthenitcate && isAuthenitcate.equalsIgnoreCase("false")){
			return true;
		}*/
		if(null!=request){
			for (String path : paths) {
				if(request.getRequestURL().toString().contains(path)){
					return true;
				}
			}
		}
		return false;
	}
	
	public void addExclusion(String path){
		paths.add(path);
	}
	
	

}
