/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard.page;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.LambdaFunctionComposite;

public class NewLambdaFunctionWizardPage extends NewTypeWizardPage {

    private final static String PAGE_NAME = "NewLambdaFunctionWizardPage";

    private final LambdaFunctionWizardDataModel dataModel;
    private final DataBindingContext dataBindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;
    private LambdaFunctionComposite lambdaFunctionComposite;

    public NewLambdaFunctionWizardPage(LambdaFunctionWizardDataModel dataModel) {
        super(true, PAGE_NAME);

        setTitle("Create a new AWS Lambda function");
        setDescription("Create a new AWS Lambda function in the workspace");

        this.dataModel = dataModel;
        this.dataBindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                populateValidationStatus();
            }
        });
    }

    public void init(IStructuredSelection selection) {
        IJavaElement jelem = getInitialJavaElement(selection);
        initContainerPage(jelem);
        initTypePage(jelem);
    }

    private void doStatusUpdate() {

        // update data model
        dataModel.getLambdaFunctionDataModel().setPackageName(getPackageText());
        dataModel.getLambdaFunctionDataModel().setClassName(getTypeName());

        // the mode severe status will be displayed and the OK button enabled/disabled.
        updateStatus(getValidationStatus());
    }

    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);
        doStatusUpdate();
    }

    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Reuse the project name and package control of the system Java New Type wizard
        createProjectPackageClassNameControls(composite);
        createLambdaFunctionComposite(composite);
        setControl(composite);

        doStatusUpdate();
    }

    private void createProjectPackageClassNameControls(Composite composite) {
        Composite containerComposite = new Composite(composite, SWT.NONE);
        containerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        int nColumns = 4;
        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        containerComposite.setLayout(layout);
        createContainerControls(containerComposite, nColumns);
        createPackageControls(containerComposite, nColumns);
        createTypeNameControls(containerComposite, nColumns);
    }

    private void createLambdaFunctionComposite(Composite composite) {
        lambdaFunctionComposite = new LambdaFunctionComposite(
                composite, dataModel.getLambdaFunctionDataModel(), dataBindingContext);
        lambdaFunctionComposite.createInputTypeControl();
        lambdaFunctionComposite.createSeparator();
        lambdaFunctionComposite.createHandlerSourcePreview();
        lambdaFunctionComposite.initialize();
    }

    @Override
    public void dispose() {
        lambdaFunctionComposite.dispose();
        super.dispose();
    }


    private void populateValidationStatus() {
        IStatus[] statuses = getValidationStatus();

        for (IStatus status : statuses) {
            if (status == null) continue;
            else if (status.getSeverity() == IStatus.OK) {
                this.setErrorMessage(null);
                super.setPageComplete(true);
            } else if (status.getSeverity() == IStatus.ERROR) {
                setErrorMessage(status.getMessage());
                super.setPageComplete(false);
                break;
            }
        }
    }

    private IStatus[] getValidationStatus() {
        Object value = aggregateValidationStatus.getValue();
        IStatus aggregateStatus = (IStatus) value;

        // status of all used components
        return new IStatus[] {
            fContainerStatus,
            fPackageStatus,
            fTypeNameStatus,
            aggregateStatus
        };
    }

}
