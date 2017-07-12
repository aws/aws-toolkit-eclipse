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
package com.amazonaws.eclipse.codedeploy.deploy.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.DeployProjectToCodeDeployWizard;

public class DeployProjectToCodeDeployHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
                .getActivePage().getSelection();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structurredSelection = (IStructuredSelection)selection;

            Object firstSeleciton = structurredSelection.getFirstElement();
            if (firstSeleciton instanceof IProject) {
                IProject selectedProject = (IProject) firstSeleciton;

                try {
                    WizardDialog wizardDialog = new WizardDialog(
                            Display.getCurrent().getActiveShell(),
                            new DeployProjectToCodeDeployWizard(selectedProject));

                    wizardDialog.open();

                } catch (Exception e) {
                    CodeDeployPlugin.getDefault().reportException(
                            "Failed to launch deployment wizard.", e);
                }

            } else {
                CodeDeployPlugin.getDefault().logInfo(
                        "Invalid selection: " + firstSeleciton + " is not a project.");
            }
        }

        return null;
    }

}
