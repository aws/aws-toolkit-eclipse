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
package com.amazonaws.eclipse.explorer.s3.dnd;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;

public class KeySelectionDialog extends MessageDialog {

    String keyName;

    public KeySelectionDialog(Shell shell, File toUpload) {
        this(shell, "", toUpload);
    }

    public KeySelectionDialog(Shell shell, String prefix, File toUpload) {
        super(shell, "Choose a key name", AwsToolkitCore.getDefault()
                .getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON), "Enter a key to upload the file", 0,
                new String[] { "OK", "Cancel" }, 0);
        this.keyName = prefix + toUpload.getName();
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        final Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        text.setText(keyName);

        text.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                keyName = text.getText();
            }
        });

        return parent;
    }

    public String getKeyName() {
        return keyName;
    }

}
