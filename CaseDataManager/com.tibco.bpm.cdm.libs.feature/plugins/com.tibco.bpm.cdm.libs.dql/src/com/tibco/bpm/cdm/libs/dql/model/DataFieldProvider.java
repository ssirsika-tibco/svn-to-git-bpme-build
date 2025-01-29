package com.tibco.bpm.cdm.libs.dql.model;

public interface DataFieldProvider {

    /**
     * Return a ModelAttribute that is equivalent to the parameter specification used in a DQL query string.
     * 
     * @ parameterPath Path to the data parameter e.g. data.customerInfo.name
     * @return ModelAttribute or null if invalid data-field-path.
     */
    com.tibco.bpm.cdm.libs.dql.model.ModelAttribute getDataField(String parameterPath);	
    
}
