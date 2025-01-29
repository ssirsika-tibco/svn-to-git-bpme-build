package com.tibco.bpm.cdm.core.dao.impl.mssql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.CaseOutOfSyncError;
import com.tibco.bpm.cdm.api.exception.CasedataException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.dao.ConditionRenderer;
import com.tibco.bpm.cdm.core.dao.SimpleSearchRenderer;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.util.TimestampOp;
import com.tibco.bpm.da.dm.api.StructuredType;

/*
 * Implementation specific to MS-SQL database
 */
public class CaseDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.CaseDAOImpl
{

	// Note that we do a secondary sort on id (descending) to discriminate between cases created in the same millisecond.
	private static final String		SQL_GET	 = "SELECT %s FROM cdm_cases_int c%s WHERE c.type_id=?%s "
			+ "%s OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

	private static final String		SQL_GET_WITHOUT_SKIP	= "SELECT %s FROM cdm_cases_int c%s WHERE c.type_id=?%s "
			+ "%s OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";

	private static final String		SQL_GET_WHERE_FRAG_STATE_IS_TERMINAL			= " AND s.is_terminal = 0";

	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	private static final String		SQL_CREATE										= "INSERT INTO cdm_cases_int (version, type_id, casedata, cid, state_id, "
			+ "created_by, modified_by) VALUES "
			+ "( 0, ?, ?, ?, ?, ?, ?)";
	
	private static final String		SQL_UPDATE										= "UPDATE cdm_cases_int SET modified_by=?, casedata=?, version=?, "
			+ "state_id=?, modification_timestamp=? WHERE id=?";

	private static final String		SQL_DELETE										= "DELETE cdm_cases_int FROM cdm_cases_int c "
			+ "INNER JOIN cdm_states s ON c.state_id = s.id "
			+ "INNER JOIN cdm_types t ON t.id = c.type_id "
			+ "INNER JOIN cdm_datamodels d ON d.id = t.datamodel_id "
			+ "WHERE c.type_id = ? AND s.value = ? AND c.modification_timestamp <= ?";
	
	private static final String		SQL_GET_CASEREFS_TOBE_DELETED						= "SELECT c.id, c.version, d.namespace, d.major_version, t.name FROM cdm_cases_int c " + 
			"INNER JOIN cdm_types t ON t.id = c.type_id " + 
			"INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id " + 
			"INNER JOIN cdm_states s ON s.type_id = c.type_id " + 
			"WHERE c.type_id=? AND s.value=? AND c.modification_timestamp <= ?";

	private static final String		SQL_GET_SINGLE_FOR_UPDATE						= "SELECT c.version, c.state_id, s.is_terminal, c.cid FROM cdm_cases_int c "
			+ "INNER JOIN cdm_states s ON c.state_id=s.id "
			+ "WHERE c.id=? AND c.marked_for_purge = 0";

	private static final String		SQL_GET_SINGLE_VERSION							= "SELECT c.version FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id = t.id "
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id = d.id "
			+ "WHERE c.marked_for_purge = 0 and t.name = ? and d.namespace = ? and d.major_version = ?";

	private static final String		SQL_EXISTS										= "SELECT 1 FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id " 
			+ "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "WHERE c.marked_for_purge = 0 AND c.id = ? AND t.name = ? AND d.namespace = ? AND d.major_version = ?";

	/**
	 * Query to obtain cases by a list of reference, along with fragments for dynamically constructing the column list,
	 * depending on what needs to be returned.
	 */
	private static final String		SQL_GET_BY_REFS									= "SELECT %s FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "WHERE c.marked_for_purge = 0 AND %s";


	public static final String		SQL_GET_IS_TERMINAL								= "SELECT is_terminal FROM cdm_states WHERE id IN "
			+ "(SELECT state_id FROM cdm_cases_int WHERE marked_for_purge = 0 AND id = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?)))";

	public static final String		SQL_PRIME_FOR_PURGE								= "SELECT c.id AS id, t.name AS type_name, "
			+ "d.namespace AS namespace, d.major_version AS major_version, c.version AS version FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id = t.id INNER JOIN cdm_datamodels d ON t.datamodel_id = d.id "
			+ "WHERE c.marked_for_purge = 0 AND c.modification_timestamp <= ? AND state_id IN "
			+ "(SELECT id FROM cdm_states WHERE is_terminal = 1 AND type_id = ?)";

