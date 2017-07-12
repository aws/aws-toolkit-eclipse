/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.identitymanagement.role;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.identitymanagement.databinding.DataBindingUtils;

public class CreateRoleSecondPage extends WizardPage {



    private final String[] services = {"Amazon EC2(Allows EC2 instances to call AWS services on your behalf.)",
                                       "AWS Data Pipeline(Allows Data Pipeline to call AWS Services on your behalf.)",
                                       "Amazon EC2 Role for Data Pipeline(Provides access to services for EC2 instances that are launched by Data Pipeline.)",
                                       "Amazon Elastic Transcoder(Allows Elastic Transcoder to call S3 and SNS on your behalf.)",
                                       "AWS OpsWorks(Allows OpsWorks to create and manage AWS resources on your behalf.)"};

    private final String[] IDENTITY_PROVIDERS = { "Facebook", "Google", "Login with Amazon"};

    private Button serviceRolesButton;
    private Button accountRolesButon;
    private Button thirdPartyRolesButton;
    private Button webFederationRolesButton;
    private Combo servicesCombo;
    private Text accountIdText;
    private Text internalAccountIdText;
    private Text externalAccountIdText;
    private Combo IdentityProvidersCombo;
    private Text applicationIdText;
    private Label applicationIdLabel;


    private IObservableValue service;
    private IObservableValue accountId;
    private IObservableValue internalAccountId;
    private IObservableValue externalAccountId;
    private IObservableValue serviceRoles;
    private IObservableValue accountRoles;
    private IObservableValue thirdPartyRoles;
    private IObservableValue webProviderRoles;
    private IObservableValue webProvider;
    private IObservableValue applicationId;


    private final String ConceptUrl = "http://docs.aws.amazon.com/IAM/latest/UserGuide/WorkingWithRoles.html";
    private final String webIdentityRoleHelpMessage = "Select the identity provider to trust and then enter your Application "
            + "ID or Audience as supplied by your identity provider. "
            + "Users logged into your application from this provider will be able to access resources from this AWS account.";

    CreateRoleWizard wizard;

    private final DataBindingContext bindingContext = new DataBindingContext();

    public CreateRoleSecondPage(CreateRoleWizard wizard) {
        super("");
        service = PojoObservables.observeValue(wizard.getDataModel(), "service");
        accountId = PojoObservables.observeValue(wizard.getDataModel(), "accountId");
        internalAccountId = PojoObservables.observeValue(wizard.getDataModel(), "internalAccountId");
        externalAccountId = PojoObservables.observeValue(wizard.getDataModel(), "externalAccountId");
        serviceRoles = PojoObservables.observeValue(wizard.getDataModel(), "serviceRoles");
        accountRoles = PojoObservables.observeValue(wizard.getDataModel(), "accountRoles");
        thirdPartyRoles = PojoObservables.observeValue(wizard.getDataModel(), "thirdPartyRoles");
        webProviderRoles = PojoObservables.observeValue(wizard.getDataModel(), "webProviderRoles");
        applicationId = PojoObservables.observeValue(wizard.getDataModel(), "applicationId");
        webProvider = PojoObservables.observeValue(wizard.getDataModel(), "webProvider");

        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        GridLayout layout = new GridLayout(1, false);
        layout.marginLeft = 5;
        composite.setLayout(layout);
        createServiceRoleControl(composite);
        createAccountRoleControl(composite);
        createThirdPartyControl(composite);
        createWebIdentityProviderControl(composite);
        CreateHelpLinkControl(composite);

        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false)
                    return;

