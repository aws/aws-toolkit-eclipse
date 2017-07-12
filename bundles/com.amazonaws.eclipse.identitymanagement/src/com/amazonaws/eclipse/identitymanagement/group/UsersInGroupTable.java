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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.AbstractUserTable;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;

public class UsersInGroupTable extends AbstractUserTable {

    private Group group;
    AmazonIdentityManagement iam;


    public UsersInGroupTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit, EditorInput userEditorInput) {
        super(iam, parent, toolkit, userEditorInput);

        this.iam = iam;
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
                            for (int index : viewer.getTable().getSelectionIndices()) {
                                String userName = contentProvider.getItemByIndex(index).getUserName();
                                removeUser(userName);
                            }
                            refresh();
                        }

                        @Override
                        public String getText() {
                            if (viewer.getTable().getSelectionIndices().length > 1) {
                                return "Remove Users";
                            }
                            return "Remove User";
                        }

                    });
                }
            }
        });
        viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));
    }



    private void removeUser(String userName) {
        iam.removeUserFromGroup(new RemoveUserFromGroupRequest().withGroupName(group.getGroupName()).withUserName(userName));
    }

    public void setGroup(Group group) {
        this.group = group;
        refresh();
    }

    @Override
    public void refresh() {
        new LoadUserTableThread().start();
    }

    @Override
    protected void listUsers() {
        if (group != null) {
        users = iam.getGroup(new GetGroupRequest(group.getGroupName())).getUsers();
        } else {
            users = null;
        }
    }
}
