package com.tibco.bpm.cdm.core.script;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

public interface CaseDataScriptAPI
{

	//Create,Update,Delete 

	public String create(Map caseData, String caseType) throws ScriptException;

	public Object createAll(List<Map> cases, String caseType) throws ScriptException;

	public String updateByRef(String ref, Map caseData) throws ScriptException;

	public Object updateAllByRef(Object refs, List<Map> cases) throws ScriptException;

	public void deleteByRef(String ref) throws ScriptException;

	//Link/Unlink

	public void link(String sourceRef, String targetRef, String linkName) throws ScriptException;

	public void linkAll(String sourceRef, Object targetRefs, String linkName) throws ScriptException;

	public void unlink(String sourceRef, String targetRef, String linkName) throws ScriptException;

	public void unlinkAll(String sourceRef, Object targetRefs, String linkName) throws ScriptException;

	public void unlinkAllByLinkName(String sourceRef, String linkName) throws ScriptException;

	//API exposed in scripts

	//Read
	public Object read(String caseRef) throws ScriptException;

	public Object readAll(Object caseRefs) throws ScriptException;

	//FindByCID
	public String findByCaseIdentifier(String cid, String caseType) throws ScriptException;

	//FindAll
	public Object findAll(String caseType, long fromIndex, long pageSize) throws ScriptException;

	//FindByCriteria
	public Object findByCriteria(String dql, String caseType, long fromIndex, long pageSize) throws ScriptException;

	public Object findBySimpleSearch(String searchString, String caseType, long fromIndex, long pageSize)
			throws ScriptException;

	//NavigateByCriteria
	public Object navigateByCriteria(String caseRef, String linkName, String dql, long fromIndex, long pageSize)
			throws ScriptException;

	public Object navigateBySimpleSearch(String caseRef, String linkName, String searchString, long fromIndex,
			long pageSize) throws ScriptException;

	//Navigate All
	public Object navigateAll(String caseRef, String linkName, long fromIndex, long pageSize) throws ScriptException;

	public Object getLatestCaseRef(String caseRef) throws ScriptException;

	//FindByCompositeIdentifier
	public String findByCompositeIdentifier(Object[] cid, String caseType) throws ScriptException;
}
