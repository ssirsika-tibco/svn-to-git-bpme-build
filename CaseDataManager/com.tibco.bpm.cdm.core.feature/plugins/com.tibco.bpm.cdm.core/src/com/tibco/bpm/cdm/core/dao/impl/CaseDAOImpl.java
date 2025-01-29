package com.tibco.bpm.cdm.core.dao.impl;

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

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.CaseOutOfSyncError;
import com.tibco.bpm.cdm.api.exception.CasedataException;
import com.tibco.bpm.cdm.api.exception.NonUniqueCaseIdentifierError;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.aspect.CaseAspectSelection;
import com.tibco.bpm.cdm.core.dao.CaseDAO;
import com.tibco.bpm.cdm.core.dao.ConditionRenderer;
import com.tibco.bpm.cdm.core.dao.SimpleSearchRenderer;
import com.tibco.bpm.cdm.core.dto.CaseInfoDTO;
import com.tibco.bpm.cdm.core.dto.CaseUpdateDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.cdm.util.TimestampOp;
import com.tibco.bpm.da.dm.api.StructuredType;

public class CaseDAOImpl extends AbstractDAOImpl implements CaseDAO
{
	private static final String		UNIQUE_CONSTRAINT_NAME							= "cdm_cases_int_unq";

	private static final String		SQLSTATE_UNIQUE_VIOLATION						= "23505";

	// Note that we do a secondary sort on id (descending) to discriminate between cases created in the same millisecond.
	protected String				SQL_GET											= "SELECT b.* FROM (SELECT a.*, row_number() over() FROM ("
			+ "SELECT %s FROM cdm_cases_int c%s WHERE c.type_id=?%s "
			+ "%s) AS a) AS b WHERE row_number > ? LIMIT ?";

	protected String				SQL_GET_WITHOUT_SKIP							= "SELECT %s FROM cdm_cases_int c%s WHERE c.type_id=?%s "
			+ "%s LIMIT ?";

	private static final String		SQL_DEFAULT_ORDER_BY_FRAG						= "ORDER BY modification_timestamp DESC, c.id DESC";
	
	protected static final String	SQL_GET_JOIN_STATES								= " INNER JOIN cdm_states s ON s.id=c.state_id";

	private static final String		SQL_GET_COLS_REF								= "c.id, version";

	private static final String		SQL_GET_COLS_CASEDATA							= "casedata";

	private static final String		SQL_GET_COLS_METADATA							= "created_by, creation_timestamp, modified_by, modification_timestamp";

	protected String				SQL_GET_WHERE_FRAG_STATE_IS_TERMINAL			= " AND s.is_terminal = FALSE";

	private static final String		SQL_GET_WHERE_FRAG_CID							= " AND cid=?";

	private static final String		SQL_GET_WHERE_FRAG_STATE_VALUE					= " AND state_id IN (SELECT id FROM cdm_states WHERE value=? "
			+ "AND type_id=?)";

	private static final String		SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP = " AND modification_timestamp<=?";
	private static final String		SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP_GT = " AND modification_timestamp>?";
	private static final String		SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP_EQ	= " AND modification_timestamp=?";

	// Fragment for wrapping the SQL for a DQL query
	private static final String		SQL_GET_WHERE_FRAG_CONDITION_TEMPLATE			= " AND (%s)";

	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	protected String		SQL_CREATE										= "INSERT INTO cdm_cases_int (id, version, type_id, casedata, cid, state_id, "
			+ "created_by, creation_timestamp, modified_by, modification_timestamp) VALUES "
			+ "(NEXTVAL('cdm_cases_int_seq'), 0, ?, ?::jsonb, ?, ?, ?, DATE_TRUNC('milliseconds',NOW()), ?, DATE_TRUNC('milliseconds',NOW()))";

