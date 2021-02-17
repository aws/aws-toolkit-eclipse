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
package com.amazonaws.eclipse.core.accounts;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.http.annotation.Contract;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.preferences.PluginPreferenceStoreAccountOptionalConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkCredentialsFileMonitor;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesFactory;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.preferences.PreferencePropertyChangeListener;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;

/**
 * This class acts as a facade for all the account-related configurations for
 * the plugin. Different feature components should use this class to query/set
 * the current default account. It is also responsible to notify the registered
 * listeners about the change of default account.
 * <p>
 * When requested to retrieve a specific account info, this class delegates to a
 * list of AccountInfoProvider implementations to aggregate all the accounts
 * configured via different ways (e.g. by the Eclipse preference store system,
 * or loaded from the local credentials file).
 */
@Contract (threading = org.apache.http.annotation.ThreadingBehavior.UNSAFE)
public final class AwsPluginAccountManager {

    /**
     * The preference store where the configuration for the global/regional
     * default account is persisted..
     */
    private final IPreferenceStore preferenceStore;

    /** Monitors for changes of global/regional default account preference */
    private DefaultAccountMonitor defaultAccountMonitor;

    /** Monitors the configured location of the credentials file as specified in the preference store */
    private final SdkCredentialsFileMonitor sdkCredentialsFileMonitor;

    /**
     * The AccountInfoProvider from which the manager retrieves the AccountInfo
     * objects.
     */
    private final AccountInfoProvider accountInfoProvider;

    /**
     * The AccountInfo object to return when no account is configured and the
     * toolkit is unable to bootstrap the credentials file.
     */
    private final AccountInfo tempAccount;

    private boolean noAccountConfigured = false;

    public AwsPluginAccountManager(IPreferenceStore preferenceStore,
            AccountInfoProvider accountInfoProvider) {
        this.preferenceStore = preferenceStore;
        this.accountInfoProvider = accountInfoProvider;

        this.sdkCredentialsFileMonitor = new SdkCredentialsFileMonitor();

        String accountId = UUID.randomUUID().toString();
        tempAccount =  new AccountInfoImpl(accountId,
                new SdkProfilesCredentialsConfiguration(preferenceStore, accountId,
                        SdkProfilesFactory.newEmptyBasicProfile("temp")),
                new PluginPreferenceStoreAccountOptionalConfiguration(preferenceStore, accountId));
    }

    /**
     * Start all the monitors on account-related preference properties.
     */
    public void startAccountMonitors() {
        if (defaultAccountMonitor == null) {
            defaultAccountMonitor = new DefaultAccountMonitor();
            getPreferenceStore().addPropertyChangeListener(
                    defaultAccountMonitor);
        }
    }

    /**
     * Stop all the monitors on account-related preference properties.
     */
    public void stopAccountMonitors() {
        if (defaultAccountMonitor != null) {
            getPreferenceStore().removePropertyChangeListener(defaultAccountMonitor);
        }
    }

    /**
     * Start monitoring the location and content of the credentials file
     */
    public void startCredentialsFileMonitor() {
        sdkCredentialsFileMonitor.start(preferenceStore);
    }

    /**
     * Returns the AccountInfoProvider that is used by this class.
     */
    public AccountInfoProvider getAccountInfoProvider() {
        return accountInfoProvider;
    }

