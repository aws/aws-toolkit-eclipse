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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

/**
 * Adding JSON policy dialog base class for user, group and role.
 */
public abstract class AbstractAddPolicyDialog extends TitleAreaDialog {

    protected Text policyDocText;
    protected Text policyNameText;
    protected final FormToolkit toolkit;
    protected final String ConceptUrl = "http://docs.aws.amazon.com/IAM/latest/UserGuide/AccessPolicyLanguage_KeyConcepts.html";
    protected AmazonIdentityManagement iam;
    protected AbstractPolicyTable policyTable;
    // The OK button for the dialog.
    protected Button okButton;

    public AbstractAddPolicyDialog(AmazonIdentityManagement iam, Shell parentShell, FormToolkit toolkit, AbstractPolicyTable policyTable) {
        super(parentShell);
        this.toolkit = toolkit;
        this.iam = iam;
        this.policyTable = policyTable;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("You can customize permissions by editing the following policy document.");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        okButton = getButton(IDialogConstants.OK_ID);
        validate();
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));
        composite.setBackground(toolkit.getColors().getBackground());
        toolkit.createLabel(composite, "Policy Name:");
        policyNameText = toolkit.createText(composite, "", SWT.BORDER);
        policyNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        policyNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });
        toolkit.createLabel(composite, "Policy Documentation:");
        policyDocText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.minimumHeight = 250;
        policyDocText.setLayoutData(gridData);
        policyDocText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        Link link = new Link(composite, SWT.NONE | SWT.WRAP);
        link.setText("For more information about the access policy language, " +
                "see <a href=\"" +
                ConceptUrl + "\">Key Concepts</a> in Using AWS Identity and Access Management.");

        link.addListener(SWT.Selection, new WebLinkListener());
        gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
        link.setBackground(toolkit.getColors().getBackground());
        return composite;
    }

    @Override
    protected void okPressed() {

        final String policyName = policyNameText.getText();
        final String policyDoc = policyDocText.getText();
        new Job("Adding policy") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    putPolicy(policyName, policyDoc);
                    policyTable.refresh();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to add the policy: " + e.getMessage(), e);
                }

            }
        }.schedule();

        super.okPressed();
    }

    private void validate() {
        boolean hasPolicyName = policyNameText.getText() != null && policyNameText.getText().length() > 0;
        boolean hasPolicyDoc = policyDocText.getText() != null && policyDocText.getText().length() > 0;
        if (hasPolicyName && hasPolicyDoc) {
           setErrorMessage(null);
           // Enable the OK button
           okButton.setEnabled(true);
        } else {
            setErrorMessage("Please input a valid policy name and policy documentation.");
            // Disable the OK Button
            okButton.setEnabled(false);
        }
    }

    protected abstract void putPolicy(String policyName, String policyDoc);

}
