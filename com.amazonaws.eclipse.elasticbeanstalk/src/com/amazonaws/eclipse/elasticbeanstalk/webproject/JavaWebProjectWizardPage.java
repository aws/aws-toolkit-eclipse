/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.webproject;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.core.ui.WebLinkListener;

final class JavaWebProjectWizardPage extends WizardPage {

    private static final String TOMCAT_SESSION_MANAGER_DOCUMENTATION =
            "http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-tomcat-session-manager.html";

    private Text projectNameText;
    private Button basicTemplateRadioButton;
    private Button workerTemplateRadioButton;
    private AccountSelectionComposite accountSelectionComposite;

    private final NewAwsJavaWebProjectDataModel dataModel;
    private DataBindingContext bindingContext = new DataBindingContext();

    /** Collective status of all validators in our binding context */
    protected AggregateValidationStatus aggregateValidationStatus =
        new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);

    private Group sessionManagerGroup;
    private Button useDynamoDBSessionManagerCheckBox;


    JavaWebProjectWizardPage(NewAwsJavaWebProjectDataModel dataModel) {
        super("New AWS Java Web Project Wizard Page");
        this.dataModel = dataModel;

        this.setTitle("New AWS Java Web Project");
        this.setDescription("Configure the options for creating a new AWS Java web project.");

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false) {
                    return;
                }

                IStatus status = (IStatus)value;
                if (status.getSeverity() == IStatus.OK) {
                    setMessage(null);
                    setErrorMessage(null);
                    setPageComplete(true);
                } else {
                    if (status.getSeverity() == IStatus.ERROR) {
                        setErrorMessage(status.getMessage());
                    } else {
                        setMessage(status.getMessage());
                    }
                    setPageComplete(false);
                }
            }
        });
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 10;
        composite.setLayout(layout);
        this.setControl(composite);

        new Label(composite, SWT.NONE).setText("Project name: ");
        projectNameText = new Text(composite, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.horizontalSpan = 2;
        layoutData.widthHint = getContainerWidth() - 50;

        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(layoutData);
        group.setLayout(new FillLayout());
        accountSelectionComposite = new AccountSelectionComposite(group, SWT.None);

        dataModel.setAccountId(AwsToolkitCore.getDefault().getCurrentAccountId());
        accountSelectionComposite.selectAccountId(dataModel.getAccountId());

        dataModel.setProjectTemplate(JavaWebProjectTemplate.DEFAULT);

        createSamplesGroup(composite);
        createSessionManagerGroup(composite);

        bindControls();
        composite.pack();
    }

    private void createSessionManagerGroup(Composite composite) {
        sessionManagerGroup = new Group(composite, SWT.NONE);
        sessionManagerGroup.setText("Amazon DynamoDB Session Management:");
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 3;
        sessionManagerGroup.setLayout(layout);
        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.horizontalSpan = 2;
        sessionManagerGroup.setLayoutData(layoutData);

        useDynamoDBSessionManagerCheckBox = new Button(sessionManagerGroup, SWT.CHECK);
        useDynamoDBSessionManagerCheckBox.setText("Store session data in Amazon DynamoDB");

        newLabel(sessionManagerGroup, "This option configures your project with a custom session manager that persists sessions using Amazon DynamoDB when running in an AWS Elastic Beanstalk environment.  ");
        newLabel(sessionManagerGroup, "This option requires running in a Tomcat 7 environment and is not currently supported for Tomcat 6.");
        newLink(sessionManagerGroup, "<a href=\"" + TOMCAT_SESSION_MANAGER_DOCUMENTATION + "\">More information on the Amazon DynamoDB Session Manager for Tomcat</a>");
    }

    private Label newLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalIndent = 20;
        data.widthHint = getContainerWidth() - 30;
        label.setLayoutData(data);
        return label;
    }

    private Link newLink(Composite parent, String text) {
        Link link = new Link(parent, SWT.WRAP);
        link.setText(text);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalIndent = 20;
        data.widthHint = getContainerWidth() - 30;
        link.setLayoutData(data);
        link.addListener(SWT.Selection, new WebLinkListener());

        return link;
    }

    private void createSamplesGroup(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        group.setText("Start from:");
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 3;
        group.setLayout(layout);
        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.horizontalSpan = 2;
        group.setLayoutData(layoutData);

        basicTemplateRadioButton = new Button(group, SWT.RADIO);
        basicTemplateRadioButton.setText("Basic Java Web Application");
        basicTemplateRadioButton.setSelection(true);
        Label basicTemplateDescriptionLabel = new Label(group, SWT.WRAP);
        basicTemplateDescriptionLabel.setText("A simple Java web application with a single JSP.");
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalIndent = 20;
        gridData.widthHint = getContainerWidth() - 30;
        basicTemplateDescriptionLabel.setLayoutData(gridData);

        basicTemplateRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                sessionManagerGroup.setEnabled(true);
                for (Control control : sessionManagerGroup.getChildren()) {
                    control.setEnabled(true);
                }
            }
        });

        workerTemplateRadioButton = new Button(group, SWT.RADIO);
        workerTemplateRadioButton.setText("Basic Amazon Elastic Beanstalk Worker Tier Application");
        gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.verticalIndent = 5;
        workerTemplateRadioButton.setLayoutData(gridData);

        Label workerDescriptionLabel = new Label(group, SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalIndent = 20;
        gridData.widthHint = getContainerWidth() - 30;
        workerDescriptionLabel.setLayoutData(gridData);
        workerDescriptionLabel.setText(
            "A simple Amazon Elastic Beanstalk Worker Tier Application that "
            + "accepts work requests via POST request.");

        workerTemplateRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                sessionManagerGroup.setEnabled(false);
                for (Control control : sessionManagerGroup.getChildren()) {
                    control.setEnabled(false);
                }
            }
        });
    }

    @SuppressWarnings("static-access")
    private void bindControls() {
        UpdateValueStrategy projectNameUpdateStrategy = new UpdateValueStrategy();
        projectNameUpdateStrategy.setAfterConvertValidator(new NewProjectNameValidator());

        bindingContext.bindValue(
                SWTObservables.observeText(projectNameText, SWT.Modify),
                PojoObservables.observeValue(dataModel, dataModel.PROJECT_NAME),
                projectNameUpdateStrategy, null);

        final IObservableValue accountId = new WritableValue();
        accountId.setValue(dataModel.getAccountId());
        accountSelectionComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                accountId.setValue(accountSelectionComposite.getSelectedAccountId());
            }
        });
        bindingContext.bindValue(accountId,
                PojoObservables.observeValue(dataModel, dataModel.ACCOUNT_ID), new UpdateValueStrategy(
                        UpdateValueStrategy.POLICY_UPDATE), new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));

        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setConverter(new Converter(Boolean.class, JavaWebProjectTemplate.class) {
            public Object convert(Object fromObject) {
                Boolean from = (Boolean) fromObject;
                if (from == null || from == false) {
                    return JavaWebProjectTemplate.DEFAULT;
                } else {
                    return JavaWebProjectTemplate.WORKER;
                }
            }
        });

        bindingContext.bindValue(
            SWTObservables.observeSelection(workerTemplateRadioButton),
            PojoObservables.observeValue(dataModel, dataModel.PROJECT_TEMPLATE),
            strategy,
            new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER)
        );

        bindingContext.bindValue(SWTObservables.observeSelection(useDynamoDBSessionManagerCheckBox),
                PojoObservables.observeValue(dataModel, dataModel.USE_DYNAMODB_SESSION_MANAGEMENT), null, null).updateTargetToModel();
    }

    private int getContainerWidth() {
        return this.getContainer().getShell().getSize().x;
    }

    /**
     * Simple validator implementation that validates that the associated value
     * is not empty and does not match an existing project in the workspace.
     */
    private static class NewProjectNameValidator implements IValidator {
        public IStatus validate(Object obj) {
            String value = (String)obj;

            if (value == null || value.trim().length() == 0) {
                String errorMessage = "Enter a project name.";
                return ValidationStatus.info(errorMessage);
            }

            for (IProject existingProject : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                if (existingProject.getName().equals(value)) {
                    String errorMessage = "Project '" + value + "' already exists in the workspace";
                    return ValidationStatus.error(errorMessage);
                }
            }

            return Status.OK_STATUS;
        }
    }
}
