package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.DeploymentException;
import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.ApplicationDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDAO.ApplicationIdAndMajorVersion;
import com.tibco.bpm.cdm.core.dto.TypeInfoDTO.TypeInfoDependencyDTO;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.dt.rasc.Version;
import com.tibco.bpm.dt.rasc.VersionRange;
import com.tibco.bpm.dt.rasc.VersionRange.Endpoint;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * PostgreS implementation of the ApplicationDAO interface that persists applications in the cdm_applications database table.
 * @author smorgan
 * @since 2019
 */
public class ApplicationDAOImpl extends AbstractDAOImpl implements ApplicationDAO
{
	private static CLFClassContext	logCtx									= CloudLoggingFramework
			.init(ApplicationDAOImpl.class, CDMLoggingInfo.instance);

	// Insert into cdm_datamodels, auto-populating id and deployment_timestamp, returning id
	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	protected String		SQL_CREATE								= "INSERT INTO cdm_applications (id, application_name, application_id, "
			+ "deployment_id, major_version, minor_version, micro_version, qualifier, deployment_timestamp,is_case_app) VALUES "
			+ "(NEXTVAL('cdm_applications_seq'), ?, ?, ?, ?, ?, ?, ?, DATE_TRUNC('milliseconds',NOW()),?)";

	protected String 		SQL_SELECT_FOR_UPDATE					= "SELECT id FROM cdm_applications WHERE "
			+ "application_id = ? AND major_version = ? FOR UPDATE";

	// Note use of DATE_TRUNC to strip off unwanted microseconds.
	private static final String		SQL_UPDATE								= "UPDATE cdm_applications SET deployment_id = ? , application_name = ?, "
			+ "major_version = ?, minor_version = ?, micro_version = ?, qualifier = ?, deployment_timestamp = ? WHERE id = ?";

	private static final String		SQL_DELETE								= "DELETE FROM cdm_applications WHERE deployment_id = ?";

	private static final String		SQL_GET_BY_VERSION_RANGE				= "SELECT id, major_version, minor_version, micro_version "
			+ "FROM cdm_applications WHERE application_id = ? AND " + "major_version = ?";

	private static final String		SQL_GET_APP_ID_BY_DEPLOYMENT_ID			= "SELECT application_id FROM cdm_applications WHERE deployment_id = ?";

	// Find apps that depend on the given app (via dependencies in the cdm_datamodel_deps table)
	private static final String		SQL_GET_APPS_DEPENDING_ON_APP			= "SELECT application_id, major_version, minor_version, micro_version, qualifier "
			+ "FROM cdm_applications a WHERE id IN " + "(SELECT application_id FROM cdm_datamodels WHERE id IN "
			+ "(SELECT datamodel_id_from FROM cdm_datamodel_deps WHERE datamodel_id_to IN "
			+ "(SELECT id FROM cdm_datamodels WHERE application_id IN"
			+ "(SELECT id FROM cdm_applications WHERE deployment_id = ? AND id != a.id))))";

	// Get details of namespaces (and containing apps) that a given application depends on (excluding other namespaces in the _same_ app)
	private static final String		SQL_GET_FOREIGN_NAMESPACE_DEPENDENCIES	= "SELECT d.namespace, a.application_id, a.major_version "
			+ " FROM cdm_datamodels d INNER JOIN cdm_applications a ON d.application_id = a.id WHERE d.id IN "
			+ "(SELECT datamodel_id_to FROM cdm_datamodel_deps WHERE datamodel_id_from IN "
			+ "(SELECT dd.id FROM cdm_datamodels dd INNER JOIN cdm_applications aa ON dd.application_id = aa.id WHERE "
			+ "aa.application_id = ? AND aa.major_version = ? AND NOT aa.id = a.id))";

	private static final String SQL_CHECK_IS_CASE_APP = "SELECT a.is_case_app FROM cdm_applications a"
			+ "	LEFT JOIN cdm_datamodels b"
			+ "	ON b.application_id = a.id"
			+ "	LEFT JOIN cdm_types c"
			+ "	ON c.datamodel_id = b.id"
			+ "	LEFT JOIN cdm_cases_int d"
			+ "	ON d.type_id = c.id"
			+ "	WHERE d.id = ?";
	
	private static final String IS_CASE_APP = "is_case_app";
	
	public ApplicationDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	/**
	 * Checks that the version comprised of the given components is at least the specified minimum
	 * @param range
	 * @param major
	 * @param minor
	 * @param micro
	 * @return
	 */
	private boolean versionIsAtLeast(Version minimumVersion, int major, int minor, int micro)
	{
		// 1 May 2019: Mark says that real system should have numeric qualifier, BUT
		// the qualifier is insignificant and cannot be compared/ordered with other qualifiers.
		// The Version class appears to consider qualifier when comparing, so clear it.
		Version v1 = new Version(minimumVersion.getMajor(), minimumVersion.getMinor(), minimumVersion.getMicro(), null);
		Version v2 = new Version(major, minor, micro, null);
		return v2.compareTo(v1) >= 0;
	}	
	
