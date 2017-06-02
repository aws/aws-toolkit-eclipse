/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.explorer.lambda;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.DeleteResourceConfirmationDialog;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.eclipse.explorer.lambda.LambdaContentProvider.FunctionNode;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.NewLambdaJavaFunctionProjectWizard;
import com.amazonaws.eclipse.lambda.project.wizard.NewServerlessProjectWizard;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;

public class LambdaActionProvider extends CommonActionProvider {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.
     * action.IMenuManager)
     */
    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection) getActionSite().getStructuredViewer().getSelection();
        if (selection.size() != 1) {
            return;
        }

        menu.add(new CreateLambdaProjectAction());
        menu.add(new CreateServerlessProjectAction());

        Object firstElement = selection.getFirstElement();
        if ( firstElement instanceof FunctionNode ) {
            menu.add(new DeleteFunctionAction((FunctionNode) firstElement));
        }
    }

    private final class CreateLambdaProjectAction extends Action {
        public CreateLambdaProjectAction() {
            this.setText("Create a Lambda Project");
            this.setDescription("Create a new Lambda project in your current workspace, this does not upload to AWS Lambda.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        public void run() {
            NewLambdaJavaFunctionProjectWizard newWizard = new NewLambdaJavaFunctionProjectWizard();
            newWizard.init(PlatformUI.getWorkbench(), null);
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
            dialog.open();
        }
    }

    private final class CreateServerlessProjectAction extends Action {
        public CreateServerlessProjectAction() {
            this.setText("Create a Serverless Project");
            this.setDescription("Create a new Serverless project in your current workspace, this does not deploy to Amazon CloudFormation.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        public void run() {
            NewServerlessProjectWizard newWizard = new NewServerlessProjectWizard();
            newWizard.init(PlatformUI.getWorkbench(), null);
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
            dialog.open();
        }
    }

    private final class DeleteFunctionAction extends Action {
        private final FunctionNode function;

        public DeleteFunctionAction(FunctionNode function) {
            this.function = function;
            this.setText("Delete Function");
            this.setDescription("Deleting this Lambda function will permanently remove the associated code. The associated event source mappings will also be removed, but the logs and role will not be deleted.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void run() {
            Dialog dialog = new DeleteResourceConfirmationDialog(Display.getDefault().getActiveShell(),
                    function.getName(), "function");
            if (dialog.open() != Window.OK) {
                return;
            }

            Job deleteStackJob = new Job("Deleting Function...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    AWSLambda lambda = AwsToolkitCore.getClientFactory().getLambdaClient();

                    IStatus status = Status.OK_STATUS;

                    try {
                        lambda.deleteFunction(new DeleteFunctionRequest()
                                .withFunctionName(function.getName()));
                    } catch (AWSLambdaException e) {
                        status = new Status(IStatus.ERROR, LambdaPlugin.getDefault().getPluginId(), e.getMessage(), e);
                    }
                    ContentProviderRegistry.refreshAllContentProviders();
                    return status;
                }
            };

            deleteStackJob.schedule();
        }
    }

}
