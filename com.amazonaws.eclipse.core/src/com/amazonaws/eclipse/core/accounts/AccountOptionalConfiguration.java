/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core.accounts;

import java.io.File;

/**
 * Abstract class describing the interface for accessing configured AWS account
 * preferences and provides hooks for notification of change events.
 */
public abstract class AccountOptionalConfiguration {

    /**
     * Returns the currently configured AWS user account ID.
     *
     * @return The currently configured AWS user account ID.
     */
    public abstract String getUserId();

    /**
     * Sets the currently configured AWS user account ID.
     */
    public abstract void setUserId(String userId);

    /**
     * Returns the currently configured EC2 private key file.
     *
     * @return The currently configured EC2 private key file.
     */
    public abstract String getEc2PrivateKeyFile();

    /**
     * Sets the currently configured EC2 private key file.
     */
    public abstract void setEc2PrivateKeyFile(String ec2PrivateKeyFile);

    /**
     * Returns the currently configured EC2 certificate file.
     *
     * @return The currently configured EC2 certificate file.
     */
    public abstract String getEc2CertificateFile();

    /**
     * Sets the currently configured EC2 certificate file.
     */
    public abstract void setEc2CertificateFile(String ec2CertificateFile);

    /**
     * Persist the optional configurations in the source where it was loaded,
     * e.g. save the related preference store properties, or save output the
     * information to an external file.
     */
    public abstract void save();

    /**
     * Delete the optional configurations in the source where it was loaded.
     */
    public abstract void delete();

    /**
     * Returns true if this class contains in-memory changes that are not saved
     * yet.
     */
    public abstract boolean isDirty();

    /**
     * Returns true if and only if the configured certificate and corresponding
     * private key are valid. Currently that distinction is made by verifying
     * that the referenced files exist.
     *
     * @return True if and only if the configured certificate and corresponding
     *         private key are valid.
     */
    public boolean isCertificateValid() {
        String certificateFile = getEc2CertificateFile();
        String privateKeyFile = getEc2PrivateKeyFile();

        if (certificateFile == null || certificateFile.length() == 0) return false;
        if (privateKeyFile == null || privateKeyFile.length() == 0) return false;

        return (new File(certificateFile).isFile() &&
                new File(privateKeyFile).isFile());
    }

}
