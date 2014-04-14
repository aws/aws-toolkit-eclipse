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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.util.StringUtils;

/**
 * A tab item contained in the AWS account preference page. Each tab item
 * corresponds to a set of region-specific (or global) accounts.
 */
public class AwsAccountPreferencePageTab extends TabItem {

    /** The preference store persisted by this tab */
    private final IPreferenceStore prefStore;

    /** The preference page to which the validation error message should be reported. */
    private final AwsAccountPreferencePage parentPrefPage;

    /** The region associated with this tat. Null if this is the tab for global account */
    private final Region region;

    // State variables to simplify bookkeeping
    private String currentRegionAccountId;
    private final Map<String, String> accountNamesByIdentifier = new HashMap<String, String>();

    // Page controls
    BooleanFieldEditor enableRegionDefaultAccount;
    private Composite accountInfoSection;
    private Combo accountSelector;
    private AccountStringFieldEditor accountNameFieldEditor;
    private AccountStringFieldEditor userIdFieldEditor;
    private AccountStringFieldEditor accessKeyFieldEditor;
    private AccountStringFieldEditor secretKeyFieldEditor;
    private AccountFileFieldEditor certificateFieldEditor;
    private AccountFileFieldEditor certificatePrivateKeyFieldEditor;

    /*
     * The set of field editors that need to know when the account or its name
     * changes.
     */
    private final Collection<AccountFieldEditor> accountFieldEditors = new LinkedList<AccountFieldEditor>();

    /** The checkbox controlling how we display the secret key */
    private Button hideSecretKeyCheckbox;

    /** The Text control in the secret key field editor */
    private Text secretKeyText;

    private Font italicFont;

