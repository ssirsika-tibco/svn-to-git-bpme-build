package com.tibco.bpm.cdm.core;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.tibco.bpm.cdm.api.exception.PersistenceException;
import com.tibco.bpm.cdm.core.logging.CDMLoggingInfo;
import com.tibco.bpm.logging.cloud.api.CLFClassContext;
import com.tibco.bpm.logging.cloud.api.CloudLoggingFramework;
import com.tibco.bpm.logging.cloud.context.CLFMethodContext;
import com.tibco.n2.common.orm.SequenceDAO;
import com.tibco.n2.logging.annotations.metadata.N2LFMethodContext;
import com.tibco.n2.logging.api.N2LogMethodContext;

public class CaseIdDaemonHandlerThread implements Runnable {

	static CLFClassContext				logCtx					= CloudLoggingFramework.init(CaseIdDaemonHandlerThread.class,
			CDMLoggingInfo.instance);

	/*
	 * =====================================================
	 * TYPE : DaemonOperation
	 * =====================================================
	 */
	/**
	 * Interface used to process all daemon operations
	 */
	private interface DaemonOperation
	{
		/*
		 * =====================================================
		 * METHOD : run
		 * =====================================================
		 */
		/**
		 * Performs the operation that the daemon is required to perform
		 */
		public void run();
	}

	/*
	 * =====================================================
	 * TYPE : SequenceUpdateOperation
	 * =====================================================
	 */
	/**
	 * Class used to process the Sequence update operation
	 */
	private class SequenceUpdateOperation implements DaemonOperation
	{
		private CaseSequenceID	sequenceIDObj;

		private int			batchSize;

		private int			idCacheSize;

		private SequenceDAO	sequenceDAO;

		public SequenceUpdateOperation(CaseSequenceID theSequenceIDObj, int theBatchSize, int theIdCacheSize)
		{
			sequenceIDObj = theSequenceIDObj;
			batchSize = theBatchSize;
			idCacheSize = theIdCacheSize;
		}

		/* (non-Javadoc)
		 * @see com.tibco.n2.brm.services.impl.DaemonHandlerThread.DaemonOperation#run()
		 */
		public void run()
		{
			// call the interface on the sequence to trigger it to cache more IDs
			try {
				sequenceIDObj.cacheMoreIDs(batchSize, idCacheSize, sequenceDAO);
			} catch (PersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Controls whether the thread has to stop.
	 */
	private boolean									bStop				= true;

	/**
	 * The current thread for this runnable object
	 */
	private Thread									myThread			= null;

	/**
	 * Queue that already provides sync access
	 */
	private ConcurrentLinkedQueue<DaemonOperation>	daemonOperations	= null;

	/*
	 * =====================================================
	 * CONSTRUCTOR : DaemonHandlerThread
	 * =====================================================
	 */
	/**
	 * Populate the initial class data
	 *
	 */
	public CaseIdDaemonHandlerThread()
	{
		daemonOperations = new ConcurrentLinkedQueue<DaemonOperation>();
	}

	public void addToSequenceUpdateQueue(CaseSequenceID sequenceIDObj, int batchSize, int idCacheSize, SequenceDAO sequenceDAO)
	{
		CLFMethodContext clf = logCtx.getMethodContext("addToSequenceUpdateQueue");
		clf.local.debug("Started addToSequenceUpdateQueue" );

		// Add this item to the queue of messages to be processed, this is
		// already a thread-safe queue so no need to synchronise
		daemonOperations.add(new SequenceUpdateOperation(sequenceIDObj, batchSize, idCacheSize));

		// Now trigger the thread to make sure it wakes up and processes the message
		wakeUpDaemon();

		clf.local.debug("Ended addToSequenceUpdateQueue" );
	}

	/*
	 * =====================================================
	 * METHOD : wakeUpDaemon
	 * =====================================================
	 */
	/**
	 * Wakes up the Daemon to process any messages in the queue
	 *
	 */
	private void wakeUpDaemon()
	{
		CLFMethodContext clf = logCtx.getMethodContext("wakeUpDaemon");
		clf.local.debug("Started wakeUpDaemon" );

		// Now trigger the thread to make sure it wakes up and processes the message
		try
		{
			synchronized (myThread)
			{
				myThread.notify();
			}
		}
		catch (Exception e)
		{
			//wakeUpDaemon.exception(this, BRMExceptionMessages.DAEMON_THREAD_NOTIFY_FAILED, e);
		}
		clf.local.debug("Ended wakeUpDaemon" );
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#start()
	 */
	public void start()
	{
		/*
		 * If the thread is not already running, create the stop object and start it.
		 */
		if (myThread == null)
		{
			bStop = false;
			myThread = new Thread(this);
			myThread.setContextClassLoader(Thread.currentThread().getContextClassLoader());

			myThread.start();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		CLFMethodContext clf = logCtx.getMethodContext("DaemonHandlerThread_run");
		clf.local.debug("Started DaemonHandlerThread_run" );

		// Loop continually until told to stop
		while (bStop == false)
		{
			try
			{
				// Check to see if there is any work in the queue to perform
				while (!daemonOperations.isEmpty())
				{
					// This call will remove and return the oldest element in the queue
					// The queue is self synchronising, and will return null if the
					// queue is empty
					DaemonOperation daemonOp = daemonOperations.poll();

					// Make sure that the message is OK to run
					if (daemonOp != null)
					{
						daemonOp.run();
					}
				}

				// If all the queued messages have been processed then wait for the
				// next notify that something else needs to be done by this thread.
				synchronized (myThread)
				{
					myThread.wait();
				}
			}
			catch (InterruptedException e)
			{
				// We've been woken up
			}
			catch (Exception e)
			{
				//DaemonHandlerThread_run.exception(this, CDMExceptionMessages.DAEMON_THREAD_PROCESSING_FAILED, e);
			}
		}

		clf.local.debug("Ended DaemonHandlerThread_run" );
	}

	/*
	 * =====================================================
	 * METHOD : stopThread
	 * =====================================================
	 */
	/**
	 * Stops this thread and waits for it to complete
	 */
	public void stopThread()
	{
		if (myThread != null)
		{
			bStop = true;
		
			try
			{
				myThread.interrupt();
				myThread.join();
			}
			catch (InterruptedException e)
			{
			}
			
			myThread = null;
		}
	}
}
