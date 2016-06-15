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
import com.amazonaws.eclipse.lambda.project.wizard.util.LambdaFunctionGroup;

public class NewLambdaFunctionWizardPage extends NewTypeWizardPage {

    private final static String PAGE_NAME = "NewLambdaFunctionWizardPage";

    private final LambdaFunctionWizardDataModel dataModel;
    private final LambdaFunctionGroup lambdaFunctionGroup;

    public NewLambdaFunctionWizardPage() {
        super(true, PAGE_NAME);

        setTitle("Create a new AWS Lambda function");
        setDescription("Create a new AWS Lambda function in the workspace");

        this.dataModel = new LambdaFunctionWizardDataModel();
        this.lambdaFunctionGroup = new LambdaFunctionGroup(this, dataModel);
    }

    public void init(IStructuredSelection selection) {
        IJavaElement jelem= getInitialJavaElement(selection);
        initContainerPage(jelem);
        initTypePage(jelem);
    }

    private void doStatusUpdate() {

        // status of self-defined controls
        IStatus handlerInfoStatus = lambdaFunctionGroup == null ?
                null : lambdaFunctionGroup.getHandlerInfoValidationStatus();

        // status of all used components
        IStatus[] status= new IStatus[] {
            fContainerStatus,
            fPackageStatus,
            fTypeNameStatus,
            handlerInfoStatus
        };

        // update data model
        dataModel.setHandlerPackageName(getPackageText());
        dataModel.setHandlerClassName(getTypeName());

        // the mode severe status will be displayed and the OK button enabled/disabled.
        updateStatus(status);
    }

    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);

        doStatusUpdate();
    }

    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Reuse the project name and package control of the system Java New Type wizard
        createProjectPackageClassNameControls(composite);

        lambdaFunctionGroup.init(composite);
        lambdaFunctionGroup.createLambdaHandlerControl();
        lambdaFunctionGroup.createSeparator();
        lambdaFunctionGroup.createHandlerSourcePreview();

        lambdaFunctionGroup.initializeValidators(new IChangeListener() {
            public void handleChange(ChangeEvent arg0) {
                doStatusUpdate();
            }
        });
        lambdaFunctionGroup.initializeDefaults();
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

    @Override
    public void dispose() {
        lambdaFunctionGroup.dispose();
        super.dispose();
    }

    public LambdaFunctionWizardDataModel getDataModel() {
        return dataModel;
    }

}
