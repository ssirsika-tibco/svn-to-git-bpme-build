package com.tibco.bpm.cdm.core.dao.impl.db2;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO;
import com.tibco.bpm.da.dm.api.DataModel;

/**
 * IBM DB2 implementation of the TypelDAO interface that persists types in the
 * cdm_types database table. Reusing the implementation for the oracle database.
 * 
 * @author spanse
 * @since 01-Dec-2021
 */
public class TypeDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.TypeDAOImpl
{
    private static final String SQL_CREATE =
            "INSERT INTO cdm_types (datamodel_id, name, is_case) "
                    + "VALUES (?, ?, ?)";

    private static final String SQL_GET =
            "SELECT b.* FROM (SELECT a.*, row_number() over() rn FROM ("
                    + "SELECT t.name as name, t.is_case as is_case, d.id as datamodel_id, d.namespace as namespace, "
                    + "d.major_version as major_version, a.application_id as application_id FROM cdm_types t "
                    + "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
                    + "INNER JOIN cdm_applications a ON d.application_id=a.id %s "
                    + "ORDER BY a.application_id, d.major_version, d.namespace, t.name) AS a) "
                    + "AS b WHERE rn BETWEEN ? AND ?";

    private static final String SQL_GET_NO_SKIP =
            "SELECT b.* FROM (SELECT a.*, row_number() over() rn FROM ("
                    + "SELECT t.name as name, t.is_case as is_case, d.id as datamodel_id, d.namespace as namespace, "
                    + "d.major_version as major_version, a.application_id as application_id FROM cdm_types t "
                    + "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
                    + "INNER JOIN cdm_applications a ON d.application_id=a.id %s "
                    + "ORDER BY a.application_id, d.major_version, d.namespace, t.name) AS a) "
                    + "AS b WHERE rn BETWEEN 1 AND ?";
	
	public TypeDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
    @Override
    public Map<String, BigInteger> create(BigInteger dataModelId,
            DataModel dataModel) throws PersistenceException {

        super.SQL_CREATE = SQL_CREATE;
        return super.create(dataModelId, dataModel);
    }

    @Override
    public List<TypeInfoDTO> getTypes(String applicationId, String namespace,
            Integer majorVersion, Boolean isCase, Integer skip, Integer top)
            throws PersistenceException {

        super.SQL_GET = SQL_GET;
        super.SQL_GET_NO_SKIP = SQL_GET_NO_SKIP;
        return super.getTypes(applicationId,
                namespace,
                majorVersion,
                isCase,
                skip,
                top);
    }

    @Override
    public Map<String, BigInteger> update(BigInteger dataModelId,
            DataModel oldDataModel, DataModel newDataModel)
            throws PersistenceException {

        super.SQL_CREATE = SQL_CREATE;
        return super.update(dataModelId, oldDataModel, newDataModel);

    }

}
