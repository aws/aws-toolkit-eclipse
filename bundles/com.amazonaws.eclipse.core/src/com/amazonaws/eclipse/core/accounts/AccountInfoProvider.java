/*
 * Copyright 2010-2014 Amazon Technologies, Inc.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.auth.profile.ProfilesConfigFileWriter;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.auth.profile.internal.Profile;
import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.preferences.PluginPreferenceStoreAccountCredentialsConfiguration;
import com.amazonaws.eclipse.core.accounts.preferences.PluginPreferenceStoreAccountOptionalConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;
import com.amazonaws.eclipse.core.util.FileUtils;
import com.amazonaws.util.StringUtils;

/**
 * A class that loads and returns all the configured AccountInfo. It's
 * also responsible for hooking the AccountInfoChangeListener.
 */
public class AccountInfoProvider {

    private final IPreferenceStore prefStore;

    /**
     * Loading from the credentials file is an expensive operation, so we want
     * to cache all the loaded AccountInfo objects.
     */
    Map<String, AccountInfo> profileAccountInfoCache = new LinkedHashMap<>();

    /** All the registered AccountInfoChangeListener */
    List<AccountInfoChangeListener> listeners = new LinkedList<>();

    /**
     * Initialize the provider with the given preference store instance.
     */
    public AccountInfoProvider(IPreferenceStore prefStore) {
        this.prefStore = prefStore;
    }

    /**
     * Returns all the account info that are loaded from the credential profiles
     * file. For performance reason, this method won't attempt to reload the
     * accounts upon each method call, therefore the returned result might be
     * out of sync with the external source. Call the {@link #refresh()} method
     * to forcefully reload all the account info.
     *
     * @return Map from the accountId to each of the profile-based AccountInfo
     *         object.
     */
    public Map<String, AccountInfo> getAllProfileAccountInfo() {
        return Collections.unmodifiableMap(profileAccountInfoCache);
    }

    /**
     * Returns the profile account info by the account identifier.
     *
     * @see #getAllProfileAccountInfo()
     */
    public AccountInfo getProfileAccountInfo(String accountId) {
        return profileAccountInfoCache.get(accountId);
    }

    /**
     * Loads and returns all the legacy account configuration from the
     * preference store system. The identifiers of legacy accounts could be
     * found in the following preference keys:
     * (1) accountIds              (ids of all the global accounts)
     * (2) accountIds-region       (e.g. "accountIds-cn-north-1", ids of all the regional accounts)
     *
     * @return Map from the accountId to each of the legacy AccountInfo
     *         object.
     */
    public Map<String, AccountInfo> getAllLegacyPreferenceStoreAccontInfo() {
        Map<String, AccountInfo> preferenceStoreAccounts = new LinkedHashMap<>();

        // Global accounts
        preferenceStoreAccounts.putAll(loadPreferenceStoreAccountsByRegion(null));

        // Regional accounts
        List<String> regionsWithDefaultAccount = AwsAccountPreferencePage
                .getRegionsWithDefaultAccounts(prefStore);
        for (String regionId : regionsWithDefaultAccount) {
            preferenceStoreAccounts.putAll(loadPreferenceStoreAccountsByRegion(regionId));
        }

        return preferenceStoreAccounts;
    }

    /**
     * Returns the legacy preference store account info by the account
     * identifier.
     *
     * @see #getAllLegacyPreferenceStoreAccontInfo()
     */
    public AccountInfo getLegacyPreferenceStoreAccountInfo(String accountId) {
        return getAllLegacyPreferenceStoreAccontInfo().get(accountId);
    }

    /**
     * Forces the provider to refresh all the profile AccountInfo it vends.
     *
     * @param boostrapCredentialsFile
     *            If set true, this method will create a credentials file with a
     *            default profile if no account is currently configured in the
     *            toolkit.
     * @param showWarningOnFailure
     *            If true, this method will show a warning message box if it
     *            fails to load accounts.
     */
    public synchronized void refreshProfileAccountInfo(final boolean boostrapCredentialsFile, final boolean showWarningOnFailure) {
        reloadProfileAccountInfo(boostrapCredentialsFile, showWarningOnFailure);

        // Notify the change listeners
        for (AccountInfoChangeListener listener : listeners) {
            listener.onAccountInfoChange();
        }
    }

    /**
     * Update the following preference value:
     *  (1) P_CREDENTIAL_PROFILE_ACCOUNT_IDS - ids of all the accounts that were loaded from credential profiles file.
     *  (2) accountId:P_CREDENTIAL_PROFILE_NAME - name of the credential profile
     *
     * This method needs to be called whenever we reload profile credentials.
     */
    public void updateProfileAccountMetadataInPreferenceStore(Collection<AccountInfo> accounts) {
        List<String> accountIds = new LinkedList<>();
        for (AccountInfo accountInfo : accounts) {
            accountIds.add(accountInfo.getInternalAccountId());
        }

        String accountIdsString = StringUtils.join(PreferenceConstants.ACCOUNT_ID_SEPARATOR,
                accountIds.toArray(new String[accountIds.size()]));
        prefStore.setValue(
                PreferenceConstants.P_CREDENTIAL_PROFILE_ACCOUNT_IDS, accountIdsString);

        for(AccountInfo profileAccount : accounts) {
            String profileNamePrefKey = profileAccount.getInternalAccountId()
                    + ":" + PreferenceConstants.P_CREDENTIAL_PROFILE_NAME;
            prefStore.setValue(
                    profileNamePrefKey, profileAccount.getAccountName());
        }
    }

