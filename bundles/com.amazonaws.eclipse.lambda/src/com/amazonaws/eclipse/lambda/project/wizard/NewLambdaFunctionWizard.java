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
package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.refreshProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaFunctionWizardPage;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;

@SuppressWarnings("restriction")
public class NewLambdaFunctionWizard extends NewElementWizard implements INewWizard {

    private final LambdaFunctionWizardDataModel dataModel = new LambdaFunctionWizardDataModel();
    private NewLambdaFunctionWizardPage pageOne;

    @Override
    public void addPages() {
        super.addPages();
        if (pageOne == null) {
            pageOne = new NewLambdaFunctionWizardPage(dataModel);
            pageOne.setWizard(this);
            pageOne.init(getSelection());
        }
        addPage(pageOne);
    }

    @Override
    protected void finishPage(IProgressMonitor monitor)
            throws InterruptedException, CoreException {

        final IProject currentProject = getCurrentProject();
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {

                try {
                    FunctionProjectUtil.createLambdaHandler(currentProject, dataModel.getLambdaFunctionDataModel());
                    refreshProject(currentProject);

                } catch (Exception e) {
                    LambdaAnalytics.trackLambdaFunctionCreationFailed();
                    StatusManager.getManager().handle(
                            new Status(IStatus.ERROR, LambdaPlugin.PLUGIN_ID,
                                    "Failed to create new Lambda function",
                                    e),
                                    StatusManager.SHOW);
                    return;
                }
                LambdaAnalytics.trackLambdaFunctionCreationSucceeded();
                try {
                    IResource handlerClass = pageOne.getModifiedResource();
                    selectAndReveal(handlerClass); // show in explorer
                    openResource((IFile) handlerClass);
                } catch (Exception e) {
                    LambdaPlugin.getDefault().logWarning(
                            "Failed to open the handler class", e);
                }
            }
        });
    }

    @Override
    public IJavaElement getCreatedElement() {
        return pageOne.getCreatedType();
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackLambdaFunctionCreationCanceled();
        return true;
    }

    private IProject getCurrentProject() {
        return pageOne.getJavaProject().getProject();
    }
}
