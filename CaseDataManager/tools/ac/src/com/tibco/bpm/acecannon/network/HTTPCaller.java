package com.tibco.bpm.acecannon.network;

import java.net.HttpURLConnection;
import java.util.Base64;

import com.tibco.bpm.acecannon.DialogDisplayingErrorObserver;
import com.tibco.bpm.acecannon.SimpleConsoleLogger;
import com.tibco.bpm.acecannon.config.ConfigManager;

/**
 * Cannon UI-specific wrapper for the non-UI HTTPCaller that ensures that the ConfigManager is set to observe cookie changes
 * and errors will be directed to UI dialogs where requested.
 */
public class HTTPCaller extends BaseHTTPCaller
{
	private HTTPCaller(String method, String url, String cookie, String body)
	{
		super(method, url, cookie, body);
		this.logger = new SimpleConsoleLogger();
		setErrorObserver(new DialogDisplayingErrorObserver());
	}

	@Override
	protected void preCallHook(HttpURLConnection con)
	{
		String unencoded = String.format("%s:%s",
				ConfigManager.INSTANCE.getActiveProfile().getIdentification().getUserName(),
				ConfigManager.INSTANCE.getActiveProfile().getIdentification().getPassword());
		byte[] encodedBytes = Base64.getEncoder().encode(unencoded.getBytes());
		String encoded = new String(encodedBytes);
		con.setRequestProperty("Authorization", "Basic " + encoded);
	}

	public static HTTPCaller newGet(String url, String cookie)
	{
		return new HTTPCaller("GET", url, cookie, null);
	}

	public static HTTPCaller newPost(String url, String cookie, String body)
	{
		return new HTTPCaller("POST", url, cookie, body);
	}

	public static HTTPCaller newPut(String url, String cookie, String body)
	{
		return new HTTPCaller("PUT", url, cookie, body);
	}

	public static HTTPCaller newDelete(String url, String cookie)
	{
		return new HTTPCaller("DELETE", url, cookie, null);
	}
}
