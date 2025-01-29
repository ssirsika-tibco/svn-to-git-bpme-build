/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.tibco.bpm.acecannon.ErrorObserver;
import com.tibco.bpm.acecannon.Logger;
import com.tibco.bpm.acecannon.SimpleConsoleLogger;

public class BaseHTTPCaller
{
	public static class StringWithByteCount
	{
		private String	string;

		private int		byteCount;

		public StringWithByteCount(String string, int byteCount)
		{
			this.string = string;
			this.byteCount = byteCount;
		}

		public String getString()
		{
			return string;
		}

		public int getByteCount()
		{
			return byteCount;
		}
	}

	private static final SimpleDateFormat	dateFormat	= new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");

	private String							method;

	private String							url;

	private String							cookie;

	private String							body;

	private String							responseBody;

	private int								responseCode;

	protected Logger						logger;

	private CookieHolder					cookieHolder;

	private ErrorObserver					errorObserver;

	protected BaseHTTPCaller()
	{
		throw new UnsupportedOperationException("not supported");
	}

	protected void preCallHook(HttpURLConnection con)
	{
		// Nothing to do in this base implementation
	}

	protected BaseHTTPCaller(String method, String url, String cookie, String body)
	{
		this.method = method;
		this.url = url;
		this.cookie = cookie;
		this.body = body;
		this.logger = new SimpleConsoleLogger();
	}

	public static BaseHTTPCaller newGet(String url, String cookie)
	{
		return new BaseHTTPCaller("GET", url, cookie, null);
	}

	public static BaseHTTPCaller newPost(String url, String cookie, String body)
	{
		return new BaseHTTPCaller("POST", url, cookie, body);
	}

	public static BaseHTTPCaller newPut(String url, String cookie, String body)
	{
		return new BaseHTTPCaller("PUT", url, cookie, body);
	}

	public static BaseHTTPCaller newDelete(String url, String cookie)
	{
		return new BaseHTTPCaller("DELETE", url, cookie, null);
	}

	private static void setCorrelationId(HttpURLConnection con, String msg)
	{
		con.setRequestProperty("X-Context-Id", "CaseCannon:" + Thread.currentThread().getName() + ":" + msg);
	}

	public void setCookieHolder(CookieHolder cookieHolder)
	{
		this.cookieHolder = cookieHolder;
	}

	public void setErrorObserver(ErrorObserver errorObserver)
	{
		this.errorObserver = errorObserver;
	}

	//	public void setHeader(String name, String value)
	//	{
	//		requestProperties.put(name, value);
	//	}
	//
	private static void setAtmosphereToken(HttpURLConnection con, String cookie)
	{
		// TODO This isn't going to do anything sensible with the real cookies, but that 
		// doesn't matter as that will all be via nginx anyway.
		int pos = cookie.indexOf('=');
		if (pos >= 0)
		{
			String token = cookie.substring(pos + 1);
			con.setRequestProperty("X-Atmosphere-Token", token);
		}
	}

	private static String commaize(long num)
	{
		String string = Long.toString(num);
		StringBuilder buf = new StringBuilder();
		int length = string.length();
		for (int i = 0; i < length; i++)
		{
			buf.append(string.charAt(i));
			if (((length - 1 - i) % 3) == 0 && i < (length - 1))
			{
				buf.append(",");
			}
		}
		return buf.toString();
	}

	public static StringWithByteCount readInputStreamToString(InputStream inputStream) throws IOException
	{
		char[] buffer = new char[16384];
		StringBuilder buf = new StringBuilder();
		int totalCount = 0;
		if (inputStream != null)
		{
			Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			int count = 0;
			while (count >= 0)
			{
				count = in.read(buffer, 0, buffer.length);
				if (count > 0)
				{
					totalCount += count;
					buf.append(buffer, 0, count);
				}
			}
		}
		return new StringWithByteCount(buf.toString(), totalCount);
	}

	private static String createCorrelationId()
	{
		String cid = String.format("CC-%s", dateFormat.format(new Date()));
		return cid;
	}

	public void call() throws IOException
	{
		call(false);
	}

