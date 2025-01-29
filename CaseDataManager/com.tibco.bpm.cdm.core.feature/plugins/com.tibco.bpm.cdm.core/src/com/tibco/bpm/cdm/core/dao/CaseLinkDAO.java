package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.LinkDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * DAO for links created between pairs of cases, based on a Link definition.
 * 
 * @author smorgan
 * @since 2019
 */
public interface CaseLinkDAO
{

	void create(BigInteger typeId, CaseReference caseReference, List<CaseLinkDTO> dtos)
			throws PersistenceException, ReferenceException, InternalException;

	public List<CaseLinkDTO> get(CaseReference caseReference, StructuredType st, String name, Integer skip, Integer top,
			String search, SearchConditionDTO searchCondition) throws PersistenceException, InternalException, ReferenceException;

	public List<CaseLinkDTO> delete(CaseReference caseReference, List<CaseReference> targetCaseReferences, LinkDTO link,
			Integer originEnd) throws PersistenceException, InternalException, ReferenceException;
	
	public void setSimpleSearchRenderer(SimpleSearchRenderer renderer);
	
	public void setConditionRenderer(ConditionRenderer renderer);
	
	public void setLinkDAO(LinkDAO linkDAO);
	

}
