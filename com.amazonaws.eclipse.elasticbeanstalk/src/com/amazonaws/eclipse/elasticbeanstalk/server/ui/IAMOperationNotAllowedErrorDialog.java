/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.resources.BeanstalkResourceProvider;

/**
 * The dialog to show if the current user doens't have sufficient permission to perform
 * iam:listRoles when configuring the IAM role for a new Beanstalk environment.
 */
public class IAMOperationNotAllowedErrorDialog extends MessageDialog {

    public static final int OK_BUTTON_CODE = 0;
    public static final int CLOSE = -1;

    private static final String TITLE = "IAM operation not allowed";
    private static final String IMAGE_NAME = AwsToolkitCore.IMAGE_AWS_ICON;

    private final BeanstalkResourceProvider resourceProvider = new BeanstalkResourceProvider();

    private static final String MESSAGE = "The current IAM user does not have permissions to list IAM roles or create instance profiles. "
            + "If these permissions are not granted to the current user all IAM related configuration will "
            + "have to be entered manually and the Toolkit will be unable to create the required resources "
            + "on your behalf.";

    public IAMOperationNotAllowedErrorDialog(Shell parentShell) {
        super(parentShell, TITLE, AwsToolkitCore.getDefault().getImageRegistry().get(IMAGE_NAME), MESSAGE,
                MessageDialog.WARNING, new String[] { "OK" }, OK_BUTTON_CODE);
    }

    @Override
    public Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 15;
        layout.marginBottom = 15;
        composite.setLayout(layout);

        Group addPermissionsInstructionsGruop = displayPermissionsInstructions(composite);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 650;
        addPermissionsInstructionsGruop.setLayoutData(gridData);

        return composite;
    }

    private Group displayPermissionsInstructions(final Composite parent) {
        Group group = new Group(parent, SWT.BORDER);
        group.setText("To grant the needed permissions do the following");
        group.setLayout(new GridLayout(1, false));

        Label label = new Label(group, SWT.WRAP);
        label.setText(String.format(getInstructionsText()));
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        return group;
    }

    private String getInstructionsText() {
        return "(1) Open up a browser and log into the IAM Management Console (https://console.aws.amazon.com/iam/home) using your admin account.%n"
                + "(2) Go to users%n"
                + "(3) Select the user that the toolkit is configured to use%n"
                + "(4) Modify the existing policy to allow access to IAM actions or add a new policy granting the needed permissions.%n"
                + "    - The IAMFullAccess Managed Policy has the needed permissions.%n"
                + "    - Alternatively you can create an inline policy granting the minimum permissions required with the following content:%n"
                + resourceProvider.getMinimumIamPermissionsPolicy().asString();
    }

}