    /**
     * Returns the currently selected account info. If the current account id is
     * not found in the loaded accounts (e.g. when the previously configured
     * account is removed externally in the credentials file), this method falls
     * back to returning the "default" profile account (or the first profile
     * account if the "default" profile doesn't exist). If no account is
     * configured in the toolkit (most probably because the toolkit failed to
     * load the credentials file), this method returns a temporary empty
     * AccountInfo object. In short, this method never returns null.
     *
     * @return The user's AWS account info.
     */
    public AccountInfo getAccountInfo() {
        if (noAccountConfigured) {
            return tempAccount;
        }

        AccountInfo currentAccount = getAccountInfo(getCurrentAccountId());
        if (currentAccount != null) {
            return currentAccount;
        }

        // Find an existing account to fall back to
        Collection<AccountInfo> allAccounts = getAllAccountInfo().values();
        if ( !allAccounts.isEmpty() ) {
            AwsToolkitCore.getDefault().logInfo("The current accountId is not found in the system. " +
                    "Switching to the default account.");

            AccountInfo fallbackAccount = allAccounts.iterator().next();

            // Find the "default" account
            for (AccountInfo account : allAccounts) {
                if (account.getAccountName().equals(PreferenceConstants.DEFAULT_ACCOUNT_NAME)) {
                    fallbackAccount = account;
                }
            }

            setCurrentAccountId(fallbackAccount.getInternalAccountId());
            return fallbackAccount;
        }

        AwsToolkitCore.getDefault().logInfo("No account could be found. " +
                "Switching to a temporary account.");

        // Directly return the temp AccountInfo object if no account could be found
        noAccountConfigured = true;
        return tempAccount;
    }

    /**
     * Gets account info for the given account name. The query is performed by
     * the AccountInfoProvider instance included in this manager. This method
     * still checks for the legacy pref-store-based accounts if the account id
     * cannot be found in the profile-based accounts.
     *
     * @param accountId
     *            The id of the account for which to get info.
     */
    public AccountInfo getAccountInfo(String accountId) {

        if (accountInfoProvider.getProfileAccountInfo(accountId) != null) {
            return accountInfoProvider.getProfileAccountInfo(accountId);
        }

        if (accountInfoProvider.getLegacyPreferenceStoreAccountInfo(accountId) != null) {
            return accountInfoProvider.getLegacyPreferenceStoreAccountInfo(accountId);
        }

        return null;
    }

    /**
     * Refresh all the account info providers.
     */
    public void reloadAccountInfo() {
        noAccountConfigured = false;

        final boolean noLegacyAccounts = accountInfoProvider.getAllLegacyPreferenceStoreAccontInfo().isEmpty();

        // Only bootstrap the credentials file if no legacy account exists
        final boolean boostrapCredentialsFile = noLegacyAccounts;
        // Only show warning for the credentials file loading failure if no legacy account exists
        final boolean showWarningOnFailure = noLegacyAccounts;
        accountInfoProvider.refreshProfileAccountInfo(boostrapCredentialsFile, showWarningOnFailure);
    }

    /**
     * Returns the current account Id
     */
    public String getCurrentAccountId() {
        return getPreferenceStore().getString(
                PreferenceConstants.P_CURRENT_ACCOUNT);
    }

    /**
     * Sets the current account id. No error checking is performed, so ensure
     * the given account Id is valid.
     */
    public void setCurrentAccountId(String accountId) {
        getPreferenceStore().setValue(PreferenceConstants.P_CURRENT_ACCOUNT,
                accountId);
    }

    /**
     * Update the default account to use according to the current default
     * region.
     */
    public void updateCurrentAccount() {
        updateCurrentAccount(RegionUtils.getCurrentRegion());
    }

    /**
     * Set the given account identifier as the default account for a region. If
     * this region does not have any default account setting (or setting is
     * disabled), then this method will set it as the global default account.
     */
    public void setDefaultAccountId(Region region, String accountId) {
        if (AwsAccountPreferencePage.isRegionDefaultAccountEnabled(
                getPreferenceStore(), region)) {
            getPreferenceStore()
                    .setValue(
                            PreferenceConstants
                                    .P_REGION_CURRENT_DEFAULT_ACCOUNT(region),
                            accountId);
        } else {
            getPreferenceStore().setValue(
                    PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT,
                    accountId);
        }
    }

