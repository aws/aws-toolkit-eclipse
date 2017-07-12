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
package com.amazonaws.eclipse.core.ui.setupwizard;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesFactory;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;

final class ConfigureAccountWizardPage extends WizardPage {

    private static final String GETTING_STARTED_GUIDE_URL = "http://docs.aws.amazon.com/AWSToolkitEclipse/latest/GettingStartedGuide/Welcome.html";
    private static final String CREATE_ACCOUNT_URL = "https://portal.aws.amazon.com/gp/aws/developer/registration/index.html?ie=UTF8&utm_source=eclipse&utm_campaign=awstoolkitforeclipse&utm_medium=ide&";
    private static final String SECURITY_CREDENTIALS_URL = "https://portal.aws.amazon.com/gp/aws/securityCredentials";

    private final InitialSetupWizardDataModel dataModel;
    private final IPreferenceStore preferenceStore;

    private final DataBindingContext bindingContext = new DataBindingContext();

    // Finally provide aggregate status reporting for the entire wizard page
    private final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(
        bindingContext, AggregateValidationStatus.MAX_SEVERITY);

    private Button openExplorerCheckBox;


    ConfigureAccountWizardPage(InitialSetupWizardDataModel dataModel,
            IPreferenceStore preferenceStore) {
        super("initialSetupWizard");
        this.dataModel = dataModel;
        this.preferenceStore = preferenceStore;
        setTitle("Welcome to the AWS Toolkit for Eclipse");
        setDescription("Configure the toolkit with your AWS account credentials");
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        setControl(composite);

        WebLinkListener linkListener = new WebLinkListener();
        GridDataFactory fullRowGridDataFactory = GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).grab(true, false).span(2, 1);
        GridDataFactory firstColumnGridDataFactory  = GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER);
        GridDataFactory secondColumnGridDataFactory = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false);

        Label label = new Label(composite, SWT.WRAP);
        label.setText("Before you can start using the toolkit, you need to configure an AWS account.");
        fullRowGridDataFactory.applyTo(label);

        Link link = new Link(composite, SWT.WRAP);
        link.addListener(SWT.Selection, linkListener);
        link.setText("Use your <a href=\"" + SECURITY_CREDENTIALS_URL + "\">existing credentials</a> or " +
                "<a href=\"" + CREATE_ACCOUNT_URL + "\">create a new AWS account</a>.");
        fullRowGridDataFactory.applyTo(link);


        // AWS Access Key ID row
        Label accessKeyLabel = new Label(composite, SWT.NONE);
        accessKeyLabel.setText("Access Key ID:");
        firstColumnGridDataFactory.copy().indent(10, 5).applyTo(accessKeyLabel);
        Text accessKeyText = new Text(composite, SWT.BORDER);
        secondColumnGridDataFactory.copy().indent(0, 5).applyTo(accessKeyText);
        accessKeyText.setFocus();

        IObservableValue accessKeyModelObservable = PojoObservables.observeValue(dataModel, dataModel.ACCESS_KEY_ID);
        bindingContext.bindValue(SWTObservables.observeText(accessKeyText, SWT.Modify), accessKeyModelObservable);
        bindingContext.addValidationStatusProvider(
            new ChainValidator<String>(accessKeyModelObservable, new NotEmptyValidator("Please provide an AWS Access Key ID")));


        // AWS Secret Key row
        Label secretKeyLabel = new Label(composite, SWT.NONE);
        secretKeyLabel.setText("Secret Access Key:");
        firstColumnGridDataFactory.copy().indent(10, 0).applyTo(secretKeyLabel);
        Text secretKeyText = new Text(composite, SWT.BORDER);
        secondColumnGridDataFactory.applyTo(secretKeyText);

        IObservableValue secretKeyModelObservable = PojoObservables.observeValue(dataModel, dataModel.SECRET_ACCESS_KEY);
        bindingContext.bindValue(SWTObservables.observeText(secretKeyText, SWT.Modify), secretKeyModelObservable);
        bindingContext.addValidationStatusProvider(
            new ChainValidator<String>(secretKeyModelObservable, new NotEmptyValidator("Please provide an AWS Secret Access Key")));


        // Open Explorer view row
        openExplorerCheckBox = new Button(composite, SWT.CHECK);
        openExplorerCheckBox.setText("Open the AWS Explorer view");
        openExplorerCheckBox.setSelection(true);
        fullRowGridDataFactory.indent(0, 5).applyTo(openExplorerCheckBox);
        bindingContext.bindValue(SWTObservables.observeSelection(openExplorerCheckBox), PojoObservables.observeValue(dataModel, dataModel.OPEN_EXPLORER));


        Composite spacer = new Composite(composite, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));

        link = new Link(composite, SWT.WRAP);
        link.addListener(SWT.Selection, linkListener);
        link.setText("For a full walkthrough of the features available in the AWS Toolkit for Eclipse, " +
                "see the <a href=\"" + GETTING_STARTED_GUIDE_URL + "\">AWS Toolkit for Eclipse Getting Started Guide</a>.");
        fullRowGridDataFactory.applyTo(link);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false ) return;

                IStatus status = (IStatus)value;
                setPageComplete(status.isOK());
            }
        });

        setPageComplete(false);
        parent.getShell().pack(true);

        Rectangle workbenchBounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
        Point dialogSize = this.getShell().getSize();

        this.getShell().setLocation(
          workbenchBounds.x + (workbenchBounds.width - dialogSize.x) / 2,
          workbenchBounds.y + (workbenchBounds.height - dialogSize.y) / 2);
    }

    public boolean performFinish() {
        String internalAccountId = UUID.randomUUID().toString();
        saveToCredentialsFile(internalAccountId);
        preferenceStore.setValue(PreferenceConstants.P_CURRENT_ACCOUNT, internalAccountId);
        preferenceStore.setValue(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT, internalAccountId);
        if (preferenceStore instanceof IPersistentPreferenceStore) {
            IPersistentPreferenceStore persistentPreferenceStore = (IPersistentPreferenceStore)preferenceStore;
            try {
                persistentPreferenceStore.save();
            } catch (IOException e) {
                AwsToolkitCore.getDefault().logError("Unable to write the account information to disk", e);
            }
        }
        AwsToolkitCore.getDefault().getAccountManager().reloadAccountInfo();

        if (dataModel.isOpenExplorer()) {
            openAwsExplorer();
        }

        return true;
    }

    /**
     * Persist the credentials entered in the wizard to the AWS credentials file
     * @param internalAccountId - Newly generated UUID to identify the account in eclipse
     */
    private void saveToCredentialsFile(String internalAccountId) {
        BasicProfile emptyProfile = SdkProfilesFactory.newEmptyBasicProfile(PreferenceConstants.DEFAULT_ACCOUNT_NAME);
        SdkProfilesCredentialsConfiguration credentialsConfig = new SdkProfilesCredentialsConfiguration(
                preferenceStore, internalAccountId, emptyProfile);
        credentialsConfig.setAccessKey(dataModel.getAccessKeyId());
        credentialsConfig.setSecretKey(dataModel.getSecretAccessKey());

        try {
            credentialsConfig.save();
        } catch (AmazonClientException e) {
            AwsToolkitCore.getDefault().reportException("Could not write profile information to the credentials file", e);
        }
    }

    private void openAwsExplorer() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(AwsToolkitCore.EXPLORER_VIEW_ID);
                } catch (PartInitException e) {
                    AwsToolkitCore.getDefault().reportException("Unable to open the AWS Explorer view", e);
                }
            }
        });
    }
}