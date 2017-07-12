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
package com.amazonaws.eclipse.core.accounts.profiles;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.auth.profile.ProfilesConfigFileWriter;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.AccountCredentialsConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.util.StringUtils;

/**
 * Concrete implementation of AccountCredentialsConfiguration, which uses the
 * credential profiles file to persist the credential configurations.
 */
public class SdkProfilesCredentialsConfiguration extends
        AccountCredentialsConfiguration {

    /**
     * Use this preference store to save the mapping from an accountId to a
     * profile name.
     */
    private final IPreferenceStore prefStore;

    /**
     * The profile instance loaded from the credentials file.
     */
    private BasicProfile profile;

    /**
     * The name of the preference property that maps an internal account id to
     * the name of the credential profile associated with the account.
     */
    private final String profileNamePreferenceName;

    /** The property values set in memory */
    private String profileNameInMemory;
    private String accessKeyInMemory;
    private String secretKeyInMemory;
    private Boolean useSessionToken;
    private String sessionTokenInMemory;

    public SdkProfilesCredentialsConfiguration(
                IPreferenceStore prefStore,
                String accountId,
                BasicProfile profile) {
        if (prefStore == null)
            throw new IllegalAccessError("prefStore must not be null.");
        if (accountId == null)
            throw new IllegalAccessError("accountId must not be null.");
        if (profile == null)
            throw new IllegalAccessError("profile must not be null.");

        this.prefStore = prefStore;
        this.profile   = profile;

        this.profileNamePreferenceName = String.format("%s:%s", accountId,
                PreferenceConstants.P_CREDENTIAL_PROFILE_NAME);
    }

    /* All credential-related information are B64-encoded */

    /**
     * Use the profile name as the account name shown in the toolkit UI.
     */
    @Override
    public String getAccountName() {
        return this.profileNameInMemory != null ?
                this.profileNameInMemory
                :
                profile.getProfileName();
    }

    @Override
    public void setAccountName(String accountName) {
        this.profileNameInMemory = accountName;
    }

    @Override
    public String getAccessKey() {
        return this.accessKeyInMemory != null ?
                this.accessKeyInMemory
                :
                profile.getAwsAccessIdKey();
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
                profile.getAwsSecretAccessKey();
    }

    @Override
    public void setSecretKey(String secretKey) {
        this.secretKeyInMemory = secretKey;
    }


    @Override
    public boolean isUseSessionToken() {
        if (useSessionToken != null) {
            return useSessionToken;
        }
        return !StringUtils.isNullOrEmpty(profile.getAwsSessionToken());
    }

    @Override
    public void setUseSessionToken(boolean useSessionToken) {
        this.useSessionToken = useSessionToken;
    }

    @Override
    public String getSessionToken() {
        if (sessionTokenInMemory != null) {
            return sessionTokenInMemory;
        }
        return profile.getAwsSessionToken();
    }

    @Override
    public void setSessionToken(String sessionToken) {
        this.sessionTokenInMemory = sessionToken;
    }

    /**
     * Write all the in-memory property values in the credentials file.
     */
    @Override
    public void save() {
        if (isDirty()) {
            // Clean up the properties before saving it
            if (profileNameInMemory != null) {
                profileNameInMemory = profileNameInMemory.trim();
            }
            if (accessKeyInMemory != null) {
                accessKeyInMemory = accessKeyInMemory.trim();
            }
            if (secretKeyInMemory != null) {
                secretKeyInMemory = secretKeyInMemory.trim();
            }
            if (sessionTokenInMemory != null) {
                sessionTokenInMemory = sessionTokenInMemory.trim();
            }

            BasicProfile newBasicProfile = SdkProfilesFactory.newBasicProfile(
                    getAccountName(), getAccessKey(), getSecretKey(), isUseSessionToken() ? getSessionToken() : null);
            // Output the new profile to the credentials file
            File credentialsFile = new File(
                    prefStore.getString(
                            PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION));

            // Create the file if it doesn't exist yet
            // TODO: ideally this should be handled by ProfilesConfigFileWriter
            if ( !credentialsFile.exists() ) {
                try {
                    if (credentialsFile.getParentFile() != null) {
                        credentialsFile.getParentFile().mkdirs();
                    }
                    credentialsFile.createNewFile();
                } catch (IOException ioe) {
                    AwsToolkitCore.getDefault().reportException("Failed to create credentials file at " +
                                            credentialsFile.getAbsolutePath(), ioe);
                }
            }

            String prevProfileName = profile.getProfileName();
            ProfilesConfigFileWriter.modifyOneProfile(credentialsFile,
                    prevProfileName, SdkProfilesFactory.convert(newBasicProfile));

            // Persist the profile metadata in the preference store:
            // accountId:credentialProfileName=profileName
            prefStore.setValue(
                    profileNamePreferenceName,
                    newBasicProfile.getProfileName());

            this.profile = newBasicProfile;
            clearInMemoryValue();
        }
    }

    /**
     * Deletes the profile name property associated with this account in the
     * preferece store instance. Also remove the profile from the credentials
     * file.
     */
    @Override
    public void delete() {
        prefStore.setToDefault(profileNamePreferenceName);

        File credentialsFile = new File(
                prefStore.getString(
                        PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION));

        String prevProfileName = profile.getProfileName();
        ProfilesConfigFileWriter.deleteProfiles(credentialsFile, prevProfileName);

        clearInMemoryValue();
    }

    @Override
    public boolean isDirty() {
        return (hasPropertyChanged(profile.getProfileName(), profileNameInMemory)
                || hasPropertyChanged(profile.getAwsAccessIdKey(), accessKeyInMemory)
                || hasPropertyChanged(profile.getAwsSecretAccessKey(), secretKeyInMemory)
                || hasPropertyChanged(!StringUtils.isNullOrEmpty(profile.getAwsSessionToken()), useSessionToken)
                || hasPropertyChanged(profile.getAwsSessionToken(), sessionTokenInMemory));
    }

    /**
     * @return True if the new value of the property is non null and differs
     *         from the original, false otherwise
     */
    private boolean hasPropertyChanged(Object originalValue, Object newValue) {
        return newValue != null && !newValue.equals(originalValue);
    }

    private void clearInMemoryValue() {
        profileNameInMemory = null;
        accessKeyInMemory = null;
        secretKeyInMemory = null;
        useSessionToken = null;
        sessionTokenInMemory = null;
    }

}
