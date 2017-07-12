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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.connection.drivers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributorInformation;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.ISimpleDBConnectionProfileConstants;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.SimpleDBConnectionUtils;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.Messages;

public class SimpleDBDriverUIContributor implements IDriverUIContributor, Listener {

    private static final String DATABASE_LABEL = Messages.database;

    /**
     * * Name of resource property for the selection of workbench or project settings **
     */
    public static final String USE_PROJECT_SETTINGS = "useProjectSettings"; //$NON-NLS-1$

    protected IDriverUIContributorInformation contributorInformation;

    private ScrolledComposite parentComposite;

    private Properties properties;

    private boolean isReadOnly = false;

    /** Combo control for users to select the SimpleDB endpoint */
    private Combo endpointCombo;

    private AccountSelectionComposite accountSelection;

    /**
     * SimpleDB connection utils, listing endpoints, filling in missing required
     * properties, etc.
     */
    private SimpleDBConnectionUtils simpleDBConnectionUtils = new SimpleDBConnectionUtils();

    private DialogPage parentPage;

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#determineContributorCompletion()
     */
    @Override
    public boolean determineContributorCompletion() {
        return accountValid();
    }

    protected boolean accountValid() {
        String accountId = this.accountSelection.getSelectedAccountId();
        return AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(accountId).isValid();
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#getContributedDriverUI(org.eclipse.swt.widgets.Composite, boolean)
     */
    @Override
    public Composite getContributedDriverUI(final Composite parent, final boolean isReadOnly) {

        if ((this.parentComposite == null) || this.parentComposite.isDisposed() || (this.isReadOnly != isReadOnly)) {
            GridData gd;

            this.isReadOnly = isReadOnly;

            this.parentComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
            this.parentComposite.setExpandHorizontal(true);
            this.parentComposite.setExpandVertical(true);
            this.parentComposite.setLayout(new GridLayout());

            Composite baseComposite = new Composite(this.parentComposite, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            baseComposite.setLayout(layout);

            Label endpointLabel = new Label(baseComposite, SWT.NONE);
            endpointLabel.setText(Messages.CUI_NEWCW_ENDPOINT_LBL_UI_);
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

            this.parentComposite.setContent(baseComposite);
            this.parentComposite.setMinSize(baseComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

            initialize();
        }

        return this.parentComposite;
    }

    private Composite createHeader(final Composite parent) {
        this.accountSelection = new AccountSelectionComposite(parent, SWT.NONE);
        this.accountSelection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return this.accountSelection;
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#getSummaryData()
     */
    @Override
    public List<String[]> getSummaryData() {
        return Collections.emptyList();
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#loadProperties()
     */
    @Override
    public void loadProperties() {
        // Ensure that all required properties are present
        this.simpleDBConnectionUtils.initializeMissingProperties(this.properties);

        removeListeners();

        String accountId = this.properties.getProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID);
        Map<String, String> accounts = AwsToolkitCore.getDefault().getAccountManager().getAllAccountNames();
        String accountName = accounts.get(accountId);
        this.accountSelection.selectAccountName(accountName);

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
    @Override
    public void setDialogPage(final DialogPage parentPage) {
        this.parentPage = parentPage;
        updateErrorMessage();
    }

    /**
     * @see org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor#setDriverUIContributorInformation(org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributorInformation)
     */
    @Override
    public void setDriverUIContributorInformation(final IDriverUIContributorInformation contributorInformation) {
        this.contributorInformation = contributorInformation;
        this.properties = contributorInformation.getProperties();
    }

    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if (!this.isReadOnly) {
            setConnectionInformation();
            updateErrorMessage();
        }
    }

    protected void updateErrorMessage() {
        if ( this.parentPage != null && !this.parentPage.getControl().isDisposed()) {
            if ( !this.accountValid() ) {
                this.parentPage.setErrorMessage("Selected account is not correctly configured");
            } else {
                this.parentPage.setErrorMessage(null);
            }
        }
    }

    private void setConnectionInformation() {
        this.properties.setProperty(IJDBCDriverDefinitionConstants.URL_PROP_ID, "jdbc:simpledb"); // avoids DTP asserts //$NON-NLS-1$

        this.properties.setProperty(IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID, DATABASE_LABEL);

        String endpoint = (String)this.endpointCombo.getData(this.endpointCombo.getText());
        this.properties.setProperty(ISimpleDBConnectionProfileConstants.ENDPOINT, endpoint);
        String accountId = this.accountSelection.getSelectedAccountId();
        this.properties.setProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID, accountId);

        this.contributorInformation.setProperties(this.properties);
    }

    private void initialize() {
        addListeners();
    }

    private void addListeners() {
        this.endpointCombo.addListener(SWT.Selection, this);
        this.accountSelection.addListener(SWT.Selection, this);
    }

    private void removeListeners() {
        this.endpointCombo.removeListener(SWT.Selection, this);
    }

}
