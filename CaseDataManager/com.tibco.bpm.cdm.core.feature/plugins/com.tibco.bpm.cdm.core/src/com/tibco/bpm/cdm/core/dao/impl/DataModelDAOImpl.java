package com.tibco.bpm.cdm.core.dao.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.dao.DataModelDAO;
import com.tibco.bpm.cdm.core.dao.DataModelDependencyDAO.Dependency;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * PostgreS implementation of the DataModelDAO interface that persists data models in the cdm_datamodels database table.
 * @author smorgan
 * @since 2019
 */
public class DataModelDAOImpl extends AbstractDAOImpl implements DataModelDAO
{
	private static CLFClassContext	logCtx					= CloudLoggingFramework.init(DataModelDAOImpl.class,
			CDMLoggingInfo.instance);

	// Insert into cdm_datamodels, auto-populating id and deployment_timestamp, returning id
	protected String		SQL_CREATE				= "INSERT INTO cdm_datamodels (id, application_id, major_version, namespace, model,script) VALUES "
			+ "(NEXTVAL('cdm_datamodels_seq'), ?, ?, ?, ?, ?)";

	private static final String		SQL_READ				= "SELECT id, model FROM cdm_datamodels WHERE application_id = ? AND "
			+ "namespace = ? AND major_version = ?";

	private static final String		SQL_READ_BY_NS_AND_VER	= "SELECT id, model FROM cdm_datamodels WHERE namespace = ? AND major_version = ?";

	private static final String		SQL_READ_BY_ID			= "SELECT id, model FROM cdm_datamodels WHERE id = ?";

	private static final String		SQL_READ_ID_BY_APPS		= "SELECT id FROM cdm_datamodels WHERE application_id IN "
			+ "(SELECT id FROM cdm_applications WHERE application_id = ?) AND major_version = ?";

	private static final String		SQL_READ_BY_APPS		= "SELECT id, model FROM cdm_datamodels WHERE application_id IN (%s)";

	private static final String		SQL_UPDATE				= "UPDATE cdm_datamodels SET model = ?, script = ? WHERE id = ?";

	private static final String		SQL_COUNT				= "SELECT count(*) FROM cdm_datamodels d INNER JOIN cdm_applications a "
			+ "ON d.application_id = a.id AND a.deployment_id = ?";

	private static final String		SQL_SCRIPT_READ			= "SELECT id, script, namespace FROM cdm_datamodels WHERE id = ?";

	// Get id/script for all datamodels on which the given datamodel depends
	private static final String		SQL_SCRIPT_DEPS			= "SELECT id, script, namespace FROM cdm_datamodels WHERE id IN "
			+ "(SELECT datamodel_id_to FROM cdm_datamodel_deps WHERE datamodel_id_from = ?)";

	// Get models that the given model depends on.
	private static final String		SQL_GET_DEPENDENCIES	= "SELECT id, model FROM cdm_datamodels WHERE id IN "
			+ "(SELECT datamodel_id_to FROM cdm_datamodel_deps WHERE datamodel_id_from = ?)";

	private static final String		SQL_EXISTS_OTHER_APP	= "SELECT count(*) FROM cdm_datamodels WHERE "
			+ "namespace = ? AND major_version = ? AND application_id <> ?";