	private BigInteger getExisting(Connection conn, String applicationId, int majorVersion) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getExisting");
		clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		try
		{

			ps = conn.prepareStatement(SQL_SELECT_FOR_UPDATE);
			ps.setString(1, applicationId);
			ps.setInt(2, majorVersion);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					id = rset.getBigDecimal(1).toBigInteger();
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, null);
		}
		return id;
	}

	private String getApplicationId(Connection conn, BigInteger deploymentId) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getApplicationId");

		PreparedStatement ps = null;
		Statement ts = null;
		String applicationId = null;
		try
		{

			ps = conn.prepareStatement(SQL_GET_APP_ID_BY_DEPLOYMENT_ID);
			ps.setBigDecimal(1, new BigDecimal(deploymentId));

			boolean success = ps.execute();
			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					applicationId = rset.getString(1);
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, null);
		}
		return applicationId;
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#create(java.math.BigInteger, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public CreationResult createOrReadForUpdate(BigInteger deploymentId, String applicationName, String applicationId,
			String applicationVersion,boolean isCaseApp) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("createOrReadForUpdate");
		clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
		clf.addMethodAttribute(CommonMetaData.APPLICATION_NAME, applicationName);
		clf.addMethodAttribute(CloudMetaData.APPLICATION_VERSION, applicationVersion);

		CreationResult result = null;
		BigInteger id = null;
		int majorVersion = new Version(applicationVersion).getMajor();
		Connection conn = null;
		try
		{
			conn = getConnection();
			id = getExisting(conn, applicationId, majorVersion);

			if (id != null)
			{
				// This is replacing an existing version with the same major version number
				clf.local.debug(
						"Selected-for-update existing application (entry id %s) for deploymentId=%s, applicationVersion=%s",
						id, deploymentId, applicationVersion);
				result = new CreationResult(id, true);
			}
			else
			{
				PreparedStatement ps = null;
				Statement ts = null;
				try
				{

					ps = conn.prepareStatement(SQL_CREATE, new String[]{"id"});
					ps.setString(1, applicationName);
					ps.setString(2, applicationId);
					ps.setBigDecimal(3, new BigDecimal(deploymentId));
					Version version = new Version(applicationVersion);
					ps.setInt(4, version.getMajor());
					ps.setInt(5, version.getMinor());
					ps.setInt(6, version.getMicro());
					ps.setString(7, version.getQualifier());
					ps.setBoolean(8, isCaseApp);
					
					
					int success = ps.executeUpdate();
					ResultSet rset = ps.getGeneratedKeys();
					if (rset.next())
					{
						id = rset.getBigDecimal(1).toBigInteger();
					}
					clf.local.debug(
							"Persisted new application (entry id %s) for deploymentId=%s, applicationVersion=%s",
							id, deploymentId, applicationVersion);
					result = new CreationResult(id, false);
					
				}
				catch (SQLException e)
				{
					throw PersistenceException.newRepositoryProblem(e);
				}
				finally
				{
					cleanUp(ts, ps, null);
				}
			}
		}
		finally
		{
			cleanUp(null, null, conn);

		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#delete(java.math.BigInteger)
	 */
	@Override
	public String delete(BigInteger deploymentId) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("delete");
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		String applicationId = null;
		try
		{
			conn = getConnection();

			// Get the application_id for the given deploymentId. 
			applicationId = getApplicationId(conn, deploymentId);

			ps = conn.prepareStatement(SQL_DELETE);
			ps.setBigDecimal(1, new BigDecimal(deploymentId));

			int success = ps.executeUpdate();

			if (success == 1)
			{
				clf.local.debug("Deleted application deploymentId=%s", deploymentId);
				return applicationId;
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

		return applicationId;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#update(java.math.BigInteger, java.math.BigInteger, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void update(BigInteger id, BigInteger deploymentId, String applicationName, String applicationId,
			String applicationVersion) throws PersistenceException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("update");
		clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
		clf.addMethodAttribute(CommonMetaData.APPLICATION_NAME, applicationName);
		clf.addMethodAttribute(CloudMetaData.APPLICATION_VERSION, applicationVersion);
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_UPDATE);
			ps.setBigDecimal(1, new BigDecimal(deploymentId));
			ps.setString(2, applicationName);

			Version version = new Version(applicationVersion);
			ps.setInt(3, version.getMajor());
			ps.setInt(4, version.getMinor());
			ps.setInt(5, version.getMicro());
			ps.setString(6, version.getQualifier());
			java.sql.Timestamp stamp = new java.sql.Timestamp(System.currentTimeMillis());
			ps.setTimestamp(7, stamp);
			ps.setBigDecimal(8, new BigDecimal(id));

			int rowsAffected = ps.executeUpdate();
			if (rowsAffected != 1)
			{
				throw InternalException
						.newInternalException("App updated expected to affect 1 row, but affected " + rowsAffected);
			}
			clf.local.debug("Updated application (entry id %s), deploymentId=%s, applicationVersion=%s", id,
					deploymentId, applicationVersion);

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
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#getByVersionRange(java.lang.String, com.tibco.bpm.dt.rasc.VersionRange)
	 */
	@Override
	public List<BigInteger> getByVersionRange(String applicationId, VersionRange versionRange)
			throws InternalException, DeploymentException, PersistenceException
	{
		List<BigInteger> result = new ArrayList<BigInteger>();
		Version lowerVersion = versionRange.getLower();
		int lowerMajorVersion = lowerVersion.getMajor();
		if (versionRange.getLowerEnd() != Endpoint.INCLUSIVE)
		{
			throw DeploymentException.newInvalidVersionDependency(versionRange.toString());
		}

		// We've only supporting patterns like:
		// Application-Dependencies: SanitySubProcesses;version="[1.2.7.qualifier,2)"
		// i.e. Where the upper end is exclusive and consists of the lower major + 1
		Version upperVersion = versionRange.getUpper();
		if (upperVersion.getMajor() != lowerMajorVersion + 1 || upperVersion.getMinor() != 0
				|| upperVersion.getMicro() != 0 || upperVersion.getQualifier() != null)
		{
			throw DeploymentException.newInvalidVersionDependency(versionRange.toString());
		}

		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_GET_BY_VERSION_RANGE);
			ps.setString(1, applicationId);
			ps.setInt(2, lowerMajorVersion);

			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					BigInteger id = rset.getBigDecimal(1).toBigInteger();

					// 1 May 2019: Mark says that real system should have numeric qualifier, BUT
					// the qualifier is insignificant and cannot be compared/ordered with other qualifiers.
					int major = rset.getInt(2);
					int minor = rset.getInt(3);
					int micro = rset.getInt(4);

					if (versionIsAtLeast(lowerVersion, major, minor, micro))
					{
						result.add(id);
					}
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
			cleanUp(ts, ps, conn);
		}

	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#getApplicationsThatDependOnApplication(java.math.BigInteger)
	 */
	public List<ApplicationIdAndVersion> getApplicationsThatDependOnApplication(BigInteger deploymentId)
			throws PersistenceException
	{
		List<ApplicationIdAndVersion> result = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_GET_APPS_DEPENDING_ON_APP);
			ps.setBigDecimal(1, new BigDecimal(deploymentId));

			if (ps.execute())
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					String applicationId = rset.getString(1);
					int majorVersion = rset.getInt(2);
					int minorVersion = rset.getInt(3);
					int microVersion = rset.getInt(4);
					String qualifier = rset.getString(5);
					Version version = new Version(majorVersion, minorVersion, microVersion, qualifier);
					ApplicationIdAndVersion app = new ApplicationIdAndVersion(applicationId, version.toString());
					result.add(app);
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
			cleanUp(ts, ps, conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.ApplicationDAO#getForeignNamespaceDependencies(java.util.List)
	 */
	@Override
	public Map<ApplicationIdAndMajorVersion, List<TypeInfoDependencyDTO>> getForeignNamespaceDependencies(
			List<ApplicationIdAndMajorVersion> applications) throws PersistenceException
	{
		Map<ApplicationIdAndMajorVersion, List<TypeInfoDependencyDTO>> result = new HashMap<>();
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();

			ps = conn.prepareStatement(SQL_GET_FOREIGN_NAMESPACE_DEPENDENCIES);

			for (ApplicationIdAndMajorVersion application : applications)
			{
				ps.setString(1, application.getApplicationId());
				ps.setInt(2, application.getMajorVersion());
				if (ps.execute())
				{
					ResultSet rset = ps.getResultSet();
					List<TypeInfoDependencyDTO> dependencies = new ArrayList<>();
					while (rset.next())
					{
						String dNamespace = rset.getString(1);
						String dApplicationId = rset.getString(2);
						int dMajorVersion = rset.getInt(3);
						TypeInfoDependencyDTO depDTO = new TypeInfoDependencyDTO();
						depDTO.setNamespace(dNamespace);
						depDTO.setApplicationId(dApplicationId);
						depDTO.setApplicationMajorVersion(dMajorVersion);
						dependencies.add(depDTO);
					}
					if (!dependencies.isEmpty())
					{
						result.put(application, dependencies);
					}
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
			cleanUp(ts, ps, conn);
		}
	}
	
	@Override
	public boolean isCaseApp(BigInteger caseId) throws PersistenceException{
		
		CLFMethodContext clf = logCtx.getMethodContext("isCaseApp");
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		boolean isCaseApp = false;

		try {
			conn = getConnection();

			ps = conn.prepareStatement(SQL_CHECK_IS_CASE_APP);
			ps.setBigDecimal(1, new BigDecimal(caseId));
			
			ps.execute();
			ResultSet rset = ps.getResultSet();
			if(rset.next()) {
				isCaseApp = rset.getBoolean(IS_CASE_APP);	 
}
			return isCaseApp;
			
		}catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	
	}
}
