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

import com.amazonaws.eclipse.core.accounts.AccountCredentialsConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Concrete implementation of AccountCredentialsConfiguration, which uses the
 * preference store instance to persist the credentials configurations.
 */
public class PluginPreferenceStoreAccountCredentialsConfiguration extends
        AccountCredentialsConfiguration {

    private final IPreferenceStore prefStore;

    /** The names of the preference properties relating to this account */
    private final String accountNamePreferenceName;
    private final String accessKeyPreferenceName;
    private final String secretKeyPreferenceName;

    /** The property values set in memory */
    private String accountNameInMemory;
    private String accessKeyInMemory;
    private String secretKeyInMemory;

    @SuppressWarnings("deprecation")
    public PluginPreferenceStoreAccountCredentialsConfiguration(
            IPreferenceStore prefStore, String accountId) {
        if (prefStore == null)
            throw new IllegalAccessError("prefStore must not be null.");
        if (accountId == null)
            throw new IllegalAccessError("accountId must not be null.");

        this.prefStore = prefStore;

        this.accountNamePreferenceName = String.format("%s:%s", accountId, PreferenceConstants.P_ACCOUNT_NAME);
        this.accessKeyPreferenceName   = String.format("%s:%s", accountId, PreferenceConstants.P_ACCESS_KEY);
        this.secretKeyPreferenceName   = String.format("%s:%s", accountId, PreferenceConstants.P_SECRET_KEY);
    }

    /* All credential-related information are B64-encoded */

    @Override
    public String getAccountName() {
        return this.accountNameInMemory != null ?
                this.accountNameInMemory
                :
                PreferenceValueEncodingUtil.decodeString(
                    prefStore.getString(accountNamePreferenceName));
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountNameInMemory = accountName;
    }

    @Override
    public String getAccessKey() {
        return this.accessKeyInMemory != null ?
                this.accessKeyInMemory
                :
                PreferenceValueEncodingUtil.decodeString(
                    prefStore.getString(accessKeyPreferenceName));
    }

    @Override
    public void setAccessKey(String accessKey) {
        this.accessKeyInMemory = accessKey;
    }

    @Override
    public String getSecretKey() {
        return this.secretKeyInMemory != null ?
                this.secretKeyInMemory
                :
                PreferenceValueEncodingUtil.decodeString(
                    prefStore.getString(secretKeyPreferenceName));
    }

    @Override
    public void setSecretKey(String secretKey) {
        this.secretKeyInMemory = secretKey;
    }

    /**
     * Session token is not supported when the preference store is in use as the
     * data source; this method always return false.
     */
    @Override
    public boolean isUseSessionToken() {
        return false;
    }

    /**
     * Session token is not supported when the preference store is in use as the
     * data source; this method doesn't have any effect
     */
    @Override
    public void setUseSessionToken(boolean useSessionToken) {
    }

    /**
     * Session token is not supported when the preference store is in use as the
     * data source; this method always return null.
     */
    @Override
    public String getSessionToken() {
        return null;
    }

    /**
     * Session token is not supported when the preference store is in use as the
     * data source; this method doesn't have any effect
     */
    @Override
    public void setSessionToken(String sessionToken) {
    }

    /**
     * Persist all the in-memory property values in the preference store.
     */
    @Override
    public void save() {
        if (accountNameInMemory != null) {
            String newAccountName = accountNameInMemory.trim();
            prefStore.setValue(accountNamePreferenceName,
                    PreferenceValueEncodingUtil.encodeString(newAccountName));
        }

        if (accessKeyInMemory != null) {
            String newAccessKey = accessKeyInMemory.trim();
            prefStore.setValue(accessKeyPreferenceName,
                    PreferenceValueEncodingUtil.encodeString(newAccessKey));
        }

        if (secretKeyInMemory != null) {
            String newSecretKey = secretKeyInMemory.trim();
            prefStore.setValue(secretKeyPreferenceName,
                    PreferenceValueEncodingUtil.encodeString(newSecretKey));
        }

        clearInMemoryValue();
    }

    /**
     * Remove all the preference properties relating to this account's
     * credential information
     */
    @Override
    public void delete() {
        prefStore.setToDefault(accountNamePreferenceName);
        prefStore.setToDefault(accessKeyPreferenceName);
        prefStore.setToDefault(secretKeyPreferenceName);

        clearInMemoryValue();
    }

    @Override
    public boolean isDirty() {
        // For performance reason, we only check whether there exists in-memory
        // property values (no matter it differs from the source value or not.)
        return accountNameInMemory != null
                || accessKeyInMemory != null
                || secretKeyInMemory != null;
    }

    private void clearInMemoryValue() {
        accountNameInMemory = null;
        accessKeyInMemory = null;
        secretKeyInMemory = null;
    }

}
