/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.overview.HyperlinkHandler;

/**
 * Simple dialog to inform users that they need to configure their AWS credentials.
 */
public class NoCredentialsDialog extends MessageDialog {

    public NoCredentialsDialog() {
        super(Display.getDefault().getActiveShell(), "Invalid AWS credentials", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "No AWS security credentials configured", 0, new String[] { "OK" }, 0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        return createComposite(parent);
    }

    /**
     * Creates and returns a composite with the "no credentials" message.
     * Assumes a GridLayout for the parent.
     */
    public static Composite createComposite(Composite parent) {
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.horizontalSpan = 1;

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        Label label = new Label(composite, SWT.WRAP);
        label.setText("Your AWS security credentials aren't configured yet");
        label.setLayoutData(gridData);

        Hyperlink preferenceLink = new Hyperlink(composite, SWT.WRAP);
        preferenceLink.setText("Open the AWS Toolkit for Eclipse preferences to enter your credentials.");
        preferenceLink.setLayoutData(gridData);
        preferenceLink.setHref("preference:" + AwsToolkitCore.ACCOUNT_PREFERENCE_PAGE_ID);
        preferenceLink.setUnderlined(true);
        preferenceLink.addHyperlinkListener(new HyperlinkHandler());

        return composite;
    }
}