                IStatus status = (IStatus) value;
                if (status.isOK()) {
                    setErrorMessage(null);
                } else if (status.getSeverity() == Status.WARNING) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                }

                setPageComplete(status.isOK());
            }
        });

        setPageComplete(false);
        setControl(composite);
    }

    private void createServiceRoleControl(Composite comp) {
        serviceRolesButton = new Button(comp, SWT.RADIO);
        serviceRolesButton.setText("AWS Service Roles");
        serviceRolesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                servicesCombo.setEnabled(true);
                accountIdText.setEnabled(false);
                internalAccountIdText.setEnabled(false);
                externalAccountIdText.setEnabled(false);
                IdentityProvidersCombo.setEnabled(false);
                applicationIdText.setEnabled(false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        bindingContext.bindValue(SWTObservables.observeSelection(serviceRolesButton), serviceRoles);
        servicesCombo = new Combo(comp, SWT.NONE);
        for (String service : services) {
            servicesCombo.add(service);
        }

        servicesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        servicesCombo.setEnabled(false);
        bindingContext.bindValue(SWTObservables.observeSelection(servicesCombo), service);

        ChainValidator<String> serviceRoleValidationStatusProvider = new ChainValidator<>(service,
                serviceRoles, new NotEmptyValidator("Please select a service"));

         bindingContext.addValidationStatusProvider(serviceRoleValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(servicesCombo, serviceRoleValidationStatusProvider);
    }

    private void createAccountRoleControl(Composite comp) {
        accountRolesButon = new Button(comp, SWT.RADIO);
        accountRolesButon.setText("Provide access between AWS accounts you own");
        accountRolesButon.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                servicesCombo.setEnabled(false);
                accountIdText.setEnabled(true);
                internalAccountIdText.setEnabled(false);
                externalAccountIdText.setEnabled(false);
                IdentityProvidersCombo.setEnabled(false);
                applicationIdText.setEnabled(false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        bindingContext.bindValue(SWTObservables.observeSelection(accountRolesButon), accountRoles);

        new Label(comp, SWT.NONE).setText("Account Id:");
        accountIdText = new Text(comp, SWT.BORDER);
        accountIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        accountIdText.setEnabled(false);

        bindingContext.bindValue(SWTObservables.observeText(accountIdText, SWT.Modify), accountId);

        ChainValidator<String> accountIdValidationStatusProvider = new ChainValidator<>(accountId,
                accountRoles, new NotEmptyValidator("Please enter your account Id"));

         bindingContext.addValidationStatusProvider(accountIdValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(accountIdText, accountIdValidationStatusProvider);
    }

    private void createThirdPartyControl(Composite comp) {
        thirdPartyRolesButton = new Button(comp, SWT.RADIO);
        thirdPartyRolesButton.setText("Provide access to a 3rd party AWS account");
        thirdPartyRolesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                servicesCombo.setEnabled(false);
                accountIdText.setEnabled(false);
                IdentityProvidersCombo.setEnabled(false);
                applicationIdText.setEnabled(false);
                internalAccountIdText.setEnabled(true);
                externalAccountIdText.setEnabled(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        bindingContext.bindValue(SWTObservables.observeSelection(thirdPartyRolesButton), thirdPartyRoles);
        new Label(comp, SWT.NONE).setText("Account Id:");
        internalAccountIdText = new Text(comp, SWT.BORDER);
        internalAccountIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        internalAccountIdText.setEnabled(false);

        bindingContext.bindValue(SWTObservables.observeText(internalAccountIdText, SWT.Modify), internalAccountId);

        ChainValidator<String> internalAccountIdValidationStatusProvider = new ChainValidator<>(internalAccountId,
                thirdPartyRoles, new NotEmptyValidator("Please enter the internal account Id"));

         bindingContext.addValidationStatusProvider(internalAccountIdValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(internalAccountIdText, internalAccountIdValidationStatusProvider);

        new Label(comp, SWT.NONE).setText("External Id:");;

        externalAccountIdText = new Text(comp, SWT.BORDER);
        externalAccountIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        externalAccountIdText.setEnabled(false);

        bindingContext.bindValue(SWTObservables.observeText(externalAccountIdText, SWT.Modify), externalAccountId);

        ChainValidator<String> externalAccountIdValidationStatusProvider = new ChainValidator<>(externalAccountId,
                thirdPartyRoles, new NotEmptyValidator("Please enter the external account Id"));

         bindingContext.addValidationStatusProvider(externalAccountIdValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(externalAccountIdText, externalAccountIdValidationStatusProvider);

    }

    private void createWebIdentityProviderControl(Composite comp) {
        webFederationRolesButton = new Button(comp, SWT.RADIO);
        webFederationRolesButton.setText("Provide access to web identity providers");
        webFederationRolesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                servicesCombo.setEnabled(false);
                accountIdText.setEnabled(false);
                internalAccountIdText.setEnabled(false);
                externalAccountIdText.setEnabled(false);
                IdentityProvidersCombo.setEnabled(true);
                applicationIdText.setEnabled(true);

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        Label label = new Label(comp, SWT.NONE | SWT.WRAP);
        label.setText(webIdentityRoleHelpMessage);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        label.setLayoutData(gridData);

        bindingContext.bindValue(SWTObservables.observeSelection(webFederationRolesButton), webProviderRoles);
        new Label(comp, SWT.NONE).setText("Identity Provider");
        IdentityProvidersCombo = new Combo(comp, SWT.BORDER);
        for (String provider : IDENTITY_PROVIDERS) {
            IdentityProvidersCombo.add(provider);
        }


        IdentityProvidersCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (IDENTITY_PROVIDERS[IdentityProvidersCombo.getSelectionIndex()].equals("Google")) {
                    applicationIdLabel.setText("Audience");
                } else {
                    applicationIdLabel.setText("Application Id");
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        IdentityProvidersCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        IdentityProvidersCombo.setEnabled(false);

        bindingContext.bindValue(SWTObservables.observeSelection(IdentityProvidersCombo), webProvider);

        ChainValidator<String> webProviderValidationStatusProvider = new ChainValidator<>(webProvider, webProviderRoles, new NotEmptyValidator(
                "Please select an identity provider"));

        bindingContext.addValidationStatusProvider(webProviderValidationStatusProvider);

        IdentityProvidersCombo.setText("Facebook");

        applicationIdLabel = new Label(comp, SWT.NONE);
        applicationIdLabel.setText("Application Id");

        applicationIdText = new Text(comp, SWT.BORDER);
        applicationIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        applicationIdText.setEnabled(false);

        bindingContext.bindValue(SWTObservables.observeText(applicationIdText, SWT.Modify), applicationId);

        ChainValidator<String> applicationIdValidationStatusProvider = new ChainValidator<>(applicationId, webProviderRoles, new NotEmptyValidator(
                "Please enter application Id or Audience"));

         bindingContext.addValidationStatusProvider(applicationIdValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(applicationIdText, applicationIdValidationStatusProvider);

    }

    private void CreateHelpLinkControl(Composite comp) {
        Link link = new Link(comp, SWT.NONE | SWT.WRAP);
        link.setText("\n\nFor more information about IAM roles, see " +
                "<a href=\"" +
                ConceptUrl + "\">Delegating API access by using roles</a> in the using IAM guide.");

        link.addListener(SWT.Selection, new WebLinkListener());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
    }
}
