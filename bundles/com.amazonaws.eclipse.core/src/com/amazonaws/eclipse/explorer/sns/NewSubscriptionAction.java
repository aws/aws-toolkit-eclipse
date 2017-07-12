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

import java.util.LinkedHashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Topic;

class NewSubscriptionAction extends Action {
    private final AmazonSNS sns;
    private final Topic topic;
    private final IRefreshable refreshable;

    public NewSubscriptionAction(AmazonSNS sns, Topic topic, IRefreshable refreshable) {
        this.sns = sns;
        this.topic = topic;
        this.refreshable = refreshable;

        this.setText("Create Subscription");
        this.setToolTipText("Create a new subscription for this topic");
        this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
    }

    private class NewSubscriptionDialog extends MessageDialog {

        private Label endpointLabel;
        private Combo protocolCombo;
        private Text endpointText;

        private String selectedProtocol;
        private String selectedEndpoint;

        public NewSubscriptionDialog() {
            super(Display.getDefault().getActiveShell(),
                  "Create New Subscription",
                  AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                  "Select the message delivery protocol and details for a new subscription to this topic.",
                  MessageDialog.INFORMATION,
                  new String[] {"OK", "Cancel"},
                  0);
        }

        public String getSelectedProtocol() {
            return selectedProtocol;
        }

        public String getSelectedEndpoint() {
            return selectedEndpoint;
        }

        private void updateControls() {
            selectedProtocol = (String)protocolCombo.getData(protocolCombo.getText());
            selectedEndpoint = endpointText.getText();

            boolean finished = (selectedProtocol.length() > 0 &&
                               selectedEndpoint.length() > 0);

            if (getButton(0) != null) getButton(0).setEnabled(finished);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            new Label(composite, SWT.NONE).setText("Subscription Protocol:");
            protocolCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
            protocolCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            LinkedHashMap<String, String> protocolMap = new LinkedHashMap<>();
            protocolMap.put("Email (plain text)", "email");
            protocolMap.put("Email (JSON)", "email-json");
            protocolMap.put("SQS", "sqs");
            protocolMap.put("HTTP", "http");
            protocolMap.put("HTTPS", "https");

            protocolCombo.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateControls();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });

            for (String label : protocolMap.keySet()) {
                protocolCombo.add(label);
                protocolCombo.setData(label, protocolMap.get(label));
            }
            protocolCombo.select(0);


            endpointLabel = new Label(composite, SWT.NONE);
            endpointLabel.setText("Endpoint:");

            endpointText = new Text(composite, SWT.BORDER);
            endpointText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            endpointText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    updateControls();
                }
            });

            updateControls();
            return composite;
        }
    }


    @Override
    public void run() {
        NewSubscriptionDialog newSubscriptionDialog = new NewSubscriptionDialog();
        if (newSubscriptionDialog.open() == 0) {
            String protocol = newSubscriptionDialog.getSelectedProtocol();
            String endpoint = newSubscriptionDialog.getSelectedEndpoint();

            try {
                sns.subscribe(new SubscribeRequest(topic.getTopicArn(), protocol, endpoint));
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to subscribe to topic", e);
            }

            if (refreshable != null) refreshable.refreshData();
        }
    }
}
