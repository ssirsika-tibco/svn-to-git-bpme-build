package com.tibco.bpm.cdm.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.orm.SequenceDAO;

public class CaseSequenceCache {
	
	static CLFClassContext		logCtx						= CloudLoggingFramework.init(CaseSequenceCache.class,
			CDMLoggingInfo.instance);
	// Class to handle the trigger for dealing with re-caching in another thread
	private CaseIdDaemonHandlerThread	daemonHandlerThread;
	
	protected int cacheSize = 50;
	
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	private Map<BigInteger, CaseSequenceID> sequencesMap = new ConcurrentHashMap<BigInteger,CaseSequenceID>();
		/**
	 * Returns a batch of work item IDs
	 *
	 * @return	Array of work item IDs
	 */
		
	public String[] getCaseIds(BigInteger typeId, int batchSize, SequenceDAO sequenceDao) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getCaseIds");
		String[] idArray = null;
		synchronized (sequencesMap.computeIfAbsent(typeId, k -> new CaseSequenceID(typeId.intValue()))) {
			//clf.local.debug("Type ID : '%d',Case item object hashcode '%s' " , typeId.intValue() ,sequencesMap.get(typeId).hashCode());
			idArray = sequencesMap.get(typeId).getSequenceIDs(batchSize, cacheSize, sequenceDao);
			clf.local.debug("Type ID : '%d', Returning case sequences '%s' " , typeId.intValue() , Arrays.toString(idArray));
		}
		return idArray;
	}

	public CaseIdDaemonHandlerThread getDaemonHandlerThread() {
		return daemonHandlerThread;
	}

	public void setDaemonHandlerThread(CaseIdDaemonHandlerThread daemonHandlerThread) {
		this.daemonHandlerThread = daemonHandlerThread;
	}

}
