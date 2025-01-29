package com.tibco.bpm.auth.saml.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.joda.time.DateTime;
import org.opensaml.util.resource.AbstractFilteredResource;
import org.opensaml.util.resource.ResourceException;

/**
 * Resource Wrapper for xml metadata provided in String form. 
 * @author ssirsika
 */
public class StringMetadataResourceWrapper extends AbstractFilteredResource {

	private final String stringMetadata;
	private final DateTime lastModifiedTime;
	
	/**
	 * 
	 * @param stringMetadata metadata in string form
	 */
	public StringMetadataResourceWrapper(String stringMetadata) {
		super();
		this.stringMetadata = stringMetadata;
		this.lastModifiedTime = new DateTime();
	}
	
	/**
	 * Return Empty location
	 */
	@Override
	public String getLocation() {
		return "";
	}

	/**
	 * 
	 */
	@Override
	public boolean exists() throws ResourceException {
		return this.stringMetadata!=null;
	}

	/**
	 *  Return {@link InputStream} for provided string metadata
	 */
	@Override
	public InputStream getInputStream() throws ResourceException {
		if(this.stringMetadata!=null){
			return new ByteArrayInputStream(this.stringMetadata.getBytes());
		}
		return null;
	}

	/**
	 * Returns last modified time of the resource
	 */
	@Override
	public DateTime getLastModifiedTime() throws ResourceException {
		return this.lastModifiedTime;
	}
	
    /** {@inheritDoc} */
    public int hashCode() {
        return this.stringMetadata.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof StringMetadataResourceWrapper) {
        	if(this.stringMetadata==null && ((StringMetadataResourceWrapper) obj).getResource()==null)
        		return true;
        	
        	if(this.stringMetadata!=null && ((StringMetadataResourceWrapper) obj).getResource()!=null)
        		return this.stringMetadata.equals(((StringMetadataResourceWrapper) obj).getResource());
        }

        return false;
    }

	/**
	 *  Return the string metadata
	 */
	private Object getResource() {
		return this.stringMetadata;
	}
}
