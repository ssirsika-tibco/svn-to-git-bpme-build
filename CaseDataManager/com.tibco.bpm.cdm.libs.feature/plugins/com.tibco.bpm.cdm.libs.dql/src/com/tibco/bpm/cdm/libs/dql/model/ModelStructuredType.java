package com.tibco.bpm.cdm.libs.dql.model;

import java.util.List;

import com.tibco.bpm.cdm.libs.dql.exception.DQLException;

/**
 * A structured type (class) with attributes. A class that implements this
 * interface is expected to wrap a representation of a structured type and
 * provide methods that expose its name and attributes.
 * @author smorgan
 * @since 2019
 */
public interface ModelStructuredType extends ModelAbstractType
{
	/**
	 * Returns the name of the type
	 * @return
	 */
	@Override
    String getName();

	/**
	 * Get a list of all attributes of the type
	 * @return
	 */
	List<ModelAttribute> getAttributes();

	/**
	 * Get attribute by name (case-sensitive)
	 * @param name
	 * @return attribute if it exists. 
	 * Otherwise throws UnknownDataTypeException or UnknownAttributeException 
	 * @throws DQLException 
	 */
    ModelAttribute getAttribute(String name) throws DQLException;

}