    /**
     * Register an AccountInfoChangeListener to this provider. All the
     * registered listeners will be notified whenever this provider is
     * refreshed.
     *
     * @param listener
     *            The new AccountInfoChangeListener to be registered.
     */
    public void addAccountInfoChangeListener(AccountInfoChangeListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Remove an AccountInfoChangeListener from this provider.
     */
    public void removeAccountInfoChangeListener(AccountInfoChangeListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    /* Profile-based account info */

    private void reloadProfileAccountInfo(final boolean boostrapCredentialsFile, final boolean showWarningOnFailure) {

        profileAccountInfoCache.clear();

        /* Load the credential profiles file */

        String credFileLocation = prefStore
                .getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION);

        ProfilesConfigFile profileConfigFile = null;

        try {
            Path credFilePath = Paths.get(credFileLocation);
            if (!Files.exists(credFilePath) && boostrapCredentialsFile) {
                File credFile = FileUtils.createFileWithPermission600(credFileLocation);
                // TODO We need to reconsider whether to dump an empty credentials profile when we cannot find one.
                ProfilesConfigFileWriter.dumpToFile(
                        credFile,
                        true, // overwrite=true
                        new Profile(PreferenceConstants.DEFAULT_ACCOUNT_NAME, new BasicAWSCredentials("", "")));
            }

            profileConfigFile = new ProfilesConfigFile(credFilePath.toFile());
        } catch (Exception e) {
            String errorMsg = String.format("Failed to load credential profiles from (%s).",
                    credFileLocation);
            AwsToolkitCore.getDefault().logInfo(errorMsg + e.getMessage());

            if ( showWarningOnFailure ) {
                MessageDialog.openError(null,
                                "Unable to load profile accounts",
                                errorMsg + " Please check that your credentials file is at the correct location "
                                + "and that it is in the correct format.");
            }
        }

        if (profileConfigFile == null) return;

        /* Set up the AccountInfo objects */

        // Map from profile name to its pre-configured account id
        Map<String, String> exisitingProfileAccountIds = getExistingProfileAccountIds();

        // Iterate through the newly loaded profiles. Re-use the existing
        // account id if the profile name is already configured in the toolkit.
        // Otherwise assign a new UUID for it.

        for (Entry<String, BasicProfile> entry : profileConfigFile.getAllBasicProfiles().entrySet()) {
            String profileName = entry.getKey();
            BasicProfile basicProfile = entry.getValue();

            String accountId = exisitingProfileAccountIds.get(profileName);
            if (accountId == null) {
                AwsToolkitCore.getDefault().logInfo("No profile found: " + profileName);
                accountId = UUID.randomUUID().toString();
            }

            AccountInfo profileAccountInfo = new AccountInfoImpl(accountId,
                    new SdkProfilesCredentialsConfiguration(prefStore, accountId, basicProfile),
                    // Profile accounts use profileName as the preference name prefix
                    // @see PluginPreferenceStoreAccountOptionalConfiguration
                    new PluginPreferenceStoreAccountOptionalConfiguration(prefStore, profileName));
            profileAccountInfoCache.put(accountId, profileAccountInfo);
        }

        /* Update the preference store metadata for the newly loaded profile accounts */

        updateProfileAccountMetadataInPreferenceStore(profileAccountInfoCache.values());
    }

    /**
     * The toolkit uses the "credentialProfileAccountIds" preference property to
     * store the identifiers of all the profile-based accounts. This method
     * checks this preference value and returns a map of all the existing
     * profile accounts that are already configured in the toolkit.
     *
     * @return Key - profile name;  Value - account id
     */
    private Map<String, String> getExistingProfileAccountIds() {
        Map<String, String> exisitingProfileAccounts = new HashMap<>();

        // Ids of all the profile accounts currently configured in the toolkit
        String[] profileAccountIds = prefStore.getString(
                PreferenceConstants.P_CREDENTIAL_PROFILE_ACCOUNT_IDS).split(
                PreferenceConstants.ACCOUNT_ID_SEPARATOR_REGEX);

        for (String accountId : profileAccountIds) {
            String configuredProfileName = prefStore.getString(
                    accountId + ":" + PreferenceConstants.P_CREDENTIAL_PROFILE_NAME);
            if (configuredProfileName != null && !configuredProfileName.isEmpty()) {
                exisitingProfileAccounts.put(configuredProfileName, accountId);
            }
        }

        return exisitingProfileAccounts;
    }

    /* Pref-store-based account info */

    /**
     * Returns a map of all the preference-store-based account info.
     *
     * @param region
     *            Null value indicates the global region
     */
    @SuppressWarnings("deprecation")
    private Map<String, AccountInfo> loadPreferenceStoreAccountsByRegion(String regionId) {
        String p_regionalAccountIds = regionId == null ?
                PreferenceConstants.P_ACCOUNT_IDS
                :
                PreferenceConstants.P_ACCOUNT_IDS + "-" + regionId;

        String[] accountIds = prefStore.getString(p_regionalAccountIds)
                .split(PreferenceConstants.ACCOUNT_ID_SEPARATOR_REGEX);

        Map<String, AccountInfo> accounts = new LinkedHashMap<>();

        for ( String accountId : accountIds ) {
            if (accountId.length() > 0) {
                // Both credential and optional configurations are preference-store-based
                AccountInfo prefStoreAccountInfo = new AccountInfoImpl(accountId,
                        new PluginPreferenceStoreAccountCredentialsConfiguration(prefStore, accountId),
                        new PluginPreferenceStoreAccountOptionalConfiguration(prefStore, accountId));

                accounts.put(accountId, prefStoreAccountInfo);
            }
        }

        return accounts;
    }
}
