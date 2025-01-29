package com.tibco.bpm.cdm.core.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;

/**
 * Renders an SQL where clause fragment from a SearchConditionDTO and applies parameters
 * to a corresponding PreparedStatement.
 * 
 * @author smorgan
 * @since 2019
 */
public interface ConditionRenderer
{
    final String SQL_GET_CONDITION_AND_DELIM = " AND "; //$NON-NLS-1$

    final String SQL_GET_CONDITION_OR_DELIM = " OR "; //$NON-NLS-1$

    final String PRECEED_PARENTHESES = " ( "; //$NON-NLS-1$

    final String SUCCEED_PARENTHESES = " ) "; //$NON-NLS-1$

    /**
	 * Generates an SQL where clause fragment representing the supplied search condition
     * @param dto
     * @return
     */
	public String render(SearchConditionDTO dto);
	
    /**
	 * Generates an SQL fragment representing the 'order by' clause supplied in search condition
     * @param dto
     * @return
     */
	public String renderOrderBy(SearchConditionDTO dto);

	/**
	 * Assuming the given PreparedStatement contains the SQL returned from {@link #render(SearchConditionDTO)} with
	 * the first corresponding parameter starting at position idx, sets the parameters and returns the index
	 * value for the next parameter.
	 * 
	 * @param condition
	 * @param ps
	 * @param idx
	 * @return
	 * @throws SQLException
	 */
	public int setParameters(SearchConditionDTO condition, PreparedStatement ps, int idx) throws SQLException;
}
