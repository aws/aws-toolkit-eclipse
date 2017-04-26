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
package com.amazonaws.eclipse.core.accounts.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.accounts.AccountOptionalConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Concrete implementation of AccountOptionalConfiguration, which uses the
 * preference store instance to persist the account optional configurations.
 */
public class PluginPreferenceStoreAccountOptionalConfiguration extends
        AccountOptionalConfiguration {

    private final IPreferenceStore prefStore;

    /** The names of the preference properties relating to this account */
    private final String userIdPreferenceName;
    private final String ec2PrivateKeyFilePreferenceName;
    private final String ec2CertificateFilePreferenceName;

    /** The property values set in memory */
    private String userIdInMemory;
    private String ec2PrivateKeyFileInMemory;
    private String ec2CertificateFileInMemory;

    /**
     * @param preferenceNamePrefix
     *            The prefix of the preference names for the optional
     *            configurations. Legacy accounts use the internal accountId as
     *            the prefix, while the profile-based accounts use the profile
     *            name as the prefix. The reason for such difference is because
     *            the profile accounts might be assigned with a new accountId
     *            after the credentials file is reloaded for multiple times. We
     *            want to always associate the optional configuration to the
     *            same profile account no matter what the current accountId it
     *            is assigned to.
     */
    public PluginPreferenceStoreAccountOptionalConfiguration(
            IPreferenceStore prefStore, String preferenceNamePrefix) {
        if (prefStore == null)
            throw new IllegalAccessError("prefStore must not be null.");
        if (preferenceNamePrefix == null)
            throw new IllegalAccessError("preferenceNamePrefix must not be null.");

        this.prefStore = prefStore;

        this.userIdPreferenceName             = String.format("%s:%s", preferenceNamePrefix, PreferenceConstants.P_USER_ID);
        this.ec2PrivateKeyFilePreferenceName  = String.format("%s:%s", preferenceNamePrefix, PreferenceConstants.P_PRIVATE_KEY_FILE);
        this.ec2CertificateFilePreferenceName = String.format("%s:%s", preferenceNamePrefix, PreferenceConstants.P_CERTIFICATE_FILE);
    }

    @Override
    public String getUserId() {
        // User-id is stored in B64-encoded format
        return this.userIdInMemory != null ?
                this.userIdInMemory
                :
                PreferenceValueEncodingUtil.decodeString(
                    prefStore.getString(userIdPreferenceName));
    }

    @Override
    public void setUserId(String userId) {
        this.userIdInMemory = userId;
    }

    @Override
    public String getEc2PrivateKeyFile() {
        return this.ec2PrivateKeyFileInMemory != null ?
                this.ec2PrivateKeyFileInMemory
                :
                prefStore.getString(ec2PrivateKeyFilePreferenceName);
    }

    @Override
    public void setEc2PrivateKeyFile(String ec2PrivateKeyFile) {
        this.ec2PrivateKeyFileInMemory = ec2PrivateKeyFile;
    }

    @Override
    public String getEc2CertificateFile() {
        return this.ec2CertificateFileInMemory != null ?
                this.ec2CertificateFileInMemory
                :
                prefStore.getString(ec2CertificateFilePreferenceName);
    }

    @Override
    public void setEc2CertificateFile(String ec2CertificateFile) {
        this.ec2CertificateFileInMemory = ec2CertificateFile;
    }

    /**
     * Persist all the in-memory property values in the preference store.
     */
    @Override
    public void save() {
        // Clean up the AWS User-id and store it in B64-encoded format
        if (userIdInMemory != null) {
            String newUserId = userIdInMemory.replace("-", "");
            newUserId        = newUserId.replace(" ", "");
            prefStore.setValue(userIdPreferenceName,
                    PreferenceValueEncodingUtil.encodeString(newUserId));
        }


        if (ec2PrivateKeyFileInMemory != null) {
            prefStore.setValue(ec2PrivateKeyFilePreferenceName,
                    ec2PrivateKeyFileInMemory);
        }

        if (ec2CertificateFileInMemory != null) {
            prefStore.setValue(ec2CertificateFilePreferenceName,
                    ec2CertificateFileInMemory);
        }

        clearInMemoryValue();
    }

    /**
     * Remove all the preference properties relating to this account's
     * optional configurations
     */
    @Override
    public void delete() {
        prefStore.setToDefault(userIdPreferenceName);
        prefStore.setToDefault(ec2PrivateKeyFilePreferenceName);
        prefStore.setToDefault(ec2CertificateFilePreferenceName);

        clearInMemoryValue();
    }

    @Override
    public boolean isDirty() {
        // For performance reason, we only check whether there exists in-memory
        // property values (no matter it differs from the source value or not.)
        return userIdInMemory != null
                || ec2PrivateKeyFileInMemory != null
                || ec2CertificateFileInMemory != null;
    }


    private void clearInMemoryValue() {
        userIdInMemory = null;
        ec2PrivateKeyFileInMemory = null;
        ec2CertificateFileInMemory = null;
    }
}
