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
package com.amazonaws.eclipse.lambda.project.wizard.page;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.amazonaws.eclipse.lambda.project.classpath.LambdaRuntimeClasspathContainer;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryManager;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.LambdaFunctionGroup;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;

public class NewLambdaJavaFunctionProjectWizardPageOne extends NewJavaProjectWizardPageOne {

    private final LambdaFunctionWizardDataModel dataModel;
    private final LambdaFunctionGroup lambdaFunctionGroup;

    private boolean isProjectNameValid;

    public NewLambdaJavaFunctionProjectWizardPageOne(LambdaFunctionWizardDataModel dataModel) {
        setTitle("Create a new AWS Lambda Java project");
        setDescription("Create a new AWS Lambda Java project in the workspace");

        this.dataModel = dataModel;
        this.lambdaFunctionGroup = new LambdaFunctionGroup(this, dataModel);
    }

    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Reuse the project name control of the system Java project wizard
        Control nameControl = createNameControl(composite);
        nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        lambdaFunctionGroup.init(composite);
        lambdaFunctionGroup.createPackageNameControl();
        lambdaFunctionGroup.createClassNameControl();
        lambdaFunctionGroup.createLambdaHandlerControl();
        lambdaFunctionGroup.createSeparator();
        lambdaFunctionGroup.createHandlerSourcePreview();
        lambdaFunctionGroup.createShowReadmeFileCheckBox();

        lambdaFunctionGroup.initializeValidators(new IChangeListener() {
            public void handleChange(ChangeEvent arg0) {
                populateHandlerValidationStatus();
            }
        });
        lambdaFunctionGroup.initializeDefaults();

        setControl(composite);
    }

    /**
     * @return returns the default class path entries, which includes all the
     *         default JRE entries plus the Lambda runtime API.
     */
    @Override
    public IClasspathEntry[] getDefaultClasspathEntries() {

        IClasspathEntry[] classpath = super.getDefaultClasspathEntries();

        classpath = addJunitLibrary(classpath);
        classpath = addLambdaRuntimeLibrary(classpath);

        if (dataModel.requireSdkDependency()) {
            classpath = addJavaSdkLibrary(classpath);
        }

        return classpath;
    }

    private IClasspathEntry[] addLambdaRuntimeLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new LambdaRuntimeClasspathContainer(
                        LambdaRuntimeLibraryManager.getInstance().getLatestVersion()
                        ).getPath());

        return augmentedClasspath;
    }

    private IClasspathEntry[] addJavaSdkLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new AwsClasspathContainer(
                        JavaSdkManager.getInstance().getDefaultSdkInstall()
                        ).getPath());

        return augmentedClasspath;
    }

    private IClasspathEntry[] addJunitLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        final String JUNIT_CONTAINER_ID= "org.eclipse.jdt.junit.JUNIT_CONTAINER";
        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new Path(JUNIT_CONTAINER_ID).append("4"));

        return augmentedClasspath;
    }

    /**
     * A very hacky way of combining the project name validation with our custom
     * validation logic.
     */
    @Override
    public void setPageComplete(boolean pageComplete) {
        isProjectNameValid = pageComplete;
        if (!pageComplete) {
            super.setPageComplete(pageComplete);
        } else {
            lambdaFunctionGroup.runHandlerValidators();
            populateHandlerValidationStatus();
        }
    }
    @Override
    public void setMessage(String newMessage, int newType) {
        super.setMessage(newMessage, newType);
        populateHandlerValidationStatus();
    }

    private void populateHandlerValidationStatus() {

        if (lambdaFunctionGroup == null) return;
        IStatus handlerInfoStatus = lambdaFunctionGroup.getHandlerInfoValidationStatus();
        if (handlerInfoStatus == null) return;

        boolean isHandlerInfoValid = (handlerInfoStatus.getSeverity() == IStatus.OK);

        if (isProjectNameValid && isHandlerInfoValid) {
            // always call super methods when handling our custom
            // validation status
            setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            if (!isProjectNameValid) {
                setErrorMessage("Enter a valid project name");
            } else {
                setErrorMessage(handlerInfoStatus.getMessage());
            }
            super.setPageComplete(false);
        }
    }

    @Override
    public void dispose() {
        lambdaFunctionGroup.dispose();
        super.dispose();
    }
}