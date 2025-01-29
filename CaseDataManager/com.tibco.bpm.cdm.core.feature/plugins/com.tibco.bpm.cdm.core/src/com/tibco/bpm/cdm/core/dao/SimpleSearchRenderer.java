package com.tibco.bpm.cdm.core.dao;

import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * Renders an SQL where clause fragment that performs a 'simple search' of all searchable
 * attributes of the given type.
 * @author smorgan
 * @since 2019
 */
public interface SimpleSearchRenderer
{
	public String render(StructuredType st);
	
    public int getNoOfSubstitutions(String render);
}
