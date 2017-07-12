/*
 * Copyright 2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.cloudformation;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.DeleteResourceConfirmationDialog;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.eclipse.explorer.cloudformation.CloudFormationContentProvider.StackNode;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizard;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CancelUpdateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;

/**
 * Action provider when right-clicking on Cloud Formation nodes in the explorer.
 */
public class CloudFormationExplorerActionProvider extends CommonActionProvider {

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
        if ( selection.size() != 1 )
            return;

        menu.add(new CreateStackAction());

        Object firstElement = selection.getFirstElement();
        if ( firstElement instanceof StackNode ) {
            menu.add(new Separator());
            menu.add(new SaveStackTemplateAction((StackNode) firstElement));
            menu.add(new CancelStackUpdateAction((StackNode) firstElement));
            menu.add(new UpdateStackAction((StackNode) firstElement));
            menu.add(new DeleteStackAction((StackNode) firstElement));
        }
    }

    private final class CreateStackAction extends Action {

        @Override
        public String getDescription() {
            return "Create a new stack";
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return CloudFormationPlugin.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD);
        }

        @Override
        public String getText() {
            return "Create Stack";
        }

        @Override
        public void run() {
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateStackWizard());
            dialog.open();
        }

    }

    private final class UpdateStackAction extends Action {
        private final StackNode stack;

        public UpdateStackAction(StackNode stack) {
            this.stack = stack;
        }

        @Override
        public String getDescription() {
            return "Update Stack";
        }

        @Override
        public String getText() {
            return "Update Stack";
        }

        @Override
        public void run() {
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateStackWizard(stack.getName(), Mode.Update));
            dialog.open();
        }

    }

    private final class CancelStackUpdateAction extends Action {
        private final StackNode stack;

        public CancelStackUpdateAction(StackNode stack) {
            this.stack = stack;
        }

        @Override
        public String getDescription() {
            return "Cancel stack update";
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return CloudFormationPlugin.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
        }

        @Override
        public String getText() {
            return "Cancel Stack Update";
        }

        @Override
        public void run() {
             new Job("Cancel stack update") {

                 @Override
                protected IStatus run(final IProgressMonitor monitor) {
                     try {
                         AmazonCloudFormation cloudFormationClient = AwsToolkitCore.getClientFactory()
                                 .getCloudFormationClient();
                         cloudFormationClient.cancelUpdateStack(new CancelUpdateStackRequest().withStackName(stack.getName()));
                     } catch ( Exception e ) {
                         CloudFormationPlugin.getDefault().logError("Couldn't cancel the stack update", e);
                         return new Status(Status.ERROR, CloudFormationPlugin.PLUGIN_ID,
                                 "Couldn't cancel the stack update:", e);
                     }
                     return Status.OK_STATUS;
                 }
             }.schedule();
        }

    }

    private final class SaveStackTemplateAction extends Action {

        private final StackNode stack;

        public SaveStackTemplateAction(StackNode stack) {
            this.stack = stack;
        }

        @Override
        public String getDescription() {
            return "Save Stack Template As...";
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT);
        }

        @Override
        public String getText() {
            return "Save Stack Template As...";
        }

        @Override
        public void run() {
            FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath());
            final String path = dialog.open();
            if ( path != null ) {
                new Job("Downloading stack template") {

                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {
                        try {
                            AmazonCloudFormation cloudFormationClient = AwsToolkitCore.getClientFactory()
                                    .getCloudFormationClient();
                            GetTemplateResult template = cloudFormationClient.getTemplate(new GetTemplateRequest()
                                    .withStackName(stack.getName()));

                            String formattedText = template.getTemplateBody();
                            try {
                                formattedText = JsonFormatter.format(template.getTemplateBody());
                            } catch (Exception e) {
                                CloudFormationPlugin.getDefault().logError("Unable to format template", e);
                            }

                            FileUtils.writeStringToFile(new File(path), formattedText);
                            String workspaceAbsolutePath = ResourcesPlugin.getWorkspace().getRoot()
                                    .getLocation().toFile().getAbsolutePath();
                            if ( path.startsWith(workspaceAbsolutePath) ) {
                                final String relPath = path.substring(workspaceAbsolutePath.length());
                                ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(relPath))
                                .refreshLocal(1, monitor);
                            }
                        } catch ( Exception e ) {
                            CloudFormationPlugin.getDefault().logError("Couldn't save cloud formation template", e);
                            return new Status(Status.ERROR, CloudFormationPlugin.PLUGIN_ID,
                                    "Couldn't save cloud formation template:", e);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }

    }

    private final class DeleteStackAction extends Action {
        private final StackNode stack;

        public DeleteStackAction(StackNode stack) {
            this.stack = stack;
            this.setText("Delete Stack");
            this.setDescription("Deleting a stack deletes all stack resources.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void run() {
            Dialog dialog = new DeleteResourceConfirmationDialog(Display.getDefault().getActiveShell(),
                    stack.getName(), "stack");
            if (dialog.open() != Window.OK) {
                return;
            }

            Job deleteStackJob = new Job("Deleting Stack...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClient();

                    IStatus status = Status.OK_STATUS;

                    try {
                        cloudFormation.deleteStack(new DeleteStackRequest()
                                .withStackName(stack.getName()));
                    } catch (AmazonCloudFormationException e) {
                        status = new Status(IStatus.ERROR, CloudFormationPlugin.getDefault().getPluginId(), e.getMessage(), e);
                    }
                    ContentProviderRegistry.refreshAllContentProviders();
                    return status;
                }
            };

            deleteStackJob.schedule();
        }
    }

}
