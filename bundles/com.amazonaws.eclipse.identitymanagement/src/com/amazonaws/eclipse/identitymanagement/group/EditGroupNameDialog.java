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
package com.amazonaws.eclipse.identitymanagement.group;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

public class EditGroupNameDialog extends MessageDialog {

    private String newGroupName = "";
    private String oldGroupName;

    public String getNewGroupName() {
        return newGroupName.trim();
    }

    public String getOldGroupName() {
        return oldGroupName.trim();
    }

    protected EditGroupNameDialog(String groupName) {
        super(Display.getCurrent().getActiveShell(), "Enter New Group Name", null, "Enter a new group name", MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0);
        this.oldGroupName = groupName;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        final Text text = new Text(parent, SWT.BORDER);
        text.setText(oldGroupName);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(text);
        text.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                newGroupName = text.getText();
                validate();
            }
        });

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        validate();
    }

    public void validate() {
        if (getButton(0) == null)
            return;
        if (getNewGroupName().length() == 0 || getNewGroupName().equals(getOldGroupName())) {
            getButton(0).setEnabled(false);
            return;
        }
        getButton(0).setEnabled(true);
        return;
    }
}
