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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfilesConfigFileWriter;
import com.amazonaws.auth.profile.internal.Profile;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.AccountCredentialsConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

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
    private Profile profile;

    /** The names of the preference properties relating to this account */
    private final String profileNamePreferenceName;

    /** The property values set in memory */
    private String profileNameInMemory;
    private String accessKeyInMemory;
    private String secretKeyInMemory;

    public SdkProfilesCredentialsConfiguration(
                IPreferenceStore prefStore,
                String accountId,
                Profile profile) {
        if (prefStore == null)
            throw new IllegalAccessError("prefStore must not be null.");
        if (accountId == null)
            throw new IllegalAccessError("accountId must not be null.");
        if (profile == null)
            throw new IllegalAccessError("profile must not be null.");

        this.prefStore = prefStore;
        this.profile   = profile;

        this.profileNamePreferenceName = String.format("%s:%s", accountId, PreferenceConstants.P_CREDENTIAL_PROFILE_NAME);
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
                profile.getCredentials().getAWSAccessKeyId();
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
                profile.getCredentials().getAWSSecretKey();
    }

    @Override
    public void setSecretKey(String secretKey) {
        this.secretKeyInMemory = secretKey;
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

            // Output the new profile to the credentials file
            Profile newProfile = new Profile(
                    getAccountName(),
                    new BasicAWSCredentials(getAccessKey(), getSecretKey()));
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
                    StatusManager.getManager().handle(
                            new Status(IStatus.ERROR, AwsToolkitCore.PLUGIN_ID,
                                    "Failed to create credentials file at " +
                                            credentialsFile.getAbsolutePath(),
                                    ioe),
                                    StatusManager.SHOW);
                }
            }

            String prevProfileName = profile.getProfileName();
            ProfilesConfigFileWriter.modifyOneProfile(credentialsFile,
                    prevProfileName, newProfile);

            // Persist the profile metadata in the preference store:
            // accountId:credentialProfileName=profileName
            prefStore.setValue(
                    profileNamePreferenceName,
                    newProfile.getProfileName());

            this.profile = newProfile;
            clearInMemoryValue();
        }
    }

    /**
     * Deletes the profile name property associated with this account in the
     * preferece store instance. Also remove the profile from the credentials
     * file.
     */
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
        // For performance reason, we only check whether there exists in-memory
        // property values (no matter it differs from the source value or not.)
        return profileNameInMemory != null
                || accessKeyInMemory != null
                || secretKeyInMemory != null;
    }

    private void clearInMemoryValue() {
        profileNameInMemory = null;
        accessKeyInMemory = null;
        secretKeyInMemory = null;
    }

}