    public AwsAccountPreferencePageTab(TabFolder tabFolder,
                                        int tabIndex,
                                        AwsAccountPreferencePage parentPrefPage,
                                        Region region) {
        this(tabFolder, tabIndex, parentPrefPage, region, AwsToolkitCore.getDefault().getPreferenceStore());
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
     * @param regionId
     *            Caller should be responsible to provide a valid regionId.
     * @param prefStore
     *            The preference store that this tab persists with.
     */
    public AwsAccountPreferencePageTab(TabFolder tabFolder, int tabIndex,
            AwsAccountPreferencePage parentPrefPage, Region region,
            IPreferenceStore prefStore) {
        super(tabFolder, SWT.NONE, tabIndex);
        this.region = region;
        this.parentPrefPage = parentPrefPage;
        this.prefStore = prefStore;

        if (region == null) {
            setText("Global Account");
        } else {
            // Use the region name as the tab title
            setText(region.getName());
        }

        readPreferenceValues();

        writeDefaultPreferenceValues();

        addControls();
    }

    /*
     * Public interfaces
     */

    public void loadDefault() {
        userIdFieldEditor.loadDefault();
        accessKeyFieldEditor.loadDefault();
        secretKeyFieldEditor.loadDefault();

        certificateFieldEditor.loadDefault();
        certificatePrivateKeyFieldEditor.loadDefault();
    }
    
    public void doStore() {
        // Store the enabled check box
        if (enableRegionDefaultAccount != null) {
            enableRegionDefaultAccount.store();
        }
        
        // Clean up the AWS User ID and store it
        String userId = userIdFieldEditor.getStringValue();
        userId = userId.replace("-", "");
        userId = userId.replace(" ", "");
        userIdFieldEditor.setStringValue(userId);
        userIdFieldEditor.store();

        // Clean up the account name and store it
        String accountName = accountNameFieldEditor.getStringValue();
        accountName = accountName.trim();
        accountNameFieldEditor.setStringValue(accountName);
        accountNameFieldEditor.store();

        // Clean up the AWS Access Key and store it
        String accessKey = accessKeyFieldEditor.getStringValue();
        accessKey = accessKey.trim();
        accessKeyFieldEditor.setStringValue(accessKey);
        accessKeyFieldEditor.store();

        // Clean up the AWS Secret Key and store it
        String secretKey = secretKeyFieldEditor.getStringValue();
        secretKey = secretKey.trim();
        secretKeyFieldEditor.setStringValue(secretKey);
        secretKeyFieldEditor.store();

        // Save the certificate and private key
        certificateFieldEditor.store();
        certificatePrivateKeyFieldEditor.store();

        // Finally update the list of account identifiers and the current
        // account
        String accountIds = StringUtils.join(PreferenceConstants.ACCOUNT_ID_SEPARATOR,
                accountNamesByIdentifier.keySet().toArray(new String[accountNamesByIdentifier.size()]));
        getPreferenceStore().setValue(getRegionAccountsPrefKey(), accountIds);
        getPreferenceStore().setValue(getRegionCurrentAccountPrefKey(), currentRegionAccountId);
    }

    @Override
    public void dispose() {
        if (italicFont != null) italicFont.dispose();
        super.dispose();
    }

    public Region getRegion() {
        return region;
    }

    public boolean isGlobalAccoutTab() {
        return region == null;
    }

    public String[] getAccountNames() {
        return accountSelector == null ?
                new String[0] : accountSelector.getItems();
    }

    /**
     * Utility function that recursively enable/disable all the children
     * controls contained in the given Composite.
     */
    public static void setEnabledOnAllChildern(Composite composite, boolean enabled) {
        for (Control child : composite.getChildren()) {
            if (child instanceof Composite) {
                setEnabledOnAllChildern((Composite)child, enabled);
            }
            child.setEnabled(enabled);
        }
    }

    /*
     * Private Interface
     */

    private void readPreferenceValues() {
        accountFieldEditors.clear();
        accountNamesByIdentifier.clear();
        accountNamesByIdentifier.putAll(getRegionalAccounts());

        currentRegionAccountId = getPreferenceStore().getString(getRegionCurrentAccountPrefKey());
    }

    private void writeDefaultPreferenceValues(){
        if ( !isGlobalAccoutTab() ) {
            getPreferenceStore().setDefault(getRegionAccountEnabledPrefKey(), true);
        }
    }

    private void addControls() {
        Composite composite = new Composite(getParent(), SWT.NONE);
        composite.setLayout(new GridLayout(3, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gridData);

        if ( !isGlobalAccoutTab() ) {
            enableRegionDefaultAccount = new BooleanFieldEditor(
                    getRegionAccountEnabledPrefKey(), "Enable region default account for " + region.getName(), composite);
            enableRegionDefaultAccount.setPreferenceStore(getPreferenceStore());
            
            Button removeTabButton = new Button(composite, SWT.PUSH);
            removeTabButton.setText("Remove");
            removeTabButton.setToolTipText("Remove all accounts for this region.");
            removeTabButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_REMOVE));
            removeTabButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
            removeTabButton.addSelectionListener(new SelectionAdapter() {
                
                public void widgetSelected(SelectionEvent e) {
                    MessageDialog confirmRemoveTabDialog = new MessageDialog(
                            Display.getDefault().getActiveShell(),
                            "Remove all accounts for " + region.getName(),
                            AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                            "Are you sure you want to remove all the configured accounts for " + region.getName() + "?",
                            MessageDialog.CONFIRM,
                            new String[] { "Cancel", "OK" }, 1);
                    if (confirmRemoveTabDialog.open() == 1) {
                        AwsAccountPreferencePageTab.this.dispose();
                    }
                    
                }
            });
        }
        
        accountInfoSection = createAccountInfoSection(composite);
        
        if (enableRegionDefaultAccount != null) {
            enableRegionDefaultAccount.setPropertyChangeListener(new IPropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    boolean enabled = (Boolean) event.getNewValue();
                    AwsAccountPreferencePageTab.this.toggleAccountInfoSectionEnabled(enabled);
                }
            });
            
