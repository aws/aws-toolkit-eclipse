/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.webproject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;

/**
 * Composite for a user to enter in their AWS security credentials.
 */
public class CredentialsComposite extends Composite {
    Text accessKeyText;
    Text secretKeyText;
    private Button hideSecretKeyCheckbox;

    protected String accessKey;
    protected String secretKey;

    private static final String ACCESS_KEYS_URL = "https://aws.amazon.com/security-credentials";

    public CredentialsComposite(Composite parent) {
        super(parent, SWT.NONE);
        createControls();
    }

    private void createControls() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        this.setLayout(layout);

        Label description = new Label(this, SWT.WRAP);
        description.setText("Your AWS security credentials will be automatically added to the AwsCredentials.properties file in your new project.");

        GridData g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.horizontalSpan = 2;
        description.setLayoutData(g);

        Composite spacerComposite = new Composite(this, SWT.NONE);
        g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.horizontalSpan = 2;
        g.minimumHeight = 3;
        g.heightHint = 3;
        spacerComposite.setLayoutData(g);

        Label accessKeyFieldLabel = new Label(this, SWT.NONE);
        accessKeyFieldLabel.setText("Access Key ID: ");
        accessKeyFieldLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        accessKeyText = new Text(this, SWT.BORDER);
        g = new GridData(SWT.FILL, SWT.CENTER, true, false);
        g.grabExcessHorizontalSpace = true;
        accessKeyText.setLayoutData(g);
        accessKeyText.setText(AwsToolkitCore.getDefault().getAccountInfo().getAccessKey());
        accessKey = accessKeyText.getText();
        accessKeyText.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event e) {
                accessKey = accessKeyText.getText();
            }
        });

        Label secretKeyFieldLabel = new Label(this, SWT.NONE);
        secretKeyFieldLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        secretKeyFieldLabel.setText("Secret Access Key: ");
        secretKeyText = new Text(this, SWT.BORDER);
        secretKeyText.setLayoutData(g);
        secretKeyText.setText(AwsToolkitCore.getDefault().getAccountInfo().getSecretKey());
        secretKeyText.setEchoChar('*');
        secretKey = secretKeyText.getText();
        secretKeyText.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event e) {
                secretKey = secretKeyText.getText();
            }
        });

        // Just to align the checkbox
        new Label(this, SWT.NONE);

        Composite additionalInfoComposite = new Composite(this, SWT.NONE);
        GridLayout additionalInfoLayout = new GridLayout(2, false);
        additionalInfoLayout.marginHeight = 0;
        additionalInfoComposite.setLayout(additionalInfoLayout);
        additionalInfoComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        hideSecretKeyCheckbox = new Button(additionalInfoComposite, SWT.CHECK);
        hideSecretKeyCheckbox.setText("Show Secret Access Key");
        hideSecretKeyCheckbox.setSelection(false);
        hideSecretKeyCheckbox.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                updateSecretKeyText();
            }
        });
        g = new GridData(SWT.FILL, SWT.TOP, true, false);
        hideSecretKeyCheckbox.setLayoutData(g);

        newLink(additionalInfoComposite,
                "<a href=\"" + ACCESS_KEYS_URL + "\">View my AWS security credentials</a>", 1);
    }

    private Link newLink(Composite parent, String text, int colspan) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        link.addListener(SWT.Selection, new WebLinkListener());
        GridData gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        link.setLayoutData(gridData);
        return link;
    }

    private void updateSecretKeyText() {
        if (hideSecretKeyCheckbox == null) return;
        if (secretKeyText == null) return;

        if (hideSecretKeyCheckbox.getSelection()) {
            secretKeyText.setEchoChar('\0');
        } else {
            secretKeyText.setEchoChar('*');
        }
    }
}
