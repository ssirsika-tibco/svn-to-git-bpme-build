package com.tibco.bpm.cdm.core.dao;

import java.math.BigInteger;
import java.util.List;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.n2.common.orm.SequenceDAO;

/**
 * DAO for obtaining values for auto-generated case identifiers
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public interface IdentifierValueDAO extends SequenceDAO
{
	/**
	 * Obtains a list of identifier values for the given type, based on the IdentifierInitialisationInfo specified
	 * for a case type in the Data Model.  Returns an empty list if no such configuration exists for the Data Model.
	 * See {@link com.tibco.bpm.da.dm.api.StructuredType#hasDynamicIdentifier()}.
	 *
	 * @param typeId
	 * @param quantity number of identifiers to generate
	 * @return
	 * @throws PersistenceException
	 */
	public List<String> getIdentifierValues(BigInteger typeId, int quantity) throws PersistenceException;
}
