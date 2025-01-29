package com.tibco.bpm.acecannon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.tibco.bpm.acecannon.config.ConfigManager;
import com.tibco.bpm.acecannon.config.UI.Position;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// ACE Cannon Wish List:
//
// Create cases from background thread.  Consider multi-threaded option (more for thread safety testing than performance).
// Auto-purge dashboard combining job queue, lastPurge time and config settings
// On deployment, allow override of properties (at least id, version, etc, potentially everything, including editing
// artifact content).
// Make case deletion from CasesController not depend on DB, like how force undeploy works.
// Attribute list to use Text(400), Number(15, 2), etc to indicate constraint
// Edit case: More features... Show diff. Option to overwrite if version mismatch.
// Generic mechanism for existing ability to Preserve split positions (specifically the left/right of Cases)
// Centralised log manager that things can log to. Things can observe logs - all or just certain things.
//  (e.g. lower-left panel on Cases could have a tab that shows just logs resulting from the Cases window (as could
//  other windows).  Main log window (and console, and file...) could show everthing.
// Enable View Cases and Create Cases on Types for all type, but display warning if choosing non-case type
// (to allow testing that server correctly rejects such requests)
// ModelEditor to show Links.
// ModelEditor to show when III is present/absent (checkbox that creates III and then enables text boxes below itself?).
// Convert value from FixedValuesConjurer to appropriate JSON type (to allow use for non-string attributes) - perhaps make explicit choice for -ve testing.
// ConjuringPane to have panel (bottom right?) showing attribute definition (type, constraints, etc.)
// 
public class AceMain extends Application
{
	private static final String	LOGO	= "  _____     ___  _____  _____   _____                               \r\n"
			+ " |A .  |   / _ \\/  __ \\|  ___| /  __ \\                              \r\n"
			+ " | /.\\ |  / /_\\ \\ /  \\/| |__   | /  \\/ __ _ _ __  _ __   ___  _ __  \r\n"
			+ " |(_._)|  |  _  | |    |  __|  | |    / _` | '_ \\| '_ \\ / _ \\| '_ \\ \r\n"
			+ " |  |  |  | | | | \\__/\\| |___  | \\__/\\ (_| | | | | | | | (_) | | | |\r\n"
			+ " |____V|  \\_| |_/\\____/\\____/   \\____/\\__,_|_| |_|_| |_|\\___/|_| |_|";

	private String				cannonHome;

	private String				propsFileName;

	@Override
	public void start(Stage primaryStage)
	{
		Thread.currentThread().setName("CannonUI");
		try
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/acemain.fxml"));
			VBox root = (VBox) loader.load();
			MainController mainController = loader.getController();
			Scene scene = new Scene(root, 250, 220);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("ACE Cannon");
			primaryStage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
			Position position = ConfigManager.INSTANCE.getConfig().getUI().getPosition("main");
			if (position != null)
			{
				Double x = position.getX();
				Double y = position.getY();
				if (x != null)
				{
					primaryStage.setX(x);
				}
				if (y != null)
				{
					primaryStage.setY(y);
				}
				Double width = position.getWidth();
				Double height = position.getHeight();
				if (position.getIsMaximised())
				{
					primaryStage.setMaximized(true);
				}
				else
				{
					if (width != null)
					{
						primaryStage.setWidth(width);
					}
					if (height != null)
					{
						primaryStage.setHeight(height);
					}
				}

			}
			primaryStage.setOnCloseRequest(ev -> {
				ConfigManager.INSTANCE.getConfig().getUI().setPosition("main", primaryStage.getX(), primaryStage.getY(),
						primaryStage.getWidth(), primaryStage.getHeight(), primaryStage.isMaximized());
				mainController.saveChildStageInfo();
				try
				{
					File f = null;
					if (cannonHome != null)
					{
						log("Resolving properties path using home \"" + cannonHome + "\" and file path \""
								+ propsFileName + "\"");
						f = new File(cannonHome, propsFileName);
					}
					else
					{
						log("Resolving properties path using file path \"" + propsFileName + "\"");
						f = new File(propsFileName);
					}
					f = f.getCanonicalFile();
					int pCount = ConfigManager.INSTANCE.getConfig().getProfileNames().size();
					log("Saving " + pCount + " profile" + (pCount > 1 ? "s" : "") + " to " + f);
					ConfigManager.INSTANCE.getConfig()
							.setActiveProfile(mainController.cbProfile.getSelectionModel().getSelectedItem());
					ConfigManager.INSTANCE.writeConfigToFile(f);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log("JavaFX platform exiting");
				Platform.exit();
			});

			primaryStage.show();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void log(String msg)
	{
		System.out.println(Thread.currentThread().getName() + "\t" + msg);
	}

	@Override
	public void init() throws FileNotFoundException, IOException, URISyntaxException, SQLException
	{
		log("Welcome to ACE Cannon - The ad hoc testing smörgåsbord for ACE CDM");
		
		// Set DM to ACE mode
		System.setProperty("com.tibco.bpm.da.dm.environment", "ACE");
		Parameters p = getParameters();
		List<String> args = p.getRaw();
		int i = 0;
		while (i < args.size())
		{
			if (args.get(i).equals("--home"))
			{
				cannonHome = (i + 1 < args.size() ? args.get(++i) : null);
				log("cannonHome=" + cannonHome);
			}
			else if (args.get(i).equals("--p"))
			{
				propsFileName = (i + 1 < args.size() ? args.get(++i) : null);
				log("propsFileName=" + propsFileName);
			}
			i++;
		}

		if (propsFileName != null)
		{
			File file = new File(cannonHome, propsFileName);
			ConfigManager.INSTANCE.readConfigFromFile(file);
			ConfigManager.INSTANCE.getConfig().getUI().fixOutOfBoundsPositions();
			int pCount = ConfigManager.INSTANCE.getConfig().getProfileNames().size();
			log("Loaded " + pCount + " profile" + (pCount > 1 ? "s" : "") + " from " + file);
			ConfigManager.INSTANCE.getConfig()
					.setActiveProfile(ConfigManager.INSTANCE.getConfig().getActiveProfileName());
			log("Activated profile '" + ConfigManager.INSTANCE.getConfig().getActiveProfileName() + "'");
		}
	}

	public static void main(String[] args)
	{
		Arrays.asList(LOGO.split("[\\r\\n]+")).stream().forEach(l -> log(l));
		log("");
		launch(args);
	}
}
