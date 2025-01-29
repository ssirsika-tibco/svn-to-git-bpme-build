package com.tibco.bpm.cdm.core.dao;

import com.tibco.bpm.cdm.api.exception.PersistenceException;

/**
 * DAO for arbitrary name/value pairs stored in the database
 * @author smorgan
 * @since 2019
 */
public interface PropertyDAO
{
	public static final String NAME_LAST_AUTO_PURGE_TIME = "lastAutoPurgeTime";

	public String get(String name) throws PersistenceException;

	public void set(String name, String value) throws PersistenceException;
}
