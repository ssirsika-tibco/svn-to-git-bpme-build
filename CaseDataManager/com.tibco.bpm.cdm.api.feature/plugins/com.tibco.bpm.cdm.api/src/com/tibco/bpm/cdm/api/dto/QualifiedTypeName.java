package com.tibco.bpm.cdm.api.dto;

import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.api.exception.ReferenceException;

/**
 * Represents a 'qualified' type name; i.e. a namespace and name combined in the form "{namespace}.{name}"
 * @author smorgan
 * @since 2019
 */
public class QualifiedTypeName
{
	private String	original;

	private String	namespace;

	private String	name;

	/**
	 * Constructs an instance from a String, such as 'com.example.mymodel.Class1'
	 * @param qtn
	 * @throws ArgumentException 
	 */
	public QualifiedTypeName(String qtn) throws ArgumentException
	{
		if (qtn == null)
		{
			throw ReferenceException.newTypeInvalid(null);
		}
		this.original = qtn;
		int pos = qtn.lastIndexOf('.');
		name = pos == -1 || pos == qtn.length() - 1 ? qtn : qtn.substring(pos + 1);
		namespace = pos == -1 ? null : qtn.substring(0, pos);
	}

	/**
	 * Constructs an instance from a namespace (e.g. 'com.example.mymodel') and name (e.g. 'Class1')
	 * 
	 * @param namespace
	 * @param name
	 */
	public QualifiedTypeName(String namespace, String name)
	{
		this.namespace = namespace;
		this.name = name;
	}

	@Override
	// Auto-generated
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	@Override
	// Auto-generated
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QualifiedTypeName other = (QualifiedTypeName) obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (namespace == null)
		{
			if (other.namespace != null) return false;
		}
		else if (!namespace.equals(other.namespace)) return false;
		return true;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public String getName()
	{
		return name;
	}

	public String toString()
	{
		return original != null ? original : namespace + "." + name;
	}
}
