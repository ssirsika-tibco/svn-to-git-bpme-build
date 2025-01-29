package com.tibco.bpm.cdm.core.queue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.api.exception.TransientPersistenceException;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.cdm.core.model.Job;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.bpm.msg.aq.DqdMessage;
import com.tibco.bpm.msg.pq.CloudPQ;
import com.tibco.bpm.msg.pq.PQMessage;

/**
 * An internal queue  CDM uses to post jobs to itself for later
 * (asynchronous) processing.
 * 
 * @author smorgan
 * @since 2019
 */
public class JobQueueImpl extends CloudPQ implements JobQueue
{
	private static CLFClassContext		logCtx		= CloudLoggingFramework.init(JobQueueImpl.class,
			CDMLoggingInfo.instance);

	private static final ObjectMapper	om			= new ObjectMapper();

	private static final String			QUEUE_NAME	= "cdm_job_queue";

	private DataSource					dataSource	= null;

	private Connection					connection	= null;

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	private Connection getConnection() throws PersistenceException
	{
		CLFMethodContext clf = logCtx.getMethodContext("getConnection");
		try
		{
			Connection conn = DataSourceUtils.getConnection(dataSource);
			if (conn == null)
			{
				throw TransientPersistenceException.newNotConnected();
			}
			clf.local.trace("Got connection: %s", conn.toString());
			return conn;
		}
		catch (CannotGetJdbcConnectionException e)
		{
			throw TransientPersistenceException.newNotConnected(e);
		}
	}

	private void releaseConnection(Connection conn)
	{
		CLFMethodContext clf = logCtx.getMethodContext("releaseConnection");
		DataSourceUtils.releaseConnection(conn, dataSource);
		clf.local.trace("Released connection: %s", conn.toString());
	}

	/* (non-Javadoc)
	 * @see com.tibco.bpm.cm.core.queue.JobQueue#enqueueJob(com.tibco.bpm.cm.core.model.Job, java.lang.String, int)
	 */
	@Override
	public void enqueueJob(Job job, String correlationId, int delay) throws PersistenceException, QueueException
	{
		// delay - How long to delay processing the message either time in seconds or DELAY_NONE, DELAY_FOREVER
		CLFMethodContext clf = logCtx.getMethodContext("perform");
		clf.local.trace("enter");
		clf.local.debug("Enqueue Job: %s, CorrelationId: %s, delay: %d", job, correlationId, delay);
		String jobJSON;
		try
		{
			jobJSON = om.writeValueAsString(job);
		}
		catch (JsonProcessingException e)
		{
			throw new QueueException(e);
		}

		Connection connection = null;
		try
		{
			connection = getConnection();

			try
			{
				int priority = Job.getPriorityForMethod(job.getMethod());
				enqueueMessage(QUEUE_NAME, jobJSON.getBytes(StandardCharsets.UTF_8), correlationId, delay, priority,
						null, null, false, connection);
			}
			catch (Exception e)
			{
				// Wrap to something more specific
				throw new QueueException(e);
			}
		}
		finally
		{
			if (connection != null)
			{
				releaseConnection(connection);
			}
		}
	}

	public void blockForSeconds(int seconds)
	{
		CLFMethodContext clf = logCtx.getMethodContext("blockForSeconds");

		try
		{
			clf.local.debug("waiting %s seconds for a message", String.valueOf(seconds));

			waitForMessage(QUEUE_NAME, seconds);
		}
		catch (Exception e)
		{
			clf.local.warn(e, "waitForMessage threw an exception " + e.getMessage());
		}
	}

	public PQMessage getNextJob() throws PersistenceException, QueueException
	{
		PQMessage result = null;

		try
		{
			connection = getConnection();

			do
			{
				try
				{
					byte[] fetchMsgId = null;

					result = dequeueMessage(QUEUE_NAME, fetchMsgId, null, connection);
				}
				catch (NullPointerException e)
				{
					// should not happen but ignore and try again

					Thread.sleep(1000L); // avoid possibility of tight loop by 1 sec tick
				}
			}
			while (null == result);

			return result;
		}

		catch (Exception e)
		{
			throw new QueueException(e);
		}
		finally
		{
			if (connection != null)
			{
				releaseConnection(connection);
			}
		}

	}

	@Override
	public void purgeJobsWithCorrelationId(String correlationId) throws PersistenceException, QueueException
	{
		CLFMethodContext clf = logCtx.getMethodContext("purgeJobsWithCorrelationId");
		try
		{
			connection = getConnection();
			DqdMessage message = null;
			do
			{
				byte[] fetchMsgId = null;
				PQMessage msg = dequeueMessage(QUEUE_NAME, fetchMsgId, correlationId, connection);
				message = msg.getMessage();

				if (message != null)
				{
					clf.local.debug("Flushed unwanted message with correlationId=%s: %s", correlationId, message);
				}
			}
			while (message != null);
		}
		catch (Exception e)
		{
			throw new QueueException(e);
		}
		finally
		{
			if (connection != null)
			{
				releaseConnection(connection);
			}
		}
	}
}
