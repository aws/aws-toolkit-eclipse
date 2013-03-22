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

package com.amazonaws.eclipse.core.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.util.StringUtils;

/**
 * Preference page for basic AWS account information.
 */
public class AwsAccountPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    public static final String ID = "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage";

    // State variables to simplify bookkeeping
    private String currentAccountId;
    private final Map<String, String> accountNamesByIdentifier = new HashMap<String, String>();

    // Page controls
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
        currentAccountId = getPreferenceStore().getString(PreferenceConstants.P_CURRENT_ACCOUNT);

        accountFieldEditors.clear();
        accountNamesByIdentifier.clear();
        accountNamesByIdentifier.putAll(getAccounts(getPreferenceStore()));

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gridData);

        WebLinkListener webLinkListener = new WebLinkListener();

        Group accountsGroup = createAccountSelector(composite);
        Group awsGroup = createAwsAccountSection(accountsGroup, webLinkListener);
        createOptionalSection(awsGroup, webLinkListener);

        Link networkConnectionLink = new Link(composite, SWT.NULL);
        networkConnectionLink
                .setText("See <a href=\"org.eclipse.ui.net.NetPreferences\">Network connections</a> to configure how the AWS Toolkit connects to the internet.");
        PreferenceLinkListener preferenceLinkListener = new PreferenceLinkListener();
        networkConnectionLink.addListener(SWT.Selection, preferenceLinkListener);

        String javaForumLinkText = "Get help or provide feedback on the " + "<a href=\""
                + AwsUrls.JAVA_DEVELOPMENT_FORUM_URL + "\">AWS Java Development forum</a>. ";
        newLink(webLinkListener, javaForumLinkText, composite);

        return composite;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        userIdFieldEditor.loadDefault();
        accessKeyFieldEditor.loadDefault();
        secretKeyFieldEditor.loadDefault();

        certificateFieldEditor.loadDefault();
        certificatePrivateKeyFieldEditor.loadDefault();

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
        getPreferenceStore().setValue(PreferenceConstants.P_ACCOUNT_IDS, accountIds);
        getPreferenceStore().setValue(PreferenceConstants.P_CURRENT_ACCOUNT, currentAccountId);

        this.getControl().setRedraw(true);
        
        return super.performOk();
    }

    /*
     * Private Interface
     */

    /**
     * Creates the account selection section.
     */
    private Group createAccountSelector(final Composite parent) {
        Group accountSelectionGroup = new Group(parent, SWT.NONE);
        accountSelectionGroup.setText("AWS Accounts:");
        accountSelectionGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        accountSelectionGroup.setLayout(new GridLayout(4, false));
        

        new Label(accountSelectionGroup, SWT.READ_ONLY).setText("Default Account: ");
        accountSelector = new Combo(accountSelectionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        accountSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        IPreferenceStore preferenceStore = getPreferenceStore();
        Map<String, String> accounts = getAccounts(preferenceStore);

        List<String> nameList = new ArrayList<String>();
        nameList.addAll(accounts.values());
        Collections.sort(nameList);

        int selectedIndex = 0;
        for ( int i = 0; i < nameList.size(); i++ ) {
            if ( nameList.get(i).equals(accountNamesByIdentifier.get(currentAccountId)) ) {
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

        final Button addNewAccount = new Button(accountSelectionGroup, SWT.PUSH);
        addNewAccount.setText("Add account");
        final Button deleteAccount = new Button(accountSelectionGroup, SWT.PUSH);
        deleteAccount.setText("Remove account");
        deleteAccount.setEnabled(accounts.size() > 1);
        

        Label defaultAccountExplainationLabel = new Label(accountSelectionGroup, SWT.WRAP);
        defaultAccountExplainationLabel.setText("This account will be used by default for all features in the AWS Toolkit for Eclipse, unless they specifically allow you to select an account.");
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
                String newAccountName = "New Account";
                newAccountNames[currentAccountNames.length] = newAccountName;
                accountSelector.setItems(newAccountNames);
                accountSelector.select(currentAccountNames.length);
                accountSelector.getParent().getParent().layout();

                String newAccountId = UUID.randomUUID().toString();
                accountNamesByIdentifier.put(newAccountId, newAccountName);

                accountChanged(newAccountId);
                accountNameFieldEditor.setStringValue(newAccountName);

                deleteAccount.setEnabled(true);
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
                    newAccountNames = new String[] { PreferenceConstants.DEFAULT_ACCOUNT_NAME };

                accountSelector.setItems(newAccountNames);
                accountSelector.select(0);
                accountNamesByIdentifier.remove(currentAccountId);

                accountChanged(reverseAccountLookup(newAccountNames[0]));

                deleteAccount.setEnabled(newAccountNames.length > 1);
            }
        });
        
        return accountSelectionGroup;
    }

    @Override
    public void dispose() {
        if (italicFont != null) italicFont.dispose();
        super.dispose();
    }

    /**
     * Looks up an account Id by its name.
     */
    protected String reverseAccountLookup(String accountName) {
        String id = null;
        for ( Entry<String, String> entry : accountNamesByIdentifier.entrySet() ) {
            if ( entry.getValue().equals(accountName) ) {
                id = entry.getKey();
            }
        }
        return id;
    }

    /**
     * Returns the list of account names registered. If none are registered yet,
     * returns the default one.
     *
     * @param preferenceStore
     *            The preference store to use when looking up the account names.
     * @return A map of account identifier to customer-assigned names. The
     *         identifiers are the primary, immutable key used to access the
     *         account.
     */
    public static Map<String, String> getAccounts(IPreferenceStore preferenceStore) {
        String accountNamesString = preferenceStore.getString(PreferenceConstants.P_ACCOUNT_IDS);

        // bootstrapping
        if ( accountNamesString == null || accountNamesString.length() == 0 ) {
            String id = UUID.randomUUID().toString();
            preferenceStore.putValue(PreferenceConstants.P_CURRENT_ACCOUNT, id);
            preferenceStore.putValue(PreferenceConstants.P_ACCOUNT_IDS, id);
            preferenceStore.putValue(id + ":" + PreferenceConstants.P_ACCOUNT_NAME, PreferenceConstants.DEFAULT_ACCOUNT_NAME_BASE_64);
        }

        String[] accountIds = accountNamesString.split(PreferenceConstants.ACCOUNT_ID_SEPARATOR_REGEX);
        Map<String, String> names = new HashMap<String, String>();
        for ( String id : accountIds ) {
            String preferenceName = id + ":" + PreferenceConstants.P_ACCOUNT_NAME;
            names.put(id, ObfuscatingStringFieldEditor.decodeString(preferenceStore.getString(preferenceName)));
        }

        return names;
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
    private Group createAwsAccountSection(final Composite parent,
                                         WebLinkListener webLinkListener) {

        final Group awsAccountGroup = new Group(parent, SWT.NONE);
        GridData gridData1 = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData1.horizontalSpan = 4;
        gridData1.verticalIndent = 10;
        awsAccountGroup.setLayoutData(gridData1);
        awsAccountGroup.setText("Account Details:");
        
        String linkText = "<a href=\"" + AwsUrls.SIGN_UP_URL + "\">Sign up for a new AWS account</a> or "
                + "<a href=\"" + AwsUrls.SECURITY_CREDENTIALS_URL
                + "\">find your existing AWS security credentials</a>.";
        newLink(webLinkListener, linkText, awsAccountGroup);
        createSpacer(awsAccountGroup);

        accountNameFieldEditor = newStringFieldEditor(currentAccountId, PreferenceConstants.P_ACCOUNT_NAME,
                "Account &Name:", awsAccountGroup);

        accountNameFieldEditor.getTextControl(awsAccountGroup).addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String newAccountName = accountNameFieldEditor.getStringValue();
                // On linux, we have to select an index after setting the current item
                int selectionIndex = accountSelector.getSelectionIndex();
				accountSelector.setItem(selectionIndex, newAccountName);
                accountSelector.getParent().getParent().layout();
                accountSelector.select(selectionIndex);
                accountNamesByIdentifier.put(currentAccountId, newAccountName);
                updatePageValidation();
            }
        });

        accessKeyFieldEditor = newStringFieldEditor(currentAccountId, PreferenceConstants.P_ACCESS_KEY,
                "&Access Key ID:", awsAccountGroup);
        secretKeyFieldEditor = newStringFieldEditor(currentAccountId, PreferenceConstants.P_SECRET_KEY,
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
        tweakLayout((GridLayout) awsAccountGroup.getLayout());

        accountFieldEditors.add(accountNameFieldEditor);
        accountFieldEditors.add(accessKeyFieldEditor);
        accountFieldEditors.add(secretKeyFieldEditor);

        return awsAccountGroup;
    }

    /**
     * Invoked whenever the selected account information changes.
     */
    private void accountChanged(String accountIdentifier) {
        currentAccountId = accountIdentifier;
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
            setValid(false);
            setErrorMessage(errorString);
            return;
        }

        setValid(true);
        setErrorMessage(null);
    }

    /**
     * Returns an error message if there's a problem with the page's fields, or
     * null if there are no errors.
     */
    private String validate() {
        Set<String> accountNames = new HashSet<String>();

        if ( accountSelector != null ) {
            for ( String accountName : accountSelector.getItems() ) {
                if ( !accountNames.add(accountName) ) {
                    return "Duplicate account name defined";
                }
            }
        }

        if ( accountNameFieldEditor != null ) {
            if ( accountNameFieldEditor.getStringValue().trim().length() == 0 ) {
                return "Account name must not be blank";
            }

            for ( Entry<String, String> entry : accountNameFieldEditor.valuesByAccountIdentifier.entrySet() ) {
                if ( !entry.getKey().equals(currentAccountId) )
                    if ( entry.getValue().length() == 0 )
                        return "Account name must not be blank";
            }
        }

        if ( certificateFieldEditor != null ) {
            if ( invalidFile(certificateFieldEditor.getStringValue()) ) {
                return "Certificate file does not exist";
            }

            for ( Entry<String, String> entry : certificateFieldEditor.valuesByAccountIdentifier.entrySet() ) {
                if ( !entry.getKey().equals(currentAccountId) )
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
                if ( !entry.getKey().equals(currentAccountId) )
                    if ( invalidFile(entry.getValue()) )
                        return "Private key file does not exist";
            }
        }

        return null;
    }

    protected boolean invalidFile(String certFile) {
        return certFile.trim().length() > 0
                && !new File(certFile).exists();
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
        gd.horizontalSpan = LAYOUT_COLUMN_WIDTH;
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
        newLink(webLinkListener, linkText, optionalConfigGroup);

        createSpacer(optionalConfigGroup);

        userIdFieldEditor = newStringFieldEditor(currentAccountId, PreferenceConstants.P_USER_ID, "Account &Number:",
                optionalConfigGroup);
        createFieldExampleLabel(optionalConfigGroup, "ex: 1111-2222-3333");

        certificateFieldEditor = newFileFieldEditor(currentAccountId, PreferenceConstants.P_CERTIFICATE_FILE,
                "&Certificate File:", optionalConfigGroup);
        certificatePrivateKeyFieldEditor = newFileFieldEditor(currentAccountId, PreferenceConstants.P_PRIVATE_KEY_FILE,
                "&Private Key File:", optionalConfigGroup);

        tweakLayout((GridLayout) optionalConfigGroup.getLayout());

        accountFieldEditors.add(userIdFieldEditor);
        accountFieldEditors.add(certificateFieldEditor);
        accountFieldEditors.add(certificatePrivateKeyFieldEditor);
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
        fieldEditor.setPage(this);
        fieldEditor.setPreferenceStore(this.getPreferenceStore());
        fieldEditor.load();

        // For backwards compatibility with single-account storage
        if ( accountNamesByIdentifier.get(currentAccount) != null
                && accountNamesByIdentifier.get(currentAccount).equals(PreferenceConstants.DEFAULT_ACCOUNT_NAME)
                && (fieldEditor.getStringValue() == null || fieldEditor.getStringValue().length() == 0) ) {
            String currentPrefValue = getPreferenceStore().getString(preferenceKey);
            if ( ObfuscatingStringFieldEditor.isBase64(currentPrefValue) ) {
                currentPrefValue = ObfuscatingStringFieldEditor.decodeString(currentPrefValue);
            }
            fieldEditor.setStringValue(currentPrefValue);
        }

        fieldEditor.fillIntoGrid(parent, LAYOUT_COLUMN_WIDTH);
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
}
