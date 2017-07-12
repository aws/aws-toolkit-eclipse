/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.sns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.eclipse.explorer.sns.SNSContentProvider.TopicNode;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.Topic;

public class SNSActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection)getActionSite().getStructuredViewer().getSelection();

        menu.add(new CreateTopicAction());

        boolean showDeleteMenuItem = true;
        List<Topic> selectedTopics = new ArrayList<>();
        Iterator iterator = selection.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof TopicNode) {
                selectedTopics.add(((TopicNode)next).getTopic());
            } else {
                showDeleteMenuItem = false;
            }
        }

        if (selectedTopics.size() > 0 && showDeleteMenuItem) {
            menu.add(new DeleteTopicAction(selectedTopics));
            menu.add(new Separator());
        }

        if (selectedTopics.size() == 1) {
            AmazonSNS sns = AwsToolkitCore.getClientFactory().getSNSClient();
            menu.add(new PublishMessageAction(sns, selectedTopics.get(0)));
            menu.add(new NewSubscriptionAction(sns, selectedTopics.get(0), null));
        }
    }

    private Dialog newConfirmationDialog(String title, String message) {
        return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.WARNING, new String[] {"OK", "Cancel"}, 0);
    }

    public class DeleteTopicAction extends Action {
        private final List<Topic> topics;

        public DeleteTopicAction(List<Topic> topics) {
            this.topics = topics;

            this.setText("Delete Topic" + (topics.size() > 1 ? "s" : ""));
            this.setToolTipText("Delete the selected Amazon SNS topics");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void run() {
            Dialog dialog = newConfirmationDialog("Delete selected topics?", "Are you sure you want to delete the selected Amazon SNS topics?");
            if (dialog.open() != 0) return;

            AmazonSNS sns = AwsToolkitCore.getClientFactory().getSNSClient();
            for (Topic topic : topics) {
                try {
                    sns.deleteTopic(new DeleteTopicRequest(topic.getTopicArn()));
                } catch (Exception e) {
                    AwsToolkitCore.getDefault().reportException("Unable to delete Amazon SNS topic", e);
                }
            }

            ContentProviderRegistry.refreshAllContentProviders();
        }
    }

    public static class CreateTopicAction extends Action {
        public CreateTopicAction() {
            this.setText("Create New Topic");
            this.setToolTipText("Create a new Amazon SNS Topic");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        public void run() {
            WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), new CreateTopicWizard());
            dialog.open();
        }
    }

}
