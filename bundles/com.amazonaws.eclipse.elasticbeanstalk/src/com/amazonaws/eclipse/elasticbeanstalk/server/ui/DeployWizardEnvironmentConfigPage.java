/*
- * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.ec2.databinding.ValidKeyPairValidator;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.NoInvalidNameCharactersValidator;

class DeployWizardEnvironmentConfigPage extends AbstractDeployWizardPage {

    private KeyPairComposite keyPairComposite;
    private Button usingCnameButton;
    private Text cname;
    private Button usingKeyPair;
    private Button incrementalDeploymentButton;
    private Text healthCheckText;
    private Text workerQueueUrlText;

    private ISWTObservableValue usingKeyPairObservable;
    private ISWTObservableValue usingCnameObservable;
    private ISWTObservableValue healthCheckURLObservable;
    private ISWTObservableValue sslCertObservable;
    private ISWTObservableValue snsTopicObservable;
    private ISWTObservableValue workerQueueUrlObservable;

    public DeployWizardEnvironmentConfigPage(DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        wizardHandle = handle;
        handle.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createKeyPairComposite(composite);
        createSSLCertControls(composite);
        createCNAMEControls(composite);
        createHealthCheckURLControls(composite);
        createQueueURLControls(composite);
        createSNSTopicControls(composite);
        newLabel(composite, "");
        createIncrementalDeploymentControls(composite);

        bindControls();
        initializeDefaults();
        return composite;
    }

    private void createQueueURLControls(final Composite parent) {
        newLabel(parent, "Worker Queue URL");

        workerQueueUrlText = newText(parent, "");
        workerQueueUrlObservable = SWTObservables.observeText(workerQueueUrlText, SWT.Modify);
    }

    @Override
    public void enter() {
        String environmentType = wizardDataModel.getEnvironmentType();

        // Health check isn't applicable for single-instance environments.
        if (ConfigurationOptionConstants.SINGLE_INSTANCE_ENV.equals(environmentType)) {
            healthCheckText.setText("");
            healthCheckText.setEnabled(false);
        } else {
            healthCheckText.setEnabled(true);
        }

        // CName isn't applicable for worker environments; worker queue is.
        if (ConfigurationOptionConstants.WORKER_ENV.equals(environmentType)) {
            usingCnameButton.setSelection(false);
            usingCnameButton.setEnabled(false);

            cname.setText("");
            cname.setEnabled(false);

            workerQueueUrlText.setEnabled(true);
        } else {
            usingCnameButton.setEnabled(true);
            cname.setEnabled(usingCnameButton.getSelection());

            workerQueueUrlText.setText("");
            workerQueueUrlText.setEnabled(false);
        }
    }

    private void createKeyPairComposite(Composite composite) {
        usingKeyPair = newCheckbox(composite, "Deploy with a key pair", 1);
        keyPairComposite = new KeyPairComposite(composite, AwsToolkitCore.getDefault().getCurrentAccountId(),
                wizardDataModel.getRegion());
        wizardDataModel.setKeyPairComposite(keyPairComposite);

        usingKeyPair.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                keyPairComposite.setEnabled(usingKeyPair.getSelection());
            }
        });

        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.heightHint = 140;
        keyPairComposite.setLayoutData(layoutData);
    }

    private void createSSLCertControls(Composite composite) {
        newLabel(composite, "SSL certificate Id");
        Text text = newText(composite, "");
        sslCertObservable = SWTObservables.observeText(text, SWT.Modify);
    }

    private void createCNAMEControls(Composite composite) {
        usingCnameButton = newCheckbox(composite, "Assign CNAME prefix to new server", 1);
        cname = newText(composite);

        usingCnameButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cname.setEnabled(usingCnameButton.getSelection());
            }
        });
    }

    private void createIncrementalDeploymentControls(Composite parent) {
        incrementalDeploymentButton = newCheckbox(parent, "Use incremental deployment", 1);
        incrementalDeploymentButton.setSelection(true);
    }

    private void createHealthCheckURLControls(Composite composite) {
        newLabel(composite, "Application health check URL");
        healthCheckText = newText(composite, "");
        healthCheckURLObservable = SWTObservables.observeText(healthCheckText, SWT.Modify);
    }

    private void createSNSTopicControls(Composite composite) {
        newLabel(composite, "Email address for notifications");
        Text text = newText(composite, "");
        snsTopicObservable = SWTObservables.observeText(text, SWT.Modify);
    }

    private void initializeDefaults() {
        usingCnameObservable.setValue(false);
        usingKeyPairObservable.setValue(false);
        keyPairComposite.setEnabled(false);
        cname.setEnabled(false);
        cname.setText("");
        sslCertObservable.setValue("");
        snsTopicObservable.setValue("");
        healthCheckURLObservable.setValue("");
        workerQueueUrlObservable.setValue("");

        // No change event is necessarily fired from the above updates, so we
        // fire one manually in order to display the appropriate button enablement
        changeListener.handleChange(null);
    }

    /**
     * Creates validation bindings for the controls on this page.
     */
    private void bindControls() {
        initializeValidators();

        // Key pair
        usingKeyPairObservable = SWTObservables.observeSelection(usingKeyPair);
        bindingContext.bindValue(usingKeyPairObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.USING_KEY_PAIR), null, null);
        IViewerObservableValue keyPairSelectionObservable = ViewersObservables.observeSingleSelection(keyPairComposite
                .getViewer());

        bindingContext.bindValue(keyPairSelectionObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.KEY_PAIR), null, null);
        ChainValidator<String> keyPairValidator = new ChainValidator<>(keyPairSelectionObservable,
                usingKeyPairObservable, new ValidKeyPairValidator(AwsToolkitCore.getDefault().getCurrentAccountId()));
        bindingContext.addValidationStatusProvider(keyPairValidator);

        usingCnameObservable = SWTObservables.observeSelection(usingCnameButton);
        bindingContext.bindValue(usingCnameObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.USING_CNAME), null, null)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeText(cname, SWT.Modify),
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.CNAME), null, null)
                .updateTargetToModel();

        // SSL cert
        bindingContext.bindValue(sslCertObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.SSL_CERTIFICATE_ID));

        // CNAME
        // TODO: make CNAME conform to exact spec, check for in-use
        ChainValidator<String> chainValidator = new ChainValidator<>(
                SWTObservables.observeText(cname, SWT.Modify), usingCnameObservable, new NotEmptyValidator(
                        "CNAME cannot be empty."), new NoInvalidNameCharactersValidator("Invalid characters in CNAME."));
        bindingContext.addValidationStatusProvider(chainValidator);
        ControlDecoration cnameDecoration = newControlDecoration(cname, "Enter a CNAME to launch your server");
        new DecorationChangeListener(cnameDecoration, chainValidator.getValidationStatus());

        // Health check URL
        bindingContext.bindValue(healthCheckURLObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.HEALTH_CHECK_URL));

        // SNS topic "email address"
        bindingContext.bindValue(snsTopicObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.SNS_ENDPOINT));

        // Incremental deployment
        bindingContext.bindValue(SWTObservables.observeSelection(incrementalDeploymentButton),
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.INCREMENTAL_DEPLOYMENT));

        // Worker Queue URL
        bindingContext.bindValue(workerQueueUrlObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.WORKER_QUEUE_URL));
    }

    @Override
    public String getPageTitle() {
        return "Advanced configuration";
    }

    @Override
    public String getPageDescription() {
        return "Specify advanced properties for your environment";
    }

}
