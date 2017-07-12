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
package com.amazonaws.eclipse.identitymanagement.role;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.explorer.identitymanagement.AbstractAddPolicyDialog;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.Role;

class AddRolePolicyDialog extends AbstractAddPolicyDialog {

    private final Role role;

    public AddRolePolicyDialog(AmazonIdentityManagement iam, Shell parentShell, FormToolkit toolkit, Role role, RolePermissionTable rolePermissionTable) {
        super(iam, parentShell, toolkit, rolePermissionTable);
        this.iam = iam;
        this.role = role;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Manage Role Permission");
    }

    @Override
    protected void putPolicy(String policyName, String policyDoc) {
        iam.putRolePolicy(new PutRolePolicyRequest().withRoleName(role.getRoleName()).withPolicyDocument(policyDoc).withPolicyName(policyName));
    }

}
