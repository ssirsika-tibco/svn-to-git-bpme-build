package com.tibco.bpm.cdm.api.dto;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;

/**
 * Object representation of a case reference.
 * Serialized in the format: {id}-{namespace}.{case type name}-{application major version}-{version} 
 * e.g. 101-com.example.ordermodel.Order-1-0
 * @author smorgan
 * @since 2019
 */
public class CaseReference
{
	private static final String		FORMAT	= "%s-%s.%s-%s-%d";

	private static final Pattern	PATTERN	= Pattern.compile("(\\d+)-(.*)-(\\d+)-(\\d+)");

	private QualifiedTypeName		type;

	private int						applicationMajorVersion;

	private BigInteger				id;

	private int						version;

	public CaseReference(String string) throws ArgumentException
	{
		if (string == null)
		{
			throw ReferenceException.newInvalidFormat(null);
		}
		Matcher matcher = PATTERN.matcher(string);
		if (matcher.matches())
		{
			String idString = matcher.group(1);
			try
			{
				id = new BigInteger(idString);
			}
			catch (NumberFormatException e)
			{
				throw ReferenceException.newInvalidId(string, idString, e);
			}
			String typeString = matcher.group(2);
			try
			{
				type = new QualifiedTypeName(typeString);
			}
			catch (IllegalArgumentException e)
			{
				throw ReferenceException.newInvalidType(typeString, e);
			}
			try
			{
				applicationMajorVersion = Integer.parseInt(matcher.group(3));
			}
			catch (NumberFormatException e)
			{
				throw ReferenceException.newInvalidMajorVersion(string, matcher.group(3), e);
			}
			try
			{
				version = Integer.parseInt(matcher.group(4));
			}
			catch (NumberFormatException e)
			{
				throw ReferenceException.newInvalidVersion(string, matcher.group(4), e);
			}
		}
		else
		{
			throw ReferenceException.newInvalidFormat(string);
		}
	}

	public CaseReference(QualifiedTypeName qName, int applicationMajorVersion, BigInteger id, int version)
			throws ReferenceException
	{
		this.type = qName;
		this.applicationMajorVersion = applicationMajorVersion;
		this.id = id;
		this.version = version;
	}

	@Override
	// Auto-generated
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + applicationMajorVersion;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + version;
		return result;
	}

	@Override
	// Auto-generated
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CaseReference other = (CaseReference) obj;
		if (applicationMajorVersion != other.applicationMajorVersion) return false;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (type == null)
		{
			if (other.type != null) return false;
		}
		else if (!type.equals(other.type)) return false;
		if (version != other.version) return false;
		return true;
	}

	public QualifiedTypeName getQualifiedTypeName()
	{
		return type;
	}

	public int getApplicationMajorVersion()
	{
		return applicationMajorVersion;
	}

	public void setApplicationMajorVersion(int applicationMajorVersion)
	{
		this.applicationMajorVersion = applicationMajorVersion;
	}

	public BigInteger getId()
	{
		return id;
	}

	public int getVersion()
	{
		return version;
	}

	public void setVersion(int version)
	{
		this.version = version;
	}

	public String toString()
	{
		String string = String.format(FORMAT, id, type.getNamespace(), type.getName(), applicationMajorVersion,
				version);
		return string;
	}

	public CaseReference duplicate()
	{
		try
		{
			return new CaseReference(type, applicationMajorVersion, id, version);
		}
		catch (ReferenceException e)
		{
			// Theoretically impossible, as reference is validated on creation.
			throw new IllegalArgumentException("Existing case reference invalid", e);
		}
	}
}
