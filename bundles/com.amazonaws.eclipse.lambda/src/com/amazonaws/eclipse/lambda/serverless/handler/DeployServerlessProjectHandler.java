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

import java.util.List;

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
import org.eclipse.ui.handlers.HandlerUtil;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.serverless.wizard.DeployServerlessProjectWizard;
import com.amazonaws.eclipse.lambda.upload.wizard.util.UploadFunctionUtil;

public class DeployServerlessProjectHandler extends AbstractHandler {

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
        List<String> handlerClasses = UploadFunctionUtil.findValidHandlerClass(project);
        if (handlerClasses.isEmpty()) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    "Invalid AWS Lambda Project",
                    "No Lambda function handler class is found in the project. " +
                    "You need to have at least one concrete class that implements the " +
                    "com.amazonaws.services.lambda.runtime.RequestHandler interface.");
            return;
        }
        String packagePrefix = getPackagePrefix(handlerClasses);
        if (packagePrefix == null) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Invalid Serverless Project",
                    "The Serverless project function structure is not valid.");
            return;
        }
        WizardDialog wizardDialog = new WizardDialog(
                Display.getCurrent().getActiveShell(),
                new DeployServerlessProjectWizard(project, packagePrefix));
        wizardDialog.open();
    }

    //TODO: this is hacky to get the package prefix. we should store the prefix to metadata and load from there.
    //      Or, we can customize the original temlate file and store this data there to load from the template file.
    private static String getPackagePrefix(List<String> fqcnClasses) {
        String sampleClass = fqcnClasses.get(0);
        int index = sampleClass.indexOf(".function.");
        return index == -1 ? null : sampleClass.substring(0, index);
    }
}
