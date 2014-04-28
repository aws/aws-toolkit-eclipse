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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.util.StringUtils;

/**
 * Preference page for basic AWS account information.
 */
public class AwsAccountPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    public static final String ID = "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage";

    private TabFolder accountsTabFolder;

    private IntegerFieldEditor connectionTimeout;
    private IntegerFieldEditor socketTimeout;

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
        boolean configured = getRegionsWithDefaultAccounts(preferenceStore).contains(region.getId());
        // getBoolean(...) method returns false when the property is not specified.
        // We use isDefault(...) instead so that only an explicit "false" indicates the region accounts are disabled.
        boolean enabled = preferenceStore.isDefault(PreferenceConstants.P_REGION_DEFAULT_ACCOUNT_ENABLED(region));
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
     * For debug purpose only.
     * Print out the property values of all the region-related preferences.
     */
    @SuppressWarnings("serial")
    public static void printPrefs(final IPreferenceStore preferenceStore) {
        List<String> prefKeys = new LinkedList<String>() {{
            add(PreferenceConstants.P_REGIONS_WITH_DEFAULT_ACCOUNTS);
            for (Region region : RegionUtils.getRegions()) {
                addAll(getAllRelatedPreferenceKeys(preferenceStore, region));
            }
        }};
        for (String key : prefKeys) {
            System.out.println(key + " = " + preferenceStore.getString(key));
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
     * Returns all the registered global default accounts. Returns an empty map
     * if none is registered yet.
     */
    public static Map<String, String> getGlobalAccounts(IPreferenceStore preferenceStore) {
        return getRegionalAccounts(preferenceStore, null, false);
    }

    /**
     * Returns all account names registered to the given region. If none are
     * registered yet, it will optionally bootstrap the default setting..
     *
     * @param preferenceStore
     *            The preference store to use when looking up the account names.
     * @param region
     *            Null indicates the global account
     * @param bootstrap
     *            If true, this method will bootstrap the default configuration
     *            if no accounts are registered.
     * @return A map of account identifier to customer-assigned names. The
     *         identifiers are the primary, immutable key used to access the
     *         account.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getRegionalAccounts(IPreferenceStore preferenceStore, Region region, boolean bootstrap) {
        String p_regionalAccountIds = PreferenceConstants.P_ACCOUNT_IDS(region);
        String p_regionalCurrentAccount = PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region);
        String regionalDefaultAccoutNameB64 = PreferenceConstants.DEFAULT_ACCOUNT_NAME_BASE_64(region);

        String accountIdsString = preferenceStore.getString(p_regionalAccountIds);

        // bootstrapping
        if ( accountIdsString == null || accountIdsString.length() == 0 ) {
            if ( !bootstrap ) {
                return Collections.EMPTY_MAP;
            }
            String id = UUID.randomUUID().toString();
            preferenceStore.putValue(p_regionalCurrentAccount, id);
            preferenceStore.putValue(p_regionalAccountIds, id);
            preferenceStore.putValue(id + ":" + PreferenceConstants.P_ACCOUNT_NAME, regionalDefaultAccoutNameB64);
        }

        String[] accountIds = preferenceStore.getString(p_regionalAccountIds)
                .split(PreferenceConstants.ACCOUNT_ID_SEPARATOR_REGEX);
        Map<String, String> names = new HashMap<String, String>();
        for ( String id : accountIds ) {
            if (id.length() > 0) {
                String preferenceName = id + ":" + PreferenceConstants.P_ACCOUNT_NAME;
                names.put(id, ObfuscatingStringFieldEditor.decodeString(preferenceStore.getString(preferenceName)));
            }
        }

        return names;
    }

    /**
     * Returns all the preference keys related to the given region.
     */
    @SuppressWarnings("serial")
    public static Set<String> getAllRelatedPreferenceKeys(final IPreferenceStore preferenceStore, final Region region) {
        Set<String> allPrefKeys = new HashSet<String>() {{
            add(PreferenceConstants.P_ACCOUNT_IDS(region));
            add(PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region));
            add(PreferenceConstants.P_REGION_DEFAULT_ACCOUNT_ENABLED(region));

            // Information of each account for the region
            for (String accountId : getRegionalAccounts(preferenceStore, region, false).keySet()) {
                add(accountId + ":" + PreferenceConstants.P_ACCOUNT_NAME);
                add(accountId + ":" + PreferenceConstants.P_ACCESS_KEY);
                add(accountId + ":" + PreferenceConstants.P_SECRET_KEY);
                add(accountId + ":" + PreferenceConstants.P_USER_ID);
                add(accountId + ":" + PreferenceConstants.P_CERTIFICATE_FILE);
                add(accountId + ":" + PreferenceConstants.P_PRIVATE_KEY_FILE);
            }
        }};
        return Collections.unmodifiableSet(allPrefKeys);
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
        composite.setLayoutData(gridData);

        // Accounts section
        createAccountsSectionGroup(composite);

        // Timeouts section
        createTimeoutSectionGroup(composite);

        // The weblinks at the bottom part of the page
        WebLinkListener webLinkListener = new WebLinkListener();
        String javaForumLinkText = "Get help or provide feedback on the " + "<a href=\""
                + AwsUrls.JAVA_DEVELOPMENT_FORUM_URL + "\">AWS Java Development forum</a>. ";
        AwsToolkitPreferencePage.newLink(webLinkListener, javaForumLinkText, composite);

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
        List<String> regionIds = new LinkedList<String>();
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
        // Because of the somewhat "unorthodox" way we have implemented the
        // field editors, there is some flickering here if we don't lock the
        // display before this update.
        this.getControl().setRedraw(false);

        List<String> configuredRegions = getConfiguredRegions();
        // Update P_REGIONS_WITH_DEFAULT_ACCOUNTS preference property
        markRegionsWithDefaultAccount(getPreferenceStore(), configuredRegions);

        // Clear all related preference properties of un-configured regions
        for (Region region : RegionUtils.getRegions()) {
            if ( !configuredRegions.contains(region.getId()) ) {
                for (String prefKey : getAllRelatedPreferenceKeys(getPreferenceStore(), region)) {
                    getPreferenceStore().setToDefault(prefKey);
                }
            }
        }

        // Call doStore on each tab.
        if (accountsTabFolder != null) {
            for (TabItem tab : accountsTabFolder.getItems()) {
                if (tab instanceof AwsAccountPreferencePageTab) {
                    ((AwsAccountPreferencePageTab) tab).doStore();
                }
            }
        }

        if (connectionTimeout != null) {
            connectionTimeout.store();
        }
        if (socketTimeout != null) {
            socketTimeout.store();
        }

        this.getControl().setRedraw(true);

        return super.performOk();
    }

    /**
     * Check for duplicate account names accross all region account tabs.
     */
    public void checkDuplicateAccountName() {
        Set<String> accountNames = new HashSet<String>();

        for (TabItem tab : accountsTabFolder.getItems()) {
            if (tab instanceof AwsAccountPreferencePageTab) {
                String[] names = ((AwsAccountPreferencePageTab) tab).getAccountNames();
                for ( String accountName : names ) {
                    if ( !accountNames.add(accountName) ) {
                        setValid(false);
                        setErrorMessage("Duplicate account name defined");
                        return;
                    }
                }
            }
        }
        setValid(true);
        setErrorMessage(null);
    }

    /*
     * Private Interface
     */

    private Group createAccountsSectionGroup(final Composite parent) {
        Group accountsSectionGroup = new Group(parent, SWT.NONE);
        accountsSectionGroup.setText("AWS Accounts:");
        accountsSectionGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        accountsSectionGroup.setLayout(new GridLayout());

        // A TabFolder containing the following tabs:
        // Global Account | region-1 | region-2 | ... | + Configure Regional Account
        final TabFolder tabFolder = new TabFolder(accountsSectionGroup, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder.pack();

        int tabIndex = 0;

        // Global default accounts
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

        return accountsSectionGroup;
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
