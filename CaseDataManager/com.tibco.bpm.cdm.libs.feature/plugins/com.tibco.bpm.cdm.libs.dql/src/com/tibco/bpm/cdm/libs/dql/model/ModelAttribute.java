package com.tibco.bpm.cdm.libs.dql.model;

/**
 * An attribute of a ModelStructuredType. A class that implements this
 * interface is expected to wrap a representation of am attribute and provide
 * methods to determine its type (base type or another ModelStructuredType)
 * and whether it is an array and/or searchable.
 * @author smorgan
 * @since 2019
 */
public interface ModelAttribute
{
	/**
	 * Determines if the attribute is searchable
	 * @return true if searchable
	 */
	boolean isSearchable();

	/**
	 * Determines if the attribute is an array
	 * @return true if an array
	 */
	boolean isArray();
	
	/**
	 * If the attribute is of a base type, this should return one of the
	 * predefined static instances of ModelBaseType. Otherwise, it should
	 * wrap the target type in a ModelStructuredType and return that.
	 * @return
	 */
	ModelAbstractType getType();

	/**
	 * Determines the name of the attribute (internal name, by which it
	 * would be referred to in a DQL query, not a cosmetic label).
	 * @return
	 */
	String getName();

    /**
     * Determines qualified name for the attribute in case this is a child
     * attribute (internal name, by which it would be referred to in a DQL
     * query, not a cosmetic label).
     * 
     * @return
     */
    String getQualifiedName();
    
    /**
     * Returns the constraint value
     * @param constraintName
     * @return
     */
    String getConstraint(String constraintName);

    /**
     * Sets the reference name  which contains complete path with indices if any.
     * This is primarily applicable to the leaf node of the attribute path.
     */
    void setReferenceName(String ref);
    
    /**
     * Returns the reference name which contains complete path with indices if any.
     * This is primarily applicable to the leaf node of the attribute path.
     * @return reference name
     */
    String getReferenceName();
    
}
