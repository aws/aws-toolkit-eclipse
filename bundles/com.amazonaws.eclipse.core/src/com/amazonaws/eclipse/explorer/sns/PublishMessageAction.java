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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;

final class PublishMessageAction extends Action {
    private final Topic topic;
    private final AmazonSNS sns;

    public PublishMessageAction(AmazonSNS sns, Topic topic) {
        this.sns = sns;
        this.topic = topic;

        this.setText("Publish Message");
        this.setToolTipText("Publish a message to this topic");
        this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_PUBLISH));
    }

    @Override
    public void run() {
        PublishMessageAction.NewMessageDialog newMessageDialog = new NewMessageDialog();
        if (newMessageDialog.open() == 0) {
            String topicArn = topic.getTopicArn();
            String subject = newMessageDialog.getSelectedSubject();
            String message = newMessageDialog.getSelectedMessage();

            try {
                sns.publish(new PublishRequest(topicArn, message, subject));
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to publish message", e);
            }
        }
    }

    private static class NewMessageDialog extends MessageDialog {
        private Text subjectText;
        private Text messageText;

        private String selectedSubject;
        private String selectedMessage;

        public NewMessageDialog() {
            super(Display.getDefault().getActiveShell(),
                "Publish New Message",
                AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                "Enter the subject and message body to send to the subscribers of this topic.",
                MessageDialog.INFORMATION,
                new String[] {"OK", "Cancel"},
                0);
        }

        public String getSelectedMessage() {
            return selectedMessage;
        }

        public String getSelectedSubject() {
            return selectedSubject;
        }

        private void updateControls() {
            selectedMessage = messageText.getText();
            selectedSubject = subjectText.getText();

            boolean finished = (selectedMessage.length() > 0 &&
                               selectedSubject.length() > 0);

            if (getButton(0) != null) getButton(0).setEnabled(finished);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));


            new Label(composite, SWT.NONE).setText("Subject:");
            subjectText = new Text(composite, SWT.BORDER);
            subjectText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            subjectText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    updateControls();
                }
            });

            Label messageLabel = new Label(composite, SWT.NONE);
            messageLabel.setText("Message:");
            messageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            messageText = new Text(composite, SWT.BORDER | SWT.MULTI);
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.heightHint = 100;
            messageText.setLayoutData(gridData);
            messageText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    updateControls();
                }
            });

            updateControls();
            return composite;
        }
    }
}
