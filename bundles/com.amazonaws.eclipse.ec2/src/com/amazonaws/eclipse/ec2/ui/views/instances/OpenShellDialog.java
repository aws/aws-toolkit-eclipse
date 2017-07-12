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
package com.amazonaws.eclipse.ec2.ui.views.instances;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;

public final class OpenShellDialog extends MessageDialog {

    private Text userNameText;
    private String userName;

    public OpenShellDialog() {
        super(Display.getDefault().getActiveShell(),
            "SSH Connection Options", null,
            "Configure the SSH connection to your Amazon EC2 instance with the options below.",
            MessageDialog.INFORMATION,
            new String[] {"Connect"}, 0);
    }


    @Override
    protected Control createCustomArea(Composite parent) {
        parent.setLayout(new FillLayout());

        Composite control = new Composite(parent, SWT.NONE);
        control.setLayout(new GridLayout(2, false));

        new Label(control, SWT.NONE).setText("User Name: ");

        userNameText = new Text(control, SWT.BORDER);
        userNameText.setText(Ec2Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SSH_USER));
        userNameText.setSelection(0, userNameText.getText().length());
        userNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        userNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                userName = userNameText.getText();
                getButton(0).setEnabled(userName.length() > 0);
            }
        });

        return control;
    }

    public String getUserName() {
        return userName;
    }
}
