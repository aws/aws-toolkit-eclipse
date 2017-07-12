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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.AbstractUserTable;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteSigningCertificateRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListSigningCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListSigningCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesResult;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.SigningCertificate;
import com.amazonaws.services.identitymanagement.model.User;

public class UserTable extends AbstractUserTable {

    private UserSummary userSummary = null;
    private UserPermission userPermission = null;
    private GroupsForUser userGroups = null;


    public UserTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit, EditorInput userEditorInput) {
        super(iam, parent, toolkit, userEditorInput);

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
                            userSummary.setUser(null);
                            userPermission.setUser(null);
                            userGroups.setUser(null);
                            deleteMultipleUsers(viewer.getTable().getSelectionIndices());
                        }

                        @Override
                        public String getText() {
                            if (viewer.getTable().getSelectionIndices().length > 1) {
                                return "Delete Users";
                            }
                            return "Delete User";
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
                User user = contentProvider.getItemByIndex(viewer.getTable().getSelectionIndex());
                userSummary.setUser(user);
                userPermission.setUser(user);
                userGroups.setUser(user);
                } else {
                    userSummary.setUser(null);
                    userPermission.setUser(null);
                    userGroups.setUser(null);
                }
            }
        });
    }

    public void setUserSummary(UserSummary userSummary) {
        this.userSummary = userSummary;
    }

    public void setUserPermission(UserPermission userPermission) {
        this.userPermission = userPermission;
    }

    public void setGroups(GroupsForUser groups) {
        this.userGroups = groups;
    }

    private void deleteMultipleUsers(final int[] indices) {
        new Job("Delete users") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (int index : indices) {
                    String userName = contentProvider.getItemByIndex(index).getUserName();
                    try {
                        deleteUser(userName);
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete users: " + e.getMessage(), e);
                    }
                }
                refresh();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void deleteUser(String userName) {
        deleteAccessKeysForUser(userName);
        deleteUserPoliciesForUser(userName);
        deleteCertificatesForUser(userName);
        deleteUserFromGroups(userName);
        try {
            iam.deleteLoginProfile(new DeleteLoginProfileRequest().withUserName(userName));
        } catch (Exception e) {
            // No body cares.
        }
        iam.deleteUser(new DeleteUserRequest().withUserName(userName));
    }

    private void deleteAccessKeysForUser(String userName) {
        ListAccessKeysResult response = iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName));
        for (AccessKeyMetadata akm : response.getAccessKeyMetadata()) {
            iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(userName).withAccessKeyId(akm.getAccessKeyId()));
        }
    }

    private void deleteUserPoliciesForUser(String userName) {
        ListUserPoliciesResult response = iam.listUserPolicies(new ListUserPoliciesRequest().withUserName(userName));
        for (String pName : response.getPolicyNames()) {
            iam.deleteUserPolicy(new DeleteUserPolicyRequest().withUserName(userName).withPolicyName(pName));
        }
    }

    private void deleteCertificatesForUser(String userName) {
        ListSigningCertificatesResult response = iam.listSigningCertificates(new ListSigningCertificatesRequest().withUserName(userName));
        for (SigningCertificate cert : response.getCertificates()) {
            iam.deleteSigningCertificate(new DeleteSigningCertificateRequest().withUserName(userName).withCertificateId(cert.getCertificateId()));
        }
    }

    private void deleteUserFromGroups(String userName) {
        List<Group> groups = iam.listGroupsForUser(new ListGroupsForUserRequest().withUserName(userName)).getGroups();
        for (Group group : groups) {
            iam.removeUserFromGroup(new RemoveUserFromGroupRequest().withGroupName(group.getGroupName()).withUserName(userName));
        }
    }

    @Override
    public void refresh() {
        new LoadUserTableThread().start();
    }

    @Override
    protected void listUsers() {
        users = iam.listUsers().getUsers();
    }
}
