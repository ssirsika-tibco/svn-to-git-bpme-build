package com.tibco.bpm.cdm.core.dao.impl.mssql;



import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.PersistenceException;

/*
 * Implementation for DAO for obtaining values for auto-generated case identifiers
 *
 * Implementation specific to MS-SQL database
 */
public class IdentifierValueDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.IdentifierValueDAOImpl 
{

    protected String SQL_GET_IDENTIFIER =
            "SELECT next_num, prefix, suffix, min_num_length "
			+ "FROM cdm_identifier_infos WHERE type_id=?";

    protected String SQL_UPDATE_IDENTIFIER =
            "UPDATE cdm_identifier_infos SET next_num=? WHERE type_id=?";

	public IdentifierValueDAOImpl(DataSource dataSource) {
		super(dataSource);
	}


    @Override
    public List<String> getIdentifierValues(BigInteger typeId,
            int quantity) throws PersistenceException {

        PreparedStatement ps = null;
        PreparedStatement psForSeparateUpdate = null;

        Connection conn = null;
        List<String> result = new ArrayList<>();
        Statement ts = null;
        BigDecimal bdTypeId = new BigDecimal(typeId);
        BigInteger currentIdNumber = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(SQL_GET_IDENTIFIER);
            ps.setBigDecimal(1, bdTypeId);
            ResultSet resultSet = ps.executeQuery();

            if (resultSet.next()) {
                // Construct the identifier string
                BigInteger firstIdNumber =
                        resultSet.getBigDecimal(1).toBigInteger();
                String prefix = resultSet.getString(2);
                String suffix = resultSet.getString(3);
                Integer minNumLength = resultSet.getInt(4);

                // Generate ids starting at the first number and continuing to
                // produce the required quantity.
                currentIdNumber = firstIdNumber;
                for (int i = 0; i < quantity; i++) {
                    result.add(bakeIdentifier(prefix,
                            currentIdNumber,
                            suffix,
                            minNumLength));
                    currentIdNumber = currentIdNumber.add(BigInteger.ONE);
                }
            }
        } catch (SQLException e) {
            throw PersistenceException.newRepositoryProblem(e);
        } finally {
            cleanUp(ts, ps, conn);
        }

        try {
            conn = getConnection();
            boolean autoCommitFlag = conn.getAutoCommit();
            if (!autoCommitFlag) {
				// Set the auto commit flag for the update to true.
                conn.setAutoCommit(true);
            }
            psForSeparateUpdate = conn.prepareStatement(SQL_UPDATE_IDENTIFIER);

            BigInteger nextIdNumber = currentIdNumber;
            psForSeparateUpdate.setBigDecimal(1, new BigDecimal(nextIdNumber));
            psForSeparateUpdate.setBigDecimal(2, bdTypeId);
            int updateStatus = psForSeparateUpdate.executeUpdate();
			// Restore the auto commit flag to its original state.
            conn.setAutoCommit(autoCommitFlag);
        } catch (SQLException e) {
            throw PersistenceException.newRepositoryProblem(e);
        } finally {
            cleanUp(ts, psForSeparateUpdate, conn);
        }

        return result;
    }

}
