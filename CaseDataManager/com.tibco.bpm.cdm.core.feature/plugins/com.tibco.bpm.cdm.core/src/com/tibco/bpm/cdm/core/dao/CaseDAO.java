package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.CasedataException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.util.TimestampOp;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * DAO for Cases. 
 * @author smorgan
 * @since 2019
 */
public interface CaseDAO
{
	/**
	 * Value object containing information used during case creation
	 */
	public static class CaseCreationInfo
	{
		private String				casedata;

		private String				cid;

		private BigInteger			stateId;

		private String				stateValue;

		private Map<String, String>	removals;

		public CaseCreationInfo(String casedata, String cid, BigInteger stateId, String stateValue)
		{
			this.casedata = casedata;
			this.cid = cid;
			this.stateId = stateId;
			this.stateValue = stateValue;
		}

		@Override
		public String toString()
		{
			return "CaseCreationInfo [casedata=" + casedata + ", cid=" + cid + ", stateId=" + stateId + ", stateValue="
					+ stateValue + "]";
		}

		public String getCasedata()
		{
			return casedata;
		}

		public String getCid()
		{
			return cid;
		}

		public BigInteger getStateId()
		{
			return stateId;
		}

		public String getStateValue()
		{
			return stateValue;
		}

		public Map<String, String> getRemovals()
		{
			return removals;
		}

		public void setRemovals(Map<String, String> removals)
		{
			this.removals = removals;
		}
	}

	/**
	 * Creates case(s) of the given type. Returns an equal quantity of case reference(s) in the same order.
	 * @param typeId
	 * @param infos
	 * @param createdBy A GUID representing the creating user
	 * @return
	 * @throws PersistenceException
	 * @throws CasedataException
	 */
	public List<BigInteger> create(BigInteger typeId, List<CaseCreationInfo> infos, String createdBy)
			throws PersistenceException, CasedataException;

	/**
	 * Reads cases of the given type, subject to various optional restrictions.
	 * @param typeId
	 * @param skip
	 * @param top
	 * @param cid
	 * @param stateValue
	 * @param maxModificationTimestamp
     * @param opr
	 * @param search
	 * @param condition
	 * @param st
	 * @param aspectSelection
	 * @param excludeTerminalState 
	 * @return
	 * @throws PersistenceException
	 */
	public List<CaseInfoDTO> read(BigInteger typeId, Integer skip, Integer top, String cid, String stateValue,
			Calendar maxModificationTimestamp, TimestampOp opr, String search, SearchConditionDTO condition, StructuredType st,
			CaseAspectSelection aspectSelection, boolean excludeTerminalState) throws PersistenceException;

	/**
	 * Reads a single case by reference.
	 * @param caseReference
	 * @param aspectSelection
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException
	 */
	public CaseInfoDTO read(CaseReference caseReference, CaseAspectSelection aspectSelection)
			throws PersistenceException, ReferenceException;

	/**
	 * Reads cases, given a list of references. Returns cases in the same order as the reference list.
	 * @param caseReferences
	 * @param aspectSelection
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException
	 */
	public List<CaseInfoDTO> read(List<CaseReference> caseReferences, CaseAspectSelection aspectSelection)
			throws PersistenceException, ReferenceException;

	/**
	 * Updates one or more cases.
	 * @param cases
	 * @param modifiedBy A GUID representing the modifying user
	 * @throws PersistenceException
	 * @throws ReferenceException
	 * @throws CasedataException
	 */
	public void update(List<CaseUpdateDTO> cases, String modifiedBy)
			throws PersistenceException, ReferenceException, CasedataException;

	/**
	 * Deletes a single case by reference.
	 * @param ref
	 * @throws PersistenceException
	 * @throws ReferenceException
	 */
	public void delete(CaseReference ref) throws PersistenceException, ReferenceException;

	/**
	 * Deletes all cases of the given type that are in the given state and were last modified on or before the given timestamp. 
	 * Returns a list of references for the deleted cases.
	 * @param typeId
	 * @param stateValue
	 * @param maxModificationTimestamp
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException 
	 */
	public List<CaseReference> delete(BigInteger typeId, String stateValue, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException;

    /**
     * Gets all cases of the given type that are in the given state and were
     * last modified on or before the given timestamp. Returns a list of
     * references for the selected cases.
     * 
     * @param typeId
     * @param stateValue
     * @param maxModificationTimestamp
     * @return list of case refs
     * @throws PersistenceException
     * @throws ReferenceException
     */
    public List<CaseReference> getCasesByTypeStateTimestamp(BigInteger typeId,
            String stateValue, Calendar maxModificationTimestamp)
            throws PersistenceException, ReferenceException;

    /**
     * Gets all linked cases of the given case reference
     * 
     * @param caseRef
     * @param 1
     *            for end1, 2 for end2
     * @return List of case refs for linked cases
     * @throws PersistenceException
     * @throws ReferenceException
     */
    public List<CaseReference> getLinkedCases(CaseReference caseRef, int end)
            throws PersistenceException, ReferenceException;

	/**
	 * Checks to see if the given case exists. Returns false if the type exists, but the case doesn't.
	 * If the _type_ doesn't exist, a ReferenceException occurs.
	 * 
	 * @param ref
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException
	 */
	public boolean exists(CaseReference ref) throws PersistenceException, ReferenceException;

	/**
	 * Returns a count of cases for the given deployment.
	 * @param deploymentId
	 * @return
	 * @throws PersistenceException
	 */
	public long countByDeploymentId(BigInteger deploymentId) throws PersistenceException;

	/**
	 * Returns true if the case is in an active state, or false if it's in a terminal state.
	 * 
	 * @param ref
	 * @return
	 * @throws PersistenceException
	 */
	public Boolean getIsTerminalState(CaseReference ref) throws PersistenceException;

	/**
	 * Selects (for update) all cases of the given type that are in a terminal state and
	 * were last modified on or before the given timestamp.  A subsequent call to markForPurge(...) 
	 * can be made to mark the cases for purge (effectively making them invisible to all APIs and future 
	 * calls to this method that may occur before actual deletion has been completed); Typically, markForPurge(...) 
	 * is called with a sub-set of the cases, after excluding those that are referenced by processes 
	 * (this being the reason the priming/marking are done as separate steps).
	 * 
	 * @param typeId
	 * @param maxModificationTimestamp
	 * @return
	 * @throws PersistenceException
	 * @throws ReferenceException 
	 */
	public List<CaseReference> primeForPurge(BigInteger typeId, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException;

	/**
	 * Sets the marked_for_purge flag on the given cases, effectively making them invisible to all APIs.
	 * This is intended to be called immediately prior to scheduling the cases for deletion.
	 * @param batchOfRefs
	 * @throws PersistenceException
	 */
	public void markForPurge(List<CaseReference> batchOfRefs) throws PersistenceException;
	
	public void setSimpleSearchRenderer(SimpleSearchRenderer renderer);
	
	public void setConditionRenderer(ConditionRenderer renderer);
}