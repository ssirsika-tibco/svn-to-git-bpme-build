package com.tibco.bpm.acecannon.controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.tibco.bpm.acecannon.ACEDAO;
import com.tibco.bpm.acecannon.AceMain;
import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.network.HTTPCaller;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JobQueueController
{
	public static enum PayloadBlockType
	{

		// *** Type value MUST be 2 characters ***

		MESSAGE_ID("ID"),
		MESSAGE_BODY("BO"),
		CORRELATION_ID("CO"),
		EXECUTION_ENVIRONMENT("EE"),
		MESSAGE_ACK("AK"),
		RETRY_COUNT("RC"),
		ORIGINAL_ENQUEUE_TIME("QT"),
		QUIET_MESSAGE("QM");

		private String type;

		private PayloadBlockType(String type)
		{
			this.type = type;
		}

		public String getType()
		{
			return this.type;
		}

		public static PayloadBlockType fromString(String type)
		{
			for (PayloadBlockType value : PayloadBlockType.values())
			{
				if (value.getType().equals(type))
				{
					return value;
				}
			}

			return null;
		}
	}

	public static class BackgroundThread extends Thread
	{
		private static final String	THREAD_NAME	= "JQC-background";

		JobQueueController			controller	= null;

		private boolean				running		= true;

		public BackgroundThread(JobQueueController controller)
		{
			super(THREAD_NAME);
			this.controller = controller;
		}

		public void term()
		{
			running = false;
			interrupt();
		}

		public void run()
		{
			while (running)
			{
				controller.updateRelativeTimes();
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					//ignore
				}
			}
			AceMain.log("JobQueue background thread exiting");
		}
	}

	public static final String		SQL_READ		= "SELECT now(), message_id, correlation_id, priority, delay, payload, enq_time, retry_count FROM cdm_job_queue ORDER BY message_id";

	private Long					lastRefreshTime	= null;

	private Stage					stage;

	@FXML
	TableView<JobBean>				tvJobs;

	@FXML
	TableColumn<JobBean, String>	tcMessageId;

	@FXML
	TableColumn<JobBean, String>	tcCorrelationId;

	@FXML
	TableColumn<JobBean, Long>		tcDelay;

	@FXML
	TableColumn<JobBean, Long>		tcEnqTime;

	@FXML
	TableColumn<JobBean, String>	tcMessageBody;

	@FXML
	TableColumn<JobBean, Integer>	tcRetryCount;

	@FXML
	TableColumn<JobBean, Integer>	tcPriority;

	private long					serverTimeOffset;

	private BackgroundThread		backgroundThread;

	@FXML
	Button							buttonDelete;

	@FXML
	Button							buttonCreate;

	@FXML
	TextField						tfDelay;

	@FXML
	TableColumn<JobBean, String>	tcExecutionEnvironment;

	private AceMain					main;

	private ACEDAO					dao;

	public static JobQueueController makeStage(ACEDAO dao)
	{
		JobQueueController controller = null;
		FXMLLoader loader = new FXMLLoader(AceMain.class.getResource("fxml/jobqueue.fxml"));
		try
		{
			Parent load = loader.load();
			controller = loader.getController();

			controller.stage = new Stage();
			controller.stage.setTitle("Job Queue - ACE Cannon");
			controller.stage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
			Scene scene = new Scene(load);
			controller.stage.setScene(scene);
			controller.stage.setWidth(1070);
			controller.stage.setHeight(600);
			controller.dao = dao;
			controller.init();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return controller;
	}

	public void init()
	{
		tcMessageId.setCellValueFactory(new PropertyValueFactory<JobBean, String>("messageId"));
		tcCorrelationId.setCellValueFactory(new PropertyValueFactory<JobBean, String>("correlationId"));
		tcPriority.setCellValueFactory(new PropertyValueFactory<JobBean, Integer>("priority"));
		tcDelay.setCellValueFactory(new PropertyValueFactory<JobBean, Long>("delayRelative"));
		tcMessageBody.setCellValueFactory(new PropertyValueFactory<JobBean, String>("messageBody"));
		tcEnqTime.setCellValueFactory(new PropertyValueFactory<JobBean, Long>("enqTimeRelative"));
		tcRetryCount.setCellValueFactory(new PropertyValueFactory<JobBean, Integer>("retryCount"));
		tcExecutionEnvironment.setCellValueFactory(new PropertyValueFactory<JobBean, String>("executionEnvironment"));

		populate();
		backgroundThread = new BackgroundThread(this);
		backgroundThread.start();
	}

	private void populate()
	{
		synchronized (tvJobs)
		{
			ObservableList<JobBean> fetchJobs = fetchJobs();
			tvJobs.setItems(fetchJobs);
			if (backgroundThread != null)
			{
				backgroundThread.interrupt();
			}
		}
	}

	public void term()
	{
		if (backgroundThread != null)
		{
			backgroundThread.term();
		}
	}

	public Stage getStage()
	{
		// TODO Auto-generated method stub
		return stage;
	}

	private Connection getConnection() throws SQLException
	{
		Connection conn = dao.requestConnection();
		return conn;
	}

	private void releaseConnection(Connection conn) throws SQLException
	{
		conn.close();
	}

	public static class JobBean
	{
		private StringProperty	messageId;

		private StringProperty	correlationId;

		private IntegerProperty	priority;

		private LongProperty	delay;

		private LongProperty	delayRelative;

		private StringProperty	payload;

		private StringProperty	messageBody;

		private LongProperty	enqTime;

		private LongProperty	enqTimeRelative;

		private IntegerProperty	retryCount;

		private StringProperty	executionEnvironment;

		public JobBean()
		{
			messageId = new SimpleStringProperty();
			correlationId = new SimpleStringProperty();
			priority = new SimpleIntegerProperty();
			delay = new SimpleLongProperty();
			delayRelative = new SimpleLongProperty();
			payload = new SimpleStringProperty();
			messageBody = new SimpleStringProperty();
			enqTime = new SimpleLongProperty();
			enqTimeRelative = new SimpleLongProperty();
			retryCount = new SimpleIntegerProperty();
			executionEnvironment = new SimpleStringProperty();
		}

		public StringProperty messageIdProperty()
		{
			return messageId;
		}

		public StringProperty correlationIdProperty()
		{
			return correlationId;
		}

		public IntegerProperty priorityProperty()
		{
			return priority;
		}

		public LongProperty delayProperty()
		{
			return delay;
		}

		public LongProperty delayRelativeProperty()
		{
			return delayRelative;
		}

		public StringProperty payloadProperty()
		{
			return payload;
		}

		public StringProperty messageBodyProperty()
		{
			return messageBody;
		}

		public LongProperty enqTimeProperty()
		{
			return enqTime;
		}

		public LongProperty enqTimeRelativeProperty()
		{
			return enqTimeRelative;
		}

		public IntegerProperty retryCountProperty()
		{
			return retryCount;
		}

		public StringProperty executionEnvironmentProperty()
		{
			return executionEnvironment;
		}
	}

	public void updateRelativeTimes()
	{
		boolean autoRefresh = false;
		synchronized (tvJobs)
		{
			ObservableList<JobBean> items = tvJobs.getItems();
			if (items != null)
			{
				for (JobBean bean : items)
				{
					long delay = bean.delayProperty().get();
					long enqTime = bean.enqTimeProperty().get();
					long localNow = System.currentTimeMillis();
					long delayRelative = (delay - (localNow + serverTimeOffset));
					// Refresh if this job should have been processed by now.
					// Limit auto-refresh interval to prevent hammering.
					if (delayRelative < -1500 && !autoRefresh)
					{
						if (lastRefreshTime == null || ((System.currentTimeMillis() - lastRefreshTime) >= 5000))
						{
							AceMain.log("Auto-refreshing...");
							autoRefresh = true;
						}
						//						else
						//						{
						//							AceMain.log("Too soon to auto-refresh");
						//						}
					}
					long enqTimeRelative = (enqTime - (localNow + serverTimeOffset));
					bean.delayRelativeProperty().set(Math.max(0, delayRelative / 1000));
					bean.enqTimeRelativeProperty().set(Math.min(0, enqTimeRelative / 1000));
				}
			}
		}
		if (autoRefresh)
		{
			populate();
			lastRefreshTime = System.currentTimeMillis();
		}
	}

	private ObservableList<JobBean> fetchJobs()
	{
		ObservableList<JobBean> result = FXCollections.observableArrayList();
		Connection conn = null;
		PreparedStatement ps = null;
		try
		{
			conn = getConnection();
			if (conn != null)
			{
				ps = conn.prepareStatement(SQL_READ);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					long serverTime = rs.getTimestamp(1).getTime();
					serverTimeOffset = serverTime - System.currentTimeMillis();
					JobBean bean = new JobBean();

					bean.messageIdProperty().set(rs.getBigDecimal("message_id").toBigInteger().toString());
					bean.correlationIdProperty().set(rs.getString("correlation_id"));
					bean.priorityProperty().set(rs.getInt("priority"));
					bean.delayProperty().set(rs.getTimestamp("delay").getTime());
					try
					{
						byte[] payload = rs.getBytes("payload");
						Map<PayloadBlockType, byte[]> payloadMap = parsePayload(payload);
						byte[] messageBodyBytes = payloadMap.get(PayloadBlockType.MESSAGE_BODY);
						byte[] eeBytes = payloadMap.get(PayloadBlockType.EXECUTION_ENVIRONMENT);
						if (eeBytes != null)
						{
							String eeString = new String(eeBytes, "UTF-8");
							bean.executionEnvironmentProperty().set(eeString);
						}
						String messageBodyString = new String(messageBodyBytes, "UTF-8");
						bean.payloadProperty().set(new String(payload, "UTF-8"));
						bean.messageBodyProperty().set(messageBodyString);
					}
					catch (UnsupportedEncodingException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bean.enqTimeProperty().set(rs.getTimestamp("enq_time").getTime());
					bean.retryCountProperty().set(rs.getInt("retry_count"));
					result.add(bean);
				}
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if (ps != null)
			{
				try
				{
					ps.close();
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (conn != null)
			{
				try
				{
					releaseConnection(conn);
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	@FXML
	public void onButtonRefreshClicked()
	{
		populate();
	}

	public Map<PayloadBlockType, byte[]> parsePayload(byte[] payload)
	{
		Map<PayloadBlockType, byte[]> payloadMap = new HashMap<>();

		// Check that the payload contains at least one empty
		// block, even if the payload block value is empty it 
		// should still have a Type (2 bytes) and Length (4 bytes)
		int size = payload.length;
		if (size >= 6)
		{
			// Read the message payload and extract each individual payload block
			int index = 0;
			while (payload[index] != 0 && index < size)
			{
				// Read the payload block type
				char[] typeBytes = new char[2];
				typeBytes[0] = (char) payload[index++];
				typeBytes[1] = (char) payload[index++];
				PayloadBlockType type = PayloadBlockType.fromString(String.valueOf(typeBytes));

				// Read the payload block value length
				int len = ((payload[index + 3] & 0xFF) << 24) + ((payload[index + 2] & 0xFF) << 16)
						+ ((payload[index + 1] & 0xFF) << 8) + (payload[index] & 0xFF);
				index += 4;

				// Check the length is reasonable - doesn't exceed the total length
				// If it does exceed then this is probably a single value payload
				if ((index + len) <= size)
				{

					// Read the value of the payload block
					byte[] value = new byte[len];
					for (int idx = 0; idx < len && index < size; idx++)
					{
						value[idx] = payload[index++];
					}

					// If the type is null then we have just read a new block
					// that we don't know about, or this payload doesn't contain
					// multiple payloads. Either way don't add it to the list.
					if (type != null)
					{
						payloadMap.put(type, value);
					}
				}
			}
		}

		return payloadMap;
	}

	@FXML
	public void onButtonDeleteClicked()
	{
		//		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getXX() + "/autoPurge/cancelJobs";
		//		HTTPCaller caller = HTTPCaller.newGet(url, ConfigManager.INSTANCE.getCookie());
		//		try
		//		{
		//			caller.call();
		//			populate();
		//		}
		//		catch (IOException e)
		//		{
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
	}

	@FXML
	public void onButtonCreateClicked()
	{
		//		String url = ConfigManager.INSTANCE.getActiveProfile().getURLs().getXX() + "/autoPurge/createJob?$delay=";
		//		int delay = Integer.parseInt(tfDelay.getText().trim());
		//		url += delay;
		//		HTTPCaller caller = HTTPCaller.newGet(url, ConfigManager.INSTANCE.getCookie());
		//		try
		//		{
		//			caller.call();
		//			populate();
		//		}
		//		catch (IOException e)
		//		{
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//		populate();
	}
}
