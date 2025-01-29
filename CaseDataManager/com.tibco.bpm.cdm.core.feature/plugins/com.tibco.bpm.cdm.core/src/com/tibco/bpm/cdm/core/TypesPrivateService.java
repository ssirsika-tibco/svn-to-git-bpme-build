package com.tibco.bpm.cdm.core;

import java.util.List;

import com.tibco.bpm.cdm.api.rest.v1.model.TypeInfo;

public interface TypesPrivateService {
	
	/**
	 * 
	 * @param filter
	 * @param select
	 * @param skip
	 * @param top
	 * @return
	 * @throws Exception
	 */
	public List<TypeInfo>  typesGet(String filter,String select,Integer skip,Integer top) throws Exception;
	
}
