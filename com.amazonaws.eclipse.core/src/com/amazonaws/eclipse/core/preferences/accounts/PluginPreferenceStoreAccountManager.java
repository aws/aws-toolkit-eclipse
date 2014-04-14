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

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.preferences.PreferencePropertyChangeListener;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;

/**
 * This class is used for persisting and monitoring preference property values
 * relating to the user's AWS accounts.
 */
public class PluginPreferenceStoreAccountManager {

    /** The preference store instance the manager writes to or reads from. */
    private final IPreferenceStore preferenceStore;

    /** Monitors for changes to AWS account information, and notifies listeners */
    private AccountInfoMonitor accountInfoMonitor;

    /** Monitors for changes of global/regional default account preference */
    private DefaultAccountMonitor defaultAccountMonitor;

    public PluginPreferenceStoreAccountManager(IPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
    }

    /**
     * Start all the monitors on account-related preference properties.
     */
    public void startAccountMonitors() {
        if (accountInfoMonitor == null) {
            accountInfoMonitor = new AccountInfoMonitor();
            getPreferenceStore().addPropertyChangeListener(accountInfoMonitor);
        }
        if (defaultAccountMonitor == null) {
            defaultAccountMonitor = new DefaultAccountMonitor();
            getPreferenceStore().addPropertyChangeListener(defaultAccountMonitor);
        }
    }

    /**
     * Stop all the monitors on account-related preference properties.
     */
    public void stopAccountMonitors() {
        getPreferenceStore().removePropertyChangeListener(accountInfoMonitor);
        getPreferenceStore().removePropertyChangeListener(defaultAccountMonitor);
    }

    /**
     * Returns the currently selected account info.
     *
     * @return The user's AWS account info.
     */
    public AccountInfo getAccountInfo() {
        return getAccountInfo(null);
    }

    /**
     * Gets account info for the given account name. No error checking is
     * performed on the account name, so clients must be careful to ensure its
     * validity.
     *
     * @param accountId
     *            The id of the account for which to get info, or null for the
     *            currently selected account.
     */
    public AccountInfo getAccountInfo(String accountId) {
        if (accountId == null)
            accountId = getCurrentAccountId();
        return new PluginPreferenceStoreAccountInfo(getPreferenceStore(), accountId);
    }

    /**
     * Returns the current account Id
     */
    public String getCurrentAccountId() {
        return getPreferenceStore().getString(PreferenceConstants.P_CURRENT_ACCOUNT);
    }

    /**
     * Sets the current account id. No error checking is performed, so ensure
     * the given account Id is valid.
     */
    public void setCurrentAccountId(String accountId) {
        getPreferenceStore().setValue(PreferenceConstants.P_CURRENT_ACCOUNT, accountId);
    }

    /**
     * Update the default account to use according to the current default region.
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
        if (AwsAccountPreferencePage.isRegionDefaultAccountEnabled(getPreferenceStore(), region)) {
            getPreferenceStore().setValue(PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region),
                                          accountId);
        } else {
            getPreferenceStore().setValue(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT,
                                          accountId);
        }
    }

    /**
     * Update the current accountId according to the given region.
     */
    public void updateCurrentAccount(Region newRegion) {
        if (AwsAccountPreferencePage.isRegionDefaultAccountEnabled(getPreferenceStore(), newRegion)) {
            AwsToolkitCore.getDefault().logInfo("Switching to region-specific default account for region " + newRegion.getId());
            // Use the region-specific default account
            setCurrentAccountId(
                    getPreferenceStore().getString(
                            PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(newRegion)));
        } else {
            AwsToolkitCore.getDefault().logInfo("Switching to global default account");
            // Use the global default account
            setCurrentAccountId(
                    getPreferenceStore().getString(
                            PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT));
        }
    }

    /**
     * Returns a map of all currently registered accounts for the current
     * default region. If the region does not have any default account setting
     * (or it is disabled), then this method returns all accounts registered as
     * global default.
     */
    public Map<String, String> getAccounts() {
        return getAccounts(RegionUtils.getCurrentRegion());
    }

    /**
     * Returns a map of all currently registered accounts for the given region.
     * Account names are keyed by their ID, which can then be fetched with
     * {@link AwsToolkitCore#getAccountInfo(String)}
     */
    public Map<String, String> getAccounts(Region region) {
        if (AwsAccountPreferencePage.isRegionDefaultAccountEnabled(getPreferenceStore(), region)) {
            return AwsAccountPreferencePage.getRegionalAccounts(getPreferenceStore(), region, false);
        } else {
            return AwsAccountPreferencePage.getGlobalAccounts(getPreferenceStore());
        }
        
    }

    /**
     * Registers a listener to receive notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addAccountInfoChangeListener(PreferencePropertyChangeListener listener) {
        accountInfoMonitor.addChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeAccountInfoChangeListener(PreferencePropertyChangeListener listener) {
        accountInfoMonitor.removeChangeListener(listener);
    }
    
    /**
     * Registers a listener to receive notifications when global/regional
     * default accounts are changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addDefaultAccountChangeListener(PreferencePropertyChangeListener listener) {
        defaultAccountMonitor.addChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when global/regional
     * default accounts are changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeDefaultAccountChangeListener(PreferencePropertyChangeListener listener) {
        defaultAccountMonitor.removeChangeListener(listener);
    }

    private IPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }
}