            enableRegionDefaultAccount.load();
            toggleAccountInfoSectionEnabled(enableRegionDefaultAccount.getBooleanValue());
        }

        AwsToolkitPreferencePage.tweakLayout((GridLayout)composite.getLayout());

        setControl(composite);
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
        Group awsGroup = createAccountDetailSectionGroup(accountInfoSection, webLinkListener);
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
        

        new Label(composite, SWT.READ_ONLY).setText("Default Account: ");
        accountSelector = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        accountSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Map<String, String> accounts = getRegionalAccounts();

        List<String> nameList = new ArrayList<String>();
        nameList.addAll(accounts.values());
        Collections.sort(nameList);

        int selectedIndex = 0;
        for ( int i = 0; i < nameList.size(); i++ ) {
            if ( nameList.get(i).equals(accountNamesByIdentifier.get(currentRegionAccountId)) ) {
                selectedIndex = i;
                break;
            }
        }

        accountSelector.setItems(nameList.toArray(new String[nameList.size()]));
        accountSelector.select(selectedIndex);

        accountSelector.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String accountName = accountSelector.getItem(accountSelector.getSelectionIndex());
                String id = reverseAccountLookup(accountName);
                accountChanged(id);
            }
        });

        final Button addNewAccount = new Button(composite, SWT.PUSH);
        addNewAccount.setText("Add account");
        final Button deleteAccount = new Button(composite, SWT.PUSH);
        deleteAccount.setText("Remove account");
        deleteAccount.setEnabled(accounts.size() > 1);
        

        Label defaultAccountExplainationLabel = new Label(composite, SWT.WRAP);
        if (isGlobalAccoutTab()) {
            defaultAccountExplainationLabel.setText(
                    "This account will be used by default to access all AWS regions "
                    + "that are not configured with a region-specific account.");
        } else {
            defaultAccountExplainationLabel.setText(
                    "This account will be used by default to access AWS resources in "
                    + region.getName() + "(" + region.getId() + ") region.");
        }
        
        FontData[] fontData = defaultAccountExplainationLabel.getFont().getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        defaultAccountExplainationLabel.setFont(italicFont);
        
        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.horizontalSpan = 4;
        layoutData.widthHint = 200;
        defaultAccountExplainationLabel.setLayoutData(layoutData);
        
        addNewAccount.addSelectionListener(new SelectionAdapter() {
        
            @Override
            public void widgetSelected(SelectionEvent e) {
                String[] currentAccountNames = accountSelector.getItems();
                String[] newAccountNames = new String[currentAccountNames.length + 1];
                System.arraycopy(currentAccountNames, 0, newAccountNames, 0, currentAccountNames.length);
                String newAccountName = region == null ?
                        "New Account"
                        :
                        "New " + region.getName() + " Account";
                newAccountNames[currentAccountNames.length] = newAccountName;
                accountSelector.setItems(newAccountNames);
                accountSelector.select(currentAccountNames.length);
                accountSelector.getParent().getParent().layout();

                String newAccountId = UUID.randomUUID().toString();
                accountNamesByIdentifier.put(newAccountId, newAccountName);

                accountChanged(newAccountId);
                accountNameFieldEditor.setStringValue(newAccountName);
            }
        });

        deleteAccount.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String[] currentAccountNames = accountSelector.getItems();
                String[] newAccountNames = new String[currentAccountNames.length - 1];
                int i = 0;
                int j = 0;
                for ( String accountName : currentAccountNames ) {
                    if ( i++ != accountSelector.getSelectionIndex() ) {
                        newAccountNames[j++] = accountName;
                    }
                }
                if ( newAccountNames.length == 0 )
                    newAccountNames = new String[] { getRegionAccountDefaultName() };

                accountSelector.setItems(newAccountNames);
                accountSelector.select(0);
                accountNamesByIdentifier.remove(currentRegionAccountId);

                accountChanged(reverseAccountLookup(newAccountNames[0]));
            }
        });
        
        
        accountSelector.addModifyListener(new ModifyListener() {
            
            public void modifyText(ModifyEvent arg0) {
                if (accountSelector.getItemCount() > 1) {
                    deleteAccount.setEnabled(true);
                } else {
                    deleteAccount.setEnabled(false);
                }
                
            }
        });
        
        return composite;
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
        awsAccountGroup.setText("Account Details:");
        
        if (isGlobalAccoutTab()) {
            String linkText = "<a href=\"" + AwsUrls.SIGN_UP_URL + "\">Sign up for a new AWS account</a> or "
                    + "<a href=\"" + AwsUrls.SECURITY_CREDENTIALS_URL
                    + "\">manage your existing AWS security credentials</a>.";
            AwsToolkitPreferencePage.newLink(webLinkListener, linkText, awsAccountGroup);
            AwsToolkitPreferencePage.createSpacer(awsAccountGroup);
        }


        accountNameFieldEditor = newStringFieldEditor(currentRegionAccountId, PreferenceConstants.P_ACCOUNT_NAME,
                "Account &Name:", awsAccountGroup);

        accountNameFieldEditor.getTextControl(awsAccountGroup).addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String newAccountName = accountNameFieldEditor.getStringValue();
                // On linux, we have to select an index after setting the current item
                int selectionIndex = accountSelector.getSelectionIndex();
                accountSelector.setItem(selectionIndex, newAccountName);
                accountSelector.getParent().getParent().layout();
                accountSelector.select(selectionIndex);
                accountNamesByIdentifier.put(currentRegionAccountId, newAccountName);
                updatePageValidation();
            }
        });

        accessKeyFieldEditor = newStringFieldEditor(currentRegionAccountId, PreferenceConstants.P_ACCESS_KEY,
                "&Access Key ID:", awsAccountGroup);
        secretKeyFieldEditor = newStringFieldEditor(currentRegionAccountId, PreferenceConstants.P_SECRET_KEY,
                "&Secret Access Key:", awsAccountGroup);

        // create an empty label in the first column so that the hide secret key
        // checkbox lines up with the other text controls
        new Label(awsAccountGroup, SWT.NONE);

        hideSecretKeyCheckbox = new Button(awsAccountGroup, SWT.CHECK);
        hideSecretKeyCheckbox.setText("Show secret access key");
        hideSecretKeyCheckbox.setSelection(false);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.verticalIndent = -6;
        gridData.horizontalIndent = 3;
        hideSecretKeyCheckbox.setLayoutData(gridData);
        hideSecretKeyCheckbox.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                updateSecretKeyText();
            }
        });

        secretKeyText = secretKeyFieldEditor.getTextControl(awsAccountGroup);

        updateSecretKeyText();
        AwsToolkitPreferencePage.tweakLayout((GridLayout) awsAccountGroup.getLayout());

        accountFieldEditors.add(accountNameFieldEditor);
        accountFieldEditors.add(accessKeyFieldEditor);
        accountFieldEditors.add(secretKeyFieldEditor);

        return awsAccountGroup;
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
    private void createOptionalSection(final Composite parent, WebLinkListener webLinkListener) {

        Section expoandableComposite = new Section(parent, ExpandableComposite.TWISTIE);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = AwsToolkitPreferencePage.LAYOUT_COLUMN_WIDTH;
        expoandableComposite.setLayoutData(gd);

        Composite optionalConfigGroup = new Composite(expoandableComposite, SWT.NONE);
        optionalConfigGroup.setLayout(new GridLayout());
        optionalConfigGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        expoandableComposite.setClient(optionalConfigGroup);
        expoandableComposite.setText("Optional configuration:");
        expoandableComposite.setExpanded(false);

        expoandableComposite.addExpansionListener(new IExpansionListener() {

            public void expansionStateChanging(ExpansionEvent e) {
            }

            public void expansionStateChanged(ExpansionEvent e) {
                parent.getParent().layout();
            }
        });

        String linkText = "Your AWS account number and X.509 certificate are only needed if you want to bundle EC2 instances from Eclipse.  "
                + "<a href=\"" + AwsUrls.SECURITY_CREDENTIALS_URL + "\">Manage your AWS X.509 certificate</a>.";
        AwsToolkitPreferencePage.newLink(webLinkListener, linkText, optionalConfigGroup);

        AwsToolkitPreferencePage.createSpacer(optionalConfigGroup);

        userIdFieldEditor = newStringFieldEditor(currentRegionAccountId, PreferenceConstants.P_USER_ID, "Account &Number:",
                optionalConfigGroup);
        createFieldExampleLabel(optionalConfigGroup, "ex: 1111-2222-3333");

        certificateFieldEditor = newFileFieldEditor(currentRegionAccountId, PreferenceConstants.P_CERTIFICATE_FILE,
                "&Certificate File:", optionalConfigGroup);
        certificatePrivateKeyFieldEditor = newFileFieldEditor(currentRegionAccountId, PreferenceConstants.P_PRIVATE_KEY_FILE,
                "&Private Key File:", optionalConfigGroup);

        AwsToolkitPreferencePage.tweakLayout((GridLayout) optionalConfigGroup.getLayout());

        accountFieldEditors.add(userIdFieldEditor);
        accountFieldEditors.add(certificateFieldEditor);
        accountFieldEditors.add(certificatePrivateKeyFieldEditor);
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
        if ( fontData.length > 0 ) {
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
     * Looks up an account Id by its name.
     */
    private String reverseAccountLookup(String accountName) {
        String id = null;
        for ( Entry<String, String> entry : accountNamesByIdentifier.entrySet() ) {
            if ( entry.getValue().equals(accountName) ) {
                id = entry.getKey();
            }
        }
        return id;
    }

    /**
     * Returns all the accounts associated with this region.
     * Bootstrap the default account if there is not any.
     */
    private Map<String, String> getRegionalAccounts() {
        return AwsAccountPreferencePage.getRegionalAccounts(prefStore, region, true);
    }

    /**
     * Invoked whenever the selected account information changes.
     */
    private void accountChanged(String accountIdentifier) {
        currentRegionAccountId = accountIdentifier;
        for ( AccountFieldEditor editor : accountFieldEditors ) {
            editor.accountChanged(accountIdentifier);
        }
    }

    /**
     * Update or clear the error message and validity of the page.
     */
    private void updatePageValidation() {
        String errorString = validate();
        if ( errorString != null ) {
            parentPrefPage.setValid(false);
            parentPrefPage.setErrorMessage(errorString);
            return;
        }

        parentPrefPage.checkDuplicateAccountName();
    }

    /**
     * Returns an error message if there's a problem with the page's fields, or
     * null if there are no errors.
     */
    private String validate() {
        if ( accountNameFieldEditor != null ) {
            if ( accountNameFieldEditor.getStringValue().trim().length() == 0 ) {
                return "Account name must not be blank";
            }

            for ( Entry<String, String> entry : accountNameFieldEditor.valuesByAccountIdentifier.entrySet() ) {
                if ( !entry.getKey().equals(currentRegionAccountId) )
                    if ( entry.getValue().length() == 0 )
                        return "Account name must not be blank";
            }
        }

        if ( certificateFieldEditor != null ) {
            if ( invalidFile(certificateFieldEditor.getStringValue()) ) {
                return "Certificate file does not exist";
            }

            for ( Entry<String, String> entry : certificateFieldEditor.valuesByAccountIdentifier.entrySet() ) {
                if ( !entry.getKey().equals(currentRegionAccountId) )
                    if ( invalidFile(entry.getValue()) )
                        return "Certificate file does not exist";
            }
        }

        if ( certificatePrivateKeyFieldEditor != null ) {
            String certFile = certificatePrivateKeyFieldEditor.getStringValue();
            if ( invalidFile(certFile) ) {
                return "Private key file does not exist";
            }

            for ( Entry<String, String> entry : certificatePrivateKeyFieldEditor.valuesByAccountIdentifier.entrySet() ) {
                if ( !entry.getKey().equals(currentRegionAccountId) )
                    if ( invalidFile(entry.getValue()) )
                        return "Private key file does not exist";
            }
        }

        return null;
    }

    private boolean invalidFile(String certFile) {
        return certFile.trim().length() > 0
                && !new File(certFile).exists();
    }

    /**
     * Updates the secret key text according to whether or not the
     * "display secret key in plain text" checkbox is selected or not.
     */
    private void updateSecretKeyText() {
        if ( hideSecretKeyCheckbox == null )
            return;
        if ( secretKeyText == null )
            return;

        if ( hideSecretKeyCheckbox.getSelection() ) {
            secretKeyText.setEchoChar('\0');
        } else {
            secretKeyText.setEchoChar('*');
        }
    }

    protected AccountStringFieldEditor newStringFieldEditor(String currentAccount,
                                                            String preferenceKey,
                                                            String label,
                                                            Composite parent) {
        AccountStringFieldEditor fieldEditor = new AccountStringFieldEditor(currentAccount, preferenceKey, label,
                parent);
        setUpFieldEditor(currentAccount, preferenceKey, parent, fieldEditor);
        return fieldEditor;
    }

    protected AccountFileFieldEditor newFileFieldEditor(String currentAccount,
                                                        String preferenceKey,
                                                        String label,
                                                        Composite parent) {
        AccountFileFieldEditor fieldEditor = new AccountFileFieldEditor(currentAccount, preferenceKey, label, parent);
        setUpFieldEditor(currentAccount, preferenceKey, parent, fieldEditor);
        return fieldEditor;
    }

    protected void setUpFieldEditor(String currentAccount,
                                    String preferenceKey,
                                    Composite parent,
                                    StringFieldEditor fieldEditor) {
        fieldEditor.setPage(parentPrefPage);
        fieldEditor.setPreferenceStore(this.getPreferenceStore());
        fieldEditor.load();

        // For backwards compatibility with single-account storage
        if ( accountNamesByIdentifier.get(currentAccount) != null
                && accountNamesByIdentifier.get(currentAccount).equals(getRegionAccountDefaultName())
                && (fieldEditor.getStringValue() == null || fieldEditor.getStringValue().length() == 0) ) {
            String currentPrefValue = getPreferenceStore().getString(preferenceKey);
            if ( ObfuscatingStringFieldEditor.isBase64(currentPrefValue) ) {
                currentPrefValue = ObfuscatingStringFieldEditor.decodeString(currentPrefValue);
            }
            fieldEditor.setStringValue(currentPrefValue);
        }

        fieldEditor.fillIntoGrid(parent, AwsToolkitPreferencePage.LAYOUT_COLUMN_WIDTH);
        fieldEditor.getTextControl(parent).addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updatePageValidation();
            }
        });
    }

    private interface AccountFieldEditor {

        /**
         * Invoked when the current account changes.
         */
        public void accountChanged(String accountIdentifier);
    }

    /*
     * The next two classes are duplicates, but since their hierarchies diverge
     * there isn't much we can do about that.
     */

    /**
     * Preference editor with a dynamic key name
     */
    private class AccountStringFieldEditor extends ObfuscatingStringFieldEditor implements AccountFieldEditor {

        private String accountIdentifier;

        private final Map<String, String> valuesByAccountIdentifier = new HashMap<String, String>();

        public AccountStringFieldEditor(String accountIdentifier, String preferenceKey, String labelText,
                Composite parent) {
            super(preferenceKey, labelText, parent);
            this.accountIdentifier = accountIdentifier;
        }

        @Override
        public String getPreferenceName() {
            return accountIdentifier + ":" + super.getPreferenceName();
        }

        /**
         * Store the current value of the field before replacing the contents
         * with either the last user-entered value or else the persisted
         * preference's value.
         */
        public void accountChanged(String accountIdentifier) {
            valuesByAccountIdentifier.put(this.accountIdentifier, getStringValue());
            this.accountIdentifier = accountIdentifier;
            if ( valuesByAccountIdentifier.containsKey(accountIdentifier) ) {
                this.setStringValue(valuesByAccountIdentifier.get(accountIdentifier));
            } else {
                this.load();
            }
        }

        /**
         * Stores all the values that have been edited, not just the current one.
         */
        @Override
        protected void doStore() {
            valuesByAccountIdentifier.put(accountIdentifier, getStringValue());
            String originalAccountIdentifier = this.accountIdentifier;

            for ( Entry<String, String> entry : valuesByAccountIdentifier.entrySet() ) {
                this.accountIdentifier = entry.getKey();
                getTextControl().setText(entry.getValue());
                super.doStore();
            }

            this.accountIdentifier = originalAccountIdentifier;
            getTextControl().setText(valuesByAccountIdentifier.get(accountIdentifier));
        }

        @Override
        protected boolean checkState() {
            String errorMessage = validate();
            setErrorMessage(errorMessage);
            return errorMessage == null;
        }

        @Override
        protected void clearErrorMessage() {
        }
    }

    /**
     * Preference editor with a dynamic key name
     */
    private final class AccountFileFieldEditor extends FileFieldEditor implements AccountFieldEditor {

        private String accountIdentifier;

        private final Map<String, String> valuesByAccountIdentifier = new HashMap<String, String>();

        public AccountFileFieldEditor(String accountIdentifier, String preferenceKey, String labelText,
                Composite parent) {
            super(preferenceKey, labelText, parent);
            this.accountIdentifier = accountIdentifier;
        }

        @Override
        public String getPreferenceName() {
            return accountIdentifier + ":" + super.getPreferenceName();
        }

        /**
         * Store the current value of the field before replacing the contents
         * with either the last user-entered value or else the persisted
         * preference's value.
         */
        public void accountChanged(String accountIdentifier) {
            valuesByAccountIdentifier.put(this.accountIdentifier, getStringValue());
            this.accountIdentifier = accountIdentifier;
            if ( valuesByAccountIdentifier.containsKey(accountIdentifier) ) {
                this.setStringValue(valuesByAccountIdentifier.get(accountIdentifier));
            } else {
                this.load();
            }
        }

        /**
         * Stores all the values that have been edited, not just the current one.
         */
        @Override
        protected void doStore() {
            valuesByAccountIdentifier.put(accountIdentifier, getStringValue());
            String originalAccountIdentifier = this.accountIdentifier;

            for ( Entry<String, String> entry : valuesByAccountIdentifier.entrySet() ) {
                this.accountIdentifier = entry.getKey();
                getTextControl().setText(entry.getValue());
                super.doStore();
            }

            this.accountIdentifier = originalAccountIdentifier;
            getTextControl().setText(valuesByAccountIdentifier.get(accountIdentifier));
        }

        @Override
        protected boolean checkState() {
            String errorMessage = validate();
            setErrorMessage(errorMessage);
            return errorMessage == null;
        }

        @Override
        protected void clearErrorMessage() {
        }
    }

    private String getRegionAccountsPrefKey() {
        return PreferenceConstants.P_ACCOUNT_IDS(region);
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
        setEnabledOnAllChildern(accountInfoSection, enabled);
        if (enabled) {
            accountSelector.select(accountSelector.getSelectionIndex());
        }
    }

    /**
     * For debug purpose only.
     */
    private void printAccounts() {
        System.out.println("*** Default accounts(" + accountNamesByIdentifier.size() + ") tab for " + this.getText() + " ***");
        for (String id : accountNamesByIdentifier.keySet()) {
            System.out.println("    " + accountNamesByIdentifier.get(id) + "(" + id + ")");
        }
        System.out.println("Current default: " + accountNamesByIdentifier.get(currentRegionAccountId));
    }

    /**
     * Override this method in order to let SWT allow sub-classing TabItem
     */
    @Override
    public void checkSubclass() {
        // no-op
    }
}
