package com.tibco.bpm.cdm.core.dao.impl.db2;



import java.math.BigInteger;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * IBM DB2 implementation of the LinkDAO interface that persists link
 * definitions in the cdm_links database table. Reusing base implementation.
 * 
 * @author spanse
 * @since 20-Dec-2021
 */
public class LinkDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.LinkDAOImpl
{

    private static final String SQL_CREATE =
            "INSERT INTO cdm_links (end1_owner_id, end1_name, end1_is_array, end2_owner_id, end2_name, end2_is_array) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

	public LinkDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
    @Override
    public void create(DataModel dm, Map<String, BigInteger> typeNameToIdMap)
            throws PersistenceException, InternalException {
        super.SQL_CREATE = SQL_CREATE;
        super.create(dm, typeNameToIdMap);

    }
}
