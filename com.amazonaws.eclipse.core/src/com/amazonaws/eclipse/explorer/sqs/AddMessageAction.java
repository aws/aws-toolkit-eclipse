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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;

class AddMessageAction extends Action {
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
            sqs.sendMessage(new SendMessageRequest(queueUrl, addMessageDialog.getMessage()));

            if (refreshable != null) refreshable.refreshData();
        }
    }

    private final class AddMessageDialog extends MessageDialog {
        private Text text;
        private String message;

        public AddMessageDialog() {
            super(Display.getDefault().getActiveShell(), "Send Message",
                AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                "Enter your message:", 0, new String[] { "OK" }, 0);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            parent.setLayout(new GridLayout());

            Composite composite = new Composite(parent, SWT.BORDER);
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            gridData.heightHint = 100;
            gridData.widthHint  = 400;
            composite.setLayoutData(gridData);
            composite.setLayout(new FillLayout());
            text = new Text(composite, SWT.MULTI | SWT.BORDER);

            return composite;
        }

        @Override
        public boolean close() {
            message = text.getText();

            return super.close();
        }

        public String getMessage() {
            return message;
        }
    }
}