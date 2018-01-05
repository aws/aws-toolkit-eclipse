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
package com.amazonaws.eclipse.lambda.serverless.handler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.serverless.wizard.DeployServerlessProjectWizard;
import com.amazonaws.eclipse.lambda.upload.wizard.util.UploadFunctionUtil;

public class DeployServerlessProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
                .getActivePage().getSelection();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structurredSelection = (IStructuredSelection)selection;
            Object firstSeleciton = structurredSelection.getFirstElement();

            IProject selectedProject = null;

            if (firstSeleciton instanceof IProject) {
                selectedProject = (IProject) firstSeleciton;
            } else if (firstSeleciton instanceof IJavaProject) {
                selectedProject = ((IJavaProject) firstSeleciton).getProject();
            } else {
                LambdaPlugin.getDefault().logInfo(
                        "Invalid selection: " + firstSeleciton + " is not a Serverless project.");
                return null;
            }

            doDeployServerlessTemplate(selectedProject);
        }

        return null;
    }

    public static void doDeployServerlessTemplate(IProject project) {

        if (!WorkbenchUtils.openSaveFilesDialog(PlatformUI.getWorkbench())) {
            return;
        }

        Set<String> handlerClasses = new HashSet<>(UploadFunctionUtil.findValidHandlerClass(project));
        handlerClasses.addAll(UploadFunctionUtil.findValidStreamHandlerClass(project));
        if (handlerClasses.isEmpty()) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    "Invalid AWS Lambda Project",
                    "No Lambda function handler class is found in the project. " +
                    "You need to have at least one concrete class that implements the " +
                    "com.amazonaws.services.lambda.runtime.RequestHandler interface.");
            return;
        }
        File serverlessTemplate = FunctionProjectUtil.getServerlessTemplateFile(project);
        if (!serverlessTemplate.exists() || !serverlessTemplate.isFile()) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    "Invalid AWS Serverless Project",
                    "No serverless.template file found in your project root.");
            return;
        }
        WizardDialog wizardDialog = new WizardDialog(
                Display.getCurrent().getActiveShell(),
                new DeployServerlessProjectWizard(project, handlerClasses));
        wizardDialog.open();
    }
}
