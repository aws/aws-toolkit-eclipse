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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;

final class PublishDialog extends MessageDialog {
    private static final String PUBLISH_DIALOG_SETTINGS_SECTION = "PublishDialogSettings";

    private final IDialogSettings publishDialogSettings;
    private String versionLabel;

    public PublishDialog(Shell parentShell, String defaultVersionLabel) {
        super(parentShell, "Publishing to AWS Elastic Beanstalk", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "Select a label for your new application version.", MessageDialog.NONE, new String[] {
                IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        this.versionLabel = defaultVersionLabel;

        if (ElasticBeanstalkPlugin.getDefault().getDialogSettings().getSection(PUBLISH_DIALOG_SETTINGS_SECTION) == null) {
            publishDialogSettings = ElasticBeanstalkPlugin.getDefault().getDialogSettings().addNewSection(PUBLISH_DIALOG_SETTINGS_SECTION);
        } else {
            publishDialogSettings = ElasticBeanstalkPlugin.getDefault().getDialogSettings().getSection(PUBLISH_DIALOG_SETTINGS_SECTION);
        }
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Composite composite = parent;
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        createVersionTextBox(composite);
        createDurationLabel(composite);

        composite.pack(true);
        return composite;
    }

    private void createVersionTextBox(Composite composite) {
        Label versionLabelLabel = new Label(composite, SWT.NONE);
        versionLabelLabel.setText("Version Label:  ");

        final Text versionLabelText = new Text(composite, SWT.BORDER);
        versionLabelText.setText(versionLabel);
        versionLabelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        versionLabelText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                versionLabel = versionLabelText.getText();
                getButton(0).setEnabled(versionLabel.length() > 0);
            }
        });
    }

    private void createDurationLabel(Composite composite) {
        final Label info = new Label(composite, SWT.READ_ONLY | SWT.WRAP);
        info.setText("Note: Launching a new environment may take several minutes.  " +
                "To monitor its progress, check the Progress View.");
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = 2;
        gridData.widthHint = 300;
        info.setLayoutData(gridData);
    }
}
