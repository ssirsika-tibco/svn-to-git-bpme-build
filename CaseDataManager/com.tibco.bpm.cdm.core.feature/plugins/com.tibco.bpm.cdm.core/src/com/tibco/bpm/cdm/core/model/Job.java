package com.tibco.bpm.cdm.core.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Simple bean to encapsulate the details of a job on the internal queue.
 * @author smorgan
 * @since 2019
 */
@JsonPropertyOrder({"method", "applicationId", "typeId", "caseReferences"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Job
{
	public static final String			METHOD_DELETE_CASES				= "deleteCases";

	public static final String			METHOD_AUTO_PURGE				= "autoPurge";

	public static final String			METHOD_AUTO_PURGE_APPLICATION	= "autoPurgeApplication";

	public static final String			METHOD_AUTO_PURGE_CASE_TYPE		= "autoPurgeCaseType";

	public static final List<String>	allMethods						= Arrays.asList(new String[]{
			METHOD_DELETE_CASES, METHOD_AUTO_PURGE_CASE_TYPE, METHOD_AUTO_PURGE_APPLICATION, METHOD_AUTO_PURGE});

	// Job priorities for corresponding entries in the allMethods list
	public static final List<Integer>	priorities						= Arrays
			.asList(new Integer[]{100, 200, 300, 400});

	public static final String			AP_JOB_CORRELATION_ID			= "AP";

	private String						method;

	private BigInteger					applicationId;

	private BigInteger					typeId;

	private List<String>				caseReferences					= new ArrayList<String>();

	public Job()
	{
	}

	@JsonIgnore
	public boolean isMethodValid()
	{
		// If/when we support multiple methods, update this to compare against all of them.
		return method != null && allMethods.contains(method);
	}

	@JsonGetter("method")
	public String getMethod()
	{
		return method;
	}

	@JsonSetter("method")
	public void setMethod(String method)
	{
		this.method = method;
	}

	@JsonProperty("caseReferences")
	public List<String> getCaseReferences()
	{
		return caseReferences;
	}

	@JsonGetter("applicationId")
	public BigInteger getApplicationId()
	{
		return applicationId;
	}

	@JsonSetter("applicationId")
	public void setApplicationId(BigInteger applicationId)
	{
		this.applicationId = applicationId;
	}

	@JsonGetter("typeId")
	public BigInteger getTypeId()
	{
		return typeId;
	}

	@JsonSetter("typeId")
	public void setTypeId(BigInteger typeId)
	{
		this.typeId = typeId;
	}

	/**
	 * There is a one-to-one mapping from method to priority.
	 * 'Finer-grained' tasks are always more important.
	 * @param method
	 * @return
	 */
	public static Integer getPriorityForMethod(String method)
	{
		Integer result = null;
		if (method != null)
		{
			int idx = allMethods.indexOf(method);
			if (idx >= 0)
			{
				result = priorities.get(idx);
			}
		}
		if (result == null)
		{
			throw new IllegalArgumentException("Unrecognised method: " + method);
		}
		return result;
	}

	@Override
	public String toString()
	{
		return "Job [method=" + method + ", applicationId=" + applicationId + ", typeId=" + typeId + ", caseReferences="
				+ caseReferences + "]";
	}

}
