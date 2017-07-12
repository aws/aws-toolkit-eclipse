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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.User;

public class AddUsersToGroupDialog extends TitleAreaDialog {
    private final FormToolkit toolkit;
    private final List<Button> userButtons = new ArrayList<>();
    private final Group group;
    private final List<User> usersInGroup;
    private AmazonIdentityManagement iam;
    private UsersInGroupTable usersInGroupTable;

    public AddUsersToGroupDialog(AmazonIdentityManagement iam, Shell parentShell, FormToolkit toolkit, Group group, UsersInGroupTable usersInGroupTable) {
        super(parentShell);
        this.toolkit = toolkit;
        this.group = group;
        this.iam = iam;
        usersInGroup = getUsersInGroup();
        this.usersInGroupTable = usersInGroupTable;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("Select the users to add to the group " + "'" + group.getGroupName() + "'");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Add Users To Group");
        shell.setMinimumSize(400, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.setLayout(new GridLayout());
        composite.setBackground(toolkit.getColors().getBackground());

        ScrolledForm form = new ScrolledForm(composite, SWT.V_SCROLL);
        form.setLayoutData(new GridData(GridData.FILL_BOTH));
        form.setBackground(toolkit.getColors().getBackground());
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.getBody().setLayout(new GridLayout(1, false));

        Label label = toolkit.createLabel(form.getBody(), "User Name:");
        label.setFont(new Font(Display.getCurrent(), "Arial", 12, SWT.BOLD));

        for (User user : listUsers()) {
            Button button = toolkit.createButton(form.getBody(), user.getUserName(), SWT.CHECK);
            userButtons.add(button);
            if (usersInGroup.contains(user)) {
                button.setSelection(true);
                button.setEnabled(false);
            }
        }
        return composite;

    }

    private void addUserToGroup(String userName) {
        iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(group.getGroupName()).withUserName(userName));
    }

    private List<User> getUsersInGroup() {
        return iam.getGroup(new GetGroupRequest().withGroupName(group.getGroupName())).getUsers();
    }

    private List<User> listUsers() {
        return iam.listUsers().getUsers();
    }

    @Override
    protected void okPressed() {

        final List<String> users = new LinkedList<>();
        for (Button button : userButtons) {
            if (button.getSelection() && button.getEnabled()) {
                users.add(button.getText());
            }
        }

        new Job("Adding users to group") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    for (String user : users) {
                        addUserToGroup(user);
                    }
                    usersInGroupTable.refresh();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to add user to groups: " + e.getMessage(), e);
                }

            }
        }.schedule();
        super.okPressed();
    }
}
