/*
 * Copyright 2012 Amazon Technologies, Inc.
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to create a new SNS topic.
 */
public class CreateTopicDialog extends MessageDialog {

    private String topicName;

    public CreateTopicDialog() {
        super(Display.getDefault().getActiveShell(), "Create New SNS Topic", null, "Enter the name for your new SNS Topic.",
            MessageDialog.INFORMATION, new String[] {"OK", "Cancel"}, 0);
    }

    public String getTopicName() {
        return topicName;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        composite.setLayout(new GridLayout(2, false));

        new Label(composite, SWT.NONE).setText("Topic Name:");
        final Text topicNameText = new Text(composite, SWT.BORDER);
        topicNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        topicNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                topicName = topicNameText.getText();
                updateControls();
            }
        });

        return composite;
    }

    private void updateControls() {
        boolean isValid = (topicName != null && topicName.trim().length() > 0);

        Button okButton = this.getButton(0);
        if (okButton != null) okButton.setEnabled(isValid);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateControls();
    }
}