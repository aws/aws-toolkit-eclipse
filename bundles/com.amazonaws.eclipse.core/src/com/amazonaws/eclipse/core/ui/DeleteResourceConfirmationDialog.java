/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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

/**
 * A reusable dialog for confirming deleting an AWS resource.
 */
public class DeleteResourceConfirmationDialog extends TitleAreaDialog {
    private final String resourceName;
    private final String title;
    private final String message;
    private final String deleteResourceLabel;
    private final String deleteConfirmationLabel;

    private Text resourceNameText;

    public DeleteResourceConfirmationDialog(Shell parentShell, String resourceName, String resource) {
        super(parentShell);
        this.resourceName = resourceName;
        this.title = String.format("Delete %s", resource);
        this.message = String.format("Delete the %s %s permanently? This cannot be undone.", resource, resourceName);
        this.deleteResourceLabel = String.format("Type the name of the %s to confirm deletion:", resource);
        this.deleteConfirmationLabel = String.format("Are you sure you want to delete this %s permanently?", resource);
    }

    @Override
    public void create() {
        super.create();
        setTitle(title);
        setMessage(message, IMessageProvider.WARNING);

        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);

        createResourceNameSection(container);

        return area;
    }

    private void createResourceNameSection(Composite container) {
        new Label(container, SWT.NONE).setText(deleteResourceLabel);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;

        resourceNameText = new Text(container, SWT.BORDER);
        resourceNameText.setLayoutData(gridData);
        resourceNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                getButton(IDialogConstants.OK_ID).setEnabled(resourceName.equals(resourceNameText.getText()));
            }
        });

        new Label(container, SWT.NONE).setText(deleteConfirmationLabel);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