	protected String		SQL_GET_SINGLE_FOR_UPDATE						= "SELECT c.version, c.state_id, s.is_terminal, c.cid FROM cdm_cases_int c INNER JOIN "
			+ "cdm_states s ON c.state_id=s.id WHERE c.id=? AND c.marked_for_purge = FALSE FOR UPDATE OF c";

	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	public static final String		SQL_UPDATE										= "UPDATE cdm_cases_int SET modified_by=?, casedata=?::jsonb, version=?, "
			+ "state_id=?, modification_timestamp=? WHERE id=?";

	protected String		SQL_GET_SINGLE_VERSION							= "SELECT version FROM cdm_cases_int WHERE marked_for_purge = FALSE AND id = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?))";

	public static final String		SQL_DELETE_SINGLE								= "DELETE FROM cdm_cases_int WHERE id = ? AND version = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?))";

	// Delete cases that match the given type/state/modificationTimestamp constraints, returning what's necessary to 
	// construct a list of case references of what has been deleted.
	public static final String		SQL_DELETE										= "DELETE FROM cdm_cases_int c USING cdm_types t, cdm_datamodels d "
			+ "WHERE c.type_id=t.id AND t.datamodel_id=d.id "
			+ "AND c.type_id=? AND c.state_id IN (SELECT id FROM cdm_states WHERE value=? AND type_id=c.type_id) AND c.modification_timestamp<=? " ;
			// + "RETURNING c.id, c.version, d.namespace, d.major_version, t.name";

	public static final String		SQL_COUNT_BY_DEPLOYMENT_ID						= "SELECT COUNT(*) FROM cdm_cases_int WHERE type_id IN "
			+ "(SELECT id FROM cdm_types WHERE datamodel_id IN (SELECT id FROM cdm_datamodels WHERE application_id IN "
			+ "(SELECT id FROM cdm_applications WHERE deployment_id = ?)))";

	protected String		SQL_EXISTS										= "SELECT 1 FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ " WHERE c.marked_for_purge = FALSE AND c.id = ? AND t.name = ? AND d.namespace = ? AND d.major_version = ?";

	/**
	 * Query to obtain cases by a list of reference, along with fragments for dynamically constructing the column list,
	 * depending on what needs to be returned.
	 */
	public static final String		SQL_GET_BY_REFS									= "SELECT %s FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "WHERE c.marked_for_purge = FALSE AND %s";

	// Used in the WHERE clause when fetching multiple cases by ref
	protected static final String		SQL_GET_BY_REFS_WHERE_MULTI_REFS				= "c.id IN (%s)";

	// Used in the WHERE clause when fetching a single case by ref
	protected static final String		SQL_GET_BY_REFS_WHERE_SINGLE_REF				= "c.id = ? AND d.namespace = ? AND d.major_version = ? AND t.name = ?";

	protected static final String		SQL_GET_BY_REFS_COLS_ID							= "c.id as id, c.version as version";

	protected static final String		SQL_GET_BY_REFS_COLS_CASEDATA					= "c.casedata as casedata";

	protected static final String		SQL_GET_BY_REFS_COLS_METADATA					= "c.created_by as created_by, "
			+ "c.creation_timestamp as creation_timestamp, c.modified_by as modified_by, "
			+ "c.modification_timestamp as modification_timestamp";

	protected static final String		SQL_GET_BY_REFS_COLS_TYPE						= "d.namespace as namespace, "
			+ "d.major_version as major_version, t.name as type_name";

	public static final String		SQL_GET_IS_TERMINAL								= "SELECT is_terminal FROM cdm_states WHERE id IN "
			+ "(SELECT state_id FROM cdm_cases_int WHERE marked_for_purge = FALSE AND id = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?)))";

