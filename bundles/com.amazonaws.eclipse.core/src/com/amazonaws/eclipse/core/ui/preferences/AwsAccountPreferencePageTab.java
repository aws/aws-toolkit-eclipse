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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.accounts.AccountInfoImpl;
import com.amazonaws.eclipse.core.accounts.preferences.PluginPreferenceStoreAccountOptionalConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesFactory;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.ui.preferences.accounts.AccountInfoPropertyEditor;
import com.amazonaws.eclipse.core.ui.preferences.accounts.AccountInfoPropertyEditorFactory;
import com.amazonaws.eclipse.core.ui.preferences.accounts.AccountInfoPropertyEditorFactory.AccountInfoFilePropertyEditor;
import com.amazonaws.eclipse.core.ui.preferences.accounts.AccountInfoPropertyEditorFactory.AccountInfoStringPropertyEditor;
import com.amazonaws.eclipse.core.ui.preferences.accounts.AccountInfoPropertyEditorFactory.PropertyType;

/**
 * A tab item contained in the AWS account preference page. Each tab item
 * corresponds to a set of region-specific (or global) accounts.
 */
public class AwsAccountPreferencePageTab extends TabItem {

    /** The preference store persisted by this tab */
    private final IPreferenceStore prefStore;

    /**
     * The preference page to which the validation error message should be
     * reported.
     */
    private final AwsAccountPreferencePage parentPrefPage;

    /**
     * The region associated with this tab. Null if this is the tab for global
     * account
     */
    private final Region region;

    /**
     * The identifier of the current default account for the region represented
     * by this tab
     */
    private String currentRegionAccountId;

    /**
     * @see AwsAccountPreferencePage#getAccountInfoByIdentifier()
     */
    private final LinkedHashMap<String, AccountInfo> accountInfoByIdentifier;

    /**
     * @see AwsAccountPreferencePage#getAccountInfoToBeDeleted()
     */
    private final Set<AccountInfo> accountInfoToBeDeleted;

    /**
     * The DataBindingContext instance shared by all the account info property
     * editors
     */
    private final DataBindingContext dataBindingContext = new DataBindingContext();

    /**
     * Additional listeners to be notified when the account information is modified
     */
    private final List<ModifyListener> accountInfoFieldEditorListeners = new LinkedList<>();

    /** Page controls */
    BooleanFieldEditor enableRegionDefaultAccount;
    private Composite accountInfoSection;
    private ComboViewer accountSelector;
    private Button deleteAccount;
    private Label defaultAccountExplanationLabel;
    private AccountInfoPropertyEditor accountNameFieldEditor;
    private AccountInfoPropertyEditor accessKeyFieldEditor;
    private AccountInfoPropertyEditor secretKeyFieldEditor;
    private AccountInfoPropertyEditor sessionTokenFieldEditor;
    private AccountInfoPropertyEditor userIdFieldEditor;
    private AccountInfoPropertyEditor certificateFieldEditor;
    private AccountInfoPropertyEditor certificatePrivateKeyFieldEditor;

    /**
     * The set of field editors that need to know when the account or its name
     * changes.
     */
    private final Collection<AccountInfoPropertyEditor> accountFieldEditors = new LinkedList<>();

    /** The checkbox controlling how we display the secret key */
    private Button hideSecretKeyCheckbox;

    /** The checkbox controlling whether the credential includes the session token */
    private Button useSessionTokenCheckbox;

    /** The Text control in the secret key field editor */
    private Text secretKeyText;

    public AwsAccountPreferencePageTab(TabFolder tabFolder, int tabIndex,
            AwsAccountPreferencePage parentPrefPage, Region region) {
        this(tabFolder, tabIndex, parentPrefPage, region, AwsToolkitCore
                .getDefault().getPreferenceStore());
    }

