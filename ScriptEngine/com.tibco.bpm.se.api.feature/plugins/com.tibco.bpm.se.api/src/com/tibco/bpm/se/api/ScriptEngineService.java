package com.tibco.bpm.se.api;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

/**
 * 
 *
 *
 * @author jaugusti
 * @since 12 Apr 2019
 */
public interface ScriptEngineService
{

	public Scope createScope(Map<String, Long> businessDataApplicationInfo, List<String> dataFieldsDescriptors)
			throws ScriptException;

	public Scope createScope() throws ScriptException;

}
