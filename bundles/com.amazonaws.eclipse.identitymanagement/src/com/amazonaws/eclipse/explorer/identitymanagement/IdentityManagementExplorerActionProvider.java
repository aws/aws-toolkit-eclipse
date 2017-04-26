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
package com.amazonaws.eclipse.explorer.identitymanagement;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.explorer.identitymanagement.IdentityManagementContentProvider.GroupNode;
import com.amazonaws.eclipse.explorer.identitymanagement.IdentityManagementContentProvider.IdentityManagementRootElement;
import com.amazonaws.eclipse.explorer.identitymanagement.IdentityManagementContentProvider.RoleNode;
import com.amazonaws.eclipse.explorer.identitymanagement.IdentityManagementContentProvider.UserNode;

/**
 * Action provider when right-clicking on IAM nodes in the explorer.
 */
public class IdentityManagementExplorerActionProvider extends CommonActionProvider {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.
     * action.IMenuManager)
     */
    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection) getActionSite()
                .getStructuredViewer().getSelection();
        if (selection.size() != 1)
            return;
        if (selection.getFirstElement() instanceof IdentityManagementRootElement
                || selection.getFirstElement() instanceof UserNode) {
            menu.add(new CreateUserAction());
        }
        if (selection.getFirstElement() instanceof IdentityManagementRootElement
                || selection.getFirstElement() instanceof GroupNode) {
            menu.add(new CreateGroupAction());
        }
        if (selection.getFirstElement() instanceof IdentityManagementRootElement
                || selection.getFirstElement() instanceof RoleNode) {
            menu.add(new CreateRoleAction());
        }
    }
}
