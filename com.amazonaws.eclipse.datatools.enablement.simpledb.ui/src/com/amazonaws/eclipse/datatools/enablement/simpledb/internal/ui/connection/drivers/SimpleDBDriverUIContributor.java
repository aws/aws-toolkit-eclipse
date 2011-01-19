/*
 * Copyright 2008-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.connection.drivers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributorInformation;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.ISimpleDBConnectionProfileConstants;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.SimpleDBConnectionUtils;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.Messages;

public class SimpleDBDriverUIContributor implements IDriverUIContributor, Listener {

    private static final String DATABASE_LABEL = Messages.database;

    private static final String CUI_NEWCW_ENDPOINT_LBL_UI_ = Messages.CUI_NEWCW_ENDPOINT_LBL_UI_;

    private static final String CUI_NEWCW_IDENTITY_LBL_UI_ = Messages.CUI_NEWCW_IDENTITY_LBL_UI_;

    private static final String CUI_NEWCW_SECRET_LBL_UI_ = Messages.CUI_NEWCW_SECRET_LBL_UI_;

    private static final String CUI_NEWCW_SAVE_SECRET_LBL_UI_ = Messages.CUI_NEWCW_SAVE_SECRET_LBL_UI_;

    private static final String CUI_NEWCW_IDENTITY_SUMMARY_DATA_TEXT_ = Messages.CUI_NEWCW_IDENTITY_SUMMARY_DATA_TEXT_;

    private static final String CUI_NEWCW_SAVE_SECRET_SUMMARY_DATA_TEXT_ = Messages.CUI_NEWCW_SAVE_SECRET_SUMMARY_DATA_TEXT_;

    private static final String CUI_NEWCW_TRUE_SUMMARY_DATA_TEXT_ = Messages.CUI_NEWCW_TRUE_SUMMARY_DATA_TEXT_;

    private static final String CUI_NEWCW_FALSE_SUMMARY_DATA_TEXT_ = Messages.CUI_NEWCW_FALSE_SUMMARY_DATA_TEXT_;

    private static final String CUI_NEWCW_PROFILE_SPECIFIC = Messages.CUI_NEWCW_PROFILE_SPECIFIC;

    private static final String CUI_NEWCW_GLOBAL_SETTINGS = Messages.CUI_NEWCW_GLOBAL_SETTINGS;

    public static final String GLOBAL_PAGE_ID = "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage"; //$NON-NLS-1$

    /**
     * * Name of resource property for the selection of workbench or project settings **
     */
    public static final String USE_PROJECT_SETTINGS = "useProjectSettings"; //$NON-NLS-1$

    /**
     * Ckeckbox for enable project settings
     */
    private Button useProfileSettings;

    /**
     * Links that opens workspace settings case the page is a property
     */
    private Link globalSettings;

    protected IDriverUIContributorInformation contributorInformation;

    private Label identityLabel;

    private Text identityText;

    private Label secretLabel;

    private Text secretText;

    private Button saveSecretButton;

    private DialogPage parentPage;

    private ScrolledComposite parentComposite;

    private Properties properties;

    private boolean isReadOnly = false;

    /** Combo control for users to select the SimpleDB endpoint */
    private Combo endpointCombo;

    /**
     * SimpleDB connection utils, listing endpoints, filling in missing required
     * properties, etc.
     */
    private SimpleDBConnectionUtils simpleDBConnectionUtils = new SimpleDBConnectionUtils();

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#determineContributorCompletion()
     */
    public boolean determineContributorCompletion() {
        boolean isComplete = true;
        if (specificSettingsEnabled()) {
            if (this.identityText.getText().trim().length() < 1) {
                this.parentPage.setErrorMessage(Messages.CUI_NEWCW_VALIDATE_IDENTITY_REQ_UI_);
                isComplete = false;
            } else if (this.secretText.getText().trim().length() < 1) {
                this.parentPage.setErrorMessage(Messages.CUI_NEWCW_VALIDATE_SECRET_REQ_UI_);
                isComplete = false;
            }
        }
        return isComplete;
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#getContributedDriverUI(org.eclipse.swt.widgets.Composite, boolean)
     */
    public Composite getContributedDriverUI(final Composite parent, final boolean isReadOnly) {

        if ((this.parentComposite == null) || this.parentComposite.isDisposed() || (this.isReadOnly != isReadOnly)) {
            GridData gd;

            this.isReadOnly = isReadOnly;
            int additionalStyles = SWT.NONE;
            if (isReadOnly) {
                additionalStyles = SWT.READ_ONLY;
            }

            this.parentComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
            this.parentComposite.setExpandHorizontal(true);
            this.parentComposite.setExpandVertical(true);
            this.parentComposite.setLayout(new GridLayout());

            Composite baseComposite = new Composite(this.parentComposite, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            baseComposite.setLayout(layout);

            Label endpointLabel = new Label(baseComposite, SWT.NONE);
            endpointLabel.setText(CUI_NEWCW_ENDPOINT_LBL_UI_);
            gd = new GridData();
            gd.verticalAlignment = GridData.CENTER;
            gd.horizontalSpan = 1;
            endpointLabel.setLayoutData(gd);

            this.endpointCombo = new Combo(baseComposite, SWT.READ_ONLY);
            gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.verticalAlignment = GridData.BEGINNING;
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            this.endpointCombo.setLayoutData(gd);
            Map<String, String> availableEndpointsByRegionName = this.simpleDBConnectionUtils.getAvailableEndpoints();
            for (String regionName : availableEndpointsByRegionName.keySet()) {
                String endpoint = availableEndpointsByRegionName.get(regionName);

                String text = regionName;

                this.endpointCombo.add(text);
                this.endpointCombo.setData(text, endpoint);
            }

            Composite header = createHeader(baseComposite);
            gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.verticalAlignment = GridData.BEGINNING;
            gd.verticalIndent = 10;
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 3;
            header.setLayoutData(gd);

            this.identityLabel = new Label(baseComposite, SWT.NONE);
            this.identityLabel.setText(CUI_NEWCW_IDENTITY_LBL_UI_);
            gd = new GridData();
            gd.verticalAlignment = GridData.CENTER;
            this.identityLabel.setLayoutData(gd);

            this.identityText = new Text(baseComposite, SWT.SINGLE | SWT.BORDER | additionalStyles);
            gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.verticalAlignment = GridData.CENTER;
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            this.identityText.setLayoutData(gd);

            this.secretLabel = new Label(baseComposite, SWT.NONE);
            this.secretLabel.setText(CUI_NEWCW_SECRET_LBL_UI_);
            gd = new GridData();
            gd.verticalAlignment = GridData.CENTER;
            this.secretLabel.setLayoutData(gd);

            this.secretText = new Text(baseComposite, SWT.SINGLE | SWT.BORDER
                    /* | SWT.PASSWORD */| additionalStyles);
            gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.verticalAlignment = GridData.CENTER;
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            this.secretText.setLayoutData(gd);

            this.saveSecretButton = new Button(baseComposite, SWT.CHECK);
            this.saveSecretButton.setText(CUI_NEWCW_SAVE_SECRET_LBL_UI_);
            gd = new GridData();
            gd.horizontalAlignment = GridData.FILL;
            gd.verticalAlignment = GridData.BEGINNING;
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            this.saveSecretButton.setLayoutData(gd);

            this.parentComposite.setContent(baseComposite);
            this.parentComposite.setMinSize(baseComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            initialize();
        }
        return this.parentComposite;
    }

    private Composite createHeader(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        this.useProfileSettings = new Button(composite, SWT.CHECK);

        this.useProfileSettings.setFont(composite.getFont());
        this.useProfileSettings.setText(CUI_NEWCW_PROFILE_SPECIFIC);

        this.useProfileSettings.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(final SelectionEvent e) {
                updateFieldEditors();
            }

            public void widgetSelected(final SelectionEvent e) {
                updateFieldEditors();
            }
        });

        this.globalSettings = createLink(composite, CUI_NEWCW_GLOBAL_SETTINGS);
        this.globalSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        this.globalSettings.setEnabled(true);

        Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        horizontalLine.setFont(composite.getFont());

        return composite;
    }

    private Link createLink(final Composite composite, final String text) {
        Link link = new Link(composite, SWT.NONE);
        link.setFont(composite.getFont());
        link.setText("<A>" + text + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(new SelectionListener() {
            public void widgetSelected(final SelectionEvent e) {
                doLinkActivated((Link) e.widget);
            }

            public void widgetDefaultSelected(final SelectionEvent e) {
                doLinkActivated((Link) e.widget);
            }
        });
        return link;
    }

    /**
     * Activate the link to open workspace settings
     * 
     * @param link
     */
    final void doLinkActivated(final Link link) {
        PreferencesUtil.createPreferenceDialogOn(((IDialogPage) this.contributorInformation).getControl().getShell(),
                GLOBAL_PAGE_ID, new String[] { GLOBAL_PAGE_ID }, null).open();
    }

    protected boolean specificSettingsEnabled() {
        return this.useProfileSettings.getSelection();
    }

    /**
     * Enables or disables the field editors and buttons of this page.
     */
    protected void updateFieldEditors() {
        boolean enable = specificSettingsEnabled();

        this.identityLabel.setEnabled(enable);
        this.identityText.setEnabled(enable);
        this.secretLabel.setEnabled(enable);
        this.secretText.setEnabled(enable);
        this.saveSecretButton.setEnabled(enable);
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#getSummaryData()
     */
    public List<String[]> getSummaryData() {
        List<String[]> summaryData = new ArrayList<String[]>();

        summaryData.add(new String[] { CUI_NEWCW_IDENTITY_SUMMARY_DATA_TEXT_, this.identityText.getText().trim() });
        summaryData
        .add(new String[] {
                CUI_NEWCW_SAVE_SECRET_SUMMARY_DATA_TEXT_,
                this.saveSecretButton.getSelection() ? CUI_NEWCW_TRUE_SUMMARY_DATA_TEXT_
                        : CUI_NEWCW_FALSE_SUMMARY_DATA_TEXT_ });
        return summaryData;
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#loadProperties()
     */
    public void loadProperties() {
        // Ensure that all required properties are present
        this.simpleDBConnectionUtils.initializeMissingProperties(this.properties);

        removeListeners();

        String useGlobal = this.properties.getProperty(ISimpleDBConnectionProfileConstants.USE_GLOBAL_SETTINGS);
        if (useGlobal != null) {
            this.useProfileSettings.setSelection(!Boolean.valueOf(useGlobal).booleanValue());
        }

        String username = this.properties.getProperty(IJDBCDriverDefinitionConstants.USERNAME_PROP_ID);
        if (username != null) {
            this.identityText.setText(username);
        }

        String password = this.properties.getProperty(IJDBCDriverDefinitionConstants.PASSWORD_PROP_ID);
        if (password != null) {
            this.secretText.setText(password);
        }

        String savePassword = this.properties.getProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID);
        if (savePassword != null) {
            this.saveSecretButton.setSelection(Boolean.valueOf(savePassword).booleanValue());
        }

        String endpoint = this.properties.getProperty(ISimpleDBConnectionProfileConstants.ENDPOINT);
        if (endpoint != null) {
            for (int i = 0; i < this.endpointCombo.getItemCount(); i++) {
                String availableEndpoint = (String)this.endpointCombo.getData(this.endpointCombo.getItem(i));

                if (endpoint.equals(availableEndpoint)) {
                    this.endpointCombo.select(i);
                }
            }
        }

        initialize();
        setConnectionInformation();
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#setDialogPage(org.eclipse.jface.dialogs.DialogPage)
     */
    public void setDialogPage(final DialogPage parentPage) {
        this.parentPage = parentPage;
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#setDriverUIContributorInformation(org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributorInformation)
     */
    public void setDriverUIContributorInformation(final IDriverUIContributorInformation contributorInformation) {
        this.contributorInformation = contributorInformation;
        this.properties = contributorInformation.getProperties();
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void handleEvent(final Event event) {
        if (this.isReadOnly) {
            if (event.widget == this.saveSecretButton) {
                this.saveSecretButton.setSelection(!this.saveSecretButton.getSelection());
            }
        } else {
            setConnectionInformation();
        }
    }

    private void setConnectionInformation() {
        this.properties.setProperty(IJDBCDriverDefinitionConstants.URL_PROP_ID, "jdbc:simpledb"); // avoids DTP asserts //$NON-NLS-1$

        this.properties.setProperty(IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID, DATABASE_LABEL);

        this.properties.setProperty(ISimpleDBConnectionProfileConstants.USE_GLOBAL_SETTINGS, String
                .valueOf(!specificSettingsEnabled()));

        this.properties.setProperty(IJDBCDriverDefinitionConstants.PASSWORD_PROP_ID, this.secretText.getText());
        this.properties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String
                .valueOf(this.saveSecretButton.getSelection()));
        this.properties.setProperty(IJDBCDriverDefinitionConstants.USERNAME_PROP_ID, this.identityText.getText());

        String endpoint = (String)this.endpointCombo.getData(this.endpointCombo.getText());
        this.properties.setProperty(ISimpleDBConnectionProfileConstants.ENDPOINT, endpoint);

        this.contributorInformation.setProperties(this.properties);
    }

    private void initialize() {
        addListeners();
        updateFieldEditors();
    }

    private void addListeners() {
        this.useProfileSettings.addListener(SWT.Selection, this);
        this.identityText.addListener(SWT.Modify, this);
        this.secretText.addListener(SWT.Modify, this);
        this.saveSecretButton.addListener(SWT.Selection, this);
        this.endpointCombo.addListener(SWT.Selection, this);
    }

    private void removeListeners() {
        this.useProfileSettings.removeListener(SWT.Selection, this);
        this.identityText.removeListener(SWT.Modify, this);
        this.secretText.removeListener(SWT.Modify, this);
        this.saveSecretButton.removeListener(SWT.Selection, this);
        this.endpointCombo.removeListener(SWT.Selection, this);
    }

}
