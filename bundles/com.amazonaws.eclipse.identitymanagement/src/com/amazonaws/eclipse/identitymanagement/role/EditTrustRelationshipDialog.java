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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.UpdateAssumeRolePolicyRequest;

public class EditTrustRelationshipDialog extends TitleAreaDialog {
    private Text policyText;
    private Role role;
    private AmazonIdentityManagement iam;

    public EditTrustRelationshipDialog(AmazonIdentityManagement iam, Shell parentShell, Role role) {
        super(parentShell);
        this.role = role;
        this.iam = iam;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        setTitle("Edit Trust Relationship");
        setMessage("You can customize trust relationships by editing the following access control policy document. ");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));

        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        policyText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        policyText.setLayoutData(new GridData(GridData.FILL_BOTH));
        try {
            policyText.setText(getAssumeRolePolicy());
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
        Dialog.applyDialogFont(parent);
        return composite;
    }

    @Override
    protected void okPressed() {

        final String policyDoc = policyText.getText();

        new Job("Update assume role policy") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {

                    updateAssumeRolePolicy(policyDoc);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to update the assume role policies: " + e.getMessage(), e);
                }
            }
        }.schedule();
        super.okPressed();
    }

    private void updateAssumeRolePolicy(String policyDoc) {
        iam.updateAssumeRolePolicy(new UpdateAssumeRolePolicyRequest().withRoleName(role.getRoleName()).withPolicyDocument(policyDoc));
    }

    private String getAssumeRolePolicy() throws UnsupportedEncodingException  {
        String policyDoc = role.getAssumeRolePolicyDocument();
        return URLDecoder.decode(policyDoc, "UTF-8");
    }

}
