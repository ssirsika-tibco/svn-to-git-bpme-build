package com.tibco.bpm.cdm.core.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tibco.bpm.cdm.api.CaseDataManager;
import com.tibco.bpm.cdm.api.dto.CaseInfo;
import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.CDMException;
import com.tibco.bpm.cdm.api.exception.CaseOutOfSyncError;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.NonUniqueCaseIdentifierError;
import com.tibco.bpm.cdm.api.exception.UserApplicationError;
import com.tibco.bpm.cdm.api.exception.ValidationException;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.DataModelInfo;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.StructuredType;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.logging.cloud.context.CLFThreadContext;
import com.tibco.bpm.se.api.Scope;
import com.tibco.bpm.se.internal.InternalScope;

public class CaseDataScriptAPIImpl implements CaseDataScriptAPI
{

	private Map<String, Long>	caseTypesInfo		= new HashMap<String, Long>();

	private CaseDataManager		api					= null;

	ObjectMapper				mapper				= new ObjectMapper();

	ScriptEngineManager			scriptEngineManager	= new ScriptEngineManager();

	InternalScope				scope				= null;

	AtomicInteger				atomicInt			= new AtomicInteger(0);
	
	private DataModelDAO				dataModelDAO;

	static CLFClassContext		logCtx				= CloudLoggingFramework.init(CaseDataScriptAPIImpl.class,
			CDMLoggingInfo.instance);

	public CaseDataScriptAPIImpl(Map<String, Long> caseTypesInfo, CaseDataManager privateAPI, Scope scope, DataModelDAO dataModelDAO)
	{
		this.caseTypesInfo = caseTypesInfo;
		this.api = privateAPI;
		this.scope = (InternalScope) scope;
		this.dataModelDAO = dataModelDAO;
	}

	@Override
	public String create(Map caseData, String caseType) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("create");
		clfMethodContext.local.trace("enter");
		String ref = null;

