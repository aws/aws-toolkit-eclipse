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
package com.amazonaws.eclipse.core.ui.preferences;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.accounts.AwsPluginAccountManager;
import com.amazonaws.eclipse.core.diagnostic.utils.EmailMessageLauncher;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.EmailLinkListener;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.ui.overview.Toolkit;
import com.amazonaws.util.StringUtils;

/**
 * Preference page for basic AWS account information.
 */
public class AwsAccountPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    public static final String ID = "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage";

    private static final int PREFERRED_PAGE_WIDTH = 800;

    private TabFolder accountsTabFolder;

    /**
     * Map of all the account info, keyed by account identifier. This map
     * instance is shared by all the tabs, so that they have the synchronized
     * views over the configured accounts. This map is updated in-memory
     * whenever any account is added/removed/modified, and it will only be saved
     * in the external source when {@link #performOk()} is called. Note that the
     * accounts deleted in the preference page will only be removed from this
     * map; the actual deletion in the external source is handled separately.
     */
    private final LinkedHashMap<String, AccountInfo> accountInfoByIdentifier = new LinkedHashMap<>();

    /**
     * All the accounts that are to be deleted.
     */
    private final Set<AccountInfo> accountInfoToBeDeleted = new HashSet<>();

    private FileFieldEditor credentailsFileLocation;
    private boolean credentailsFileLocationChanged = false;

    private BooleanFieldEditor alwaysReloadWhenCredFileChanged;

    private IntegerFieldEditor connectionTimeout;
    private IntegerFieldEditor socketTimeout;

    private Font italicFont;

    /**
     * Creates the preference page and connects it to the plugin's preference
     * store.
     */
    public AwsAccountPreferencePage() {
        super("AWS Toolkit Preferences");

        setPreferenceStore(AwsToolkitCore.getDefault().getPreferenceStore());
        setDescription("AWS Toolkit Preferences");
    }

    /*
     * Public static methods for retrieving/updating account configurations
     * from/to the preference store
     */

    /**
     * Returns whether the given region is configured with region-specific
     * default accounts and that the configuration is not disabled.
     */
    public static boolean isRegionDefaultAccountEnabled(IPreferenceStore preferenceStore, Region region) {
        boolean configured = getRegionsWithDefaultAccounts(preferenceStore)
                .contains(region.getId());
        // getBoolean(...) method returns false when the property is not specified.
        // We use isDefault(...) instead so that only an explicit "false"
        // indicates the region accounts are disabled.
        boolean enabled = preferenceStore.isDefault(
                PreferenceConstants.P_REGION_DEFAULT_ACCOUNT_ENABLED(region));
        return configured && enabled;
    }

    /**
     * Returns the default account id associated with the given region. If the
     * region is not configured with default accounts, then this method returns
     * the global account id.
     */
    public static String getDefaultAccountByRegion(IPreferenceStore preferenceStore, Region region) {
        if (isRegionDefaultAccountEnabled(preferenceStore, region)) {
            return preferenceStore.getString(PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region));
        } else {
            return preferenceStore.getString(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT);
        }
    }

    /**
     * Returns list of region ids that are configured with region-specific
     * default accounts, no matter enabled or not.
     */
    public static List<String> getRegionsWithDefaultAccounts(IPreferenceStore preferenceStore) {
        String regionIdsString = preferenceStore.getString(PreferenceConstants.P_REGIONS_WITH_DEFAULT_ACCOUNTS);
        if (regionIdsString == null || regionIdsString.isEmpty()) {
            return Collections.emptyList();
        }
        String[] regionIds = regionIdsString.split(PreferenceConstants.REGION_ID_SEPARATOR_REGEX);
        return Arrays.asList(regionIds);
    }

    /**
     * Utility function that recursively enable/disable all the children
     * controls contained in the given Composite.
     */
    static void setEnabledOnAllChildern(Composite composite,
            boolean enabled) {
        if (composite == null) return;

        for (Control child : composite.getChildren()) {
            if (child instanceof Composite) {
                setEnabledOnAllChildern((Composite) child, enabled);
            }
            child.setEnabled(enabled);
        }
    }

    /*
     * PreferencePage Interface
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {}

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
     * .swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = PREFERRED_PAGE_WIDTH;
        composite.setLayoutData(gridData);

        // Accounts section
        createAccountsSectionGroup(composite);

        // Credentials file location section
        createCredentialsFileGroup(composite);

        // Timeouts section
        createTimeoutSectionGroup(composite);

        // The weblinks at the bottom part of the page
        createFeedbackSection(composite);

        parent.pack();
        return composite;
    }

    /**
     * This method persists the value of P_REGIONS_WITH_DEFAULT_ACCOUNTS property,
     * according to the current configuration of this preference page.
     */
    private void markRegionsWithDefaultAccount(IPreferenceStore preferenceStore, List<String> regionIds) {
        String regionIdsString = StringUtils.join(PreferenceConstants.REGION_ID_SEPARATOR,
                regionIds.toArray(new String[regionIds.size()]));
        preferenceStore.setValue(PreferenceConstants.P_REGIONS_WITH_DEFAULT_ACCOUNTS,
                                 regionIdsString);
    }

    /**
     * Returns the ids of all the regions that are configured with an account tab
     */
    private List<String> getConfiguredRegions() {
        List<String> regionIds = new LinkedList<>();
        for (TabItem tab : accountsTabFolder.getItems()) {
            if (tab instanceof AwsAccountPreferencePageTab) {
                AwsAccountPreferencePageTab accountTab = (AwsAccountPreferencePageTab) tab;
                if ( !accountTab.isGlobalAccoutTab() ) {
                    regionIds.add(accountTab.getRegion().getId());
                }
            }
        }
        return Collections.unmodifiableList(regionIds);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        // Load default preference values for the current account tab.
        if (accountsTabFolder != null && accountsTabFolder.getSelectionIndex() != -1) {
            TabItem tab = accountsTabFolder.getItem(accountsTabFolder.getSelectionIndex());
            if (tab instanceof AwsAccountPreferencePageTab) {
                ((AwsAccountPreferencePageTab) tab).loadDefault();
            }
        }

        if (credentailsFileLocation != null) {
            credentailsFileLocation.loadDefault();
        }
        if (alwaysReloadWhenCredFileChanged != null) {
            alwaysReloadWhenCredFileChanged.loadDefault();
        }

        if (connectionTimeout != null) {
            connectionTimeout.loadDefault();
        }
        if (socketTimeout != null) {
            socketTimeout.loadDefault();
        }

        super.performDefaults();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {

        try {
            // Don't support changing credentials file location and editing the
            // account infomration at the same time.
            if ( !credentailsFileLocationChanged && accountsTabFolder != null ) {
                /* Save the AccountInfo instances to the external source */
                saveAccounts();


                /* Persist the metadata in the preference store */

                // credentialProfileAccountIds=accoutId1|accountId2
                AwsToolkitCore.getDefault()
                    .getAccountManager().getAccountInfoProvider()
                        .updateProfileAccountMetadataInPreferenceStore(
                                accountInfoByIdentifier.values());

                // regionsWithDefaultAccounts=region1|region2
                List<String> configuredRegions = getConfiguredRegions();
                markRegionsWithDefaultAccount(getPreferenceStore(), configuredRegions);


                /* Call doStore on each tab. */
                for (TabItem tab : accountsTabFolder.getItems()) {
                    if (tab instanceof AwsAccountPreferencePageTab) {
                        ((AwsAccountPreferencePageTab) tab).doStore();
                    }
                }
            }

            if (credentailsFileLocation != null) {
                credentailsFileLocation.store();
            }
            if (alwaysReloadWhenCredFileChanged != null) {
                alwaysReloadWhenCredFileChanged.store();
            }

            AwsToolkitCore.getDefault().getAccountManager().reloadAccountInfo();

            if (connectionTimeout != null) {
                connectionTimeout.store();
            }
            if (socketTimeout != null) {
                socketTimeout.store();
            }

            return super.performOk();

        } catch (Exception e) {
            AwsToolkitCore.getDefault().reportException("Internal error when saving account preference configurations.", e);
            return false;
        }

    }

    /**
     * Check for duplicate account names.
     */
    public void updatePageValidationOfAllTabs() {
        for (TabItem tab : accountsTabFolder.getItems()) {
            if (tab instanceof AwsAccountPreferencePageTab) {
                AwsAccountPreferencePageTab accountTab = (AwsAccountPreferencePageTab)tab;

                // Early termination if any of the tab is invalid
                if (accountTab.updatePageValidation()) {
                    return;
                }
            }
        }
        setValid(true);
        setErrorMessage(null);
    }

    @Override
    public void dispose() {
        if (italicFont != null)
            italicFont.dispose();
        super.dispose();
    }

    List<AwsAccountPreferencePageTab> getAllAccountPreferencePageTabs() {
        List<AwsAccountPreferencePageTab> tabs = new LinkedList<>();
        for (TabItem tab : accountsTabFolder.getItems()) {
            if (tab instanceof AwsAccountPreferencePageTab) {
                tabs.add((AwsAccountPreferencePageTab)tab);
            }
        }
        return tabs;
    }

    /**
     * Package-private method that returns the map of all the configured
     * accounts.
     */
    LinkedHashMap<String, AccountInfo> getAccountInfoByIdentifier() {
        return accountInfoByIdentifier;
    }

    /**
     * Package-private method that returns the set of all the accounts that are
     * to be deleted.
     */
    Set<AccountInfo> getAccountInfoToBeDeleted() {
        return accountInfoToBeDeleted;
    }

    void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    /*
     * Private Interface
     */

    /**
     * Load all the configured accounts and clear accountInfoToBeDeleted.
     */
    private void initAccountInfo() {
        AwsPluginAccountManager accountManager = AwsToolkitCore.getDefault().getAccountManager();

        accountInfoByIdentifier.clear();
        accountInfoByIdentifier.putAll(accountManager.getAllAccountInfo());

        accountInfoToBeDeleted.clear();
    }

    private void createAccountsSectionGroup(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        composite.setLayout(new GridLayout());

        initAccountInfo();

        // If empty, simply add an error label
        if (accountInfoByIdentifier.isEmpty()) {
            new Label(composite, SWT.READ_ONLY)
            .setText(String
                    .format("Failed to load credential profiles from (%s).%n"
                            + "Please check that your credentials file is in the correct format.",
                            getPreferenceStore()
                                    .getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)));
            return;
        }

        // A TabFolder containing the following tabs:
        // Global Account | region-1 | region-2 | ... | + Configure Regional Account
        final TabFolder tabFolder = new TabFolder(composite, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder.pack();

        int tabIndex = 0;

        // Global default accounts
        @SuppressWarnings("unused")
        final AwsAccountPreferencePageTab globalAccountTab = new AwsAccountPreferencePageTab(
                tabFolder, tabIndex, this, null);

        // Region default accounts
        for (String regionId : getRegionsWithDefaultAccounts(getPreferenceStore())) {
            Region configuredRegion = RegionUtils.getRegion(regionId);
            if (configuredRegion != null) {
                new AwsAccountPreferencePageTab(
                        tabFolder, ++tabIndex, this, configuredRegion);
            }
        }

        // The fake tab for "Add default accounts for a region"
        final TabItem newRegionalAccountConfigTab = new TabItem(tabFolder, SWT.NONE);
        newRegionalAccountConfigTab.setToolTipText("Configure Regional Account");
        newRegionalAccountConfigTab.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));

        tabFolder.addSelectionListener(new SelectionAdapter() {
            int lastSelection = 0;
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item == newRegionalAccountConfigTab) {
                    // Suppress selection on the fake tab
                    tabFolder.setSelection(lastSelection);

                    // Prompt up the region selection dialog
                    RegionSelectionDialog dialog = new RegionSelectionDialog();
                    int regionSelected = dialog.open();
                    if (regionSelected == 1) {
                        Region selectedRegion = dialog.getSelectedRegion();

                        // Search for the selectedRegion from the existing tabs
                        for (int ind = 0; ind < tabFolder.getItemCount(); ind++) {
                            TabItem tab = tabFolder.getItem(ind);
                            if (tab instanceof AwsAccountPreferencePageTab) {
                                Region tabRegion = ((AwsAccountPreferencePageTab) tab).getRegion();
                                if (tabRegion != null && tabRegion.getId().equals(selectedRegion.getId())) {
                                    tabFolder.setSelection(ind);
                                    return;
                                }
                            }
                        }

                        // Create a new tab for the selected region if it's not found
                        @SuppressWarnings("unused")
                        AwsAccountPreferencePageTab newRegionAccountTab = new AwsAccountPreferencePageTab(
                                                                                tabFolder,
                                                                                tabFolder.getItemCount() - 1, // before the "new regional account" tab
                                                                                AwsAccountPreferencePage.this,
                                                                                selectedRegion);
                        tabFolder.setSelection(tabFolder.getItemCount() - 2); // Select the newly created tab
                    }
                } else {
                    // Keep track of the latest selected tab
                    lastSelection = tabFolder.getSelectionIndex();
                }
            }
        });

        this.accountsTabFolder = tabFolder;
    }

    /**
     * We currently don't support changing the credentials file location and
     * editing the account information at the same time.
     */
    private Group createCredentialsFileGroup(final Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Credentials file:");
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        GridLayout groupLayout = new GridLayout();
        groupLayout.marginWidth = 20;
        group.setLayout(groupLayout);

        final Label credentialsFileLocationLabel = new Label(group, SWT.WRAP);
        credentialsFileLocationLabel
                .setText("The location of the credentials file where " +
                        "all your configured profiles will be persisted.");
        setItalicFont(credentialsFileLocationLabel);

        final Composite composite = new Composite(group, SWT.NONE);

        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        composite.setLayoutData(data);

        credentailsFileLocation = new FileFieldEditor(
                PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION,
                "Credentials file:",
                true,
                composite);
        credentailsFileLocation.setPage(this);
        credentailsFileLocation.setPreferenceStore(getPreferenceStore());
        credentailsFileLocation.load();

        if (accountsTabFolder != null) {

            credentailsFileLocation.getTextControl(composite).addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(ModifyEvent e) {
                    String modifiedLocation = credentailsFileLocation
                            .getTextControl(composite).getText();

                    // We only allow account info editing if the
                    // credentials file location is unchanged.
                    if (modifiedLocation.equals(
                            getPreferenceStore().getString(
                                    PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION))) {
                        setEnabledOnAllChildern(accountsTabFolder, true);
                        updatePageValidationOfAllTabs();
                    } else {
                        credentailsFileLocationChanged = true;
                        setEnabledOnAllChildern(accountsTabFolder, false);

                        File newCredFile = new File(modifiedLocation);
                        if ( !newCredFile.exists() ) {
                            setValid(false);
                            setErrorMessage("Credentials file does not exist at the specified location.");
                        }
                    }

                }
            });

            for (TabItem tab : accountsTabFolder.getItems()) {
                if (tab instanceof AwsAccountPreferencePageTab) {
                    AwsAccountPreferencePageTab accountTab = (AwsAccountPreferencePageTab)tab;
                    accountTab.addAccountInfoFieldEditorModifyListener(new ModifyListener() {

                        @Override
                        public void modifyText(ModifyEvent arg0) {
                            for (AccountInfo account : accountInfoByIdentifier.values()) {
                                if (account.isDirty()) {
                                    // Disable the credential file location
                                    // editor when the account info section
                                    // is modified.
                                    setEnabledOnAllChildern(composite, false);
                                }
                            }
                        }
                    });
                }
            }
        }

        Composite secondRow = new Composite(composite, SWT.NONE);
        GridData secondRowGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        secondRowGridData.horizontalSpan = 3; // file editor takes 3 columns
        secondRow.setLayoutData(secondRowGridData);

        alwaysReloadWhenCredFileChanged = new BooleanFieldEditor(
                PreferenceConstants.P_ALWAYS_RELOAD_WHEN_CREDNENTIAL_PROFILE_FILE_MODIFIED,
                "Automatically reload accounts when the credentials file is modified in the file system.",
                secondRow);
        alwaysReloadWhenCredFileChanged.setPage(this);
        alwaysReloadWhenCredFileChanged.setPreferenceStore(getPreferenceStore());
        alwaysReloadWhenCredFileChanged.load();

        return group;
    }

    private Group createTimeoutSectionGroup(final Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Timeouts:");
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        group.setLayout(new GridLayout(2, false));

        Composite composite = new Composite(group, SWT.NONE);

        GridData data = new GridData(SWT.FILL, SWT.TOP, false, false);
        composite.setLayoutData(data);

        connectionTimeout = new IntegerFieldEditor(
                PreferenceConstants.P_CONNECTION_TIMEOUT,
                "Connection Timeout (ms)",
                composite);

        connectionTimeout.setPage(this);
        connectionTimeout.setPreferenceStore(getPreferenceStore());
        connectionTimeout.load();
        connectionTimeout.fillIntoGrid(composite, 3);

        socketTimeout = new IntegerFieldEditor(
                PreferenceConstants.P_SOCKET_TIMEOUT,
                "Socket Timeout (ms)",
                composite);

        socketTimeout.setPage(this);
        socketTimeout.setPreferenceStore(getPreferenceStore());
        socketTimeout.load();
        socketTimeout.fillIntoGrid(composite, 3);

        Link networkConnectionLink = new Link(composite, SWT.NULL);
        networkConnectionLink.setText(
                "See <a href=\"org.eclipse.ui.net.NetPreferences\">Network "
                + "connections</a> for more ways to configure how the AWS "
                + "Toolkit connects to the Internet.");

        PreferenceLinkListener preferenceLinkListener = new PreferenceLinkListener();
        networkConnectionLink.addListener(SWT.Selection, preferenceLinkListener);

        return group;
    }

    /**
     * Insert links to the Java dev forum and aws-eclipse-feedback@amazon.com
     */
    private void createFeedbackSection(final Composite composite) {
        WebLinkListener webLinkListener = new WebLinkListener();
        String javaForumLinkText = "Get help or provide feedback on the "
                + Toolkit.createAnchor("AWS Java Development Forum",
                                       AwsUrls.JAVA_DEVELOPMENT_FORUM_URL);
        AwsToolkitPreferencePage.newLink(webLinkListener, javaForumLinkText, composite);

        EmailLinkListener feedbackLinkListener
            = new EmailLinkListener(EmailMessageLauncher.createEmptyFeedbackEmail());
        String feedbackLinkText = "Or directly contact us via "
                + Toolkit.createAnchor(EmailMessageLauncher.AWS_ECLIPSE_FEEDBACK_AT_AMZN,
                                       EmailMessageLauncher.AWS_ECLIPSE_FEEDBACK_AT_AMZN);
        AwsToolkitPreferencePage.newLink(feedbackLinkListener, feedbackLinkText, composite);
    }

    /**
     * Save all the AccountInfo objects, and delete those that are to be
     * deleted.
     */
    private void saveAccounts() {
        // Save all the AccountInfo objects
        // Because of data-binding, all the edits are already reflected inside
        // these AccountInfo objects
        for (AccountInfo account : accountInfoByIdentifier.values()) {
            account.save(); // Save the in-memory changes to the external source
        }

        // Remove the deleted accounts
        for (AccountInfo deletedAccount : accountInfoToBeDeleted) {
            deletedAccount.delete();
        }
        accountInfoToBeDeleted.clear();
    }

    private static class RegionSelectionDialog extends MessageDialog {

        private Region selectedRegion;
        private Font italicFont;

        protected RegionSelectionDialog() {
            super(Display.getDefault().getActiveShell(), "Select a region", AwsToolkitCore.getDefault()
                    .getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                    "Configure the default AWS account for a specific region:" , MessageDialog.NONE, new String[] { "Cancel", "OK" }, 1);
        }

        @Override
        protected Control createCustomArea(final Composite parent) {
            final Combo regionSelector = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
            regionSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            for (Region region : RegionUtils.getRegions()) {
                regionSelector.add(region.getName());
                regionSelector.setData(region.getName(), region);
            }

            final Label regionAccountExplanationLabel = new Label(parent, SWT.WRAP);
            FontData[] fontData = regionAccountExplanationLabel.getFont().getFontData();
            for (FontData fd : fontData) {
                fd.setStyle(SWT.ITALIC);
            }
            italicFont = new Font(Display.getDefault(), fontData);
            regionAccountExplanationLabel.setFont(italicFont);

            regionSelector.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelectedRegion(regionSelector);
                }
            });

            regionSelector.select(0);
            updateSelectedRegion(regionSelector);

            return regionSelector;
        }

        private void updateSelectedRegion(final Combo regionSelector) {
            String regionName = regionSelector.getItem(regionSelector.getSelectionIndex());
            selectedRegion = (Region) regionSelector.getData(regionName);
        }

        @Override
        public boolean close() {
            if (italicFont != null) {
                italicFont.dispose();
            }
            return super.close();
        }

        public Region getSelectedRegion() {
            return selectedRegion;
        }
    }
}