    /**
     * Construct a new tab in the given TabFolder at the given index, relating
     * to the given region.
     *
     * @param tabFolder
     *            TabFolder where this tab is inserted.
     * @param tabIndex
     *            The index where this tab should be inserted.
     * @param parentPrefPage
     *            The parent account preference page which contains this tab.
     * @param region
     *            The region which this page tab manages
     * @param prefStore
     *            The preference store that this tab persists with.
     */
    public AwsAccountPreferencePageTab(TabFolder tabFolder, int tabIndex,
            AwsAccountPreferencePage parentPrefPage,
            Region region, IPreferenceStore prefStore) {
        super(tabFolder, SWT.NONE, tabIndex);
        this.parentPrefPage          = parentPrefPage;
        this.accountInfoByIdentifier = parentPrefPage.getAccountInfoByIdentifier();
        this.accountInfoToBeDeleted  = parentPrefPage.getAccountInfoToBeDeleted();
        this.region                  = region;
        this.prefStore               = prefStore;

        if (region == null) {
            setText("Global Configuration");
        } else {
            // Use the region name as the tab title
            setText(region.getName());
        }

        loadCurrentDefaultAccountId();

        writeDefaultPreferenceValues();

        final Composite composite = setUpCompositeLayout();

        // Not strictly necessary, since AwsAccountPreferencePage will never
        // construct this class with an empty map of accounts.
        if (!accountInfoByIdentifier.isEmpty()) {
            addControls(composite);
        } else {
            addCredentialsFileLoadingFailureLabel(composite);
        }

        setControl(composite);
    }

    /*
     * Public interfaces
     */

    public void loadDefault() {
        if (enableRegionDefaultAccount != null) {
            enableRegionDefaultAccount.loadDefault();
        }
    }

    public void doStore() {
        // Store the check box on whether region-default-account is enabled
        if (enableRegionDefaultAccount != null) {
            enableRegionDefaultAccount.store();
        }

        // Save the id of the current default region account
        getPreferenceStore().setValue(getRegionCurrentAccountPrefKey(),
                currentRegionAccountId);

        // Refresh the UI of the account selector
        refreshAccountSelectorUI();
    }

    public Region getRegion() {
        return region;
    }

    public boolean isGlobalAccoutTab() {
        return region == null;
    }

    /**
     * Returns names of all the accounts.
     */
    public List<String> getAccountNames() {
        List<String> accountNames = new LinkedList<>();
        for (AccountInfo account : accountInfoByIdentifier.values()) {
            accountNames.add(account.getAccountName());
        }
        return accountNames;
    }

    /**
     * Check for duplicate account names in this tab.
     *
     * @return True if duplicate account names are found in this tab.
     */
    public boolean checkDuplicateAccountName() {
        Set<String> accountNames = new HashSet<>();
        for (String accountName : getAccountNames()) {
            if (!accountNames.add(accountName)) {
                return true;
            }
        }
        return false;
    }

    synchronized void addAccountInfoFieldEditorModifyListener(ModifyListener listener) {
        accountInfoFieldEditorListeners.add(listener);
    }

    /*
     * Private Interface
     */

    private void loadCurrentDefaultAccountId() {
        currentRegionAccountId = getPreferenceStore().getString(
                getRegionCurrentAccountPrefKey());
    }

    private void writeDefaultPreferenceValues() {
        if (!isGlobalAccoutTab()) {
            getPreferenceStore().setDefault(getRegionAccountEnabledPrefKey(),
                    true);
        }
    }

    private Composite setUpCompositeLayout() {
        Composite composite = new Composite(getParent(), SWT.NONE);
        composite.setLayout(new GridLayout(3, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gridData);
        return composite;
    }

    private void addCredentialsFileLoadingFailureLabel(Composite composite) {
        new Label(composite, SWT.READ_ONLY)
                .setText(String
                        .format("Failed to load credential profiles from (%s).%n"
                                + "Please check that your credentials file is in the correct format.",
                                prefStore
                                        .getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)));
    }

