/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.identitymanagement.user;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.eclipse.identitymanagement.user.UserCredentialManagementDialog.AccessKeyTable;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;

public class NewAccessKeyDialog extends MessageDialog {

    private String accessKeyIdLabelContent = "Access Key Id: ";
    private String secretAccessKeyContent = "Secret Access Key: ";
    private AmazonIdentityManagement iam;
    private String userName;
    private Text accessKeyIdText;
    private Text secretKeyText;
    private AccessKeyTable accessKeyTable;
    private Button downloadButton;
    private CreateAccessKeyResult createAccessKeyResult;

    public NewAccessKeyDialog(AmazonIdentityManagement iam, String userName, AccessKeyTable accessKeyTable) {
        super(Display.getCurrent().getActiveShell(), "Manage Access Key", null, null, MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0);
        this.iam = iam;
        this.userName = userName;
        this.accessKeyTable = accessKeyTable;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        new Label(parent, SWT.NONE).setText("This is the last time these user security credentials will be available for download. You can manage \n and recreate these credentials any time.\n");
        accessKeyIdText = new Text(parent, SWT.READ_ONLY);
        accessKeyIdText.setText(accessKeyIdLabelContent);
        accessKeyIdText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(accessKeyIdText);
        secretKeyText = new Text(parent, SWT.READ_ONLY);
        secretKeyText.setText(secretAccessKeyContent);
        secretKeyText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(secretKeyText);
        downloadButton = new Button(parent, SWT.PUSH);
        downloadButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_DOWNLOAD));
        downloadButton.setText("Download");
        // The button will be enabled after the new key has been created
        downloadButton.setEnabled(false);
        downloadButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
                fd.setText("Save As");
                fd.setFileName("credentials.csv");
                String[] filterExt = { "*.csv" };
                fd.setFilterExtensions(filterExt);
                String path = fd.open();
                if (path != null) {
                    try {
                        saveFile(path);
                    } catch (Exception exception) {
                        Status status = new Status(IStatus.ERROR, IdentityManagementPlugin.PLUGIN_ID, "Unable to download the file: " + exception.getMessage(), exception);
                        StatusManager.getManager().handle(status, StatusManager.SHOW);
                    }
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        new CreateAccessKeyThread().start();
        return parent;
    }

    // Save the file
    private void saveFile(String fileName) throws IOException {
        File f = new File(fileName);
        // Check whether the file already exists.
        if (f.createNewFile() == false) {
            throw new IOException("File already exists " + fileName);
        }
        String content = "";
        FileWriter fstream = new FileWriter(f.getAbsoluteFile());
        try (BufferedWriter out = new BufferedWriter(fstream)) {
            out.write("\"User Name\",\"Access Key Id\",\"Secret Access Key\"\n");
            content += "\"" + userName + "\",";
            content += "\"" + createAccessKeyResult.getAccessKey().getAccessKeyId() + "\",";
            content += "\"" + createAccessKeyResult.getAccessKey().getSecretAccessKey() + "\"";
            out.write(content);
        }
    }

    // Create the new access key and update the UI
    private class CreateAccessKeyThread extends Thread {
        @Override
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        createAccessKeyResult = iam.createAccessKey(new CreateAccessKeyRequest().withUserName(userName));
                        accessKeyIdLabelContent += createAccessKeyResult.getAccessKey().getAccessKeyId();
                        secretAccessKeyContent += createAccessKeyResult.getAccessKey().getSecretAccessKey();
                        accessKeyIdText.setText(accessKeyIdLabelContent);
                        secretKeyText.setText(secretAccessKeyContent);
                        downloadButton.setEnabled(true);
                        accessKeyTable.refresh();
                    } catch (Exception e) {
                        NewAccessKeyDialog.this.close();
                        Status status = new Status(IStatus.ERROR, IdentityManagementPlugin.PLUGIN_ID, "Unable to create access key: " + e.getMessage(), e);
                        StatusManager.getManager().handle(status, StatusManager.SHOW);
                    }
                }
            });
        }
    }
}
