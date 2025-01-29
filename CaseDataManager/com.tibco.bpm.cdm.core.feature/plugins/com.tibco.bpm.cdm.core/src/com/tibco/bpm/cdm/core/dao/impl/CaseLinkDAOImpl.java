package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.dao.CaseLinkDAO;
import com.tibco.bpm.cdm.core.dao.ConditionRenderer;
import com.tibco.bpm.cdm.core.dao.LinkDAO;
import com.tibco.bpm.cdm.core.dao.SimpleSearchRenderer;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.LinkDTO;
import com.tibco.bpm.cdm.libs.dql.dto.SearchConditionDTO;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * PostgreS implementation of the CaseLinkDAO interface that persists links between cases to the cdm_case_links database table
 * 
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class CaseLinkDAOImpl extends AbstractDAOImpl implements CaseLinkDAO
{
	// Locks a case (given a set of case reference components) and returns the type id
	// (which is useful later when verifying the case is appropriate wrt the Link the definition)
	protected String		SQL_LOCK_CASE							= "SELECT type_id FROM cdm_cases_int WHERE id = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?)) FOR UPDATE";

	// Checks if a given case is in a terminal state
	protected String		SQL_CASE_STATE_TERMINALITY				= "SELECT s.is_terminal FROM cdm_cases_int c "
			+ "INNER JOIN cdm_states s ON c.state_id = s.id WHERE c.id = ? AND c.type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?))";

	protected String		SQL_CREATE								= "INSERT INTO cdm_case_links (link_id, end1_id, end2_id) "
			+ "VALUES (?, ?, ?)";

	// Deletes all links from given case (where source is end 1)
	// Returns the component parts required to construct case references for cases from 
	// which the given case has been unlinked.
	// Note '%s' token where additional WHERE clause fragments can be inserted
	private static final String		SQL_DELETE_FROM_END_1					= "DELETE FROM cdm_case_links cl "
			+ "USING cdm_cases_int c, cdm_types t, cdm_datamodels d, cdm_links l "
			+ "WHERE cl.end2_id=c.id AND c.type_id=t.id AND t.datamodel_id=d.id AND cl.link_id=l.id "
			+ "AND end1_id IN " + "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?)))%s "
			+ "RETURNING c.id, c.version, d.namespace, d.major_version, t.name, "
			+ "l.end1_name as link_name, l.end2_name as link_opposite_name";

	// Deletes all links from given case (where source is end 2)
	// Returns the component parts required to construct case references for cases from 
	// which the given case has been unlinked.
	// Note '%s' token where additional WHERE clause fragments can be inserted
	private static final String		SQL_DELETE_FROM_END_2					= "DELETE FROM cdm_case_links cl "
			+ "USING cdm_cases_int c, cdm_types t, cdm_datamodels d, cdm_links l "
			+ "WHERE cl.end1_id=c.id AND c.type_id=t.id AND t.datamodel_id=d.id AND cl.link_id=l.id "
			+ "AND end2_id IN " + "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?)))%s "
			+ "RETURNING c.id, c.version, d.namespace, d.major_version, t.name, "
			+ "l.end2_name as link_name, l.end1_name as link_opposite_name";

	// Supplementary WHERE clause fragment to constrain deletion to a single target ref
	protected static final String		SQL_DELETE_FROM_END_1_FRAG_WHERE_ID		= " AND end2_id IN "
			+ "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ " (SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?)))";

	protected static final String		SQL_DELETE_FROM_END_2_FRAG_WHERE_ID		= " AND end1_id IN "
			+ "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ " (SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?)))";

	// Supplementary WHERE clause fragment to constrain deletion to a given link name
	protected static final String		SQL_DELETE_FROM_END_1_FRAG_WHERE_NAME	= " AND link_id = ?";

	protected static final String		SQL_DELETE_FROM_END_2_FRAG_WHERE_NAME	= " AND link_id = ?";

	// Get the case reference components for all cases linked to the given case (regardless of which Link name)
	// Note '%s' token where additional WHERE clause fragments can be inserted
	// This works when navigating from end 1 to end 2
	protected String		SQL_GET_1								= "SELECT l.end1_name, c.id, d.namespace, t.name, d.major_version, "
			+ "c.version FROM cdm_case_links cl INNER JOIN cdm_links l ON cl.link_id=l.id "
			+ "INNER JOIN cdm_cases_int c ON c.id=cl.end2_id "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "WHERE cl.end1_id = ?%s ORDER BY l.end1_name, c.id";

	// Get the case reference components for all cases linked to the given case (regardless of which Link name)
	// Note '%s' token where additional WHERE clause fragments can be inserted
	// This works when navigating from end 2 to end 1
	protected String		SQL_GET_2								= "SELECT l.end2_name, c.id, d.namespace, t.name, d.major_version, "
			+ "c.version FROM cdm_case_links cl INNER JOIN cdm_links l ON cl.link_id=l.id "
			+ "INNER JOIN cdm_cases_int c ON c.id=cl.end1_id "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
			+ "WHERE cl.end2_id = ?%s ORDER BY l.end2_name, c.id";

	protected String		SQL_GET_1_FRAG_WHERE_NAME				= " AND l.end1_name = ?";

	protected String		SQL_GET_2_FRAG_WHERE_NAME				= " AND l.end2_name = ?";

	// Fragment for wrapping the SQL for a DQL query
	private static final String		SQL_GET_WHERE_FRAG_CONDITION_TEMPLATE	= " AND (%s)";

	// Used to determine which cases a given link target is already linked to
	protected String		SQL_GET_OPPOSITE_NAME_1					= "SELECT c.id "
			+ "FROM cdm_case_links cl INNER JOIN cdm_links l ON cl.link_id=l.id "
			+ "INNER JOIN cdm_cases_int c ON c.id=cl.end1_id "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
            + "WHERE cl.end2_id = ? AND l.end1_name = ? AND l.end2_name = ?";

	protected String		SQL_GET_OPPOSITE_NAME_2					= "SELECT c.id "
			+ "FROM cdm_case_links cl INNER JOIN cdm_links l ON cl.link_id=l.id "
			+ "INNER JOIN cdm_cases_int c ON c.id=cl.end2_id "
			+ "INNER JOIN cdm_types t ON c.type_id=t.id INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id "
            + "WHERE cl.end1_id = ? AND l.end2_name = ?  AND l.end1_name = ?";

	private LinkDAO					linkDAO;

	private SimpleSearchRenderer	simpleSearchRenderer;

	private ConditionRenderer		conditionRenderer;


	public CaseLinkDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	public void setSimpleSearchRenderer(SimpleSearchRenderer renderer) {
		simpleSearchRenderer = renderer;
	}
	
	public void setConditionRenderer(ConditionRenderer renderer) {
		conditionRenderer = renderer;
	}

	public void setLinkDAO(LinkDAO linkDAO)
	{
		this.linkDAO = linkDAO;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.CaseLinkDAO#create(java.math.BigInteger, com.tibco.bpm.cdm.core.dto.CaseReference, java.util.List)
	 */
	@Override
	public void create(BigInteger typeId, CaseReference caseReference, List<CaseLinkDTO> dtos)
			throws PersistenceException, ReferenceException, InternalException
	{
		// Find unique link names
		Set<String> uniqueNames = dtos.stream().map(CaseLinkDTO::getName).collect(Collectors.toSet());
		List<CaseReference> allRefs = new ArrayList<>();
		allRefs.add(caseReference);
		allRefs.addAll(dtos.stream().map(CaseLinkDTO::getCaseReference).collect(Collectors.toSet()));

		// Lock the participating cases to prevent concurrent creation of links that my violate multiplicity constraints.
		// This is done in ascending id order to prevent deadlock when other link creation attempts are happening concurrently.
		allRefs.sort((r1, r2) -> {
			return r1.getId().compareTo(r2.getId());
		});

		PreparedStatement ps = null;
		PreparedStatement psTerminality = null;
		PreparedStatement psCreate = null;
		PreparedStatement psGetExisting1 = null;
		PreparedStatement psGetExisting2 = null;
		PreparedStatement psGetExistingOpposite1 = null;
		PreparedStatement psGetExistingOpposite2 = null;

		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			


			// Lock cases (and check they exist)
			ps = conn.prepareStatement(SQL_LOCK_CASE);

			for (CaseReference ref : allRefs)
			{
				ps.setBigDecimal(1, new BigDecimal(ref.getId()));
				QualifiedTypeName qtn = ref.getQualifiedTypeName();
				ps.setString(2, qtn.getName());
				ps.setString(3, qtn.getNamespace());
				ps.setInt(4, ref.getApplicationMajorVersion());

				boolean exists = false;
				if (ps.execute())
				{
					ResultSet rset = ps.getResultSet();
					if (rset.next())
					{
						exists = true;

						// Store the type id in relevant DTO(s)
						BigInteger targetTypeId = rset.getBigDecimal(1).toBigInteger();
						dtos.stream().filter(d -> d.getCaseReference().equals(ref))
								.forEach(d -> d.setTypeId(targetTypeId));
					}
				}

				if (!exists)
				{
					throw ReferenceException.newNotExist(ref.toString());
				}
			}

			// Check that none of the cases are terminal-state, as such cases
			// can't be linked

			psTerminality = conn.prepareStatement(SQL_CASE_STATE_TERMINALITY);
			for (CaseReference ref : allRefs)
			{
				psTerminality.setBigDecimal(1, new BigDecimal(ref.getId()));
				QualifiedTypeName qtn = ref.getQualifiedTypeName();
				psTerminality.setString(2, qtn.getName());
				psTerminality.setString(3, qtn.getNamespace());
				psTerminality.setInt(4, ref.getApplicationMajorVersion());

				if (psTerminality.execute())
				{
					ResultSet rset = psTerminality.getResultSet();
					if (rset.next())
					{
						// Check the case isn't terminal-state
						boolean inTerminalState = rset.getBoolean(1);
						if (inTerminalState)
						{
							throw ReferenceException.newTerminalStatePreventsLinking(ref.toString());
						}
					}
				}
			}

			// Prepare a statement to reuse for each link creation
			psCreate = conn.prepareStatement(SQL_CREATE);

			// Prepare a statement for checking for existing links
			psGetExisting1 = conn.prepareStatement(String.format(SQL_GET_1, SQL_GET_1_FRAG_WHERE_NAME));
			psGetExisting2 = conn.prepareStatement(String.format(SQL_GET_2, SQL_GET_2_FRAG_WHERE_NAME));
			psGetExistingOpposite1 = conn.prepareStatement(SQL_GET_OPPOSITE_NAME_1);
			psGetExistingOpposite2 = conn.prepareStatement(SQL_GET_OPPOSITE_NAME_2);

			// Now all cases are locked, no other threads can be altering links between
			// them, so we can go ahead and check that creating links won't violate
			// multiplicity constraints.
			for (String name : uniqueNames)
			{
				// Check that link name exists
				LinkDTO link = linkDAO.getLink(conn, typeId, name);

				if (link == null)
				{
					throw ReferenceException.newLinkNameNotExist(name, caseReference.getQualifiedTypeName().toString(),
							caseReference.getApplicationMajorVersion());
				}

				// Work out which end is the source end and, therefore, what the target
				// type id is.
				BigInteger targetTypeId = null;
				int sourceEnd = 0;
				if (link.getEnd1TypeId().equals(typeId) && link.getEnd1Name().equals(name))
				{
					targetTypeId = link.getEnd2TypeId();
					sourceEnd = 1;
				}
				else if (link.getEnd2TypeId().equals(typeId) && link.getEnd2Name().equals(name))
				{
					targetTypeId = link.getEnd1TypeId();
					sourceEnd = 2;
				}

				// Get the DTOs for this link
				List<CaseLinkDTO> targets = dtos.stream().filter(d -> d.getName().equals(name))
						.collect(Collectors.toList());

				// Check that target case(s) are of the correct type id
				for (CaseLinkDTO target : targets)
				{
					if (!target.getTypeId().equals(targetTypeId))
					{
						// Fail, as target case is not of the correct type
						throw ReferenceException.newLinkBadType(caseReference.toString(),
								target.getCaseReference().toString(), name);
					}
					target.setOppositeName(sourceEnd == 1 ? link.getEnd2Name() : link.getEnd1Name());
				}

				// Check what is _already_ linked
				List<BigInteger> existingLinkedCaseIds = new ArrayList<BigInteger>();
				psGetExisting1.setBigDecimal(1, new BigDecimal(caseReference.getId()));
				psGetExisting1.setString(2, name);
				if (psGetExisting1.execute())
				{
					ResultSet rset1 = psGetExisting1.getResultSet();
					while (rset1.next())
					{
						existingLinkedCaseIds.add(rset1.getBigDecimal("id").toBigInteger());
					}
				}

				psGetExisting2.setBigDecimal(1, new BigDecimal(caseReference.getId()));
				psGetExisting2.setString(2, name);
				if (psGetExisting2.execute())
				{
					ResultSet rset2 = psGetExisting2.getResultSet();
					while (rset2.next())
					{
						existingLinkedCaseIds.add(rset2.getBigDecimal("id").toBigInteger());
					}
				}

				// Prevent re-linking to cases already linked to
				for (CaseLinkDTO target : targets)
				{
					if (existingLinkedCaseIds.contains(target.getCaseReference().getId()))
					{
						throw ReferenceException.newAlreadyLinked(caseReference.toString(),
								target.getCaseReference().toString(), name);
					}
				}

				// Check adding new links won't blow multiplicity
				boolean isArray = sourceEnd == 1 ? link.isEnd1IsArray() : link.isEnd2IsArray();
				int totalSize = targets.size() + existingLinkedCaseIds.size();
				if (!isArray && totalSize > 1)
				{
					throw ReferenceException.newLinkIsNotArray(caseReference.getQualifiedTypeName().toString(),
							caseReference.getApplicationMajorVersion(), name, totalSize);
				}

				// Check that each link won't blow the multiplicity of the opposite (i.e. when viewed from
				// the target cases's PoV)
				for (CaseLinkDTO target : targets)
				{
					List<BigInteger> existingOppositeLinkedCaseIds = new ArrayList<BigInteger>();
					psGetExistingOpposite1.setBigDecimal(1, new BigDecimal(target.getCaseReference().getId()));
					psGetExistingOpposite1.setString(2, name);
                    psGetExistingOpposite1.setString(3, target.getOppositeName());
					if (psGetExistingOpposite1.execute())
					{
						ResultSet rset1 = psGetExistingOpposite1.getResultSet();
						while (rset1.next())
						{
							existingOppositeLinkedCaseIds.add(rset1.getBigDecimal(1).toBigInteger());
						}
					}

					psGetExistingOpposite2.setBigDecimal(1, new BigDecimal(target.getCaseReference().getId()));
					psGetExistingOpposite2.setString(2, name);
                    psGetExistingOpposite2.setString(3, target.getOppositeName());
					if (psGetExistingOpposite2.execute())
					{
						ResultSet rset2 = psGetExistingOpposite2.getResultSet();
						while (rset2.next())
						{
							existingOppositeLinkedCaseIds.add(rset2.getBigDecimal(1).toBigInteger());
						}
					}

					// Check adding new links won't blow opposite multiplicity
					boolean isOppositeArray = sourceEnd == 1 ? link.isEnd2IsArray() : link.isEnd1IsArray();
					int totalOppositeSize = existingOppositeLinkedCaseIds.size() + 1;
					if (!isOppositeArray && totalOppositeSize > 1)
					{
						String oppositeName = sourceEnd == 1 ? link.getEnd2Name() : link.getEnd1Name();
						throw ReferenceException.newLinkOppositeIsNotArray(
								caseReference.getQualifiedTypeName().toString(),
								caseReference.getApplicationMajorVersion(), name, oppositeName);
					}
				}

				// All good, so go ahead and create the links
				psCreate.setBigDecimal(1, new BigDecimal(link.getId()));
				for (CaseLinkDTO target : targets)
				{
					if (sourceEnd == 1)
					{
						psCreate.setBigDecimal(2, new BigDecimal(caseReference.getId()));
						psCreate.setBigDecimal(3, new BigDecimal(target.getCaseReference().getId()));
					}
					else
					{
						psCreate.setBigDecimal(2, new BigDecimal(target.getCaseReference().getId()));
						psCreate.setBigDecimal(3, new BigDecimal(caseReference.getId()));
					}
					psCreate.executeUpdate();
				}
			}

		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(psTerminality);
			cleanUp(psCreate);
			cleanUp(psGetExisting1);
			cleanUp(psGetExisting2);
			cleanUp(psGetExistingOpposite1);
			cleanUp(psGetExistingOpposite2);
			cleanUp(ts, ps, conn);
		}
	}

	private CaseLinkDTO processGetRow(ResultSet rset) throws SQLException, ReferenceException
	{
		String cLinkName = rset.getString(1);
		BigInteger cId = rset.getBigDecimal(2).toBigInteger();
		String cNamespace = rset.getString(3);
		String cName = rset.getString(4);
		Integer cMajorVersion = rset.getInt(5);
		Integer cVersion = rset.getInt(6);
		QualifiedTypeName cQTN = new QualifiedTypeName(cNamespace, cName);
		CaseReference cRef = new CaseReference(cQTN, cMajorVersion, cId, cVersion);
		CaseLinkDTO dto = new CaseLinkDTO(cLinkName, cRef);
		return dto;
	}

	private String buildGetSQL(String baseDQL, String whereNameDQL, StructuredType st, String name, String search,
			SearchConditionDTO condition)
	{
		StringBuilder buf = new StringBuilder();
		if (name != null)
		{
			buf.append(whereNameDQL);
		}
		if (search != null)
		{
			buf.append(" AND ");
			buf.append(simpleSearchRenderer.render(st));
		}
		if (condition != null)
		{
			String conditionSQL = conditionRenderer.render(condition);
			buf.append(String.format(SQL_GET_WHERE_FRAG_CONDITION_TEMPLATE, conditionSQL));
		}
		String sql = String.format(baseDQL, buf.toString());
		return sql;
	}

	@Override
	public List<CaseLinkDTO> get(CaseReference caseReference, StructuredType st, String name, Integer skip, Integer top,
			String search, SearchConditionDTO searchCondition)
			throws PersistenceException, InternalException, ReferenceException
	{
		Connection conn = null;
		Statement ts = null;
		PreparedStatement ps = null;

		List<CaseLinkDTO> result = new ArrayList<>();

		try
		{
			conn = getConnection();

			String sql = buildGetSQL(SQL_GET_1, SQL_GET_1_FRAG_WHERE_NAME, st, name, search, searchCondition);
			ps = conn.prepareStatement(sql);
			int idx = 1;
			ps.setBigDecimal(idx++, new BigDecimal(caseReference.getId()));
			if (name != null)
			{
				ps.setString(idx++, name);
			}
			if (search != null)
			{
				ps.setString(idx++, search);
			}
			else if (searchCondition != null)
			{
				conditionRenderer.setParameters(searchCondition, ps, idx);
			}

			ps.execute();
			ResultSet rset = ps.getResultSet();
			while ((top == null || top > 0) && rset.next())
			{
				CaseLinkDTO dto = processGetRow(rset);
				if (skip != null && skip > 0)
				{
					skip--;
				}
				else
				{
					result.add(dto);
					top--;
				}
			}
			ps.close();
			if (top == null || top > 0)
			{
				sql = buildGetSQL(SQL_GET_2, SQL_GET_2_FRAG_WHERE_NAME, st, name, search, searchCondition);
				ps = conn.prepareStatement(sql);
				idx = 1;
				ps.setBigDecimal(idx++, new BigDecimal(caseReference.getId()));
				if (name != null)
				{
					ps.setString(idx++, name);
				}
				if (search != null)
				{
					ps.setString(idx++, search);
				}
				else if (searchCondition != null)
				{
					conditionRenderer.setParameters(searchCondition, ps, idx);
				}

				ps.execute();
				rset = ps.getResultSet();
				while ((top == null || top > 0) && rset.next())
				{
					CaseLinkDTO dto = processGetRow(rset);
					if (skip != null && skip > 0)
					{
						skip--;
					}
					else
					{
						result.add(dto);
						top--;
					}
				}
			}
			ps.close();
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
		return result;
	}

	protected CaseLinkDTO buildCaseLinkDTOFromDeleteResult(ResultSet rset) throws SQLException, ReferenceException
	{
		BigInteger deletedId = rset.getBigDecimal("id").toBigInteger();
		Integer deletedVersion = rset.getInt("version");
		String deletedNamespace = rset.getString("namespace");
		Integer deletedMajorVersion = rset.getInt("major_version");
		String deletedName = rset.getString("name");
		CaseReference deletedRef = new CaseReference(new QualifiedTypeName(deletedNamespace, deletedName),
				deletedMajorVersion, deletedId, deletedVersion);
		CaseLinkDTO deletedDTO = new CaseLinkDTO(rset.getString("link_name"), deletedRef);
		deletedDTO.setOppositeName(rset.getString("link_opposite_name"));
		return deletedDTO;
	}

    public List<CaseLinkDTO> delete(CaseReference caseReference, List<CaseReference> targetCaseReferences, LinkDTO link,
			Integer originEnd) throws PersistenceException, InternalException, ReferenceException
	{
		List<CaseLinkDTO> result = null;

		if (originEnd != null && originEnd == 1)
		{
			result = doDeleteWithOriginEnd(caseReference, targetCaseReferences, link, 1);
		}
		else if (originEnd != null && originEnd == 2)
		{
			result = doDeleteWithOriginEnd(caseReference, targetCaseReferences, link, 2);
		}
		else
		{
			// Do both ends and combine result
			result = doDeleteWithOriginEnd(caseReference, targetCaseReferences, link, 1);
			result.addAll(doDeleteWithOriginEnd(caseReference, targetCaseReferences, link, 2));
		}
		return result;
	}

	protected List<CaseLinkDTO> doDeleteWithOriginEnd(CaseReference caseReference,
			List<CaseReference> targetCaseReferences, LinkDTO link, int originEnd)
			throws PersistenceException, InternalException, ReferenceException
	{
		// We assume that all case references supplied are of the correct types, as this
		// is checked in CaseManager.
		Connection conn = null;
		Statement ts = null;
		PreparedStatement ps = null;
		List<CaseLinkDTO> deletedDTOs = new ArrayList<>();

		try
		{
			conn = getConnection();


			String sqlTemplate = null;
			StringBuilder whereFrags = new StringBuilder();
			if (originEnd == 1)
			{
				sqlTemplate = SQL_DELETE_FROM_END_1;
			}
			else if (originEnd == 2)
			{
				sqlTemplate = SQL_DELETE_FROM_END_2;
			}

			// If link name supplied, add WHERE clause fragment
			if (link != null)
			{
				if (originEnd == 1)
				{
					whereFrags.append(SQL_DELETE_FROM_END_1_FRAG_WHERE_NAME);
				}
				else if (originEnd == 2)
				{
					whereFrags.append(SQL_DELETE_FROM_END_2_FRAG_WHERE_NAME);
				}

				// If case reference(s) are supplied, add WHERE clause fragment
				if (targetCaseReferences != null && !targetCaseReferences.isEmpty())
				{
					// originEnd already validated, above.
					whereFrags.append(
							originEnd == 1 ? SQL_DELETE_FROM_END_1_FRAG_WHERE_ID : SQL_DELETE_FROM_END_2_FRAG_WHERE_ID);
				}
			}

			String sql = String.format(sqlTemplate, whereFrags.toString());

			ps = conn.prepareStatement(sql);
			ps.setBigDecimal(1, new BigDecimal(caseReference.getId()));
			QualifiedTypeName qtn = caseReference.getQualifiedTypeName();
			ps.setString(2, qtn.getName());
			ps.setString(3, qtn.getNamespace());
			ps.setInt(4, caseReference.getApplicationMajorVersion());

			if (link == null)
			{
				ps.execute();
			}
			else
			{
				ps.setBigDecimal(5, new BigDecimal(link.getId()));

				// If no case references are supplied, it's a single query
				if (targetCaseReferences == null || targetCaseReferences.isEmpty())
				{
					ps.execute();
				}
				else
				{
					// Delete link for each case reference
					for (CaseReference targetCaseReference : targetCaseReferences)
					{
						ps.setBigDecimal(6, new BigDecimal(targetCaseReference.getId()));
						QualifiedTypeName tQTN = targetCaseReference.getQualifiedTypeName();
						ps.setString(7, tQTN.getName());
						ps.setString(8, tQTN.getNamespace());
						ps.setInt(9, targetCaseReference.getApplicationMajorVersion());
						ps.execute();
						ResultSet rset = ps.getResultSet();
						if (rset.next())
						{
							CaseLinkDTO deletedDTO = buildCaseLinkDTOFromDeleteResult(rset);
							deletedDTOs.add(deletedDTO);
						}
						else
						{
							String linkName = originEnd == 1 ? link.getEnd1Name() : link.getEnd2Name();
							throw ReferenceException.newLinkNotLinked(caseReference.toString(),
									targetCaseReference.toString(), linkName);
						}
					}
				}
			}

			ResultSet rset = ps.getResultSet();
			while (rset.next())
			{
				CaseLinkDTO deletedDTO = buildCaseLinkDTOFromDeleteResult(rset);
				deletedDTOs.add(deletedDTO);
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
		return deletedDTOs;
	}
}
