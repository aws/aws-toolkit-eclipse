/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.sqs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.eclipse.explorer.sqs.SQSContentProvider.QueueNode;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;

public class SQSActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection)getActionSite().getStructuredViewer().getSelection();

        menu.add(new CreateQueueAction());

        boolean onlyQueuesSelected = true;
        List<String> selectedQueues = new ArrayList<String>();
        Iterator<?> iterator = selection.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof QueueNode) {
                selectedQueues.add(((QueueNode)next).getQueueUrl());
            } else {
                onlyQueuesSelected = false;
            }
        }

        if (selectedQueues.size() > 0 && onlyQueuesSelected) {
            menu.add(new DeleteQueueAction(selectedQueues));
        }

        if (selectedQueues.size() == 1 && onlyQueuesSelected) {
            menu.add(new Separator());
            menu.add(new AddMessageAction(AwsToolkitCore.getClientFactory().getSQSClient(), selectedQueues.get(0), null));
        }
    }

    private static class CreateQueueAction extends Action {
        public CreateQueueAction() {
            this.setText("Create New Queue");
            this.setToolTipText("Create a new Amazon SQS Queue");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        public void run() {
            AmazonSQS sqs = AwsToolkitCore.getClientFactory().getSQSClient();

            CreateQueueDialog createQueueDialog = new CreateQueueDialog();
            if (createQueueDialog.open() == 0) {
                try {
                    sqs.createQueue(new CreateQueueRequest(createQueueDialog.getQueueName()));
                    ContentProviderRegistry.refreshAllContentProviders();
                } catch (Exception e) {
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, "Unable to create SQS Queue", e);
                    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
                }
            }
        }
    }

    private static class CreateQueueDialog extends MessageDialog {

        private String queueName;

        public CreateQueueDialog() {
            super(Display.getDefault().getActiveShell(),
                "Create New SQS Queue", null, "Enter the name for your new SQS Queue.",
                MessageDialog.INFORMATION, new String[] {"OK", "Cancel"}, 0);
        }

        public String getQueueName() {
            return queueName;
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            composite.setLayout(new GridLayout(2, false));

            new Label(composite, SWT.NONE).setText("Queue Name:");
            final Text topicNameText = new Text(composite, SWT.BORDER);
            topicNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            topicNameText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    queueName = topicNameText.getText();
                    updateControls();
                }
            });

            return composite;
        }

        private void updateControls() {
            boolean isValid = (queueName != null && queueName.trim().length() > 0);

            Button okButton = this.getButton(0);
            if (okButton != null) okButton.setEnabled(isValid);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            updateControls();
        }
    }

    private static class DeleteQueueAction extends Action {
        private final List<String> queues;

        public DeleteQueueAction(List<String> queueUrls) {
            this.queues = queueUrls;

            this.setText("Delete Queue" + (queueUrls.size() > 1 ? "s" : ""));
            this.setToolTipText("Delete the selected Amazon SQS queues");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void run() {
            Dialog dialog = newConfirmationDialog("Delete selected queues?", "Are you sure you want to delete the selected Amazon SQS queues?");
            if (dialog.open() != 0) return;

            AmazonSQS sqs = AwsToolkitCore.getClientFactory().getSQSClient();
            for (String queueUrl : queues) {
                try {
                    sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
                } catch (Exception e) {
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, "Unable to delete Amazon SQS queue " + queueUrl, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                }
            }

            ContentProviderRegistry.refreshAllContentProviders();
        }

        private Dialog newConfirmationDialog(String title, String message) {
            return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.WARNING, new String[] {"OK", "Cancel"}, 0);
        }
    }

}