	private static final String		SQL_APPLICATION_IDS		= "SELECT id FROM cdm_applications ORDER BY id";

	
	public DataModelDAOImpl(DataSource dataSource) {
		setDataSource(dataSource);
	}
	
	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#create(java.math.BigInteger, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public BigInteger create(BigInteger applicationId, int majorVersion, String namespace, String model, String script)
			throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("create");
		clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger dataModelId = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_CREATE, new String[]{"id"});
			ps.setBigDecimal(1, new BigDecimal(applicationId));
			ps.setInt(2, majorVersion);
			ps.setString(3, namespace);
			ps.setString(4, model);
			ps.setString(5, script);

			int success = ps.executeUpdate();
			ResultSet rset = ps.getGeneratedKeys();
			if (rset.next())
			{
				dataModelId = rset.getBigDecimal(1).toBigInteger();
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

		return dataModelId;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#getCountByDeploymentId(java.math.BigInteger)
	 */
	@Override
	public long getCountByDeploymentId(BigInteger deploymentId) throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		long count = 0;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_COUNT);
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
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#read(java.math.BigInteger)
	 */
	@Override
	public DataModelInfo read(BigInteger dataModelId) throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		String model = null;
		DataModelInfo result = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_READ_BY_ID);
			ps.setBigDecimal(1, new BigDecimal(dataModelId));

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					id = rset.getBigDecimal(1).toBigInteger();
					model = rset.getString(2);
					result = new DataModelInfo(id, null, null, model, null, false);
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

		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#read(java.math.BigInteger, java.lang.String, int)
	 */
	@Override
	public DataModelInfo read(BigInteger applicationId, String namespace, int majorVersion) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("read");
		clf.addMethodAttribute(CommonMetaData.APPLICATION_ID, applicationId);
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		String model = null;
		DataModelInfo result = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_READ);
			ps.setBigDecimal(1, new BigDecimal(applicationId));
			ps.setString(2, namespace);
			ps.setInt(3, majorVersion);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					id = rset.getBigDecimal(1).toBigInteger();
					model = rset.getString(2);
					result = new DataModelInfo(id, null, null, model, null, true);
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

		return result;
	}

	private void loadDependencies(DataModelInfo initialModel) throws InternalException, PersistenceException
	{
		// Add the model to a list of models for which we need to load dependencies.
		// We'll add any such models to this list, to ensure we get _their_ dependencies too.
		List<DataModelInfo> modelsToProcess = new ArrayList<>();
		modelsToProcess.add(initialModel);

		// We'll keep track of all the models we've seen, for two purposes:
		// (1) To avoid re-deserializing a model when it appears multiple times in the dependency hierarchy.
		// (2) To avoid getting stuck in a endless loop if a circular reference exists (theoretically impossible)
		List<DataModelInfo> modelCache = new ArrayList<>();

		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();
			

			// We'll reuse this PreparedStatement as we walk the dependency tree
			ps = conn.prepareStatement(SQL_GET_DEPENDENCIES);

			// Keep going until there's nothing else to do.
			while (!modelsToProcess.isEmpty())
			{
				DataModelInfo info = modelsToProcess.remove(0);
				BigInteger id = info.getId();
				ps.setBigDecimal(1, new BigDecimal(id));
				boolean success = ps.execute();
				if (success)
				{
					ResultSet rset = ps.getResultSet();
					while (rset.next())
					{
						BigInteger dependencyId = rset.getBigDecimal(1).toBigInteger();

						// Have we already seen this model elsewhere in the dependency hierarchy?
						DataModelInfo cachedInfo = modelCache.stream().filter(i -> i.getId().equals(dependencyId))
								.findFirst().orElse(null);

						DataModel modelToLink = null;
						if (cachedInfo != null)
						{
							// Yes - so use this existing cached model and don't bother adding
							// this to our working list (as it's already being processed)
							modelToLink = cachedInfo.getDataModel();
						}
						else
						{
							// No - so add this to the cache in case we encounter it again later, and also
							// add to our working list to ensure we fetch _its_ dependencies too.
							//DataModelInfo dependencyInfo = new DataModelInfo(dependencyId, )
							String dependencyDataModelJson = rset.getString(2);
							DataModel dependencyDataModel = DataModel.deserialize(dependencyDataModelJson);
							DataModelInfo dependencyInfo = new DataModelInfo(dependencyId, null, dependencyDataModel,
									dependencyDataModelJson, null, false);
							modelCache.add(dependencyInfo);
							modelsToProcess.add(dependencyInfo);
							modelToLink = dependencyDataModel;
						}

						// Connect our model to this model that it depends on
						info.getDataModel().getForeignModels().add(modelToLink);
					}
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		catch (DataModelSerializationException e)
		{
			// Shouldn't be possible for a bad model to be in the database.
			throw InternalException.newInternalException(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#read(java.lang.String, int, boolean)
	 */
	@Override
	public DataModelInfo read(String namespace, int majorVersion, boolean loadDependencies)
			throws PersistenceException, InternalException
	{
		CLFMethodContext clf = logCtx.getMethodContext("read");
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		BigInteger id = null;
		String model = null;
		DataModelInfo info = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_READ_BY_NS_AND_VER);
			ps.setString(1, namespace);
			ps.setInt(2, majorVersion);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					id = rset.getBigDecimal(1).toBigInteger();
					model = rset.getString(2);
					DataModel dm = DataModel.deserialize(model);
					info = new DataModelInfo(id, null, dm, model, null, true);
					if (loadDependencies)
					{
						loadDependencies(info);
					}
				}
			}
		}
		catch (SQLException e)
		{
			throw PersistenceException.newRepositoryProblem(e);
		}
		catch (DataModelSerializationException e)
		{
			throw InternalException.newInternalException(e);
		}
		finally
		{
			cleanUp(ts, ps, conn);
		}

		return info;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#update(java.math.BigInteger, java.lang.String, java.lang.String)
	 */
	@Override
	public void update(BigInteger dataModelId, String model, String script) throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_UPDATE);
			ps.setString(1, model);
			ps.setString(2, script);
			ps.setBigDecimal(3, new BigDecimal(dataModelId));

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

	private List<BigInteger> getIdsByApplication(Connection conn, String applicationId, int majorVersion)
			throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("read");
		List<BigInteger> result = new ArrayList<>();
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{

			ps = conn.prepareStatement(SQL_READ_ID_BY_APPS);
			ps.setString(1, applicationId);
			ps.setInt(2, majorVersion);

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					BigInteger dataModelId = rset.getBigDecimal(1).toBigInteger();
					result.add(dataModelId);
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

		return result;

	}

	private List<DataModelInfo> flatten(List<DataModelInfo> infos, List<Dependency> deps)
	{
		List<DataModelInfo> result = new ArrayList<>();
		List<Dependency> workingDeps = new ArrayList<>(deps);

		while (!workingDeps.isEmpty())
		{
			List<BigInteger> froms = workingDeps.stream().map(Dependency::getFrom).collect(Collectors.toList());
			List<BigInteger> tos = workingDeps.stream().map(Dependency::getTo).collect(Collectors.toList());

			// Work out which items have no unmet depedencies (i.e. are on the 'to' side,
			// but not the 'from' side;
			List<BigInteger> work = new ArrayList<>();
			work.addAll(tos);
			work.removeAll(froms);

			// For the sake of consistency, sort by id
			Collections.sort(work);

			for (BigInteger id : work)
			{
				DataModelInfo match = infos.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
				result.add(match);

				// Remove processed dependencies
				workingDeps.removeIf(d -> d.getTo().equals(id));
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#readScripts(java.util.List)
	 */
	@Override
	public List<String> readScripts(List<ApplicationIdAndMajorVersion> applications) throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("readScripts");
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;

		List<DataModelInfo> infos = new ArrayList<>();
		List<Dependency> dependencies = new ArrayList<>();

		try
		{
			conn = getConnection();

			// Construct a list of all data models contained in the applications
			List<BigInteger> dataModelIds = new ArrayList<>();
			for (ApplicationIdAndMajorVersion application : applications)
			{
				dataModelIds.addAll(
						getIdsByApplication(conn, application.getApplicationId(), application.getMajorVersion()));
			}
			Collections.sort(dataModelIds);

			// Scripts need to be returned in an order such that they can be evaluated without
			// any chance of forward-referencing. To satisfy this, the dependency graph must be
			// topologically sorted, such that a given script is placed in the list before any
			// that may refer to it.

			List<BigInteger> idsChecked = new ArrayList<>();
			List<BigInteger> idsToCheck = new ArrayList<>();
			idsToCheck.addAll(dataModelIds);

			// Prepare a statement that we be reused as we walk the dependencies
			ps = conn.prepareStatement(SQL_SCRIPT_DEPS);

			while (!idsToCheck.isEmpty())
			{
				BigInteger id = idsToCheck.remove(0);
				idsChecked.add(id);

				ps.setBigDecimal(1, new BigDecimal(id));
				boolean success = ps.execute();

				if (success)
				{
					ResultSet rset = ps.getResultSet();

					while (rset.next())
					{
						BigInteger dId = rset.getBigDecimal(1).toBigInteger();
						String script = rset.getString(2);

						if (script == null)
						{
							script = "// No script exists for " + rset.getString(3);
						}

						// Create an info, populating just the id and script
						// (only if not already in the list)
						if (!infos.stream().map(DataModelInfo::getId).anyMatch(i -> i.equals(dId)))
						{
							DataModelInfo info = new DataModelInfo(dId, null, null, null, script, false);
							infos.add(info);

							// Add to the working list, so we can get _its_ dependencies too
							if (!idsToCheck.contains(dId) && !idsChecked.contains(dId))
							{
								idsToCheck.add(dId);
							}
						}

						// Note the model-to-model dependency. We'll use this to sort the scripts later.
						dependencies.add(new Dependency(id, dId));
					}
				}
			}
			infos = flatten(infos, dependencies);

			// Add top-level models, unless already found in the dependency tree of others
			ps.close();
			ps = null;
			for (BigInteger id : dataModelIds)
			{
				if (!infos.stream().map(DataModelInfo::getId).anyMatch(i -> i.equals(id)))
				{
					if (ps == null)
					{
						ps = conn.prepareStatement(SQL_SCRIPT_READ);
					}
					ps.setBigDecimal(1, new BigDecimal(id));
					boolean success = ps.execute();

					if (success)
					{
						ResultSet rset = ps.getResultSet();
						if (rset.next())
						{
							BigInteger dId = rset.getBigDecimal(1).toBigInteger();
							String script = rset.getString(2);
							if (script == null)
							{
								script = "// No script exists for " + rset.getString(3);
							}
							DataModelInfo info = new DataModelInfo(dId, null, null, null, script, false);
							infos.add(info);
						}
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
		return infos.stream().map(DataModelInfo::getScript).collect(Collectors.toList());
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#readForApplications(java.util.List)
	 */
	@Override
	public List<DataModelInfo> readForApplications(List<BigInteger> applicationIds) throws PersistenceException
	{
		List<DataModelInfo> result = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		try
		{
			conn = getConnection();
			

			String sql = String.format(SQL_READ_BY_APPS, renderCommaSeparatedQuestionMarks(applicationIds.size()));
			ps = conn.prepareStatement(sql);
			int idx = 1;
			for (BigInteger applicationId : applicationIds)
			{
				ps.setBigDecimal(idx++, new BigDecimal(applicationId));
			}

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				while (rset.next())
				{
					BigInteger id = rset.getBigDecimal(1).toBigInteger();
					String model = rset.getString(2);
					DataModelInfo info = new DataModelInfo(id, null, null, model, null, false);
					result.add(info);
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

		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#existsForOtherApplication(java.math.BigInteger, java.lang.String, int)
	 */
	@Override
	public boolean existsForOtherApplication(BigInteger applicationId, String namespace, int majorVersion)
			throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		boolean result = false;
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_EXISTS_OTHER_APP);
			ps.setString(1, namespace);
			ps.setInt(2, majorVersion);
			ps.setBigDecimal(3, new BigDecimal(applicationId));

			boolean success = ps.execute();

			if (success)
			{
				ResultSet rset = ps.getResultSet();
				if (rset.next())
				{
					int count = rset.getInt(1);
					result = count != 0;
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
		return result;
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cdm.core.dao.DataModelDAO#getApplicationIds()
	 */
	@Override
	public List<BigInteger> getApplicationIds() throws PersistenceException
	{
		Connection conn = null;
		PreparedStatement ps = null;
		Statement ts = null;
		List<BigInteger> result = new ArrayList<>();
		try
		{
			conn = getConnection();
			

			ps = conn.prepareStatement(SQL_APPLICATION_IDS);
			ps.execute();
			ResultSet rset = ps.getResultSet();
			while (rset.next())
			{
				result.add(rset.getBigDecimal(1).toBigInteger());
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
		return result;
	}
}