	protected String		SQL_PRIME_FOR_PURGE								= "SELECT c.id AS id, t.name AS type_name, "
			+ "d.namespace AS namespace, d.major_version AS major_version, c.version AS version FROM cdm_cases_int c "
			+ "INNER JOIN cdm_types t ON c.type_id = t.id INNER JOIN cdm_datamodels d ON t.datamodel_id = d.id "
			+ "WHERE c.marked_for_purge = FALSE AND c.modification_timestamp <= ? AND state_id IN "
			+ "(SELECT id FROM cdm_states WHERE is_terminal = TRUE AND type_id = ?)";

	protected String		SQL_MARK_FOR_PURGE								= "UPDATE cdm_cases_int SET marked_for_purge = TRUE WHERE id in (%s)";

    public static final String SQL_GET_CASES_BY_TYPE_STATE_TIMESTAMP =
            "SELECT c.id, c.version, d.namespace, d.major_version, t.name from cdm_cases_int c, cdm_types t, cdm_datamodels d "
                    + "WHERE c.type_id=t.id AND t.datamodel_id=d.id "
                    + "AND c.type_id=? AND c.state_id IN (SELECT id FROM cdm_states WHERE value=? AND type_id=c.type_id) AND c.modification_timestamp<=? ";

    public static final String SQL_GET_LINKED_CASES_BY_END1 =
            "SELECT l.end1_name, c.id, d.namespace, t.name, d.major_version, c.version FROM cdm_case_links cl "
                    + "INNER JOIN cdm_links l ON cl.link_id=l.id "
                    + "INNER JOIN cdm_cases_int c ON c.id=cl.end2_id "
                    + "INNER JOIN cdm_types t ON c.type_id=t.id "
                    + "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
                    + "WHERE cl.end1_id = ? ORDER BY l.end1_name, c.id ";

    public static final String SQL_GET_LINKED_CASES_BY_END2 =
            "SELECT l.end2_name, c.id, d.namespace, t.name, d.major_version, c.version FROM cdm_case_links cl "
                    + "INNER JOIN cdm_links l ON cl.link_id=l.id "
                    + "INNER JOIN cdm_cases_int c ON c.id=cl.end1_id "
                    + "INNER JOIN cdm_types t ON c.type_id=t.id "
                    + "INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
                    + "WHERE cl.end2_id = ? ORDER BY l.end2_name, c.id ";


	protected SimpleSearchRenderer	simpleSearchRenderer;

	protected ConditionRenderer		conditionRenderer;

