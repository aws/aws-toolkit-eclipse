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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.eclipse.identitymanagement.databinding.DataBindingUtils;

public class CreateRoleThirdPage extends WizardPage {

    private Text policyDocText;
    private Text policyNameText;
    private Button grantPermissionButton;
    private final String ConceptUrl = "http://docs.aws.amazon.com/IAM/latest/UserGuide/AccessPolicyLanguage_KeyConcepts.html";
    private final DataBindingContext bindingContext = new DataBindingContext();
    private CreateRoleWizard wizard;

    private IObservableValue policyName;
    private IObservableValue policyDoc;
    private IObservableValue grantPermission;

    private final static String OK_MESSAGE = "You can customize permissions by editing the following policy document.";

    public CreateRoleThirdPage(CreateRoleWizard wizard) {
        super(OK_MESSAGE);
        setMessage(OK_MESSAGE);
        policyName = PojoObservables.observeValue(wizard.getDataModel(), "policyName");
        policyDoc = PojoObservables.observeValue(wizard.getDataModel(), "policyDoc");
        grantPermission = PojoObservables.observeValue(wizard.getDataModel(), "grantPermission");
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginLeft = 5;
        composite.setLayout(layout);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

        grantPermissionButton = new Button(composite, SWT.CHECK);
        grantPermissionButton.setText("Grant permissions");
        grantPermissionButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (grantPermissionButton.getSelection()) {
                    policyNameText.setEnabled(true);
                    policyDocText.setEnabled(true);
                } else {
                    policyNameText.setEnabled(false);
                    policyDocText.setEnabled(false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        bindingContext.bindValue(SWTObservables.observeSelection(grantPermissionButton), grantPermission);

        new Label(composite, SWT.NONE).setText("Policy Name:");

        policyNameText = new Text(composite, SWT.BORDER);
        policyNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        bindingContext.bindValue(SWTObservables.observeText(policyNameText, SWT.Modify), policyName);

        ChainValidator<String> policyNameValidationStatusProvider = new ChainValidator<>(policyName,
                grantPermission, new NotEmptyValidator("Please enter policy name"));

         bindingContext.addValidationStatusProvider(policyNameValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(policyNameText, policyNameValidationStatusProvider);

        new Label(composite, SWT.NONE).setText("Policy Documentation:");

        policyDocText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.minimumHeight = 250;
        policyDocText.setLayoutData(gridData);
        bindingContext.bindValue(SWTObservables.observeText(policyDocText, SWT.Modify), policyDoc);

        ChainValidator<String> policyDocValidationStatusProvider = new ChainValidator<>(policyDoc,
                grantPermission, new NotEmptyValidator("Please enter valid policy doc"));

         bindingContext.addValidationStatusProvider(policyDocValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(policyDocText, policyDocValidationStatusProvider);


        Link link = new Link(composite, SWT.NONE | SWT.WRAP);
        link.setText("For more information about the access policy language, " +
                "see <a href=\"" +
                ConceptUrl + "\">Key Concepts</a> in Using AWS Identity and Access Management.");

        link.addListener(SWT.Selection, new WebLinkListener());
        gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
                AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                if ( status.isOK() ) {
                    setErrorMessage(null);
                    setMessage(OK_MESSAGE, Status.OK);
                } else if ( status.getSeverity() == Status.WARNING ) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if ( status.getSeverity() == Status.ERROR ) {
                    setErrorMessage(status.getMessage());
                }

                setPageComplete(status.isOK());
            }
        });

        setControl(composite);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (wizard.getDataModel().getServiceRoles()) {
                policyNameText.setEnabled(true);
                policyDocText.setEnabled(true);
                grantPermissionButton.setSelection(true);
                bindingContext.updateModels();
            } else {
                policyNameText.setEnabled(false);
                policyDocText.setEnabled(false);
                setPageComplete(true);
            }
            setDefaultValue();
        }
        super.setVisible(true);
    }

    private void setDefaultValue() {
        File templateFile = null;
        CreateRoleWizardDataModel dataModel = wizard.getDataModel();
        String policyContent = null;
        String path = null;
        String fileName = null;
        if (dataModel.getServiceRoles()) {
            if (dataModel.getService().startsWith("Amazon Elastic Transcoder")) {
                dataModel.setPolicyName("AmazonElasticTranscoder-3123123-201303261252");
                fileName = "AmazonElasticTranscoder-3123123-201303261252";
            } else if (dataModel.getService().startsWith("AWS Data Pipeline")) {
                dataModel.setPolicyName("AWSDataPipeline-123456-201303261249");
                fileName = "AWSDataPipeline-123456-201303261249";
            } else if (dataModel.getService().startsWith("AWS OpsWorks")) {
                dataModel.setPolicyName("AWSOpsWorks-3123123-201303261253");
                fileName = "AWSOpsWorks-3123123-201303261253";
            } else if (dataModel.getService().startsWith("Amazon EC2 Role for Data Pipeline")) {
                dataModel.setPolicyName("AmazonEC2RoleforDataPipeline-3123445-20");
                fileName = "AmazonEC2RoleforDataPipeline-3123445-20";
            } else {
                fileName = null;
            }

            if (fileName == null) {
                dataModel.setPolicyName("");
                dataModel.setPolicyDoc("");
            } else {
                try {
                    path = getPolicyTemplatesPath();
                    templateFile = new File(path + "AmazonEC2RoleforDataPipeline-3123445-20");
                    policyContent = FileUtils.readFileToString(templateFile);
                    dataModel.setPolicyDoc(policyContent);
                } catch (Exception e) {
                    StatusManager.getManager().handle(new Status(IStatus.ERROR, IdentityManagementPlugin.PLUGIN_ID, "Error loading the policy template" + ": " + e.getMessage()), StatusManager.SHOW);
                }
            }
        }
        bindingContext.updateTargets();
    }

    private String getPolicyTemplatesPath() throws IOException {
        return FileLocator.toFileURL(Platform.getBundle(IdentityManagementPlugin.PLUGIN_ID).getEntry("/policyTemplates")).getPath();
    }
}
