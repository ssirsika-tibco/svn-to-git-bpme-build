package com.tibco.bpm.acecannon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.UI.Position;
import com.tibco.bpm.acecannon.container.DockerCLI;
import com.tibco.bpm.acecannon.controllers.AdminPropertiesController;
import com.tibco.bpm.acecannon.controllers.AppsController;
import com.tibco.bpm.acecannon.controllers.CLNsController;
import com.tibco.bpm.acecannon.controllers.HealthController;
import com.tibco.bpm.acecannon.controllers.JobQueueController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class MainController
{
	AppsController				appsController				= null;

	AdminPropertiesController	adminPropertiesController	= null;

	HealthController			healthController			= null;

	private ACEDAO				dao;

	@FXML
	TextField					tfApplicationId;

	@FXML
	TextField					tfVersion;

	@FXML
	TextField					tfResourcePath;

	@FXML
	Button						buttonDeploy;

	@FXML
	TextField					tfTypeId;

	@FXML
	TextArea					taCasedata;

	@FXML
	Label						lCasedataInfo;

	@FXML
	Button						buttonCreateCases;

	@FXML
	ImageView					ivImageView;

	private ContainerManager	containerManager;

	@FXML
	ChoiceBox<String>			cbProfile;

	private CLNsController		clnsController;

	private JobQueueController	jobQueueController;

	@FXML
	public void initialize()
	{
		dao = new ACEDAO();
		ivImageView.setImage(new Image(AceMain.class.getResourceAsStream("images/icon-900.png")));
		List<String> profileNames = ConfigManager.INSTANCE.getConfig().getProfileNames();
		String activeProfileName = ConfigManager.INSTANCE.getConfig().getActiveProfileName();
		cbProfile.getItems().addAll(profileNames);
		cbProfile.getSelectionModel().select(activeProfileName);
		cbProfile.getSelectionModel().selectedIndexProperty().addListener((obs, old, nu) -> {
			if (!(cbProfile.getItems().get(nu.intValue())
					.equals(ConfigManager.INSTANCE.getConfig().getActiveProfileName())))
			{
				new Alert(AlertType.INFORMATION, "Profile change will occur next time ACE Cannon is restarted")
						.showAndWait();
			}
		});
	}

	public void saveChildStageInfo()
	{
		if (jobQueueController != null)
		{
			jobQueueController.term();
		}
		if (appsController != null)
		{
			Stage dStage = appsController.getStage();
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("applications", dStage.getX(), dStage.getY(),
					dStage.getWidth(), dStage.getHeight(), dStage.isMaximized());
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("applications", "splitHorizontal",
					appsController.spHorizontal.getDividerPositions()[0]);
			ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("applications", "splitVertical",
					appsController.spVertical.getDividerPositions()[0]);
			appsController.saveChildStageInfo();
		}
		if (containerManager != null)
		{
			Stage tStage = containerManager.getStage();
			ConfigManager.INSTANCE.getConfig().getUI().setPosition("containers", tStage.getX(), tStage.getY(),
					tStage.getWidth(), tStage.getHeight(), tStage.isMaximized());
			containerManager.term();
		}
		if (healthController != null)
		{
			healthController.saveStagePosition();
		}
		if (clnsController != null)
		{
			clnsController.saveStagePosition();
		}
	}

	protected static String readInputStreamToString(InputStream inputStream) throws IOException
	{
		char[] buffer = new char[1024];
		StringBuilder buf = new StringBuilder();
		Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		int count = 0;
		while (count >= 0)
		{
			count = in.read(buffer, 0, buffer.length);
			if (count > 0)
			{
				buf.append(buffer, 0, count);
			}
		}
		return buf.toString();
	}

	@FXML
	public void onMnuToolsDockerContainersAction(ActionEvent event)
	{
		if (containerManager == null)
		{
			DockerCLI dockerCLI = new DockerCLI();
			containerManager = new ContainerManager(this, dockerCLI);
			dockerCLI.setLogger(containerManager.getLogger());
			Stage childStage = ContainerManager.makeStage(containerManager);
			childStage.setOnCloseRequest(ev -> {
				ConfigManager.INSTANCE.getConfig().getUI().setPosition("containers", childStage.getX(),
						childStage.getY(), childStage.getWidth(), childStage.getHeight(), childStage.isMaximized());
				containerManager.term();
				containerManager = null;
			});
			Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("containers");
			if (position != null)
			{
				Double x = position.getX();
				Double y = position.getY();
				if (x != null)
				{
					childStage.setX(x);
				}
				if (y != null)
				{
					childStage.setY(y);
				}
				Double width = position.getWidth();
				Double height = position.getHeight();
				if (position.getIsMaximised())
				{
					childStage.setMaximized(true);
				}
				else
				{
					if (width != null)
					{
						childStage.setWidth(width);
					}
					if (height != null)
					{
						childStage.setHeight(height);
					}
				}
			}
			childStage.show();
			new Thread(() -> {
				try
				{
					Thread.sleep(200);
				}
				catch (Exception e)
				{
					// ignore
				}
				containerManager.getInfo();
			}).start();
		}
		else
		{
			containerManager.getStage().toFront();
		}
	}

	@FXML
	public void onMnuToolsAdminPropertiesAction(ActionEvent event)
	{
		if (adminPropertiesController != null)
		{
			adminPropertiesController.getStage().toFront();
		}
		else
		{
			try
			{
				AdminPropertiesController ctrl = AdminPropertiesController.make();
				adminPropertiesController = ctrl;
				Stage cstage = ctrl.getStage();
				cstage.setOnCloseRequest(ev -> {
					ctrl.saveStagePosition();
					adminPropertiesController = null;
				});
				Stage dStage = ctrl.getStage();
				ctrl.setStagePosition();
				dStage.show();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void onMnuToolsApplicationsAction(ActionEvent event)
	{
		if (appsController == null)
		{
			try
			{
				appsController = AppsController.make();
				appsController.setDAO(dao);
				Stage childStage = appsController.getStage();
				childStage.setOnCloseRequest(ev -> {
					ConfigManager.INSTANCE.getConfig().getUI().setPosition("applications", childStage.getX(),
							childStage.getY(), childStage.getWidth(), childStage.getHeight(), childStage.isMaximized());
					ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("applications", "splitHorizontal",
							appsController.spHorizontal.getDividerPositions()[0]);
					ConfigManager.INSTANCE.getConfig().getUI().setStateProperty("applications", "splitVertical",
							appsController.spVertical.getDividerPositions()[0]);
					appsController = null;
				});
				Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("applications");
				if (position != null)
				{
					if (position.getIsMaximised())
					{
						childStage.setMaximized(true);
					}
					else
					{
						Double x = position.getX();
						Double y = position.getY();
						if (x != null)
						{
							childStage.setX(x);
						}
						if (y != null)
						{
							childStage.setY(y);
						}
						Double width = position.getWidth();
						Double height = position.getHeight();
						if (width != null)
						{
							childStage.setWidth(width);
						}
						if (height != null)
						{
							childStage.setHeight(height);
						}
					}
				}
				childStage.show();
				appsController.initSplit();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			appsController.getStage().toFront();
		}
	}

	public void onContainerManagerClose()
	{
		containerManager = null;
	}

	@FXML
	public void onMnuToolsHealthAction(ActionEvent event)
	{
		if (healthController != null)
		{
			healthController.getStage().toFront();
		}
		else
		{
			try
			{
				HealthController ctrl = HealthController.make(this);
				healthController = ctrl;
				Stage cstage = ctrl.getStage();
				cstage.setOnCloseRequest(ev -> {
					ctrl.saveStagePosition();
					healthController = null;
				});
				Stage dStage = ctrl.getStage();
				ctrl.populate();
				Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("health");
				if (position != null)
				{
					Double x = position.getX();
					Double y = position.getY();
					if (x != null)
					{
						dStage.setX(x);
					}
					if (y != null)
					{
						dStage.setY(y);
					}
					Double width = position.getWidth();
					Double height = position.getHeight();
					if (position.getIsMaximised())
					{
						dStage.setMaximized(true);
					}
					else
					{
						if (width != null)
						{
							dStage.setWidth(width);
						}
						if (height != null)
						{
							dStage.setHeight(height);
						}
					}
				}
				dStage.show();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onHealthClose(HealthController hc)
	{
		if (hc == healthController)
		{
			healthController = null;
		}
	}

	@FXML
	public void onMnuToolsYYCLNsAction(ActionEvent event)
	{
		if (clnsController != null)
		{
			clnsController.getStage().toFront();
		}
		else
		{
			try
			{
				CLNsController ctrl = CLNsController.make();
				clnsController = ctrl;
				Stage cstage = ctrl.getStage();
				cstage.setOnCloseRequest(ev -> {
					ctrl.saveStagePosition();
					clnsController = null;
				});
				Stage dStage = ctrl.getStage();
				ctrl.setStagePosition();
				dStage.show();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void openJobQueue()
	{
		if (jobQueueController != null)
		{
			jobQueueController.getStage().toFront();
		}
		else
		{
			jobQueueController = JobQueueController.makeStage(dao);
			Stage stage = jobQueueController.getStage();
			//		reportsController.run();
			stage.setOnCloseRequest(ev -> {
				jobQueueController.term();
				jobQueueController = null;
			});
			stage.show();
		}
	}

	@FXML
	public void onMnuToolsJobQueueAction(ActionEvent event)
	{
		openJobQueue();
	}
}
