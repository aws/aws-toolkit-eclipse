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
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.User;

public class AddUserToGroupsDialog extends TitleAreaDialog {

    private FormToolkit toolkit;
    private List<Button> groupButtons = new ArrayList<>();
    private User user;
    private List<Group> groupsForUser;
    private AmazonIdentityManagement iam;
    private GroupForUserTable groupForUserTable;

    public AddUserToGroupsDialog(AmazonIdentityManagement iam, Shell parentShell, FormToolkit toolkit,  User user, GroupForUserTable groupForUserTable) {
        super(parentShell);
        this.toolkit = toolkit;
        this.user = user;
        this.iam = iam;
        groupsForUser = getGroupsForUser();
        this.groupForUserTable = groupForUserTable;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("Select the groups to add the user " + "'" + user.getUserName() + "'");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Add User To Groups");
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

        Label label = toolkit.createLabel(form.getBody(), "Group Name:");
        label.setFont(new Font(Display.getCurrent(), "Arial", 12, SWT.BOLD));

        for (Group group : listGroups()) {
            Button button = toolkit.createButton(form.getBody(), group.getGroupName(), SWT.CHECK);
            groupButtons.add(button);
            if (groupsForUser.contains(group)) {
                button.setSelection(true);
                button.setEnabled(false);
            }
        }
        return composite;
    }

    private void addUserToGroup(String groupName) {
        iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(groupName).withUserName(user.getUserName()));
    }

    private List<Group> getGroupsForUser() {
        return iam.listGroupsForUser(new ListGroupsForUserRequest().withUserName(user.getUserName())).getGroups();
    }

    private List<Group> listGroups() {
        return iam.listGroups().getGroups();
    }

    @Override
    protected void okPressed() {
        final List<String> groups = new LinkedList<>();
        for (Button button : groupButtons) {
            if (button.getSelection() && button.getEnabled()) {
                groups.add(button.getText());
            }
        }

        new Job("Adding user to groups") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    for (String group : groups) {
                        addUserToGroup(group);
                    }
                    groupForUserTable.refresh();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to adding user to groups: " + e.getMessage(), e);
                }

            }
        }.schedule();

        super.okPressed();
    }

}