	public CaseDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
    public void setSimpleSearchRenderer(SimpleSearchRenderer renderer) {
		simpleSearchRenderer = renderer;
	}
	
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
		List<BigInteger> ids = new ArrayList<>();
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		Connection conn = null;
		String cid = null;
		try
		{
			conn = getConnection();


			ps = conn.prepareStatement(SQL_CREATE, new String[]{"id"});
			ps.setBigDecimal(1, new BigDecimal(typeId));
			ps.setString(5, createdBy);
			ps.setString(6, createdBy);

			for (CaseCreationInfo info : infos)
			{
				ps.setObject(2, info.getCasedata());
				// Get CID into a variable as we may need it in a non-unique exception.
				cid = info.getCid();
				ps.setString(3, cid);
				ps.setBigDecimal(4, new BigDecimal(info.getStateId()));
				int success = ps.executeUpdate();

				if (success == 1) {
					ResultSet rset = ps.getGeneratedKeys();
					if (rset.next())
					{
						id = rset.getBigDecimal(1).toBigInteger();
						ids.add(id);
					}
				}
			}
		}
		catch (SQLException e)
		{
			if (e.getMessage().startsWith("Violation of UNIQUE KEY constraint")) {
				// Applicable to MS SQL database server
				throw NonUniqueCaseIdentifierError.newNonUniqueCID(cid);
			}
			if (e instanceof java.sql.SQLIntegrityConstraintViolationException) {
				// Applicable to Oracle database server
				throw NonUniqueCaseIdentifierError.newNonUniqueCID(cid);
			} 		
			else if (e instanceof PSQLException)
			{
				PSQLException pe = (PSQLException) e;

				if (SQLSTATE_UNIQUE_VIOLATION.equals(e.getSQLState()))
				{
					ServerErrorMessage serverErrorMessage = pe.getServerErrorMessage();
					String constraint = serverErrorMessage.getConstraint();
					if (UNIQUE_CONSTRAINT_NAME.contentEquals(constraint))
					{
						// This is caused by a duplicate CID, which is an expected
						// result when the caller provides a CID that collides with
						// an existing case.  This causes a specific failure, rather
						// than a persistence exception.
						throw NonUniqueCaseIdentifierError.newNonUniqueCID(cid);
					}
				}
			}
			
			// Failure was _not_ due to a duplicate CID.
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
		return ids;
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
;

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
					boolean inTerminalState = rset.getBoolean("is_terminal");
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

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#read(java.math.BigInteger, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.Calendar, java.lang.String, com.tibco.bpm.cdm.core.search.dto.SearchConditionDTO, com.tibco.bpm.da.dm.api.StructuredType, com.tibco.bpm.cdm.core.aspect.CaseAspectSelection)
	 */
	@Override
	public List<CaseInfoDTO> read(BigInteger typeId, Integer skip, Integer top, String cid, String stateValue,
			Calendar maxModificationTimestamp, TimestampOp opr, String search, SearchConditionDTO condition, StructuredType st,
			CaseAspectSelection aspectSelection, boolean excludeTerminalState) throws PersistenceException
	{
		List<CaseInfoDTO> result = new ArrayList<>();

		// Determine what we should return, based on the aspect selection.
		boolean includeCaseReference = aspectSelection.includesOrIsNothing(CaseAspectSelection.ASPECT_CASE_REFERENCE);
		// Casedata is required for both the casedata and summary aspect (given the latter is derived from
		// the casedata at a higher level).
		boolean includeCasedata = aspectSelection.includesAnyOrIsNothing(CaseAspectSelection.ASPECT_CASEDATA,
				CaseAspectSelection.ASPECT_SUMMARY);
		boolean includeMetadata = aspectSelection
				.includesOrIncludesSubAspectsOfOrIsNothing(CaseAspectSelection.ASPECT_METADATA);

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			

			String sql = skip == null ? SQL_GET_WITHOUT_SKIP : SQL_GET;

			// Build a list of the column name fragments, 
			// according to what we've been asked to return.
			List<String> cols = new ArrayList<>();
			if (includeCaseReference)
			{
				cols.add(SQL_GET_COLS_REF);
			}
			if (includeCasedata)
			{
				cols.add(SQL_GET_COLS_CASEDATA);
			}
			if (includeMetadata)
			{
				cols.add(SQL_GET_COLS_METADATA);
			}
			String columnClause = String.join(", ", cols);

			// Determine if extra conditions are needed in the where clause
			StringBuilder buf = new StringBuilder();
			if (excludeTerminalState)
			{
				buf.append(SQL_GET_WHERE_FRAG_STATE_IS_TERMINAL);
			}
			if (cid != null)
			{
				buf.append(SQL_GET_WHERE_FRAG_CID);
			}
			if (stateValue != null)
			{
				buf.append(SQL_GET_WHERE_FRAG_STATE_VALUE);
			}
			if (maxModificationTimestamp != null)
			{
				if (opr == TimestampOp.EQUALS) {
					buf.append(SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP_EQ);
				} else if (opr == TimestampOp.GREATER_THAN) {
					buf.append(SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP_GT);
				} else {
					buf.append(SQL_GET_WHERE_FRAG_MAX_MODIFICATION_TIMESTAMP);
				}
			}

			// If a search condition (constructed from a DQL query has been supplied,
			// render it to SQL and include in the query.
			String orderByClause = SQL_DEFAULT_ORDER_BY_FRAG;
			if (condition != null)
			{
				String conditionSQL = conditionRenderer.render(condition);
				if (conditionSQL != null)
				{
					if (!conditionSQL.isEmpty()) {
						buf.append(String.format(SQL_GET_WHERE_FRAG_CONDITION_TEMPLATE, conditionSQL));
					}
				}
				// Order By clause can be stand alone without any condition in DQL.
				// For DB2 database orderByConditionSQL contains both columnClause and orderByClause.
				// For rest of the databases orderByConditionSQL contains only orderByClause.
				String orderByConditionSQL = conditionRenderer.renderOrderBy(condition);
				if (!orderByConditionSQL.isEmpty()) {
					if (orderByConditionSQL.indexOf(" *** ") != -1) {
						int index = orderByConditionSQL.indexOf(" *** ");
						columnClause += "," + orderByConditionSQL.substring(0,index);
						orderByClause = orderByConditionSQL.substring(index+5);
					} else {
						orderByClause = orderByConditionSQL;
					}
				}		
			}
	
			int noOfSearchSubstitutions = 1;
			if (search != null)
			{
				String render = simpleSearchRenderer.render(st);
                buf.append(" AND ( ");
				buf.append(render);
                buf.append(" ) ");
				// For postgreSQL database, noOfSearchSubstitutions = 1
				// For oracle database, noOfSearchSubstitutions = no of search attributes
                noOfSearchSubstitutions =
                        simpleSearchRenderer.getNoOfSubstitutions(render);
			}

			// Add join to cdm_states, if needed and 
			// Insert extra where conditions
			sql = getSQLAfterSubstitution(skip, columnClause, excludeTerminalState, 
					buf.length() == 0 ? "" : buf.toString(), orderByClause);

			ps = conn.prepareStatement(sql);
			int idx = 1;
			ps.setBigDecimal(idx++, new BigDecimal(typeId));
			if (cid != null)
			{
				ps.setString(idx++, cid);
			}
			if (stateValue != null)
			{
				ps.setString(idx++, stateValue);
				ps.setBigDecimal(idx++, new BigDecimal(typeId));
			}
			if (maxModificationTimestamp != null)
			{
				ps.setTimestamp(idx++, new Timestamp(maxModificationTimestamp.getTimeInMillis()));
			}
			if (condition != null)
			{
				// This sets parameter(s) as needed, and returns the index of the next parameter
				idx = conditionRenderer.setParameters(condition, ps, idx);
			}

			if (search != null)
			{
				for (int i=0; i<noOfSearchSubstitutions; i++) {
					ps.setString(idx++, search);
				}
			}
			if (skip != null)
			{
				ps.setInt(idx++, skip);
			}
			ps.setInt(idx++, top);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					// Construct a DTO and populate with
					// what the resultset returned.
					CaseInfoDTO dto = new CaseInfoDTO();

					if (includeCaseReference)
					{
						dto.setId(rset.getBigDecimal("id").toBigInteger());
						dto.setVersion(rset.getInt("version"));
					}
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

					result.add(dto);
				}
			}
		}
		catch (

		SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
		return result;
	}

    protected String getSQLAfterSubstitution(Integer skip, String columnClause, boolean excludeTerminalState, String queryFragment, String orderByClause) {
    	
    	String sql = skip == null ? SQL_GET_WITHOUT_SKIP : SQL_GET;
		sql = String.format(sql, columnClause, excludeTerminalState ? SQL_GET_JOIN_STATES : "",
					queryFragment, orderByClause);
    	return sql;
    	
    }
    
	private Integer getCaseVersion(Connection conn, CaseReference ref) throws PersistenceException
	{
		Integer result = null;
		BigInteger id = ref.getId();
		QualifiedTypeName qName = ref.getQualifiedTypeName();
		String namespace = qName.getNamespace();
		int majorVersion = ref.getApplicationMajorVersion();
		String name = qName.getName();

		PreparedStatement ps = null;
		try
		{
			ps = conn.prepareStatement(SQL_GET_SINGLE_VERSION);
			ps.setBigDecimal(1, new BigDecimal(id));
			ps.setString(2, name);
			ps.setString(3, namespace);
			ps.setInt(4, majorVersion);
			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					result = rset.getInt(1);
				}
			}
			return result;
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(null, ps, null);
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#delete(com.tibco.bpm.cdm.core.dto.CaseReference)
	 */
    public void delete(CaseReference ref) throws PersistenceException, ReferenceException
	{
		BigInteger id = ref.getId();
		QualifiedTypeName qName = ref.getQualifiedTypeName();
		String namespace = qName.getNamespace();
		int majorVersion = ref.getApplicationMajorVersion();
		int version = ref.getVersion();
		String name = qName.getName();

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_DELETE_SINGLE);
			ps.setBigDecimal(1, new BigDecimal(id));
			ps.setInt(2, version);
			ps.setString(3, name);
			ps.setString(4, namespace);
			ps.setInt(5, majorVersion);
			int count = ps.executeUpdate();
			if (count == 0)
			{
				// No rows affected.  That might mean the case doesn't exist, or it may exist, but at a different version. 
				// In that latter scenario, we want to fail with a specific error.
				Integer actualVersion = getCaseVersion(conn, ref);
				if (actualVersion != null)
				{
					// Exists, but at different version
					throw CaseOutOfSyncError.newVersionMismatch(version, actualVersion, ref.toString());
				}
				else
				{
					// Doesn't exist at all
					throw ReferenceException.newNotExist(ref.toString());
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
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#delete(java.math.BigInteger, java.lang.String, java.util.Calendar)
	 */
    public List<CaseReference> delete(BigInteger typeId, String stateValue, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException
	{
		List<CaseReference> deletedRefs = new ArrayList<>();
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_DELETE);
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
					deletedRefs.add(ref);
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
		return deletedRefs;
	}

    public List<CaseReference> getCasesByTypeStateTimestamp(BigInteger typeId,
            String stateValue, Calendar maxModificationTimestamp)
            throws PersistenceException, ReferenceException {
        List<CaseReference> refs = new ArrayList<>();
        PreparedStatement ps = null;
        Statement ts = null;
        Connection conn = null;
        try {
            conn = getConnection();
            

            ps = conn.prepareStatement(SQL_GET_CASES_BY_TYPE_STATE_TIMESTAMP);
            ps.setBigDecimal(1, new BigDecimal(typeId));
            ps.setString(2, stateValue);
            ps.setTimestamp(3,
                    new Timestamp(maxModificationTimestamp.getTimeInMillis()));
            boolean success = ps.execute();

            if (success) {
                ResultSet rset = ps.getResultSet();
                while (rset.next()) {
                    BigInteger id = rset.getBigDecimal(1).toBigInteger();
                    int version = rset.getInt(2);
                    String namespace = rset.getString(3);
                    int majorVersion = rset.getInt(4);
                    String name = rset.getString(5);
                    CaseReference ref = new CaseReference(
                            new QualifiedTypeName(namespace, name),
                            majorVersion, id, version);
                    refs.add(ref);
                }
            }
        } catch (SQLException e) {
            throw PersistenceException.newRepositoryProblem(e);
        } finally {
            cleanUp(ts, ps, conn);
        }
        return refs;
    }

    public List<CaseReference> getLinkedCases(CaseReference caseRef, int end)
            throws PersistenceException, ReferenceException {

        String sql_get = SQL_GET_LINKED_CASES_BY_END1;
        if (end == 2) {
            sql_get = SQL_GET_LINKED_CASES_BY_END2;
        }

        List<CaseReference> linkedCases = new ArrayList<CaseReference>();
        PreparedStatement ps = null;
        Statement ts = null;
        Connection conn = null;
        try {
            conn = getConnection();
            

            ps = conn.prepareStatement(sql_get);
            ps.setBigDecimal(1, new BigDecimal(caseRef.getId()));

            boolean success = ps.execute();
            if (success) {
                ResultSet rset = ps.getResultSet();
                while (rset.next()) {
                    BigInteger id = rset.getBigDecimal(2).toBigInteger();
                    String namespace = rset.getString(3);
                    String name = rset.getString(4);
                    int majorVersion = rset.getInt(5);
                    int version = rset.getInt(6);
                    CaseReference ref = new CaseReference(
                            new QualifiedTypeName(namespace, name),
                            majorVersion, id, version);
                    linkedCases.add(ref);
                }
            }
        } catch (SQLException e) {
            throw PersistenceException.newRepositoryProblem(e);
        } finally {
            cleanUp(ts, ps, conn);
        }
        return linkedCases;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.tibco.bpm.cdm.core.dao.CaseDAO#countByDeploymentId(java.math.
     * BigInteger)
     */
    public long countByDeploymentId(BigInteger deploymentId) throws PersistenceException
	{
		long count = 0;
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_COUNT_BY_DEPLOYMENT_ID);
			ps.setBigDecimal(1, new BigDecimal(deploymentId));
			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					count = rset.getLong(1);
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
		return count;
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
				result = rset.getBoolean(1);
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

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#exists(com.tibco.bpm.cdm.core.dto.CaseReference)
	 */
	@Override
	public boolean exists(CaseReference ref) throws PersistenceException, ReferenceException
	{
		boolean result = false;
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

			ps = conn.prepareStatement(SQL_EXISTS);
			ps.setBigDecimal(1, new BigDecimal(id));
			ps.setString(2, name);
			ps.setString(3, namespace);
			ps.setInt(4, majorVersion);
			ps.execute();
			ResultSet rset = ps.getResultSet();
			result = rset.next();
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

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#primeForPurge(java.math.BigInteger, java.util.Calendar)
	 */
	@Override
	public List<CaseReference> primeForPurge(BigInteger typeId, Calendar maxModificationTimestamp)
			throws PersistenceException, ReferenceException
	{
		List<CaseReference> result = new ArrayList<>();

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_PRIME_FOR_PURGE);
			ps.setTimestamp(1, new Timestamp(maxModificationTimestamp.getTimeInMillis()));
			ps.setBigDecimal(2, new BigDecimal(typeId));
			ps.execute();
			ResultSet rset = ps.getResultSet();
			while (rset.next())
			{
				BigInteger id = rset.getBigDecimal("id").toBigInteger();
				String typeName = rset.getString("type_name");
				String namespace = rset.getString("namespace");
				Integer majorVersion = rset.getInt("major_version");
				Integer version = rset.getInt("version");

				CaseReference ref = new CaseReference(new QualifiedTypeName(namespace, typeName), majorVersion, id,
						version);
				result.add(ref);
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

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseDAO#markForPurge(java.util.List)
	 */
	@Override
	public void markForPurge(List<CaseReference> refs) throws PersistenceException
	{
		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();

			String sql = String.format(SQL_MARK_FOR_PURGE, renderCommaSeparatedQuestionMarks(refs.size()));
			ps = conn.prepareStatement(sql);
			int idx = 1;
			for (CaseReference ref : refs)
			{
				ps.setBigDecimal(idx++, new BigDecimal(ref.getId()));
			}
			ps.executeUpdate();
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

    protected String escapeString(String str, String targetChar) {
        int i = 0;
        int pos = 0;
        StringBuffer buff = new StringBuffer();

        while (str.indexOf(targetChar, i) != -1) {
            pos = str.indexOf(targetChar, i);
            buff.append(str.substring(i, pos));
            buff.append("\\"); //$NON-NLS-1$
            buff.append(str.substring(pos, pos + 1));
            i = pos + 1;
        }
        buff.append(str.substring(i));
        return buff.toString();
    }
}