		try
		{

			try
			{
				int majorVersion = getMajorVersion(caseType);
				String data = scope.stringifyJSObject(caseData);
				ref = this.api.createCase(new QualifiedTypeName(caseType), majorVersion, data).toString();

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (ValidationException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}

		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return ref;
	}

	private int getMajorVersion(String caseType) throws ArgumentException
	{

		Long majorVersion = caseTypesInfo.get(caseType);
		return majorVersion.intValue();
	}

	@Override
	public Object createAll(List<Map> cases, String caseType) throws ScriptException
	{

		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("createAll");
		clfMethodContext.local.trace("enter");
		Object refsJSArray = null;

		try
		{

			String data = null;

			data = scope.stringifyJSObjectArray(cases);
			List<String> refs = null;
			try
			{
				int majorVersion = getMajorVersion(caseType);
				final JsonNode jsonNode = new ObjectMapper().readTree(data);
				List<String> cases_json = StreamSupport.stream(jsonNode.spliterator(), false) // Stream
						.map(JsonNode::toString) // map to a string
						.collect(Collectors.toList());

				refs = this.api.createCases(new QualifiedTypeName(caseType), majorVersion, cases_json).stream()
						.map(CaseReference::toString).collect(Collectors.toList());
				refsJSArray = scope.toJSObjectArray(refs);
			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (ValidationException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (IOException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return refsJSArray;
	}

	@Override
	public String updateByRef(String ref, Map caseData) throws ScriptException
	{

		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("updateByRef");
		clfMethodContext.local.trace("enter");
		try
		{

			String casedata = null;

			CaseReference reference = parseCaseReference(ref);
			String caseType = reference.getQualifiedTypeName().toString();

			try
			{

				casedata = scope.stringifyJSObject(caseData);

				ref = this.api.updateCase(new CaseReference(ref), casedata).toString();
			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (ValidationException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}

		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return ref;

	}

	private CaseReference parseCaseReference(String ref) throws ScriptException
	{
		CaseReference reference = null;
		try
		{
			reference = new CaseReference(ref);
		}
		catch (ArgumentException r)
		{
			//throw new ScriptException(r);
			handleException(r);
		}
		return reference;
	}

	@Override
	public Object updateAllByRef(Object refObjects, List<Map> cases) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("updateAllByRef");
		clfMethodContext.local.trace("enter");
		try
		{

			List<String> refStrings = scope.unwrapToStringArray(refObjects);

			List<CaseReference> refs = new ArrayList<>();

			Object returnRefs = null;

			try
			{
				for (String refString : refStrings)
				{
					refs.add(new CaseReference(refString));
				}

				List<String> casesList = transformToJavaList(cases);

				returnRefs = this.api.updateCases(refs, casesList);

				Object updatedRefs = scope.toJSObjectArray(returnRefs);
				scope.reinitializeJSObjectArray(refObjects, updatedRefs);

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (ValidationException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (IOException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return refObjects;

	}

	@Override
	public void deleteByRef(String ref) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("deleteByRef");
		clfMethodContext.local.trace("enter");
		try
		{

			String caseType = null;
			try
			{
				this.api.deleteCase(new CaseReference(ref));
				CaseReference reference = parseCaseReference(ref);
				caseType = reference.getQualifiedTypeName().toString();

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
		}
		finally
		{
			CLFThreadContext.endContext();
		}
	}

	@Override
	public Object read(String caseRef) throws ScriptException
	{
		Object data = null;
		String caseType = null;
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("read");
		clfMethodContext.local.trace("enter");

		try
		{
			CaseReference ref = new CaseReference(caseRef);
			caseType = ref.getQualifiedTypeName().toString();
			CaseInfo info = this.api.readCase(ref);

			String json = info.getCasedata();
			data = scope.parse(json, caseType);
			caseRef = info.getReference().toString();

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}
		return data;
	}
	

	@Override
	public Object getLatestCaseRef(String caseRef) throws ScriptException{
		Object data = null;
		String caseType = null;
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("getLatestCaseRef");
		clfMethodContext.local.trace("enter");

		try
		{
			CaseReference ref = new CaseReference(caseRef);
			clfMethodContext.local.trace("passed  version as ["+ref.getVersion()+"]");
			caseType = ref.getQualifiedTypeName().toString();
			CaseInfo info = this.api.readCase(ref);
			caseRef = info.getReference().toString();
			clfMethodContext.local.trace("returning  version as ["+info.getReference().getVersion()+"]");

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}
		return caseRef;
	}

	@Override
	public Object readAll(Object refs) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("readAll");
		clfMethodContext.local.trace("enter");
		Object casesList = null;
		try
		{

			List<String> refStrings = scope.unwrapToStringArray(refs);
			if (refStrings == null || refStrings.size() == 0)
			{
				throw new ScriptException("null caseReference List passed");
			}

			try
			{
				List<CaseReference> caseRefs = new ArrayList<>();
				for (String refString : refStrings)
				{
					caseRefs.add(new CaseReference(refString));
				}

				List<CaseInfo> cases = this.api.readCases(caseRefs);

				List<String> casesJSONArray = cases.stream().map(CaseInfo::getCasedata).collect(Collectors.toList());

				List<String> updatedRefs = cases.stream().map(CaseInfo::getReference).map(CaseReference::toString)
						.collect(Collectors.toList());

				ArrayNode nodes = JsonNodeFactory.instance.arrayNode();
				for (String json : casesJSONArray)
				{
					nodes.add(mapper.readTree(json));
				}

				String casesJSON = mapper.writeValueAsString(nodes);

				casesList = scope.parse(casesJSON, cases.get(0).getReference().getQualifiedTypeName().toString());

				Object parseSimpleArray = scope.toJSObjectArray(updatedRefs);

				scope.reinitializeJSObjectArray(refs, parseSimpleArray);

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (ScriptException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (JsonProcessingException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
			catch (IOException e)
			{
				//throw new ScriptException(e);
				handleException(e);
			}
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return casesList;
	}

	private List<String> transformToJavaList(Object stringList)
			throws ScriptException, IOException, JsonProcessingException
	{
		String refs;

		refs = scope.stringifyJSObjectArray(stringList);

		final JsonNode jsonNode = mapper.readTree(refs);
		List<String> javaStringList = StreamSupport.stream(jsonNode.spliterator(), false) // Stream
				.map(JsonNode::toString) // map to a string
				.collect(Collectors.toList());
		return javaStringList;
	}

	@Override
	public String findByCaseIdentifier(String cid, String caseType) throws ScriptException
	{
		String ref = null;
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("findByCaseIdentifier");
		clfMethodContext.local.trace("enter");
		try
		{
			int majorVersion = getMajorVersion(caseType);
			CaseInfo info = this.api.readCase(new QualifiedTypeName(caseType), majorVersion, cid);

			ref = info.getReference().toString();

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		catch (ValidationException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return ref;
	}
	
	@Override
	public String findByCompositeIdentifier(Object[] cids, String caseType) throws ScriptException
	{
		String ref = null;
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("findByCompositeIdentifier");
		clfMethodContext.local.trace("enter");
		try
		{
			String dql = "";
			int majorVersion = getMajorVersion(caseType);
			QualifiedTypeName qtn = new QualifiedTypeName(caseType);
			DataModelInfo dataModel = dataModelDAO.read(qtn.getNamespace(), majorVersion, true);
			StructuredType structuredType = dataModel.getDataModel().getStructuredTypeByName(qtn.getName());
			List<Attribute> identifierAttributes = structuredType.getAttributes().stream()
					.filter(a -> a.getIsIdentifier()).collect(Collectors.toList());
			int i = 0;
			if(cids.length != identifierAttributes.size())
				throw new ScriptException("Incorrect number of identifier attributes passed");
			for(Attribute attribute : identifierAttributes) {
				Object value = cids[i++] ;
				if(value instanceof String)
					dql = dql + attribute.getName() +  " = '" + String.valueOf(value) + "' and "; 
				else
					dql = dql + attribute.getName() +  " = " + (int) value + " and "; 
			}
			List<CaseInfo> info = this.api.readCases(qtn, majorVersion, null, 1, null, dql.substring(0, dql.length()-4));

			if(!info.isEmpty())
			{
				ref = info.get(0).getReference().toString();
			}

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		catch (ScriptException e)
		{
			//throw new ScriptException(e);
			handleException(e,false);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return ref;
	}

	@Override
	public Object findAll(String caseType, long fromIndex, long pageSize) throws ScriptException
	{

		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("findAll");
		clfMethodContext.local.trace("enter");
		Object returnArray = null;
		try
		{

			//TODO: validate fromIndex and pageSize needs to be set 
			if (caseType == null)
			{
				throw new ScriptException("null caseType passed");
			}

			try
			{
				int majorVersion = getMajorVersion(caseType);
				returnArray = invokeReadCases(caseType, null, null, fromIndex, pageSize, majorVersion);

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
			catch (ScriptException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}

		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return returnArray;
	}

	private Object invokeReadCases(String caseType, String dql, String search, long fromIndex, long pageSize,
			int majorVersion) throws ArgumentException, InternalException, ScriptException
	{
		Object returnArray;

		//TODO: Fix this once https://jira.tibco.com/browse/ACE-2237 is fixed
		List<CaseInfo> cases = this.api.readCases(new QualifiedTypeName(caseType), majorVersion,
				Long.valueOf(fromIndex).intValue(), Long.valueOf(pageSize).intValue(), search, dql);

		List<String> updatedRefs = cases.stream().map(CaseInfo::getReference).map(CaseReference::toString)
				.collect(Collectors.toList());

		returnArray = scope.toJSObjectArray(updatedRefs);

		return returnArray;
	}

	@Override
	public Object findByCriteria(String dql, String caseType, long fromIndex, long pageSize) throws ScriptException
	{

		Object returnArray = null;

		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("findByCriteria");
		clfMethodContext.local.trace("enter");
		try
		{

            if (dql == null) {
                throw new ScriptException("null dql passed");
            }

			//TODO: validate fromIndex and pageSize needs to be set 
			if (caseType == null)
			{
				throw new ScriptException("null caseType passed");
			}

			try
			{
				int majorVersion = getMajorVersion(caseType);
				returnArray = invokeReadCases(caseType, scope.resolveDQLVariables(dql), null, fromIndex, pageSize, majorVersion);

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e,true);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
			catch (ScriptException e)
			{
				//throw new ScriptException(e);
                handleException(e, false);
			}

		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return returnArray;
	}

	@Override
	public Object findBySimpleSearch(String searchString, String caseType, long fromIndex, long pageSize)
			throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("findBySimpleSearch");
		clfMethodContext.local.trace("enter");

		Object returnArray = null;
		try
		{

			//TODO: validate fromIndex and pageSize needs to be set 
			if (caseType == null)
			{
				throw new ScriptException("null caseType passed");
			}

			try
			{
				int majorVersion = getMajorVersion(caseType);
				returnArray = invokeReadCases(caseType, null, searchString, fromIndex, pageSize, majorVersion);

			}
			catch (ArgumentException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
			catch (InternalException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
			catch (ScriptException e)
			{
				//throw new ScriptException(e);
				handleException(e,false);
			}
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return returnArray;
	}

	@Override
	public void link(String sourceRef, String targetRef, String linkName) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("link");
		clfMethodContext.local.trace("enter");
		try
		{

			this.api.linkCase(new CaseReference(sourceRef), linkName, new CaseReference(targetRef));

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

	}

	@Override
	public void linkAll(String sourceRef, Object targetRefs, String linkName) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("linkAll");
		clfMethodContext.local.trace("enter");
		try
		{

			List<String> refStrings = scope.unwrapToStringArray(targetRefs);
			List<CaseReference> caseRefs = new ArrayList<>();
			for (String refString : refStrings)
			{
				caseRefs.add(new CaseReference(refString));
			}
			this.api.linkCases(new CaseReference(sourceRef), linkName, caseRefs);

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

	}

	@Override
	public void unlink(String sourceRef, String targetRef, String linkName) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("unlink");
		clfMethodContext.local.trace("enter");
		try
		{
			this.api.unlinkCase(new CaseReference(sourceRef), linkName, new CaseReference(targetRef));

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

	}

	@Override
	public void unlinkAll(String sourceRef, Object targetRefs, String linkName) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("unlinkAll");
		clfMethodContext.local.trace("enter");
		try
		{

			List<String> refStrings = scope.unwrapToStringArray(targetRefs);
			List<CaseReference> caseRefs = new ArrayList<>();
			for (String refString : refStrings)
			{
				caseRefs.add(new CaseReference(refString));
			}
			this.api.unlinkCases(new CaseReference(sourceRef), linkName, caseRefs);

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

	}

	@Override
	public void unlinkAllByLinkName(String sourceRef, String linkName) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("unlinkAllByLinkName");
		clfMethodContext.local.trace("enter");
		try
		{
			this.api.unlinkCases(new CaseReference(sourceRef), linkName);
		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}
	}

	@Override
	public Object navigateByCriteria(String caseRef, String linkName, String dql, long fromIndex, long pageSize)
			throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("navigateByCriteria");
		clfMethodContext.local.trace("enter");

		Object returnRefs = null;

		try
		{
			List<String> refs = this.api
					.navigateLinks(new CaseReference(caseRef), linkName, Long.valueOf(fromIndex).intValue(),
							Long.valueOf(pageSize).intValue(), null, scope.resolveDQLVariables(dql))
					.stream().map(CaseReference::toString).collect(Collectors.toList());
			returnRefs = scope.toJSObjectArray(refs);
		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return returnRefs;
	}

	@Override
	public Object navigateBySimpleSearch(String caseRef, String linkName, String searchString, long fromIndex,
			long pageSize) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("navigateBySimpleSearch");
		clfMethodContext.local.trace("enter");
		Object returnRefs = null;
		try
		{
			List<String> refs = this.api
					.navigateLinks(new CaseReference(caseRef), linkName, Long.valueOf(fromIndex).intValue(),
							Long.valueOf(pageSize).intValue(), searchString, null)
					.stream().map(CaseReference::toString).collect(Collectors.toList());
			returnRefs = scope.toJSObjectArray(refs);

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}
		return returnRefs;
	}

	@Override
	public Object navigateAll(String caseRef, String linkName, long fromIndex, long pageSize) throws ScriptException
	{
		CLFThreadContext context = CLFThreadContext.beginContext();
		CLFMethodContext clfMethodContext = logCtx.getMethodContext("navigateAll");
		clfMethodContext.local.trace("enter");
		Object returnRefs = null;
		try
		{
			List<String> refs = this.api
					.navigateLinks(new CaseReference(caseRef), linkName, Long.valueOf(fromIndex).intValue(),
							Long.valueOf(pageSize).intValue(), null, null)
					.stream().map(CaseReference::toString).collect(Collectors.toList());
			returnRefs = scope.toJSObjectArray(refs);

		}
		catch (ArgumentException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		catch (InternalException e)
		{
			//throw new ScriptException(e);
			handleException(e);
		}
		finally
		{
			CLFThreadContext.endContext();
		}

		return returnRefs;
	}

	
	
	protected void handleException(Exception e,boolean throwException) throws ScriptException {
		CLFMethodContext clf = logCtx.getMethodContext("handleException");
		clf.local.trace("handleException called");
		if(null!=e) {
			UserApplicationError userApplicationError; 
			if(e instanceof CaseOutOfSyncError || e instanceof NonUniqueCaseIdentifierError) {
				throw new ScriptException(e);
			}
			else if(e instanceof CDMException) {
				
				CDMException error=(CDMException)e;
				Map<String, String> attributes = error.getAttributes();
				ArrayList<String> paramsList=new ArrayList<String>();
				if(null!=attributes) {
					Iterator<String> keys = attributes.keySet().iterator();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						paramsList.add(key);
						paramsList.add(attributes.get(key));
						
					}
				}
				String[] params= paramsList.toArray(new String[paramsList.size()]);
				userApplicationError = new UserApplicationError(error.getErrorData(),error,params);
				clf.local.error(userApplicationError.getMessage(),userApplicationError);
				if(throwException)
					throw new ScriptException(userApplicationError);
			}else if(e instanceof  UserApplicationError) {
				throw new ScriptException(e);
			}else {
				throw new ScriptException(e);
			}
		}
	}
	
	protected void handleException(Exception e) throws ScriptException {
		handleException(e,true);
	}
}
