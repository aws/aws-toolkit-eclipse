/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;

/**
 * Reusable composite to select an AWS account, with a link to configure
 * accounts.
 */
public class AccountSelectionComposite extends Composite {

    /**
     * Combo control for users to select an account
     */
    private Combo accountSelection;

    private Label noAccounts;

    private List<SelectionListener> listeners = new LinkedList<>();

    /**
     * Adds a selection listener to the account selection field.
     */
    public void addSelectionListener(SelectionListener listner) {
        listeners.add(listner);
    }

    public AccountSelectionComposite(final Composite parent, final int style) {
        super(parent, style);
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        setLayout(new GridLayout(3, false));

        createChildWidgets();
        updateAccounts();
    }

    protected void createChildWidgets() {
        if ( !validAccountsConfigured() ) {
            createNoAccountLabel();
        } else {
            createAccountSelectionCombo();
        }

        createAccountConfigurationLink();
    }

    protected void createNoAccountLabel() {
        noAccounts = new Label(this, SWT.None);
        noAccounts.setText("No AWS accounts have been configured yet.");
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        noAccounts.setLayoutData(gd);
    }

    protected void createAccountConfigurationLink() {
        Link link = new Link(this, SWT.NONE);
        link.setFont(this.getFont());
        link.setText("<A>" + "Configure AWS profiles..." + "</A>");

        link.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
                        AwsAccountPreferencePage.ID, new String[] { AwsAccountPreferencePage.ID }, null).open();
                if ( noAccounts != null && validAccountsConfigured() ) {
                    for ( Widget w : getChildren() ) {
                        w.dispose();
                    }
                    noAccounts = null;
                    createChildWidgets();
                    getShell().layout(true, true);
                }
                updateAccounts();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });

        link.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        link.setEnabled(true);
    }

    protected void createAccountSelectionCombo() {
        Label selectAccount = new Label(this, SWT.None);
        selectAccount.setText("Select profile:");

        this.accountSelection = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);

        accountSelection.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                for ( SelectionListener listener : listeners ) {
                    listener.widgetSelected(e);
                }
            }
        });
    }

    /**
     * Returns whether there are valid aws accounts configured
     */
    private boolean validAccountsConfigured() {
        return AwsToolkitCore.getDefault().getAccountManager().validAccountsConfigured();
    }

    /**
     * Updates the list of accounts from the global store, preserving selection
     * when possible. Called automatically upon composite construction and after
     * the preference page link is clicked.
     */
    public void updateAccounts() {
        if ( accountSelection == null )
            return;

        String currentAccount = this.accountSelection.getText();

        Map<String, String> accounts = AwsToolkitCore.getDefault().getAccountManager().getAllAccountNames();
        List<String> accountNames = new ArrayList<>();
        accountNames.addAll(accounts.values());
        Collections.sort(accountNames);
        this.accountSelection.setItems(accountNames.toArray(new String[accountNames.size()]));

        for ( Entry<String, String> entry : accounts.entrySet() ) {
            this.accountSelection.setData(entry.getValue(), entry.getKey());
        }

        int selectedIndex = 0;
        if ( currentAccount != null ) {
            for ( int i = 0; i < this.accountSelection.getItemCount(); i++ ) {
                if ( currentAccount.equals(this.accountSelection.getItem(i)) ) {
                    selectedIndex = i;
                    break;
                }
            }
            if ( this.accountSelection.getItemCount() > 0 ) {
                this.accountSelection.select(selectedIndex);
            }
        } else {
            selectAccountId(AwsToolkitCore.getDefault().getCurrentAccountId());
        }

        getParent().layout(true, true);
    }

    /**
     * Selects the given account name (not id) in the list
     */
    public void selectAccountName(final String accountName) {
        if ( accountSelection == null )
            return;

        int selectedIndex = -1;
        if ( accountName != null ) {
            for ( int i = 0; i < this.accountSelection.getItemCount(); i++ ) {
                if ( accountName.equals(this.accountSelection.getItem(i)) ) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        if ( selectedIndex >= 0 && this.accountSelection.getItemCount() > 0 ) {
            this.accountSelection.select(selectedIndex);
        }
    }

    /**
     * Selects the given account id (not name) in the list
     */
    public void selectAccountId(final String accountId) {
        if ( accountSelection == null )
            return;

        int selectedIndex = -1;
        if ( accountId != null ) {
            for ( int i = 0; i < this.accountSelection.getItemCount(); i++ ) {
                if ( this.accountSelection.getData(this.accountSelection.getItem(i)).equals(accountId) ) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        if ( selectedIndex >= 0 && this.accountSelection.getItemCount() > 0 ) {
            this.accountSelection.select(selectedIndex);
        }
    }

    /**
     * Returns the id (not name) of the selected account. In the case that the
     * customer hasn't configured any accounts yet, this returns the default
     * account id.
     */
    public String getSelectedAccountId() {
        if ( !validAccountsConfigured() )
            return AwsToolkitCore.getDefault().getCurrentAccountId();
        return (String) this.accountSelection.getData(this.accountSelection.getText());
    }

    /**
     * Returns whether a correctly configured account is selected.
     */
    public boolean isValidAccountSelected() {
        AccountInfo info = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(getSelectedAccountId());
        return (info != null && info.isValid());
    }
}
