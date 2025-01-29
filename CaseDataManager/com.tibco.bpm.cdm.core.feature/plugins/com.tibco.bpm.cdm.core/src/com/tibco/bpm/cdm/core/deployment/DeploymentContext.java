package com.tibco.bpm.cdm.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.dt.rasc.VersionRange;

/**
 * A number of ArtifactHandlers are used to handle the various types of artifacts found in a RASC and add things to this context.  
 * Once all artifacts have been processed, this context is used by the actual deployment process.
 * @author smorgan
 * @since 2019
 */
public class DeploymentContext
{
	public static class ApplicationDependency
	{
		private String			id;

		private VersionRange	versionRange;

		public ApplicationDependency(String id, VersionRange versionRange)
		{
			this.id = id;
			this.versionRange = versionRange;
		}

		public String getId()
		{
			return id;
		}

		public VersionRange getVersionRange()
		{
			return versionRange;
		}
	}

	/**
	 * Value object to hold a DataModel and its corresponding JSON source and JS script.
	 */
	public static class DataModelArtifact
	{
		private DataModel	dataModel;

		private String		json;

		private String		script;

		public DataModelArtifact(DataModel dataModel, String json, String script)
		{
			this.dataModel = dataModel;
			this.json = json;
			this.script = script;
		}

		public DataModel getDataModel()
		{
			return dataModel;
		}

		public String getJson()
		{
			return json;
		}

		public String getScript()
		{
			return script;
		}

		public void setScript(String script)
		{
			this.script = script;
		}

		public void setJson(String json)
		{
			this.json = json;
		}

		public void setDataModel(DataModel dm)
		{
			this.dataModel = dm;
		}
	}

	Map<String, DataModelArtifact>	mapOfDataModelArtifacts	= new HashMap<>();

	List<ApplicationDependency>		applicationDependencies	= new ArrayList<>();

	public List<DataModelArtifact> getDataModelArtifacts()
	{
		return mapOfDataModelArtifacts.values().stream().collect(Collectors.toList());
	}

	public Map<String, DataModelArtifact> getDataModelArtifactsMap()
	{
		return mapOfDataModelArtifacts;
	}

	public void addApplicationDependency(String id, VersionRange versionRange)
	{
		applicationDependencies.add(new ApplicationDependency(id, versionRange));
	}

	public List<ApplicationDependency> getApplicationDependencies()
	{
		return applicationDependencies;
	}
}
