/*
 *
 *      ENVIRONMENT:    Java Generic
 *
 *      DESCRIPTION:    TODO
 *      
 *      COPYRIGHT:      (C) 2007 Tibco Software Inc
 *
 */
package com.tibco.bpm.cdm.core;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.orm.SequenceDAO;

/*
 * =====================================================
 * TYPE : SequenceID
 * =====================================================
 */
/**
 *
 * This class contains the functionality for returning sequence
 * IDs and caching up sequences from the database by calling
 * into the SequenceDAO.
 * 
 *
 * @author S.Croall
 */
public class CaseSequenceID
{
	
	static CLFClassContext		logCtx						= CloudLoggingFramework.init(CaseSequenceID.class,
			CDMLoggingInfo.instance);
	/*
	 * holds the next prefix value
	 */
	private String	prefix	= null;
	
	/*
	 * holds the next suffix value
	 */
	private String	suffix	= null;
	
	/*
	 * holds the next minNumLength value
	 */
	private int minNumLength	= 0;

	private int numSequenceIDs;

	private BigInteger nextSequenceID;

	private Integer type;
	
	public CaseSequenceID(Integer type) {
		this.setType(type);
	}

	public CaseSequenceID(Integer type, String prefix, String suffix, int minNumLength) {
		this.setType(type);
		this.prefix = prefix;
		this.suffix = suffix;
		this.minNumLength = minNumLength;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public int getMinNumLength() {
		return minNumLength;
	}

	public void setMinNumLength(int minNumLength) {
		this.minNumLength = minNumLength;
	}
	
	/*
	 * =====================================================
	 * METHOD : getSequenceIDs
	 * =====================================================
	 */
	/**
	 * This method returns a list of sequence IDs
	 *
	 * @param itemBatchSize		The number of sequence IDs to return
	 * @param itemCacheSize		The cache size for sequence IDs
	 * @param sequenceDAO 		DAO for sequences
	 * @return sequenceIDs		Array of the next sequence IDs
	 * @throws PersistenceException 
	 */
	public String[] getSequenceIDs( int itemBatchSize, int itemCacheSize, SequenceDAO sequenceDAO) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getSequenceIDs");
		String sequenceIds[] = new String[itemBatchSize];
		
		/*
		 * Get the start of the batch of sequence IDs in a single call.
		 */
		BigInteger startBatchIDs = getStartSequenceID (itemBatchSize, itemCacheSize, sequenceDAO, true);
		
		for (int i = 0; i < itemBatchSize; i++)
		{
			startBatchIDs = startBatchIDs.add(BigInteger.ONE);
			sequenceIds[i] = ((IdentifierValueDAOImpl)sequenceDAO).bakeIdentifier(prefix, startBatchIDs, suffix, minNumLength);
		}
		
		return sequenceIds;
	}
	
	/*
	 * =====================================================
	 * METHOD : getStartSequenceID
	 * =====================================================
	 */
	/**
	 * This method returns the ID from where it is safe to use the
	 * requested batch size of IDs.  For example, passing a batch 
	 * size of 20 will result in an ID be returned from which point
	 * 20 IDs can be safely used.
	 * 
	 * If there isn't enough in the cache for the requested batch size
	 * it will throw away what's left in the cache and go to the DB
	 * to get a set of IDs based on the larger of the batch and cache
	 * sizes.
	 *
	 * @param itemBatchSize		size of batched sequence IDs
	 * @param itemCacheSize 	size of sequence cache
	 * @param sequenceDAO		DAO for sequences
	 * @param bReturnID			Whether we need to return an ID
	 * @return sequenceID	the next available sequence ID
	 * @throws PersistenceException 
	 */
	private synchronized BigInteger getStartSequenceID(int itemBatchSize, int itemCacheSize, SequenceDAO sequenceDAO, boolean bReturnID) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getStartSequenceID");
		clf.local.debug("Called for type ID '%d' , by object '%d' " , type.intValue(), this.hashCode());
		BigInteger	retID = new BigInteger("-1");

		/*
		 * If there are no more sequence IDs left in the cache, or there
		 * aren't enough to satisfy the batch size requested, go to the DB and
		 * get a new set of IDs. 
		 * 
		 * This does mean that any IDs left in the cache will be lost.
		 */
			if ((numSequenceIDs <= 0) || (numSequenceIDs < itemBatchSize))
			{
				int size = (itemBatchSize > itemCacheSize) ? itemBatchSize : itemCacheSize;
	
				/*
				 * get the next ID to use from the database
				 */
				try {
					
						clf.local.debug("Type ID '%d' : Case IDs cache exhausted. Fetching '%d' more IDs into the cache" , type.intValue(),
								itemCacheSize);
						String[] result = ((IdentifierValueDAOImpl)sequenceDAO).cacheIdentifierRowForType(type, size);
						nextSequenceID = BigInteger.valueOf(Integer.parseInt(result[0]));
						prefix = result[1];
						suffix = result[2];
						minNumLength = Integer.parseInt(result[3]);
//						clf.local.debug("Type ID '%d' : nextSequenceID after cache refresh '%d' " , type.intValue(),
//								nextSequenceID.intValue());
					
					
				} catch (Exception e) {
					clf.local.error(e, "Error occurred while refilling case ID cache for type ID " + type.toString());
				}
				
				/*
				 *  now that more IDs have been allocated, add them to the count of available IDs
				 */
				numSequenceIDs = size;
			}

	/*
		 * Only return an ID if requested to do so.  This maybe because a thread has
		 * called this method just to ensure that there are sufficient IDs in the cache
		 * for another batch ID API call.
		 */
		if (bReturnID)
		{
			retID = nextSequenceID;
			
			/*
			 * Use up the number of sequences requested by the batch size.
			 */
			numSequenceIDs -= itemBatchSize;
			nextSequenceID = nextSequenceID.add(BigInteger.valueOf(itemBatchSize));
//			clf.local.debug("Type ID '%d' : numSequenceIDs left after allocation sequence is '%d' " , type.intValue(),
//					numSequenceIDs);
		}
		return (retID);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void cacheMoreIDs(int batchSize, int idCacheSize, SequenceDAO sequenceDAO) throws PersistenceException {
		/*
		 * With this call just make sure there are enough IDs in the current
		 * cache for the given batch size.  We aren't interested in using any
		 * IDs at this time.
		 */
		getStartSequenceID(batchSize, idCacheSize, sequenceDAO, false);
	}
}
