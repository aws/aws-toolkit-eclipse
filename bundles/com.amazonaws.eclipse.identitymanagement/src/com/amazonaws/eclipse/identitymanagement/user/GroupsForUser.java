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
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.User;

public class GroupsForUser extends Composite {

    private GroupForUserTable groupForUserTable;
    private User user;
    private Button addGroupButton;

    public GroupsForUser(final AmazonIdentityManagement iam, Composite parent, final FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.setBackground(toolkit.getColors().getBackground());

        this.setLayout(new GridLayout());

        Section policySection = toolkit.createSection(this, Section.TITLE_BAR);
        policySection.setText("Groups");
        policySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite client = toolkit.createComposite(policySection, SWT.WRAP);
        client.setLayoutData(new GridData(GridData.FILL_BOTH));
        client.setLayout(new GridLayout(2, false));

        groupForUserTable = new GroupForUserTable(iam, client, toolkit);
        groupForUserTable.setLayoutData(new GridData(GridData.FILL_BOTH));


        addGroupButton = toolkit.createButton(client, "Add To Groups", SWT.PUSH);

        addGroupButton.setEnabled(false);
        addGroupButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addGroupButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addGroupButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AddUserToGroupsDialog addUserToGroupsDialog = new AddUserToGroupsDialog(iam, Display.getCurrent().getActiveShell(), toolkit, user, groupForUserTable);
                addUserToGroupsDialog.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        policySection.setClient(client);
    }

    public void setUser(User user) {
        this.user = user;
        groupForUserTable.setUser(user);
        if (user != null) {
            addGroupButton.setEnabled(true);
        } else {
            addGroupButton.setEnabled(false);
        }
    }

    public void refresh() {
        groupForUserTable.refresh();
    }


}