    /**
     * Update the current accountId according to the given region.
     */
    public void updateCurrentAccount(Region newRegion) {
        if (AwsAccountPreferencePage.isRegionDefaultAccountEnabled(
                getPreferenceStore(), newRegion)) {
            AwsToolkitCore.getDefault().logInfo(
                    "Switching to region-specific default account for region "
                            + newRegion.getId());
            // Use the region-specific default account
            setCurrentAccountId(getPreferenceStore().getString(
                    PreferenceConstants
                            .P_REGION_CURRENT_DEFAULT_ACCOUNT(newRegion)));
        } else {
            AwsToolkitCore.getDefault().logInfo(
                    "Switching to global default account");
            // Use the global default account
            setCurrentAccountId(getPreferenceStore().getString(
                    PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT));
        }
    }

    /**
     * Returns a map of the names of all the accounts configured in the toolkit.
     */
    public Map<String, String> getAllAccountNames() {
        Map<String, AccountInfo> allAccountInfo = getAllAccountInfo();
        if (allAccountInfo == null) {
            return Collections.<String, String>emptyMap();
        }

        Map<String, String> allAccountNames = new LinkedHashMap<>();
        for (Entry<String, AccountInfo> entry : allAccountInfo.entrySet()) {
            allAccountNames.put(
                    entry.getKey(),
                    entry.getValue().getAccountName());
        }
        return allAccountNames;
    }

    /**
     * Returns a map from profile names to account names in the toolkit.
     */
    public Map<String, String> getAllAccountIds() {
        Map<String, AccountInfo> allAccountInfo = getAllAccountInfo();
        if (allAccountInfo == null) {
            return Collections.<String, String>emptyMap();
        }

        Map<String, String> allAccountIds = new LinkedHashMap<>();
        for (Entry<String, AccountInfo> entry : allAccountInfo.entrySet()) {
            allAccountIds.put(
                    entry.getValue().getAccountName(),
                    entry.getKey()
                    );
        }
        return allAccountIds;
    }

    /**
     * Returns a map of all the accounts configured in the toolkit. This method
     * returns all the legacy pref-store-based accounts if none of the profile
     * accounts could be found. This method returns an empty map when it failed
     * to load accounts from the credentials file and no legacy account is
     * configured.
     */
    public Map<String, AccountInfo> getAllAccountInfo() {
        Map<String, AccountInfo> accounts = accountInfoProvider.getAllProfileAccountInfo();

        // If no profile account is found, fall back to the legacy accounts
        if (accounts.isEmpty()) {
            accounts = accountInfoProvider.getAllLegacyPreferenceStoreAccontInfo();
        }

        // If even legacy accounts cannot be found, bootstrap the credentials file
        if (accounts.isEmpty()) {
            AwsToolkitCore.getDefault().logInfo(
                    String.format("No account is configued in the toolkit. " +
                            "Bootstrapping the credentials file at (%s).",
                            preferenceStore.getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)));

            // boostrapCredentialsFile=true, showWarningOnFailure=false
            accountInfoProvider.refreshProfileAccountInfo(true, false);
            accounts = accountInfoProvider.getAllProfileAccountInfo();
        }

        return accounts;
    }

    /**
     * Registers a listener to receive notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addAccountInfoChangeListener(
            AccountInfoChangeListener listener) {
        accountInfoProvider.addAccountInfoChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeAccountInfoChangeListener(
            AccountInfoChangeListener listener) {
        accountInfoProvider.removeAccountInfoChangeListener(listener);
    }

    /**
     * Registers a listener to receive notifications when global/regional
     * default accounts are changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addDefaultAccountChangeListener(
            PreferencePropertyChangeListener listener) {
        defaultAccountMonitor.addChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when global/regional
     * default accounts are changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeDefaultAccountChangeListener(
            PreferencePropertyChangeListener listener) {
        defaultAccountMonitor.removeChangeListener(listener);
    }

    private IPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    /**
     * Returns whether there are valid aws accounts configured
     */
    public boolean validAccountsConfigured() {
        return  getAccountInfo().isValid() || getAllAccountNames().size() > 1;
    }
}