	public static final String		SQL_MARK_FOR_PURGE								= "UPDATE cdm_cases_int SET marked_for_purge = 1 WHERE id in (%s)";

	
	public CaseDAOImpl(DataSource dataSource) {
		super(dataSource);
	}
	
    @Override
    public void setSimpleSearchRenderer(SimpleSearchRenderer renderer) {
		simpleSearchRenderer = renderer;
	}
	
    @Override
    public void setConditionRenderer(ConditionRenderer renderer) {
		conditionRenderer = renderer;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#create(java.math.BigInteger, java.util.List, java.lang.String)
	 */
	@Override
	public List<BigInteger> create(BigInteger typeId, List<CaseCreationInfo> infos, String createdBy)
			throws PersistenceException, CasedataException
	{
		super.SQL_CREATE = SQL_CREATE;
		return super.create(typeId, infos, createdBy);
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#exists(com.tibco.bpm.cdm.core.dto.CaseReference)
	 */
	@Override
	public boolean exists(CaseReference ref) throws PersistenceException, ReferenceException
	{
		super.SQL_EXISTS = SQL_EXISTS;
		return super.exists(ref);
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#primeForPurge(java.math.BigInteger, java.util.Calendar)
	 */
	@Override
	public List<CaseReference> primeForPurge(BigInteger typeId, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException
	{
		super.SQL_PRIME_FOR_PURGE = SQL_PRIME_FOR_PURGE;
		return super.primeForPurge(typeId, maxModificationTimestamp);
		
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#read(java.math.BigInteger, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.Calendar, java.lang.String, com.tibco.bpm.cdm.core.search.dto.SearchConditionDTO, com.tibco.bpm.da.dm.api.StructuredType, com.tibco.bpm.cdm.core.aspect.CaseAspectSelection)
	 */
	@Override
    public List<CaseInfoDTO> read(BigInteger typeId,
            Integer skip, Integer top, String cid, String stateValue,
            Calendar maxModificationTimestamp, TimestampOp opr, String search,
            SearchConditionDTO condition, StructuredType st,
			CaseAspectSelection aspectSelection, boolean excludeTerminalState) throws PersistenceException
	{
		super.SQL_GET_WHERE_FRAG_STATE_IS_TERMINAL = SQL_GET_WHERE_FRAG_STATE_IS_TERMINAL;
		super.SQL_GET_WITHOUT_SKIP = SQL_GET_WITHOUT_SKIP;
		super.SQL_GET = SQL_GET;
		return super.read(typeId, skip, top, cid, stateValue, maxModificationTimestamp, opr, search, condition, st, aspectSelection, excludeTerminalState);
	}
	
    protected String getSQLAfterSubstitution(Integer skip, String columnClause, boolean excludeTerminalState, String queryFragment, String orderByClause) {
    	
    	String sql = skip == null ? SQL_GET_WITHOUT_SKIP : SQL_GET;
    	sql = String.format(sql, columnClause, excludeTerminalState ? SQL_GET_JOIN_STATES : "",
					queryFragment, orderByClause);
    	return sql;   	
    }  
    
    /* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#delete(com.tibco.bpm.cdm.core.dto.CaseReference)
	 */
    public void delete(CaseReference ref) throws PersistenceException, ReferenceException
	{
    	super.SQL_GET_SINGLE_VERSION = SQL_GET_SINGLE_VERSION; 
    	super.delete(ref);
	}
    
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#delete(java.math.BigInteger, java.lang.String, java.util.Calendar)
	 */
    public List<CaseReference> delete(BigInteger typeId, String stateValue, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException
	{
		List<CaseReference> tobeDeletedRefs = new ArrayList<CaseReference>();
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			// First retrieve the case refs being deleted here as MS SQL delete query won't return the resultset.
			tobeDeletedRefs = getCasesByTypeStateTimestamp(typeId, stateValue, maxModificationTimestamp);
			
			conn = getConnection();			
	
			ps = conn.prepareStatement(SQL_DELETE);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(2, stateValue);
			ps.setTimestamp(3, new Timestamp(maxModificationTimestamp.getTimeInMillis()));
			int success = ps.executeUpdate();

			if (success > 0)
			{
				return tobeDeletedRefs;
			} else {
				return new ArrayList<CaseReference>();
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}
    
    private List<CaseReference> getCaseRefsToBeDeleted(BigInteger typeId, String stateValue, Calendar maxModificationTimestamp) 
    		throws PersistenceException, ReferenceException 
    {
    	
    	List<CaseReference> tobeDeletedRefs = new ArrayList<>();

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			
			ps = conn.prepareStatement(SQL_GET_CASEREFS_TOBE_DELETED);
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(2, stateValue);
			ps.setTimestamp(3, new Timestamp(maxModificationTimestamp.getTimeInMillis()));
			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					BigInteger id = rset.getBigDecimal(1).toBigInteger();
					int version = rset.getInt(2);
					String namespace = rset.getString(3);
					int majorVersion = rset.getInt(4);
					String name = rset.getString(5);
					CaseReference ref = new CaseReference(new QualifiedTypeName(namespace, name), majorVersion, id,
							version);
					tobeDeletedRefs.add(ref);
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}    	
    	return tobeDeletedRefs;
    }
    
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#update(java.util.List, java.lang.String)
	 */
	@Override
	public void update(List<CaseUpdateDTO> cases, String modifiedBy)
			throws PersistenceException, ReferenceException, CasedataException
	{
		// Create a copy of the DTO list, sorted by id. This allows us
		// to avoid deadlocks by ensuring that all threads lock cases in the
		// same order.  The order of the original list must not be altered, as
		// we are contracted to maintain this for the purpose of returning updated
		// case references to the caller.
		List<CaseUpdateDTO> sortedDTOs = new ArrayList<>();
		sortedDTOs.addAll(cases);
		sortedDTOs.sort((dto1, dto2) -> {
			return dto1.getCaseReference().getId().compareTo(dto2.getCaseReference().getId());
		});

		Connection conn = getConnection();

		Statement ts = null;
		PreparedStatement ps = null;
		try
		{

			// Verify existence of cases, check version numbers and lock
			ps = conn.prepareStatement(SQL_GET_SINGLE_FOR_UPDATE);

			for (CaseUpdateDTO dto : sortedDTOs)
			{
				// Fetch
				CaseReference ref = dto.getCaseReference();
				ps.setBigDecimal(1, new BigDecimal(ref.getId()));
				ps.execute();

				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					// Check version 
					int actualVersion = rset.getInt("version");
					if (actualVersion != ref.getVersion())
					{
						throw CaseOutOfSyncError.newVersionMismatch(ref.getVersion(), actualVersion,
								ref.toString());
					}
					boolean inTerminalState = rset.getInt("is_terminal") == 1 ? true : false;
					if (inTerminalState)
					{
						throw ReferenceException.newTerminalStatePreventsUpdate(ref.toString());
					}
					BigInteger oldStateId = rset.getBigDecimal("state_id").toBigInteger();
					dto.setOldStateId(oldStateId);
					String oldCID = rset.getString("cid");
					String newCID = dto.getNewCID();
					if (oldCID != null && !oldCID.equals(newCID))
					{
						throw CasedataException.newCIDChanged(oldCID, newCID);
					}
				}
				else
				{
					throw ReferenceException.newNotExist(ref.toString());
				}
			}

			// Now we've established that the input is valid, go ahead and
			// perform the updates.

			// Create a prepared statement, which will be executed once for each case.
			ps = conn.prepareStatement(SQL_UPDATE);
			ps.setString(1, modifiedBy);

			for (CaseUpdateDTO dto : sortedDTOs)
			{
				CaseReference ref = dto.getCaseReference();
				CaseReference newRef = ref.duplicate();
				// Set the new ref (with bumped version) in the DTO, as the caller will use this to produce updated refs.
				int newVersion = ref.getVersion() + 1;
				newRef.setVersion(newVersion);
				dto.setNewCaseReference(newRef);
				// Update casedata and version+1
				
				java.sql.Timestamp stamp = new java.sql.Timestamp(System.currentTimeMillis());
				ps.setString(2, dto.getCasedata());
				ps.setInt(3, newVersion);
				ps.setBigDecimal(4, new BigDecimal(dto.getNewStateId()));
				ps.setTimestamp(5, stamp);
				ps.setBigDecimal(6, new BigDecimal(ref.getId()));
				ps.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}


	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#getIsTerminalState(com.tibco.bpm.cdm.core.dto.CaseReference)
	 */
    public Boolean getIsTerminalState(CaseReference ref) throws PersistenceException
	{
		Boolean result = null;
		BigInteger id = ref.getId();
		QualifiedTypeName qName = ref.getQualifiedTypeName();
		String namespace = qName.getNamespace();
		int majorVersion = ref.getApplicationMajorVersion();
		String name = qName.getName();

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_GET_IS_TERMINAL);
			ps.setBigDecimal(1, new BigDecimal(id));
			ps.setString(2, name);
			ps.setString(3, namespace);
			ps.setInt(4, majorVersion);
			ps.execute();
			ResultSet rset = ps.getResultSet();
			if (rset.next())
			{
				result = rset.getInt(1) == 1 ? true : false;
			}
			return result;
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}
	

	private String buildGetQuerySQL(boolean includeCasedata, boolean includeMetadata, boolean isMulti,
			int multiQuantity)
	{
		// Build a list of the column name fragments, 
		// according to what we've been asked to return.
		List<String> cols = new ArrayList<>();
		cols.add(SQL_GET_BY_REFS_COLS_ID);
		cols.add(SQL_GET_BY_REFS_COLS_TYPE);
		if (includeCasedata)
		{
			cols.add(SQL_GET_BY_REFS_COLS_CASEDATA);
		}
		if (includeMetadata)
		{
			cols.add(SQL_GET_BY_REFS_COLS_METADATA);
		}
		String columnClause = String.join(", ", cols);
		String whereClause;
		if (isMulti)
		{
			// id in(?, ?, ?, ...)
			String bindVariablePlaceholders = renderCommaSeparatedQuestionMarks(multiQuantity);
			whereClause = String.format(SQL_GET_BY_REFS_WHERE_MULTI_REFS, bindVariablePlaceholders);
		}
		else
		{
			// id = ?
			whereClause = SQL_GET_BY_REFS_WHERE_SINGLE_REF;
		}
		String sql = String.format(SQL_GET_BY_REFS, columnClause, whereClause);
		return sql;
	}

	private CaseInfoDTO getCaseInfoFromGetResultSet(boolean includeCasedata, boolean includeMetadata, ResultSet rset)
			throws SQLException
	{
		CaseInfoDTO dto = new CaseInfoDTO();
		BigInteger id = rset.getBigDecimal("id").toBigInteger();
		dto.setId(id);
		dto.setVersion(rset.getInt("version"));
		if (includeCasedata)
		{
			dto.setCasedata(rset.getString("casedata"));
		}
		if (includeMetadata)
		{
			dto.setCreatedBy(rset.getString("created_by"));
			Timestamp creationTimestamp = rset.getTimestamp("creation_timestamp");
			Calendar creationCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			creationCalendar.setTimeInMillis(creationTimestamp.getTime());
			dto.setCreationTimestamp(creationCalendar);

			dto.setModifiedBy(rset.getString("modified_by"));
			Timestamp modificationTimestamp = rset.getTimestamp("modification_timestamp");
			Calendar modificationCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			modificationCalendar.setTimeInMillis(modificationTimestamp.getTime());
			dto.setModificationTimestamp(modificationCalendar);
		}
		dto.setTypeName(new QualifiedTypeName(rset.getString("namespace"), rset.getString("type_name")));
		dto.setMajorVersion(rset.getInt("major_version"));
		return dto;
	}




    
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#read(com.tibco.bpm.cdm.core.dto.CaseReference, com.tibco.bpm.cdm.core.aspect.CaseAspectSelection)
	 */
    public CaseInfoDTO read(CaseReference ref, CaseAspectSelection aspectSelection)
			throws PersistenceException, ReferenceException
	{
		// Determine what we should return, based on the aspect selection.
		// Casedata is required for both the casedata and summary aspect (given the latter is derived from
		// the casedata at a higher level).
		boolean includeCasedata = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_CASEDATA,
				CaseAspectSelection.ASPECT_SUMMARY);
		boolean includeMetadata = aspectSelection
				.includesOrIncludesSubAspectsOfOrIsNothing(CaseAspectSelection.ASPECT_METADATA);

		String sql = buildGetQuerySQL(includeCasedata, includeMetadata, false, 0);
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		CaseInfoDTO dto = null;
		try
		{
			conn = getConnection();
		
			ps = conn.prepareStatement(sql);
			ps.setBigDecimal(1, new BigDecimal(ref.getId()));
			QualifiedTypeName qType = ref.getQualifiedTypeName();
			ps.setString(2, qType.getNamespace());
			ps.setInt(3, ref.getApplicationMajorVersion());
			ps.setString(4, qType.getName());
			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					// Found it. Otherwise we'll return null.
					dto = getCaseInfoFromGetResultSet(includeCasedata, includeMetadata, rset);
				}
			}
			return dto;
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#read(java.util.List, com.tibco.bpm.cdm.core.aspect.CaseAspectSelection)
	 */
    public List<CaseInfoDTO> read(List<CaseReference> caseReferences, CaseAspectSelection aspectSelection)
			throws PersistenceException, ReferenceException
	{
		List<CaseInfoDTO> infos = new ArrayList<>();
		// Determine what we should return, based on the aspect selection.
		// Casedata is required for both the casedata and summary aspect (given the latter is derived from
		// the casedata at a higher level).
		boolean includeCasedata = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_CASEDATA,
				CaseAspectSelection.ASPECT_SUMMARY);
		boolean includeMetadata = aspectSelection
				.includesOrIncludesSubAspectsOfOrIsNothing(CaseAspectSelection.ASPECT_METADATA);

		String sql = buildGetQuerySQL(includeCasedata, includeMetadata, true, caseReferences.size());

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
		
			ps = conn.prepareStatement(sql);
			int idx = 1;
			for (CaseReference ref : caseReferences)
			{
				ps.setBigDecimal(idx++, new BigDecimal(ref.getId()));
			}
			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					CaseInfoDTO dto = getCaseInfoFromGetResultSet(includeCasedata, includeMetadata, rset);
					infos.add(dto);
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}

		List<CaseInfoDTO> result = new ArrayList<>();
		for (CaseReference ref : caseReferences)
		{
			BigInteger refId = ref.getId();
			QualifiedTypeName refQName = ref.getQualifiedTypeName();
			String refNamespace = refQName.getNamespace();
			String refName = refQName.getName();
			int refMajorVersion = ref.getApplicationMajorVersion();

			// Attempt to find a matching case in what the DAO returned
			CaseInfoDTO info = infos.stream().filter(i -> i.getId().equals(refId)).findFirst().orElse(null);

			CaseInfoDTO infoToAdd = null;
			if (info != null)
			{
				// The id is correct, but we need to check the type too
				QualifiedTypeName infoQName = info.getTypeName();
				if (infoQName.getNamespace().equals(refNamespace) && infoQName.getName().equals(refName)
						&& info.getMajorVersion() == refMajorVersion)
				{
					// The id and type match, so its a valid match
					infoToAdd = info;
				}
			}

			if (infoToAdd == null)
			{
				// An info that matches the reference was not found
				throw ReferenceException.newNotExist(ref.toString());
			}
			result.add(infoToAdd);
		}

		return result;
	}  

    @Override
    public void markForPurge(List<CaseReference> refs) throws 			         PersistenceException
    {
        super.SQL_MARK_FOR_PURGE = SQL_MARK_FOR_PURGE;
        super.markForPurge(refs);
    
    }
    
    private String escapeLeftBracket(String str) {
        int i = 0;
        int pos = 0;
        StringBuffer buff = new StringBuffer();

        while (str.indexOf("[", i) != -1) {
            pos = str.indexOf("[", i);
            buff.append(str.substring(i, pos));
            buff.append(str.substring(pos, pos + 1));
            buff.append("[]");
            i = pos + 1;
        }
        buff.append(str.substring(i));
        return buff.toString();
    }

}
