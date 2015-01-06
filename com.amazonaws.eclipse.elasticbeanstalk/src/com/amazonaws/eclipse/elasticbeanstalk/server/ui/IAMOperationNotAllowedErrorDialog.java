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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPublishingUtils;

/**
 * The dialog to show if the current user doens't have sufficient permission to
 * perform iam:listRoles when configuring the IAM role for a new Beanstalk environment.
 */
public class IAMOperationNotAllowedErrorDialog extends MessageDialog {

    public static final int RETRY_IAM_OPERATION = 0;
    public static final int PROCEED_WITHOUT_ROLE = 1;
    public static final int PROCEED_WITH_EXISTING_ROLE = 2;
    public static final int CLOSE = -1;

    private static final String TITLE = "IAM operation not allowed";
    private static final String IMAGE_NAME = AwsToolkitCore.IMAGE_AWS_ICON;
    private static final String MESSAGE =  String.format(
            "Failed to load IAM roles due to insufficient permission policy " +
            "associated with the current IAM user.%n" +
            "Please read the following options and choose one of them to proceed.");

    private Font italicFont;

    private Button widget_UserSpecifiedProfileCheckbox;
    private Text widget_InstanceProfileName;

    private volatile String instanceProfileNameText;

    public IAMOperationNotAllowedErrorDialog(Shell parentShell) {
        super(parentShell, TITLE,
                AwsToolkitCore.getDefault().getImageRegistry().get(IMAGE_NAME),
                MESSAGE,
                MessageDialog.QUESTION,
                new String[] {"Reload", "Proceed without a profile", "Use the specified profile"},
                RETRY_IAM_OPERATION);
    }

    @Override
    public Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 15;
        layout.marginBottom = 15;
        composite.setLayout(layout);

        // Update user policy and reload
        Group group_a = createGroupForOption_a(composite);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 650;
        group_a.setLayoutData(gridData);

        // Manually specify an instance-profile
        Group group_b = createGroupForOption_b(composite);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 650;
        group_b.setLayoutData(gridData);

        // Proceed without configuring an instance-profile
        Group group_c = createGroupForOption_c(composite);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 650;
        group_c.setLayoutData(gridData);

        enableUserSpecifiedInstanceProfile(false);

        return composite;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);

        Button button_UserSpecifiedRole = IAMOperationNotAllowedErrorDialog.this
                .getButton(PROCEED_WITH_EXISTING_ROLE);
        if (button_UserSpecifiedRole != null) {
            button_UserSpecifiedRole.setEnabled(false);
        }

        return control;
    }
    /**
     * Get the name of the instance profile that the user manually provided in
     * the dialog.
     */
    public String getInstanceProfileName() {
        return instanceProfileNameText;
    }

    private Group createGroupForOption_a(final Composite parent) {
        Group group = new Group(parent, SWT.BORDER);
        group.setText("(a) Modify IAM User Policy and then reload");
        group.setLayout(new GridLayout(1, false));

        Label label = new Label(group, SWT.WRAP);
        label.setText(String.format(
                "AWS Elastic Beanstalk requires a valid instance-profile configuration " +
                "that grants it permission to copy environment logs to your S3 bucket. " +
                "If you want the plugin to automatically configure the instance-profile for you, " +
                "please make sure the current IAM user has sufficient permission for \"iam:*\" actions:%n" +
                "    (1) Open up a browser and log into the AWS Management Console using your admin account.%n" +
                "    (2) In the IAM service page, find the IAM user that you are currently using in the plugin.%n" +
                "    (3) Modify the user policy to allow \"iam:*\" actions.%n" +
                "    (4) Return to this dialog and press \"Reload\""));
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        label.setLayoutData(gridData);

        return group;
    }

    private Group createGroupForOption_b(final Composite parent) {
        Group group = new Group(parent, SWT.BORDER);
        group.setText("(b) Configure a specific instance-profile");
        group.setLayout(new GridLayout(2, false));

        Label label = new Label(group, SWT.WRAP);
        label.setText(String.format(
                "If you do want to configure an instance-profile but meanwhile cannot modify your IAM user policy, " +
                "you can manually specify the name of the instance-profile here, and then " +
                "click \"Use the specified profile\"."
                ));
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);

        widget_UserSpecifiedProfileCheckbox = new Button(group, SWT.CHECK);
        widget_UserSpecifiedProfileCheckbox.setText("Manually specify an instance-profile");
        gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = 2;
        widget_UserSpecifiedProfileCheckbox.setLayoutData(gridData);

        widget_UserSpecifiedProfileCheckbox.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                boolean selected = widget_UserSpecifiedProfileCheckbox.getSelection();
                enableUserSpecifiedInstanceProfile(selected);

            }
        });
        widget_UserSpecifiedProfileCheckbox.setSelection(false);


        Label textLabel = new Label(group, SWT.NONE);
        gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        textLabel.setLayoutData(gridData);
        textLabel.setText("Instance profile name: ");

        widget_InstanceProfileName = new Text(group, SWT.BORDER);
        widget_InstanceProfileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        widget_InstanceProfileName.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                instanceProfileNameText = widget_InstanceProfileName.getText();
            }
        });
        widget_InstanceProfileName.setText(ElasticBeanstalkPublishingUtils.DEFAULT_ROLE_NAME);

        Label noteLabel = new Label(group, SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        noteLabel.setLayoutData(gridData);
        noteLabel.setText(
                "Note that whenever you create an IAM role in the AWS Management Console, " +
                "an instance-profile with the same name is also automatically created.");
        setItalicFont(noteLabel);

        return group;
    }

    private Group createGroupForOption_c(final Composite parent) {
        Group group = new Group(parent, SWT.BORDER);
        group.setText("(c) Proceed without an instance-profile");
        group.setLayout(new GridLayout(1, false));

        Label label = new Label(group, SWT.WRAP);
        label.setText(
                "If you do not wish to configure an instance-profile for this environment, " +
                "click \"Proceed without a profile\".");
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        label.setLayoutData(gridData);

        return group;
    }

    private void enableUserSpecifiedInstanceProfile(boolean enable) {
        Button button_UserSpecifiedRole = IAMOperationNotAllowedErrorDialog.this
                .getButton(PROCEED_WITH_EXISTING_ROLE);
        if (button_UserSpecifiedRole != null) {
            button_UserSpecifiedRole.setEnabled(enable);
        }

        Button button_NoRole = IAMOperationNotAllowedErrorDialog.this
                .getButton(PROCEED_WITHOUT_ROLE);
        if (button_NoRole != null) {
            button_NoRole.setEnabled(!enable);
        }

        if (widget_InstanceProfileName != null) {
            widget_InstanceProfileName.setEnabled(enable);
        }

    }

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    @Override
    public boolean close() {
        if (italicFont != null)
            italicFont.dispose();
        return super.close();
    }

}
