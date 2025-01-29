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
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.dto.QualifiedTypeName;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;
import com.tibco.bpm.cdm.core.dao.CaseLinkDAO;
import com.tibco.bpm.cdm.core.dao.LinkDAO;
import com.tibco.bpm.cdm.core.dto.CaseLinkDTO;
import com.tibco.bpm.cdm.core.dto.LinkDTO;

/**
 * Implementation specific to MS-SQL database
 */
public class CaseLinkDAOImpl extends com.tibco.bpm.cdm.core.dao.impl.CaseLinkDAOImpl implements CaseLinkDAO
{
	
	private static final String		SQL_LOCK_CASE							= "SELECT type_id FROM cdm_cases_int WHERE id = ? AND type_id IN ("
			+ "SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN ("
			+ "SELECT id FROM cdm_datamodels WHERE namespace = ? and major_version = ?))";

	private static final String 	SQL_GET_LINKS_FROM_END_1 = "SELECT c.id, c.version, d.namespace, d.major_version, t.name, " + 
			"l.end1_name as link_name, l.end2_name as link_opposite_name " + 
			"FROM cdm_case_links cl " + 
			"INNER JOIN cdm_links l ON cl.link_id=l.id " + 
			"INNER JOIN cdm_cases_int c ON c.id=cl.end2_id " + 
			"INNER JOIN cdm_types t ON c.type_id=t.id " + 
			"INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id " + 
			"where cl.end1_id IN (SELECT id FROM cdm_cases_int WHERE id=? " + 
			"AND type_id IN (SELECT id FROM cdm_types WHERE name=? " + 
			"AND datamodel_id IN (SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?))) %s";
			
	private static final String		SQL_GET_LINKS_FROM_END_2 = "SELECT c.id, c.version, d.namespace, d.major_version, t.name, " + 
			"l.end2_name as link_name, l.end1_name as link_opposite_name " + 
			"FROM cdm_case_links cl " + 
			"INNER JOIN cdm_links l ON cl.link_id=l.id " + 
			"INNER JOIN cdm_cases_int c ON c.id=cl.end2_id " + 
			"INNER JOIN cdm_types t ON c.type_id=t.id " + 
			"INNER JOIN cdm_datamodels d ON t.datamodel_id=d.id " + 
			"where cl.end2_id IN (SELECT id FROM cdm_cases_int WHERE id=? " + 
			"AND type_id IN (SELECT id FROM cdm_types WHERE name=? " + 
			"AND datamodel_id IN (SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?))) %s";
	
	private static final String		SQL_DELETE_FROM_END_1					= "DELETE FROM cdm_case_links "
			+ "WHERE end1_id IN " + "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?))) %s";

	private static final String 	SQL_DELETE_FROM_END_2					= "DELETE FROM cdm_case_links "
			+ "WHERE end2_id IN " + "(SELECT id FROM cdm_cases_int WHERE id = ? AND type_id IN "
			+ "(SELECT id FROM cdm_types WHERE name = ? AND datamodel_id IN "
			+ "(SELECT id FROM cdm_datamodels WHERE namespace=? AND major_version=?))) %s";

	private LinkDAO					linkDAO;


	public CaseLinkDAOImpl(DataSource dataSource) {
        super(dataSource);
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
						boolean inTerminalState = rset.getInt(1) == 1 ? true : false;
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

	@Override
	protected List<CaseLinkDTO> doDeleteWithOriginEnd(CaseReference caseReference,
			List<CaseReference> targetCaseReferences, LinkDTO link, int originEnd)
			throws PersistenceException, InternalException, ReferenceException
	{
		// We assume that all case references supplied are of the correct types, as this
		// is checked in CaseManager.
		Connection conn = null;
		Statement ts = null;
		PreparedStatement ps = null;
		
		// First get the details about the links getting deleted here. 
		// With MS SQL database we need to get those details first before deleting the links.
		List<CaseLinkDTO> deletedDTOs = getCaseLinksToBeDeleted(caseReference, targetCaseReferences, link, originEnd);
		int success = 0;

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
				success = ps.executeUpdate();
			}
			else
			{
				ps.setBigDecimal(5, new BigDecimal(link.getId()));

				// If no case references are supplied, it's a single query
				if (targetCaseReferences == null || targetCaseReferences.isEmpty())
				{
					success = ps.executeUpdate();
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
						success = ps.executeUpdate();					
					}
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
		return deletedDTOs;
	}
	
    private List<CaseLinkDTO> getCaseLinksToBeDeleted(CaseReference caseReference,
			List<CaseReference> targetCaseReferences, LinkDTO link, int originEnd)
    		throws PersistenceException, ReferenceException 
    {
    	
    	List<CaseLinkDTO> deletedDTOs = new ArrayList<>();

		PreparedStatement ps = null;
		Statement ts = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			
			String sqlTemplate = null;
			StringBuilder whereFrags = new StringBuilder();
			if (originEnd == 1)
			{
				sqlTemplate = SQL_GET_LINKS_FROM_END_1;
			}
			else if (originEnd == 2)
			{
				sqlTemplate = SQL_GET_LINKS_FROM_END_2;
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
			
			boolean success = false;
			
			if (link == null) {
				success = ps.execute();
			} else {
				ps.setBigDecimal(5, new BigDecimal(link.getId()));
				// If no case references are supplied, it's a single query
				if (targetCaseReferences == null || targetCaseReferences.isEmpty())
				{
					success = ps.execute();
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
						success = ps.execute();	
						if (success)
						{
							ResultSet rset = ps.getResultSet();
							while (rset.next())
							{
								CaseLinkDTO deletedDTO = buildCaseLinkDTOFromDeleteResult(rset);
								deletedDTOs.add(deletedDTO);
							}
						}
					}
				}				
			}
			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					CaseLinkDTO deletedDTO = buildCaseLinkDTOFromDeleteResult(rset);
					deletedDTOs.add(deletedDTO);
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
    	return deletedDTOs;
    }

}