	public boolean call(boolean displayErrorDialog) throws IOException
	{
		boolean displayedError = false;
		URL urlObject = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
		con.setRequestMethod(method);
		if (cookie != null)
		{
			con.setRequestProperty("Cookie", cookie);
			setAtmosphereToken(con, cookie);
		}
		setCorrelationId(con, createCorrelationId());
		con.setRequestProperty("Accept", "application/json");

		String logMsg = "-> " + method + " " + url;
		byte[] bodyBytes = null;
		if (method.equals("POST") || method.equals("PUT"))
		{
			bodyBytes = body.getBytes(StandardCharsets.UTF_8);
			logMsg += " (body: " + commaize(bodyBytes.length) + " bytes)";
		}
		logger.log(logMsg);
		if (url.contains("/idm/v1/reauthorize"))
		{
			con.setRequestProperty("Host", "eu.account.cloud.tibco.com");
			con.setRequestProperty("Referer", "https://eu.account.cloud.tibco.com/tsc/choose");
		}
		preCallHook(con);
		if (method.equals("POST") || method.equals("PUT"))
		{
			if (url.contains("oauth") || url.contains("/idm/v1/reauthorize"))
			{
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			}
			else
			{
				con.setRequestProperty("Content-Type", "application/json");
			}
			con.setDoOutput(true);
			con.getOutputStream().write(bodyBytes);
		}

		long t1 = System.currentTimeMillis();
		responseCode = con.getResponseCode();
		long t2 = System.currentTimeMillis();
		long timeTaken = t2 - t1;
		if (responseCode != 200 && responseCode != 300)
		{
			StringWithByteCount error = readInputStreamToString(con.getErrorStream());
			String errorString = error.getString();
			logger.log("<- [" + responseCode + "] " + commaize(timeTaken) + "ms " + commaize(error.getByteCount())
					+ " bytes");
			logger.log("Error: " + error.toString());
			if (displayErrorDialog && errorObserver != null)
			{
				displayedError = true;
				errorObserver.notifyError(errorString, responseCode);
			}
			throw new IOException(errorString);
		}
		else
		{
			if (cookieHolder != null)
			{
				int headerIdx = 0;
				// Note that first header is typically HTTP status message, in which case the key is null,
				// but the field has a value.
				while (con.getHeaderFieldKey(headerIdx) != null || con.getHeaderField(headerIdx) != null)
				{
					// (Case-insensitive key match - Jon P says this is necessary - and the RFC says keys are case-insensitive anyway)
					String originalKey = con.getHeaderFieldKey(headerIdx);
					String key = originalKey == null ? null : originalKey.toLowerCase();
					if ("set-cookie".equals(key))
					{
						String setCookie = con.getHeaderField(headerIdx);
						logger.log("Received " + originalKey + " (header #" + headerIdx + "): " + setCookie);
						synchronized (cookieHolder)
						{
							Map<String, String> cookieMap = parseStoredCookieString(cookieHolder.getCookie());
							Map<String, String> newCookies = parseSetCookie(setCookie);
							applySetCookies(cookieMap, newCookies);
							String newCookieString = renderCookieString(cookieMap);
							cookieHolder.setCookie(newCookieString);
						}
					}
					headerIdx++;
				}
			}
			StringWithByteCount response = readInputStreamToString(con.getInputStream());
			responseBody = response.getString();
			logger.log("<- [" + responseCode + "] " + commaize(timeTaken) + "ms " + commaize(response.getByteCount())
					+ " bytes");
		}
		return displayedError;
	}

	private static String renderCookieString(Map<String, String> map)
	{
		StringBuilder buf = new StringBuilder();
		for (Entry<String, String> entry : map.entrySet())
		{
			String key = entry.getKey();
			if (!(buf.length() == 0))
			{
				buf.append("; ");
			}
			buf.append(key + "=" + entry.getValue());
		}
		return buf.toString();
	}

	private void applySetCookies(Map<String, String> cookies, Map<String, String> setCookies)
	{
		boolean doneWork = false;
		for (String key : setCookies.keySet())
		{
			String value = setCookies.get(key);
			if (cookies.containsKey(key))
			{
				if (!value.equals(cookies.get(key)))
				{
					logger.log("Replacing cookie '" + key + "' with: " + value);
					cookies.put(key, value);
					doneWork = true;
				}
			}
			else
			{
				logger.log("Adding new cookie '" + key + "': " + value);
				cookies.put(key, value);
				doneWork = true;
			}
		}
		if (doneWork)
		{
			logger.log("I now have " + cookies.size() + " cookie" + (cookies.size() != 1 ? "s" : "") + ": "
					+ cookies.keySet());
		}
	}

	private static Map<String, String> parseStoredCookieString(String cookieString)
	{
		Map<String, String> map = new HashMap<>();
		String[] assignments = cookieString.split("\\s*;\\s*");
		for (String assignment : assignments)
		{
			String[] sides = assignment.split("\\s*=\\s*");
			if (sides.length == 2)
			{
				map.put(sides[0].trim(), sides[1].trim());
			}
		}
		return map;
	}

	private static Map<String, String> parseSetCookie(String setCookie)
	{
		String[] fragments = setCookie.split("\\s*;\\s*");
		// Only interested in the first fragments
		String[] sides = fragments[0].split("\\s*=\\s*");
		Map<String, String> map = new HashMap<>();
		map.put(sides[0].trim(), sides[1].trim());
		return map;
	}

	public String getResponseBody()
	{
		return responseBody;
	}

	public int getResponseCode()
	{
		return responseCode;
	}
}
