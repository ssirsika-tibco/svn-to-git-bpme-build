package com.tibco.bpm.acecannon;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tibco.bpm.acecannon.controllers.ErrorDialogController;

import javafx.application.Platform;

public class DialogDisplayingErrorObserver implements ErrorObserver
{

	@Override
	public void notifyError(String message, int statusCode)
	{

		Platform.runLater(() -> {
			try
			{
//				Alert alert = RESTErrorDialog.buildAlert(message, statusCode);
//				Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
//				alertStage.getIcons().add(new Image(AceMain.class.getResourceAsStream("images/ace.png")));
//				alert.showAndWait();
				ErrorDialogController.make(message, statusCode, null).getStage().showAndWait();
			}
			catch (JsonProcessingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
}
