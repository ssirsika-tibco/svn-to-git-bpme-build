/**
 * Copyright (c) TIBCO Software Inc 2004 - 2020. All rights reserved.
 */

package com.tibco.bpm.auth.saml.config;

import org.joda.time.DateTime;
import org.opensaml.util.resource.ResourceException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * org.opensaml.util.resource.Resource wrapper over the org.springframework.core.io.Resource implementation
 * @author ssirsika
 * 
 */
@Deprecated
public class SpringResourceWrapperOpenSAMLResource implements org.opensaml.util.resource.Resource{


    private Resource springDelegate;

    public SpringResourceWrapperOpenSAMLResource(Resource springDelegate) throws ResourceException {
        this.springDelegate = springDelegate;
        if (!exists()) {
            throw new ResourceException("Wrapper resource does not exist: " + springDelegate);
        }

    }

    public String getLocation() {
        try {
			return springDelegate.getURL().toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

    public boolean exists() throws ResourceException {
        return springDelegate.exists();
    }

    public InputStream getInputStream() throws ResourceException {
        try {
            return springDelegate.getInputStream();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    public DateTime getLastModifiedTime() throws ResourceException {
        try {
            return new DateTime(springDelegate.lastModified());
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public int hashCode() {
        return getLocation().hashCode();
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof SpringResourceWrapperOpenSAMLResource) {
            return getLocation().equals(((SpringResourceWrapperOpenSAMLResource) o).getLocation());
        }

        return false;
    }
}
