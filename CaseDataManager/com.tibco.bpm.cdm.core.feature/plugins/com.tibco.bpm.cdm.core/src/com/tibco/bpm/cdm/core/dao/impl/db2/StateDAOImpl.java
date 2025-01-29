package com.tibco.bpm.cdm.core.dao.impl.db2;



import java.math.BigInteger;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.da.dm.api.DataModel;


/**
 * IBM DB2 implementation of the StateDAO interface that persists states in the
 * cdm_states database table. Reusing the base implementation.
 * 
 * @author spanse
 * @since 20-Dec-2021
 */
public class StateDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.StateDAOImpl 
{

    protected String SQL_CREATE =
            "INSERT INTO cdm_states (type_id, value, label, is_terminal) "
                    + "VALUES (?, ?, ?, ?)";

	public StateDAOImpl(DataSource dataSource) {
		super(dataSource);
	}

    @Override
    public void create(DataModel dm, Map<String, BigInteger> typeNameToIdMap)
            throws PersistenceException {

        super.SQL_CREATE = SQL_CREATE;
        super.create(dm, typeNameToIdMap);

    }

    @Override
    public void update(BigInteger id, DataModel oldDataModel, DataModel dm,
            Map<String, BigInteger> typeNameToIdMap)
            throws PersistenceException {

        super.SQL_CREATE = SQL_CREATE;
        super.update(id, oldDataModel, dm, typeNameToIdMap);
    }
}
