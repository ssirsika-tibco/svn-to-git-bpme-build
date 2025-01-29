package com.tibco.bpm.cdm.core.deployment;

import java.io.UnsupportedEncodingException;

import com.tibco.bpm.cdm.api.exception.InternalException;
import com.tibco.bpm.dt.rasc.RuntimeContent;

/**
 * Common functionality for artifact handlers.
 * @author smorgan
 */
public abstract class AbstractArtifactHandler implements ArtifactHandler
{
	private static final byte[] UTF8_BYTE_ORDER_MARK = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private static boolean startsWithUTF8BOM(byte[] bytes)
	{
		boolean result = true;
		for (int i = 0; i < UTF8_BYTE_ORDER_MARK.length && result; i++)
		{
			result = bytes[i] == UTF8_BYTE_ORDER_MARK[i];
		}
		return result;
	}

	protected String getContentsAsUTF8String(RuntimeContent content) throws InternalException
	{
		byte[] contentBytes = content.getContent();
		try
		{
			String contentString = null;
			if (startsWithUTF8BOM(contentBytes))
			{
				// Ignore BOM when converting to a String
				contentString = new String(contentBytes, UTF8_BYTE_ORDER_MARK.length,
						contentBytes.length - UTF8_BYTE_ORDER_MARK.length, "UTF-8");
			}
			else
			{
				contentString = new String(contentBytes, "UTF-8");
			}
			return contentString;
		}
		catch (UnsupportedEncodingException e)
		{
			throw InternalException.newInternalException(e);
		}
	}
}