    private void addControls(final Composite composite) {
        // If it's regional tab, add the enable-region-default-account check-box
        // and the remove-this-tab button
        if (!isGlobalAccoutTab()) {
            addEnableRegionDefaultControls(composite);
        }

        // The main account info section
        accountInfoSection = createAccountInfoSection(composite);

        // Set up the change listener on the enable-region-default-account
        // check-box, so that the account info section is grayed out whenever
        // it's unchecked.
        setUpEnableRegionDefaultChangeListener();

        AwsToolkitPreferencePage
                .tweakLayout((GridLayout) composite.getLayout());
    }

    /**
     * Add the enable-region-default-account check-box and the remove-this-tab
     * button
     */
    private void addEnableRegionDefaultControls(Composite parent) {
        enableRegionDefaultAccount = new BooleanFieldEditor(
                getRegionAccountEnabledPrefKey(),
                "Enable region default account for " + region.getName(), parent);
        enableRegionDefaultAccount.setPreferenceStore(getPreferenceStore());

        Button removeTabButton = new Button(parent, SWT.PUSH);
        removeTabButton.setText("Remove");
        removeTabButton
                .setToolTipText("Remove default account configuration for this region.");
        removeTabButton.setImage(AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_REMOVE));
        removeTabButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,
                false, false));
        removeTabButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                MessageDialog confirmRemoveTabDialog = new MessageDialog(
                        Display.getDefault().getActiveShell(),
                        "Remove all accounts for " + region.getName(),
                        AwsToolkitCore.getDefault().getImageRegistry()
                                .get(AwsToolkitCore.IMAGE_AWS_ICON),
                        "Are you sure you want to remove all the configured accounts for "
                                + region.getName() + "?",
                        MessageDialog.CONFIRM, new String[] { "Cancel", "OK" },
                        1);
                if (confirmRemoveTabDialog.open() == 1) {
                    AwsAccountPreferencePageTab.this.dispose();
                }

            }
        });
    }

    /**
     * Creates the whole section of account info
     */
    private Composite createAccountInfoSection(Composite parent) {
        Composite accountInfoSection = new Composite(parent, SWT.NONE);
        accountInfoSection.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 3;
        accountInfoSection.setLayoutData(gridData);
        createAccountSelector(accountInfoSection);

        WebLinkListener webLinkListener = new WebLinkListener();
        Group awsGroup = createAccountDetailSectionGroup(accountInfoSection,
                webLinkListener);
        createOptionalSection(awsGroup, webLinkListener);

        return accountInfoSection;
    }

    /**
     * Creates the account selection section.
     */
    private Composite createAccountSelector(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        composite.setLayout(new GridLayout(4, false));

        new Label(composite, SWT.READ_ONLY).setText("Default Profile: ");

        accountSelector = new ComboViewer(composite, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        accountSelector.getCombo().setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Use a List of AccountInfo objects as the data input for the combo
        // viewer
        accountSelector.setContentProvider(ArrayContentProvider.getInstance());
        accountSelector.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AccountInfo) {
                    AccountInfo account = (AccountInfo) element;
                    if (account.isDirty()) {
                        return "*" + account.getAccountName();
                    } else {
                        return account.getAccountName();
                    }
                }
                return super.getText(element);
            }
        });

        AccountInfo currentRegionAccount = accountInfoByIdentifier
                .get(currentRegionAccountId);
        // In some of the edge-cases, currentRegionAccount could be null.
        // e.g. a specific credential profile account is removed externally, but
        // the data in the preference store is not yet updated.
        if (currentRegionAccount == null) {
            currentRegionAccount = accountInfoByIdentifier.values().iterator()
                    .next();
            currentRegionAccountId = currentRegionAccount
                    .getInternalAccountId();
        }

        final List<AccountInfo> allAccounts = new LinkedList<>(
                accountInfoByIdentifier.values());

        setUpAccountSelectorItems(allAccounts, currentRegionAccount);

        // Add selection listener to the account selector, so that all the
        // account info editors are notified of the newly selected AccountInfo
        // object.
        accountSelector
                .addSelectionChangedListener(new ISelectionChangedListener() {

                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        IStructuredSelection selection = (IStructuredSelection) event
                                .getSelection();
                        Object selectedObject = selection.getFirstElement();

                        if (selectedObject instanceof AccountInfo) {
                            AccountInfo accountInfo = (AccountInfo) selectedObject;
                            accountChanged(accountInfo);
                        }
                    }

                });

        final Button addNewAccount = new Button(composite, SWT.PUSH);
        addNewAccount.setText("Add profile");
        deleteAccount = new Button(composite, SWT.PUSH);
        deleteAccount.setText("Remove profile");
        deleteAccount.setEnabled(allAccounts.size() > 1);

        defaultAccountExplanationLabel = new Label(composite, SWT.WRAP);
        defaultAccountExplanationLabel
                .setText(getDefaultAccountExplanationText());

        parentPrefPage.setItalicFont(defaultAccountExplanationLabel);

        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.horizontalSpan = 4;
        layoutData.widthHint = 200;
        defaultAccountExplanationLabel.setLayoutData(layoutData);

        addNewAccount.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String newAccountId = UUID.randomUUID().toString();
                AccountInfo newAccountInfo = createNewProfileAccountInfo(newAccountId);

                String newAccountName = region == null ? "New Profile" : "New "
                        + region.getName() + " Profile";
                newAccountInfo.setAccountName(newAccountName); // this will mark the AccountInfo object dirty

                accountInfoByIdentifier.put(newAccountId, newAccountInfo);

                setUpAccountSelectorItems(accountInfoByIdentifier.values(),
                        newAccountInfo);

                for (AwsAccountPreferencePageTab tab : parentPrefPage.getAllAccountPreferencePageTabs()) {
                    if (tab != AwsAccountPreferencePageTab.this) {
                        tab.refreshAccountSelectorItems();
                    }
                }

                parentPrefPage.updatePageValidationOfAllTabs();
            }
        });

        deleteAccount.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                accountInfoToBeDeleted.add(accountInfoByIdentifier
                        .get(currentRegionAccountId));
                accountInfoByIdentifier.remove(currentRegionAccountId);

                // If all the accounts are deleted, create a temporary
                // AccountInfo object
                if (accountInfoByIdentifier.isEmpty()) {
                    String newAccountId = UUID.randomUUID().toString();
                    AccountInfo newAccountInfo = createNewProfileAccountInfo(newAccountId);

                    // Account name : default-region-id
                    newAccountInfo
                            .setAccountName(getRegionAccountDefaultName());
                    accountInfoByIdentifier.put(newAccountId, newAccountInfo);
                }

                // Use the first AccountInfo as the next selected account
                AccountInfo nextDefaultAccount = accountInfoByIdentifier
                        .values().iterator().next();

                setUpAccountSelectorItems(accountInfoByIdentifier.values(),
                        nextDefaultAccount);

                for (AwsAccountPreferencePageTab tab : parentPrefPage.getAllAccountPreferencePageTabs()) {
                    if (tab != AwsAccountPreferencePageTab.this) {
                        tab.refreshAccountSelectorItems();
                    }
                }

                parentPrefPage.updatePageValidationOfAllTabs();
            }
        });

        accountSelector.getCombo().addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                if (accountSelector.getCombo().getItemCount() > 1) {
                    deleteAccount.setEnabled(true);
                } else {
                    deleteAccount.setEnabled(false);
                }

            }
        });

        return composite;
    }

    /**
     * Refreshes the UI of the account selector.
     */
    private void refreshAccountSelectorUI() {
        accountSelector.refresh();
    }

    /**
     * Refreshes the input data for the account selector combo and keep the current
     * selection.
     */
    private void refreshAccountSelectorItems() {
        setUpAccountSelectorItems(accountInfoByIdentifier.values(),
                                  accountInfoByIdentifier.get(currentRegionAccountId));
    }

    /**
     * Set the input data for the account selector combo and also set the
     * selection to the specified AccountInfo object.
     */
    private void setUpAccountSelectorItems(Collection<AccountInfo> allAccounts,
            AccountInfo selectedAccount) {
        accountSelector.setInput(allAccounts);

        // If the given account is not found, then select the first element
        if ( !allAccounts.contains(selectedAccount) ) {
            selectedAccount = allAccounts.iterator().next();
        }

        accountSelector.setSelection(new StructuredSelection(selectedAccount),
                true); // visible=true

        // TODO: copied from the existing code, not sure why it's necessary
        accountSelector.getCombo().getParent().getParent().layout();
    }

    /**
     * Creates the widgets for the AWS account information section on this
     * preference page.
     *
     * @param parent
     *            The parent preference page composite.
     * @param webLinkListener
     *            The listener to attach to links.
     */
    private Group createAccountDetailSectionGroup(final Composite parent,
            WebLinkListener webLinkListener) {

        final Group awsAccountGroup = new Group(parent, SWT.NONE);
        GridData gridData1 = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData1.horizontalSpan = 4;
        gridData1.verticalIndent = 10;
        awsAccountGroup.setLayoutData(gridData1);
        awsAccountGroup.setText("Profile Details:");

        if (isGlobalAccoutTab()) {
            String linkText = "<a href=\"" + AwsUrls.SIGN_UP_URL
                    + "\">Sign up for a new AWS account</a> or " + "<a href=\""
                    + AwsUrls.SECURITY_CREDENTIALS_URL
                    + "\">manage your existing AWS security credentials</a>.";
            AwsToolkitPreferencePage.newLink(webLinkListener, linkText,
                    awsAccountGroup);
            AwsToolkitPreferencePage.createSpacer(awsAccountGroup);
        }

        accountNameFieldEditor = newStringFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "accountName", "&Profile Name:", awsAccountGroup);
        accessKeyFieldEditor = newStringFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "accessKey", "&Access Key ID:", awsAccountGroup);

        /*
         * Secret key editor and hide check-box
         */

        secretKeyFieldEditor = newStringFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "secretKey", "&Secret Access Key:", awsAccountGroup);

        // create an empty label in the first column so that the hide secret key
        // checkbox lines up with the other text controls
        new Label(awsAccountGroup, SWT.NONE);
        hideSecretKeyCheckbox = createCheckbox(awsAccountGroup,
                "Show secret access key", false);

        hideSecretKeyCheckbox.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                updateSecretKeyText();
            }
        });
        secretKeyText = secretKeyFieldEditor.getTextControl();
        updateSecretKeyText();

        /*
         * Session token input controls
         */

        sessionTokenFieldEditor = newStringFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "sessionToken", "Session &Token:", awsAccountGroup);

        new Label(awsAccountGroup, SWT.NONE);
        useSessionTokenCheckbox = createCheckbox(awsAccountGroup,
                "Use session token", false);
        useSessionTokenCheckbox.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                boolean useSessionToken = useSessionTokenCheckbox.getSelection();
                AccountInfo currentAccount = accountInfoByIdentifier.get(currentRegionAccountId);
                currentAccount.setUseSessionToken(useSessionToken);
                updateSessionTokenControls();

                parentPrefPage.updatePageValidationOfAllTabs();
            }
        });

        // Update session token controls according to the current account
        updateSessionTokenControls();

        AwsToolkitPreferencePage.tweakLayout((GridLayout) awsAccountGroup
                .getLayout());

        accountFieldEditors.add(accountNameFieldEditor);
        accountFieldEditors.add(accessKeyFieldEditor);
        accountFieldEditors.add(secretKeyFieldEditor);
        accountFieldEditors.add(sessionTokenFieldEditor);

        return awsAccountGroup;
    }

    private Button createCheckbox(Composite parent, String text, boolean defaultSelection) {
        Button checkbox = new Button(parent, SWT.CHECK);
        checkbox.setText(text);
        checkbox.setSelection(defaultSelection);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.verticalIndent = -6;
        gridData.horizontalIndent = 3;
        checkbox.setLayoutData(gridData);
        return checkbox;
    }

    /**
     * Creates the widgets for the optional configuration section on this
     * preference page.
     *
     * @param parent
     *            The parent preference page composite.
     * @param webLinkListener
     *            The listener to attach to links.
     */
    private void createOptionalSection(final Composite parent,
            WebLinkListener webLinkListener) {

        Section expandableComposite = new Section(parent,
                ExpandableComposite.TWISTIE);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = AwsToolkitPreferencePage.LAYOUT_COLUMN_WIDTH;
        expandableComposite.setLayoutData(gd);

        Composite optionalConfigGroup = new Composite(expandableComposite,
                SWT.NONE);
        optionalConfigGroup.setLayout(new GridLayout());
        optionalConfigGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true,
                false));

        expandableComposite.setClient(optionalConfigGroup);
        expandableComposite.setText("Optional configuration:");
        expandableComposite.setExpanded(false);

        expandableComposite.addExpansionListener(new IExpansionListener() {

            @Override
            public void expansionStateChanging(ExpansionEvent e) {
            }

            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                parent.getParent().layout();
            }
        });

        String linkText = "Your AWS account number and X.509 certificate are only needed if you want to bundle EC2 instances from Eclipse.  "
                + "<a href=\""
                + AwsUrls.SECURITY_CREDENTIALS_URL
                + "\">Manage your AWS X.509 certificate</a>.";
        AwsToolkitPreferencePage.newLink(webLinkListener, linkText,
                optionalConfigGroup);

        AwsToolkitPreferencePage.createSpacer(optionalConfigGroup);

        userIdFieldEditor = newStringFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId), "userId",
                "AWS Account &Number:", optionalConfigGroup);
        createFieldExampleLabel(optionalConfigGroup, "ex: 1111-2222-3333");

        certificateFieldEditor = newFileFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "ec2CertificateFile", "&Certificate File:", optionalConfigGroup);
        certificatePrivateKeyFieldEditor = newFileFieldEditor(
                accountInfoByIdentifier.get(currentRegionAccountId),
                "ec2PrivateKeyFile", "&Private Key File:", optionalConfigGroup);

        AwsToolkitPreferencePage.tweakLayout((GridLayout) optionalConfigGroup
                .getLayout());

        accountFieldEditors.add(userIdFieldEditor);
        accountFieldEditors.add(certificateFieldEditor);
        accountFieldEditors.add(certificatePrivateKeyFieldEditor);
    }

    /**
     * Set up the change listener on the enable-region-default-account
     * check-box, so that the account info section is grayed out whenever it's
     * unchecked.
     */
    private void setUpEnableRegionDefaultChangeListener() {
        if (enableRegionDefaultAccount != null) {
            enableRegionDefaultAccount
                    .setPropertyChangeListener(new IPropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent event) {
                            boolean enabled = (Boolean) event.getNewValue();
                            AwsAccountPreferencePageTab.this
                                    .toggleAccountInfoSectionEnabled(enabled);
                        }
                    });

            enableRegionDefaultAccount.load();
            toggleAccountInfoSectionEnabled(enableRegionDefaultAccount
                    .getBooleanValue());
        }
    }

    /**
     * Creates a new label to serve as an example for a field, using the
     * specified text. The label will be displayed with a subtle font. This
     * method assumes that the grid layout for the specified composite contains
     * three columns.
     *
     * @param composite
     *            The parent component for this new widget.
     * @param text
     *            The example text to display in the new label.
     */
    private void createFieldExampleLabel(Composite composite, String text) {
        Label label = new Label(composite, SWT.NONE);
        Font font = label.getFont();

        label = new Label(composite, SWT.NONE);
        label.setText(text);

        FontData[] fontData = font.getFontData();
        if (fontData.length > 0) {
            FontData fd = fontData[0];
            fd.setHeight(10);
            fd.setStyle(SWT.ITALIC);

            label.setFont(new Font(Display.getCurrent(), fd));
        }

        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.verticalAlignment = SWT.TOP;
        gridData.horizontalIndent = 3;
        gridData.verticalIndent = -4;
        label.setLayoutData(gridData);
    }

    /**
     * Invoked whenever the selected account information changes.
     */
    private void accountChanged(AccountInfo accountInfo) {
        currentRegionAccountId = accountInfo.getInternalAccountId();
        for (AccountInfoPropertyEditor editor : accountFieldEditors) {
            editor.accountChanged(accountInfo);
        }
        updateSessionTokenControls();
    }

    private void updateSessionTokenControls() {
        AccountInfo accountInfo = accountInfoByIdentifier.get(currentRegionAccountId);
        useSessionTokenCheckbox.setSelection(accountInfo.isUseSessionToken());
        sessionTokenFieldEditor.getTextControl().setEnabled(accountInfo.isUseSessionToken());
    }

    /**
     * Update or clear the error message and validity of the page.
     *
     * @return True if the page is invalid.
     */
    boolean updatePageValidation() {
        String errorString = validateFieldValues();
        if (errorString != null) {
            parentPrefPage.setValid(false);
            parentPrefPage.setErrorMessage(errorString);
            return true;
        }

        if (checkDuplicateAccountName()) {
            parentPrefPage.setValid(false);
            parentPrefPage.setErrorMessage("Duplicate account name defined");
            return true;
        }

        return false;
    }

    /**
     * Returns an error message if there's a problem with the page's fields, or
     * null if there are no errors.
     */
    private String validateFieldValues() {
        for (AccountInfo accountInfo : accountInfoByIdentifier.values()) {
            if (accountInfo.getAccountName().trim().isEmpty()) {
                return "Account name must not be blank";
            }

            if (accountInfo.isUseSessionToken()
                    && (accountInfo.getSessionToken() == null ||
                        accountInfo.getSessionToken().trim().isEmpty())) {
                return "Session token must not be blank";
            }

            if (invalidFile(accountInfo.getEc2CertificateFile())) {
                return "Certificate file does not exist";
            }

            if (invalidFile(accountInfo.getEc2PrivateKeyFile())) {
                return "Private key file does not exist";
            }
        }

        return null;
    }

    private boolean invalidFile(String certFile) {
        return certFile.trim().length() > 0 && !new File(certFile).exists();
    }

    /**
     * Updates the secret key text according to whether or not the
     * "display secret key in plain text" checkbox is selected or not.
     */
    private void updateSecretKeyText() {
        if (hideSecretKeyCheckbox == null)
            return;
        if (secretKeyText == null)
            return;

        if (hideSecretKeyCheckbox.getSelection()) {
            secretKeyText.setEchoChar('\0');
        } else {
            secretKeyText.setEchoChar('*');
        }
    }

    private AccountInfoPropertyEditor newStringFieldEditor(
            AccountInfo currentAccount, String propertyName, String label,
            Composite parent) {
        AccountInfoPropertyEditor fieldEditor = AccountInfoPropertyEditorFactory
                .getAccountInfoPropertyEditor(currentAccount, propertyName,
                        PropertyType.STRING_PROPERTY, label, parent,
                        dataBindingContext);
        setUpFieldEditor(fieldEditor, parent);
        return fieldEditor;
    }

    private AccountInfoPropertyEditor newFileFieldEditor(
            AccountInfo currentAccount, String propertyName, String label,
            Composite parent) {
        AccountInfoPropertyEditor fieldEditor = AccountInfoPropertyEditorFactory
                .getAccountInfoPropertyEditor(currentAccount, propertyName,
                        PropertyType.FILE_PROPERTY, label, parent,
                        dataBindingContext);
        setUpFieldEditor(fieldEditor, parent);
        return fieldEditor;
    }

    protected void setUpFieldEditor(
            final AccountInfoPropertyEditor fieldEditor, Composite parent) {
        if (fieldEditor instanceof AccountInfoStringPropertyEditor) {
            ((AccountInfoStringPropertyEditor) fieldEditor)
                    .getStringFieldEditor().fillIntoGrid(parent,
                            AwsToolkitPreferencePage.LAYOUT_COLUMN_WIDTH);
        } else if (fieldEditor instanceof AccountInfoFilePropertyEditor) {
            ((AccountInfoFilePropertyEditor) fieldEditor).getFileFieldEditor()
                    .fillIntoGrid(parent,
                            AwsToolkitPreferencePage.LAYOUT_COLUMN_WIDTH);
        }

        // Validate the page whenever the editors are touched
        fieldEditor.getTextControl().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                /*
                 * Since the data-binding also observes the Modify event on the
                 * text control, there is an edge case where the AccountInfo
                 * model might not have been updated when the modifyText
                 * callbacked is triggered. To make sure the updated data model
                 * is visible to the subsequent operations, we forcefully push
                 * the data from the editor to the data model.
                 */
                fieldEditor.forceUpdateEditorValueToAccountInfoModel();

                /*
                 * Now that the property value change has been applied to the
                 * AccountInfo object. Since the accountSelector is backed by
                 * the collection of the same AccountInfo objects, we can simply
                 * refresh the UI of accountSelector; all the account name
                 * changes will be reflected.
                 */
                for (AwsAccountPreferencePageTab tab : parentPrefPage.getAllAccountPreferencePageTabs()) {
                    tab.refreshAccountSelectorUI();
                }

                parentPrefPage.updatePageValidationOfAllTabs();

                for (ModifyListener listener : accountInfoFieldEditorListeners) {
                    listener.modifyText(null);
                }
            }
        });
    }

    private String getRegionCurrentAccountPrefKey() {
        return PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region);
    }

    private String getRegionAccountDefaultName() {
        return PreferenceConstants.DEFAULT_ACCOUNT_NAME(region);
    }

    private String getRegionAccountEnabledPrefKey() {
        return PreferenceConstants.P_REGION_DEFAULT_ACCOUNT_ENABLED(region);
    }

    private IPreferenceStore getPreferenceStore() {
        return prefStore;
    }

    /**
     * Recursively enable/disable all the children controls of account-info
     * section.
     */
    private void toggleAccountInfoSectionEnabled(boolean enabled) {
        AwsAccountPreferencePage.setEnabledOnAllChildern(accountInfoSection, enabled);
    }

    private AccountInfo createNewProfileAccountInfo(String newAccountId) {
        return new AccountInfoImpl(newAccountId,
                new SdkProfilesCredentialsConfiguration(prefStore,
                        newAccountId, SdkProfilesFactory.newEmptyBasicProfile("")),
                new PluginPreferenceStoreAccountOptionalConfiguration(
                        prefStore, newAccountId));
    }

    private String getDefaultAccountExplanationText() {
        if (isGlobalAccoutTab()) {
            return "This credential profile will be used by default to access all AWS regions "
                    + "that are not configured with a region-specific account.";
        } else {
            return "This credential profile will be used by default to access AWS resources in "
                    + region.getName() + "(" + region.getId() + ") region.";
        }
    }

    /**
     * For debug purpose only.
     */
    @SuppressWarnings("unused")
    private void printAccounts() {
        System.out.println("*** Default accounts("
                + accountInfoByIdentifier.size() + ") tab for "
                + this.getText() + " ***");
        for (String id : accountInfoByIdentifier.keySet()) {
            System.out.println("    " + accountInfoByIdentifier.get(id) + "("
                    + id + ")");
        }
        System.out.println("Current default: "
                + accountInfoByIdentifier.get(currentRegionAccountId));
    }

    /**
     * Override this method in order to let SWT allow sub-classing TabItem
     */
    @Override
    public void checkSubclass() {
        // no-op
    }
}
