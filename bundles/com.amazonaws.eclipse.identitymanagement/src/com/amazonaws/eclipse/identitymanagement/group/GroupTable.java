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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.AbstractGroupTable;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesResult;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.UpdateGroupRequest;
import com.amazonaws.services.identitymanagement.model.User;

public class GroupTable extends AbstractGroupTable {

    private GroupSummary groupSummary;
    private UsersInGroup usersInGroup;
    private GroupPermissions groupPermissions;
    private final String DELTE_GROUP_CONFIRMATION = "All users and permissions belonging to the selected groups will be removed from the group first. Do you want to continue?";

    public GroupTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(iam, parent, toolkit);

        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if (viewer.getTable().getSelectionCount() > 0) {

                    manager.add(new Action() {

                        @Override
                        public ImageDescriptor getImageDescriptor() {
                            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                        }

                        @Override
                        public void run() {
                            boolean confirmation = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Delete Group", DELTE_GROUP_CONFIRMATION);
                            if (confirmation) {
                                groupSummary.setGroup(null);
                                usersInGroup.setGroup(null);
                                groupPermissions.setGroup(null);
                                deleteMultipleGroups(viewer.getTable().getSelectionIndices());
                            }
                        }

                        @Override
                        public String getText() {
                            if (viewer.getTable().getSelectionIndices().length > 1) {
                                return "Delete Groups";
                            }
                            return "Delete Group";
                        }

                    });

                    manager.add(new Action() {

                        @Override
                        public ImageDescriptor getImageDescriptor() {
                            return null;
                        }

                        @Override
                        public void run() {
                            EditGroupNameDialog editGroupNameDialog = new EditGroupNameDialog(contentProvider.getItemByIndex(viewer.getTable().getSelectionIndex()).getGroupName());
                            if (editGroupNameDialog.open() == 0) {
                                editGroupName(editGroupNameDialog.getOldGroupName(), editGroupNameDialog.getNewGroupName());
                            }
                        }

                        @Override
                        public String getText() {
                            return "Edit Group Name";
                        }
                    });
                }
            }
        });

        viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));

        viewer.getTable().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int index = viewer.getTable().getSelectionIndex();
                if (index >= 0) {
                Group group = contentProvider.getItemByIndex(index);
                groupSummary.setGroup(group);
                usersInGroup.setGroup(group);
                groupPermissions.setGroup(group);
                } else {
                    groupSummary.setGroup(null);
                    usersInGroup.setGroup(null);
                    groupPermissions.setGroup(null);
                }
            }
        });


    }

    public void setGroupSummary(GroupSummary groupSummary) {
        this.groupSummary = groupSummary;
    }

    public void setUsersInGroup(UsersInGroup usersInGroup) {
        this.usersInGroup = usersInGroup;
    }

    public void setGroupPermissions(GroupPermissions groupPermissions) {
        this.groupPermissions = groupPermissions;
    }

    private void deleteGroup(String groupName) {
        ListGroupPoliciesResult listGroupPoliciesResult = iam.listGroupPolicies(new ListGroupPoliciesRequest().withGroupName(groupName));
        for (String policyName : listGroupPoliciesResult.getPolicyNames()) {
            iam.deleteGroupPolicy(new DeleteGroupPolicyRequest().withGroupName(groupName).withPolicyName(policyName));
        }

        List<User> usersInGroup = iam.getGroup(new GetGroupRequest().withGroupName(groupName)).getUsers();
        for (User user : usersInGroup) {
            iam.removeUserFromGroup(new RemoveUserFromGroupRequest().withGroupName(groupName).withUserName(user.getUserName()));
        }
        iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupName));
    }

    private void editGroupName(final String oldGroupName, final String newGroupName) {
        new Job("Edit group name") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    iam.updateGroup(new UpdateGroupRequest().withGroupName(oldGroupName).withNewGroupName(newGroupName));
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to edit the group name : " + e.getMessage(), e);
                }
                refresh();
                return Status.OK_STATUS;
            }
        }.schedule();

    }

    private void deleteMultipleGroups(final int[] indices) {
        new Job("Delete groups") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (int index : indices) {
                    String groupName = contentProvider.getItemByIndex(index).getGroupName();
                    try {
                        deleteGroup(groupName);
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete groups: " + e.getMessage(), e);
                    }
                }
                refresh();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    protected void listGroups() {
        groups = iam.listGroups().getGroups();
    }



    @Override
    public void refresh() {
        new LoadGroupTableThread().start();
    }

}
