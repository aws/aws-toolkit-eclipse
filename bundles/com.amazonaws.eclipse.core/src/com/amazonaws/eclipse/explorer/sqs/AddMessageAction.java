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
package com.amazonaws.eclipse.explorer.sqs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class AddMessageAction extends Action {
    private final AmazonSQS sqs;
    private final String queueUrl;
    private final IRefreshable refreshable;

    public AddMessageAction(AmazonSQS sqs, String queueUrl, IRefreshable refreshable) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.refreshable = refreshable;

        this.setText("Send Message");
        this.setToolTipText("Sends a new message to this queue");
        this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_PUBLISH));
    }

    @Override
    public void run() {
        AddMessageDialog addMessageDialog = new AddMessageDialog();
        if (addMessageDialog.open() >= 0) {
            SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, addMessageDialog.getMessage());
            if (addMessageDialog.getDelay() > -1) {
                sendMessageRequest.setDelaySeconds(addMessageDialog.getDelay());
            }

            sqs.sendMessage(sendMessageRequest);

            if (refreshable != null) {
                refreshable.refreshData();
            }
        }
    }

    private static class AddMessageDialog extends MessageDialog {
        private Text text;
        private String message;
        private int messageDelay = -1;
        private Spinner messageDelaySpinner;

        public AddMessageDialog() {
            super(Display.getDefault().getActiveShell(), "Send Message",
                AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                "Enter your message:", 0, new String[] { "OK" }, 0);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            GridLayout parentGridLayout = new GridLayout();
            parentGridLayout.verticalSpacing = 1;
            parentGridLayout.marginHeight = 1;
            parent.setLayout(parentGridLayout);

            Composite composite = new Composite(parent, SWT.BORDER);
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            gridData.heightHint = 100;
            gridData.widthHint  = 400;
            composite.setLayoutData(gridData);
            composite.setLayout(new FillLayout());
            text = new Text(composite, SWT.MULTI | SWT.BORDER);

            Composite composite2 = new Composite(parent, SWT.NONE);
            gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            gridData.widthHint  = 400;
            composite2.setLayoutData(gridData);
            GridLayout gridLayout = new GridLayout(3, false);
            gridLayout.marginHeight = 1;
            gridLayout.marginWidth = 0;
            gridLayout.horizontalSpacing = 1;
            composite2.setLayout(gridLayout);

            final Button delayCheckButton = new Button(composite2, SWT.CHECK);
            delayCheckButton.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (delayCheckButton.getSelection()) {
                        messageDelay = messageDelaySpinner.getSelection();
                    } else {
                        messageDelay = -1;
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });
            delayCheckButton.setSelection(false);


            new Label(composite2, SWT.NONE).setText("Message Delay (seconds):");
            messageDelaySpinner = new Spinner(composite2, SWT.BORDER);
            messageDelaySpinner.setMinimum(0);
            messageDelaySpinner.setMaximum(50000);
            messageDelaySpinner.setIncrement(1);
            messageDelaySpinner.setSelection(0);
            messageDelaySpinner.setPageIncrement(60);
            messageDelaySpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            messageDelaySpinner.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    delayCheckButton.setSelection(true);
                    messageDelay = messageDelaySpinner.getSelection();
                }
            });
            messageDelaySpinner.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {}

                @Override
                public void focusGained(FocusEvent e) {
                    delayCheckButton.setSelection(true);
                }
            });


            return parent;
        }

        @Override
        public boolean close() {
            message = text.getText();

            return super.close();
        }

        public String getMessage() {
            return message;
        }

        public int getDelay() {
            return messageDelay;
        }
    }
}
