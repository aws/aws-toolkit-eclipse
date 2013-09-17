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

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.explorer.identitymanagement.AbstractAddPolicyDialog;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.User;

class AddUserPolicyDialog extends AbstractAddPolicyDialog {

    private final User user;

    public AddUserPolicyDialog(AmazonIdentityManagement iam, Shell parentShell, FormToolkit toolkit, User user, UserPermissionTable userPermissionTable) {
        super(iam, parentShell, toolkit, userPermissionTable);
        this.user = user;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Manage User Permission");
    }


    @Override
    protected void putPolicy(String policyName, String policyDoc) {
        iam.putUserPolicy(new PutUserPolicyRequest().withUserName(user.getUserName()).withPolicyDocument(policyDoc).withPolicyName(policyName));
    }

}
