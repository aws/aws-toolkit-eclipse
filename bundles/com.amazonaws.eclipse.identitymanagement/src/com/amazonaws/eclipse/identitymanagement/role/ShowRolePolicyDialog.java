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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.Role;

class ShowRolePolicyDialog extends TitleAreaDialog {
    private String policyName;
    private boolean edittable;
    private Text policyText;
    EditorInput roleEditorInput;
    private Role role;
    private AmazonIdentityManagement iam;

    public ShowRolePolicyDialog(AmazonIdentityManagement iam, Shell parentShell, Role role, String policyName, boolean edittable) {
        super(parentShell);
        this.role = role;
        this.policyName = policyName;
        this.edittable = edittable;
        this.iam = iam;

    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        setTitle("Policy Name :");
        setMessage(policyName);
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));

        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        policyText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        try {
            policyText.setText(getPolicy(policyName));
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }

        policyText.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (!edittable) {
            policyText.setEditable(false);
        }
        Dialog.applyDialogFont(parent);
        return composite;
    }

    @Override
    protected void okPressed() {
        try {
            if (edittable) {
                putPolicy(policyName, policyText.getText());
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            return;
        }
        super.okPressed();
    }

    private void putPolicy(String policyName, String policyDoc) {
         iam.putRolePolicy(new PutRolePolicyRequest().withRoleName(role.getRoleName()).withPolicyDocument(policyDoc).withPolicyName(policyName));
    }

    private String getPolicy(String policyName) throws UnsupportedEncodingException {
        String policyDoc = iam.getRolePolicy(new GetRolePolicyRequest().withPolicyName(policyName).withRoleName(role.getRoleName())).getPolicyDocument();
        return URLDecoder.decode(policyDoc, "UTF-8");
    }
}
