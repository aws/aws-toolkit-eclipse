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
package com.amazonaws.eclipse.core.ui.preferences.accounts;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.AccountInfoImpl;
import com.amazonaws.eclipse.core.accounts.AccountInfoProvider;
import com.amazonaws.eclipse.core.accounts.preferences.PluginPreferenceStoreAccountOptionalConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesFactory;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.util.StringUtils;

/**
 * A utility class responsible for merging the legacy account configurations
 * into the credentials file.
 */
public class LegacyPreferenceStoreAccountMerger {

    private static final IPreferenceStore prefStore = AwsToolkitCore.getDefault().getPreferenceStore();

    private static boolean isEmpty(String s) {
        return (s == null || s.trim().length() == 0);
    }

    /**
     * NOTE: This method is safe to be invoked in non-UI thread.
     */
    public static void mergeLegacyAccountsIntoCredentialsFile() {
        final AccountInfoProvider provider = AwsToolkitCore.getDefault()
                .getAccountManager().getAccountInfoProvider();
        final Map<String, AccountInfo> legacyAccounts = provider.getAllLegacyPreferenceStoreAccontInfo();

        // If there are no user created legacy accounts, then exit
        // early since there's nothing to merge.
        if (!hasValidLegacyAccounts(legacyAccounts)) {
            return;
        }

        final File credentialsFile = new File(
                prefStore
                        .getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION));

