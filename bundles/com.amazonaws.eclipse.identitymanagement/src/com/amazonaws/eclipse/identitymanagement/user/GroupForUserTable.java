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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.AbstractGroupTable;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.User;

public class GroupForUserTable extends AbstractGroupTable {

    private User user;

    public GroupForUserTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
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
                            for (int index : viewer.getTable().getSelectionIndices()) {
                                String groupName = contentProvider.getItemByIndex(index).getGroupName();
                                removeUserFromGroup(groupName);
                            }
                            refresh();
                        }

                        @Override
                        public String getText() {
                            if (viewer.getTable().getSelectionIndices().length > 1) {
                                return "Remove From Groups";
                            }
                            return "Remove From Group";
                        }
                    });
                }
            }
        });
        viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));
    }

    private void removeUserFromGroup(String groupName) {
        iam.removeUserFromGroup(new RemoveUserFromGroupRequest().withGroupName(groupName).withUserName(user.getUserName()));
    }

    public void setUser(User user) {
        this.user = user;
        refresh();
    }

    @Override
    public void refresh() {
        new LoadGroupTableThread().start();
    }

    @Override
    protected void listGroups() {
        if (user != null) {
            groups = iam.listGroupsForUser(new ListGroupsForUserRequest().withUserName(user.getUserName())).getGroups();
        } else {
            groups = null;
        }
    }
}
