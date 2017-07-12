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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Group;

public class UsersInGroup extends Composite {

    private UsersInGroupTable usersInGroupTable;
    private Group group;
    private Button addUserButton;

    public UsersInGroup(final AmazonIdentityManagement iam, Composite parent, final FormToolkit toolkit, final EditorInput groupEditorInput) {
        super(parent, SWT.NONE);

        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.setBackground(toolkit.getColors().getBackground());

        this.setLayout(new GridLayout());

        Section policySection = toolkit.createSection(this, Section.TITLE_BAR);
        policySection.setText("Users");
        policySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite client = toolkit.createComposite(policySection, SWT.WRAP);
        client.setLayoutData(new GridData(GridData.FILL_BOTH));
        client.setLayout(new GridLayout(2, false));

        usersInGroupTable = new UsersInGroupTable(iam, client, toolkit, groupEditorInput);
        usersInGroupTable.setLayoutData(new GridData(GridData.FILL_BOTH));


        addUserButton = toolkit.createButton(client, "Add Users", SWT.PUSH);

        addUserButton.setEnabled(false);
        addUserButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addUserButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addUserButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AddUsersToGroupDialog addUserToGroupDialog = new AddUsersToGroupDialog(iam, Display.getCurrent().getActiveShell(), toolkit, group, usersInGroupTable);
                addUserToGroupDialog.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        policySection.setClient(client);
    }

    public void setGroup(Group group) {
        this.group = group;
        usersInGroupTable.setGroup(group);
        if (group != null) {
            addUserButton.setEnabled(true);
        } else {
            addUserButton.setEnabled(false);
        }
    }

    public void refresh() {
        usersInGroupTable.refresh();
    }
}
