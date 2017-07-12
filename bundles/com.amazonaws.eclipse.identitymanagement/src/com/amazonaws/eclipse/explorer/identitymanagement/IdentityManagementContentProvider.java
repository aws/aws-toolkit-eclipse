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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;

public class IdentityManagementContentProvider extends AbstractContentProvider {

    public static final class IdentityManagementRootElement {
        public static final IdentityManagementRootElement ROOT_ELEMENT = new IdentityManagementRootElement();
    }

    public static class UserNode extends ExplorerNode {
        public UserNode() {
            super("Users", 1, loadImage(AwsToolkitCore.IMAGE_USER), new OpenUserEditorAction("Users"));
        }
    }

    public static class GroupNode extends ExplorerNode {
        public GroupNode() {
            super("Groups", 0, loadImage(AwsToolkitCore.IMAGE_GROUP), new OpenGroupEditorAction("Groups"));
        }
    }

    public static class RoleNode extends ExplorerNode {
        public RoleNode() {
            super("Roles", 2, loadImage(AwsToolkitCore.IMAGE_ROLE), new OpenRolesEditorAction("Roles"));
        }
    }

    public static class PasswordPolicyNode extends ExplorerNode {
        public PasswordPolicyNode() {
            super("Password Policy", 3, loadImage(AwsToolkitCore.IMAGE_KEY), new OpenPasswordPolicyEditorAction("Password Policy"));
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement || element instanceof IdentityManagementRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { IdentityManagementRootElement.ROOT_ELEMENT };
        }

        if (parentElement instanceof IdentityManagementRootElement) {
            List<ExplorerNode> iamNodes = new ArrayList<>();
            ExplorerNode userNode = new UserNode();
            iamNodes.add(userNode);
            ExplorerNode groupNode = new GroupNode();
            iamNodes.add(groupNode);
            ExplorerNode roleNode = new RoleNode();
            iamNodes.add(roleNode);
            ExplorerNode passworPolicyNode = new PasswordPolicyNode();
            iamNodes.add(passworPolicyNode);
            return iamNodes.toArray();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.IAM;
    }

}