        if ( !credentialsFile.exists() ) {
            /*
             * (1): The credentials file doesn't exist yet.
             *      Silently write the legacy accounts into the credentials file
             */

            saveAccountsIntoCredentialsFile(credentialsFile, legacyAccounts.values(), null);
            clearAllLegacyAccountsInPreferenceStore();

            AwsToolkitCore.getDefault().logInfo(String.format(
                    "%d legacy accounts added to the credentials file (%s).",
                    legacyAccounts.size(), credentialsFile.getAbsolutePath()));
        } else {
            provider.refreshProfileAccountInfo(false, false);
            final Map<String, AccountInfo> profileAccounts = provider.getAllProfileAccountInfo();

            if (profileAccounts.isEmpty()) {
                /*
                 *  (2) Failed to load from the existing credentials file.
                 *      Ask the user whether he/she wants to override the existing
                 *      file and dump the legacy accounts into it.
                 */
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        String MESSAGE = "The following legacy account configurations are detected in the system. " +
                                "The AWS Toolkit now uses the credentials file to persist the credential configurations. " +
                                "We cannot automatically merge the following accounts into your credentials file, " +
                                "since the file already exists and is in invalid format. " +
                                "Do you want to recreate the file using these account configurations?";
                        MergeLegacyAccountsConfirmationDialog dialog = new MergeLegacyAccountsConfirmationDialog(
                                null, MESSAGE, new String[] { "Yes", "No" }, 0,
                                legacyAccounts.values(), null, null);
                        int result = dialog.open();

                        if (result == 0) {
                            AwsToolkitCore.getDefault().logInfo("Deleting the credentials file before dumping the legacy accounts.");
                            credentialsFile.delete();
                            saveAccountsIntoCredentialsFile(credentialsFile, legacyAccounts.values(), null);
                            clearAllLegacyAccountsInPreferenceStore();
                        }
                    }

                });

            } else {
                /*
                 *  (3) Profile accounts successfully loaded.
                 *      Ask the user whether he/she wants to merge the legacy
                 *      accounts into the credentials file
                 */
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        String MESSAGE = "The following legacy account configurations are detected in the system. " +
                                "The AWS Toolkit now uses the credentials file to persist the credential configurations. " +
                                "Do you want to merge these accounts into your existing credentials file?";

                        Map<String, String> profileNameOverrides = checkDuplicateProfileName(
                                legacyAccounts.values(), profileAccounts.values());

                        MergeLegacyAccountsConfirmationDialog dialog = new MergeLegacyAccountsConfirmationDialog(
                                null, MESSAGE, new String[] { "Remove legacy accounts", "Yes", "No" }, 1,
                                legacyAccounts.values(), profileAccounts.values(), profileNameOverrides);
                        int result = dialog.open();

                        if (result == 1) {
                            AwsToolkitCore.getDefault().logInfo("Coverting legacy accounts and merging them into the credentials file.");
                            saveAccountsIntoCredentialsFile(credentialsFile, legacyAccounts.values(), profileNameOverrides);
                            clearAllLegacyAccountsInPreferenceStore();
                        } else if (result == 0) {
                            AwsToolkitCore.getDefault().logInfo("Removing legacy accounts");
                            clearAllLegacyAccountsInPreferenceStore();
                        }
                    }

                });
            }
        }
    }

    /**
     * @return True if customer has valid (the only accounts that are considered
     *         invalid is the default generated empty account) legacy accounts,
     *         false if not
     */
    private static boolean hasValidLegacyAccounts(
            Map<String, AccountInfo> legacyAccounts) {
        if (legacyAccounts.isEmpty()) {
            return false;
        } else if (legacyAccounts.size() == 1) {
            AccountInfo accountInfo = legacyAccounts.values().iterator().next();
            // A legacy default empty account is ignored
            if (accountInfo.getAccountName().equals(PreferenceConstants.DEFAULT_ACCOUNT_NAME)
                    && isEmpty(accountInfo.getAccessKey())
                    && isEmpty(accountInfo.getSecretKey())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Save the legacy preference-store-based accounts in the forms of
     * profile-based accounts. Note that we have to reuse the accountId, so that
     * the new profile accounts are still associated with the existing optional
     * configurations (userid, private key file, etc) stored in the preference
     * store.
     */
    private static void saveAccountsIntoCredentialsFile(File destination,
                                                        Collection<AccountInfo> legacyAccounts,
                                                        Map<String, String> profileNameOverrides) {
        final List<String> legacyAccountIds = new LinkedList<>();

        for (AccountInfo legacyAccount : legacyAccounts) {
            String legacyAccountName = legacyAccount.getAccountName();
            String legacyAccountId   = legacyAccount.getInternalAccountId();
            legacyAccountIds.add(legacyAccountId);

            String newProfileName = legacyAccountName;
            if (profileNameOverrides != null
                    && profileNameOverrides.get(legacyAccountName) != null) {
                newProfileName = profileNameOverrides.get(legacyAccountName);
            }

            // Construct a new profile-based AccountInfo object, reusing the accountId
            AccountInfo newProfileAccount = new AccountInfoImpl(
                    legacyAccountId,
                    new SdkProfilesCredentialsConfiguration(
                            prefStore,
                            legacyAccountId,
                            SdkProfilesFactory.newEmptyBasicProfile("")),
                    new PluginPreferenceStoreAccountOptionalConfiguration(
                            prefStore,
                            newProfileName));

            //  Explicitly use the setters so that the AccountInfo object will be marked as dirty
            newProfileAccount.setAccountName(newProfileName);
            newProfileAccount.setAccessKey(legacyAccount.getAccessKey());
            newProfileAccount.setSecretKey(legacyAccount.getSecretKey());
            newProfileAccount.setUserId(legacyAccount.getUserId());
            newProfileAccount.setEc2PrivateKeyFile(legacyAccount.getEc2PrivateKeyFile());
            newProfileAccount.setEc2CertificateFile(legacyAccount.getEc2CertificateFile());

            newProfileAccount.save();
        }

        // Then append the accountId to the "credentialProfileAccountIds" preference value

        String[] existingProfileAccountIds = prefStore.getString(
                PreferenceConstants.P_CREDENTIAL_PROFILE_ACCOUNT_IDS).split(
                PreferenceConstants.ACCOUNT_ID_SEPARATOR_REGEX);

        List<String> newProfileAccountIds = new LinkedList<>(Arrays.asList(existingProfileAccountIds));
        newProfileAccountIds.addAll(legacyAccountIds);

        String newProfileAccountIdsString = StringUtils.join(PreferenceConstants.ACCOUNT_ID_SEPARATOR,
                newProfileAccountIds.toArray(new String[newProfileAccountIds.size()]));
        prefStore.setValue(
                PreferenceConstants.P_CREDENTIAL_PROFILE_ACCOUNT_IDS, newProfileAccountIdsString);
    }

    @SuppressWarnings("deprecation")
    private static void clearAllLegacyAccountsInPreferenceStore() {
        prefStore.setToDefault(PreferenceConstants.P_ACCOUNT_IDS);

        for (Region region : RegionUtils.getRegions()) {
            prefStore.setToDefault(PreferenceConstants.P_ACCOUNT_IDS(region));
        }
    }

    /**
     * Checks duplicate names between the existing profile accounts and the
     * legacy accounts that are to be merged.
     *
     * @return A map from the duplicated legacy account name to the generated
     *         new profile name.
     */
    private static Map<String, String> checkDuplicateProfileName(Collection<AccountInfo> legacyAccounts, Collection<AccountInfo> existingProfileAccounts) {
        Set<String> exisitingProfileNames = new HashSet<>();
        for (AccountInfo existingProfileAccount : existingProfileAccounts) {
            exisitingProfileNames.add(existingProfileAccount.getAccountName());
        }

        Map<String, String> profileNameOverrides = new LinkedHashMap<>();
        for (AccountInfo legacyAccount : legacyAccounts) {
            if (exisitingProfileNames.contains(legacyAccount.getAccountName())) {
                String newProfileName = generateNewProfileName(exisitingProfileNames, legacyAccount.getAccountName());
                profileNameOverrides.put(legacyAccount.getAccountName(), newProfileName);
            }
        }

        return profileNameOverrides;
    }

    /**
     * Returns a new profile name that is not duplicate with the existing ones.
     */
    private static String generateNewProfileName(Set<String> existingProfileNames, String profileName) {
        final String profileNameBase = profileName;
        int suffix = 1;

        while (existingProfileNames.contains(profileName)) {
            profileName = String.format("%s(%d)", profileNameBase, suffix++);
        }

        return profileName;
    }

    private static class MergeLegacyAccountsConfirmationDialog extends MessageDialog {

        private final Collection<AccountInfo> legacyAccounts;
        private final Collection<AccountInfo> existingProfileAccounts;
        private final Map<String, String> profileNameOverrides;
        private final boolean showExistingProfiles;

        public MergeLegacyAccountsConfirmationDialog(
                Shell parentShell,
                String message,
                String[] buttonLabels,
                int defaultButtonIndex,
                Collection<AccountInfo> legacyAccounts,
                Collection<AccountInfo> existingProfileAccounts,
                Map<String, String> profileNameOverrides) {
            super(parentShell,
                  "Legacy Account Configurations",
                  AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                  message,
                  MessageDialog.QUESTION,
                  buttonLabels,
                  defaultButtonIndex);

            this.legacyAccounts          = legacyAccounts;
            this.existingProfileAccounts = existingProfileAccounts;
            this.profileNameOverrides    = profileNameOverrides;
            showExistingProfiles         = existingProfileAccounts != null;
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            final Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = showExistingProfiles ? 2 : 1;
            composite.setLayout(gridLayout);

            createLegacyAccountsTable(composite);

            if (showExistingProfiles) {
                createProfileAccountsTable(composite);
            }

            composite.setFocus();
            return composite;
        }

        private void createLegacyAccountsTable(Composite parent) {
            final Table table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            table.setLinesVisible(true);
            table.setHeaderVisible(true);
            GridData tableGridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
            table.setLayoutData(tableGridData);

            TableColumn col = new TableColumn(table, SWT.NONE);
            col.setText("Legacy Accounts");
            col.setWidth(75);

            for (AccountInfo legacyAccount : legacyAccounts) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(0, legacyAccount.getAccountName());
            }

            col.pack();
        }

        private void createProfileAccountsTable(Composite parent) {
            final Table table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            table.setLinesVisible(true);
            table.setHeaderVisible(true);
            GridData tableGridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
            table.setLayoutData(tableGridData);

            TableColumn col = new TableColumn(table, SWT.NONE);
            col.setText("Profile Accounts");
            col.setWidth(75);

            for (AccountInfo existingProfileAccount : existingProfileAccounts) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(0, existingProfileAccount.getAccountName());
            }

            for (AccountInfo legacyAccount : legacyAccounts) {
                TableItem item = new TableItem(table, SWT.NONE);

                String accountName = legacyAccount.getAccountName();
                String text = profileNameOverrides == null
                        || profileNameOverrides.get(accountName) == null ?
                                accountName
                                :
                                profileNameOverrides.get(accountName);

                item.setText(0, text);
                item.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
            }

            col.pack();
        }
    }
}
