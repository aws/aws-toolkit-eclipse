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
package com.amazonaws.eclipse.sdk.ui.wizard;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

/**
 * The second page of the AWS New Project Wizard.
 */
public class NewAwsJavaProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {

    private NewAwsJavaProjectWizardPageOne pageOne;
    
    public NewAwsJavaProjectWizardPageTwo(NewJavaProjectWizardPageOne mainPage) {
        super(mainPage);
        this.pageOne = (NewAwsJavaProjectWizardPageOne) mainPage;
    }
    
    @Override
    protected void initializeBuildPath(IJavaProject javaProject, IProgressMonitor monitor) throws CoreException {
        try {
            pageOne.getSelectedSdkInstall().writeMetadataToProject(javaProject);
        } catch (IOException e) {}
        super.initializeBuildPath(javaProject, monitor);
    }

}
