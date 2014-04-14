/*
 * Copyright 2008-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.preferences.accounts;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Implementation of the AccountInfo abstract class that provides AWS account
 * information through Eclipse's preference system.
 */
public class PluginPreferenceStoreAccountInfo extends AccountInfo {

    /** The preference store containing the account information */
    private final IPreferenceStore preferenceStore;
    private final String accountId;

    /**
     * Constructs a new object configured to pull account info from the
     * specified preference store. The account information is tied to the
     * account name given, but the user could still change the credentials for
     * that account, so successive calls to accessors can return different
     * results. Clients wishing to be notified of such changes should register a
     * change listener with {@link AwsToolkitCore}.
     * 
     * @see AwsToolkitCore#addAccountInfoChangeListener(AccountInfoChangeListener)
     * @param preferenceStore
     *            The preference store to pull AWS account information.
     * @param accountName
     *            The account name to use when looking up the account
     *            information.
     */
    public PluginPreferenceStoreAccountInfo(IPreferenceStore preferenceStore, String accountId) {
        this.preferenceStore = preferenceStore;
        this.accountId = accountId;
    }

    /**
     * Gets the key used to obtain the preference name, based on the current
     * account. Fall-back behavior for legacy customers supports bare preference
     * keys.
     */
    private String getAccountPreferenceName(String preferenceName) {
        return accountId + ":" + preferenceName;
    }

    /*
     * AccountInfo Interface
     */

    @Override
    public String getUserId() {
        return decodeString(preferenceStore.getString(getAccountPreferenceName(PreferenceConstants.P_USER_ID)));
    }

    @Override
    public String getAccessKey() {
        return decodeString(preferenceStore.getString(getAccountPreferenceName(PreferenceConstants.P_ACCESS_KEY)));
    }

    @Override
    public String getSecretKey() {
        return decodeString(preferenceStore.getString(getAccountPreferenceName(PreferenceConstants.P_SECRET_KEY)));
    }

    @Override
    public String getEc2PrivateKeyFile() {
        return preferenceStore.getString(getAccountPreferenceName(PreferenceConstants.P_PRIVATE_KEY_FILE));
    }

    @Override
    public String getEc2CertificateFile() {
        return preferenceStore.getString(getAccountPreferenceName(PreferenceConstants.P_CERTIFICATE_FILE));
    }

    @Override
    public void setAccessKey(String accessKey) {
        throw new RuntimeException(this.getClass().getName() + " is a read-only interface");
    }

    @Override
    public void setSecretKey(String secretKey) {
        throw new RuntimeException(this.getClass().getName() + " is a read-only interface");
    }

    /*
     * Private Interface
     */

    private String decodeString(String s) {
        return new String(Base64.decodeBase64(s.getBytes()));
    }

}
