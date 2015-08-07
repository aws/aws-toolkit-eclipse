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

/**
 * Abstract class describing the credentials information of a configured AWS
 * account. The information includes the account name (or the profile name in
 * case of profile-based accounts), the AWS access and secret keys and an
 * optional session token.
 */
public abstract class AccountCredentialsConfiguration {

    /**
     * @return The UI-friendly name for this account.
     */
    public abstract String getAccountName();

    /**
     * Sets the UI-friendly name for this account.
     *
     * @param accountName
     *            The UI-friendly name for this account.
     */
    public abstract void setAccountName(String accountName);

    /**
     * @return The currently configured AWS user access key.
     */
    public abstract String getAccessKey();

    /**
     * Sets the AWS Access Key ID for this account info object.
     *
     * @param accessKey The AWS Access Key ID.
     */
    public abstract void setAccessKey(String accessKey);

    /**
     * @return The currently configured AWS secret key.
     */
    public abstract String getSecretKey();

    /**
     * Sets the AWS Secret Access Key for this account info object.
     *
     * @param secretKey The AWS Secret Access Key.
     */
    public abstract void setSecretKey(String secretKey);

    /**
     * @return true if the current account includes a session token
     */
    public abstract boolean isUseSessionToken();

    /**
     * Sets whether the current account includes a session token
     *
     * @param useSessionToken
     *            true if the current account includes a session token
     */
    public abstract void setUseSessionToken(boolean useSessionToken);

    /**
     * @return The currently configured AWS session token.
     */
    public abstract String getSessionToken();

    /**
     * Sets the AWS session token for this account info object.
     *
     * @param sessionToken The AWS session token.
     */
    public abstract void setSessionToken(String sessionToken);

    /**
     * Persist this account credentials information in the source where it was
     * loaded, e.g. save the related preference store properties, or save output
     * the information to an external file.
     */
    public abstract void save();

    /**
     * Delete this account credentials information in the source where it was
     * loaded.
     */
    public abstract void delete();

    /**
     * Returns true if this class contains in-memory changes that are not saved
     * yet.
     */
    public abstract boolean isDirty();

    /**
     * Returns true if the configured account credentials appears to be valid,
     * i.e. both the access key and the secret key are non-empty.
     */
    public boolean isCredentialsValid() {
        if (isEmpty(getAccessKey())) return false;
        if (isEmpty(getSecretKey())) return false;
        if (isUseSessionToken() && isEmpty(getSessionToken())) return false;
        return true;
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

}
