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
package com.amazonaws.eclipse.core;

/**
 * Interface for accessing and saving configured AWS account information.
 */
public interface AccountInfo {

    /**
     * @return The account identifier used internally by the plugin.
     */
    public String getInternalAccountId();

    /**
     * @return The UI-friendly name for this account.
     */
    public String getAccountName();

    /**
     * Sets the UI-friendly name for this account.
     *
     * @param accountName
     *            The UI-friendly name for this account.
     */
    public void setAccountName(String accountName);

    /**
     * @return The currently configured AWS user access key.
     */
    public String getAccessKey();

    /**
     * Sets the AWS Access Key ID for this account info object.
     *
     * @param accessKey The AWS Access Key ID.
     */
    public void setAccessKey(String accessKey);

    /**
     * @return The currently configured AWS secret key.
     */
    public String getSecretKey();

    /**
     * Sets the AWS Secret Access Key for this account info object.
     *
     * @param secretKey The AWS Secret Access Key.
     */
    public void setSecretKey(String secretKey);

    /**
     * @return true if the current account includes a session token
     */
    public boolean isUseSessionToken();

    /**
     * Sets whether the current account includes a session token
     *
     * @param useSessionToken
     *            true if the current account includes a session token
     */
    public void setUseSessionToken(boolean useSessionToken);

    /**
     * @return The currently configured AWS session token.
     */
    public String getSessionToken();

    /**
     * Sets the AWS session token for this account info object.
     *
     * @param sessionToken The AWS session token.
     */
    public void setSessionToken(String sessionToken);

    /**
     * Returns the currently configured AWS user account ID.
     *
     * @return The currently configured AWS user account ID.
     */
    public String getUserId();

    /**
     * Sets the currently configured AWS user account ID.
     */
    public void setUserId(String userId);

    /**
     * Returns the currently configured EC2 private key file.
     *
     * @return The currently configured EC2 private key file.
     */
    public String getEc2PrivateKeyFile();

    /**
     * Sets the currently configured EC2 private key file.
     */
    public void setEc2PrivateKeyFile(String ec2PrivateKeyFile);

    /**
     * Returns the currently configured EC2 certificate file.
     *
     * @return The currently configured EC2 certificate file.
     */
    public String getEc2CertificateFile();

    /**
     * Sets the currently configured EC2 certificate file.
     */
    public void setEc2CertificateFile(String ec2CertificateFile);

    /**
     * Persist this account information in the source where it was loaded, e.g.
     * save the related preference store properties, or save output the
     * information to an external file.
     */
    public void save();

    /**
     * Delete this account information in the source where it was loaded.
     */
    public void delete();

    /**
     * Returns true if this account information contains in-memory changes that
     * are not saved in the source yet.
     */
    public boolean isDirty();

    /**
     * Returns true if the configured account information appears to be valid by
     * verifying that none of the values are missing.
     *
     * @return True if the configured account information appears to be valid,
     *         otherwise false.
     */
    public boolean isValid();

    /**
     * Returns true if and only if the configured certificate and corresponding
     * private key are valid. Currently that distinction is made by verifying
     * that the referenced files exist.
     *
     * @return True if and only if the configured certificate and corresponding
     *         private key are valid.
     */
    public boolean isCertificateValid();

}
