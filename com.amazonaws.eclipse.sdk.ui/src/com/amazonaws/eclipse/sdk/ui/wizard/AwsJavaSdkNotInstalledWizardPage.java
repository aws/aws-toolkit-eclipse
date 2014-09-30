/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.sdk.ui.wizard;


import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.core.ui.overview.Toolkit;
import com.amazonaws.eclipse.sdk.ui.preferences.JavaSDKPreferencePage;

public class AwsJavaSdkNotInstalledWizardPage extends WizardPage {

    public AwsJavaSdkNotInstalledWizardPage() {
        super("AwsJavaSdkNotInstalledWizardPage");
        setTitle("Create an AWS Java project");
        setDescription("Create a new AWS Java project in the workspace");
    }

    public void createControl(final Composite parent) {

        final Composite composite = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);

        Label label0 = new Label(composite, SWT.NONE);
        label0.setText("Cannot create the project since " +
                "AWS SDK for Java is not installed in the system.");

        Label label1 = new Label(composite, SWT.NONE);
        label1.setText("Click \"Finish\" to install the SDK now.");

        Toolkit.newPreferenceLink(composite, "Configure the download location for the SDK", JavaSDKPreferencePage.ID);

        setControl(composite);
    }

}
