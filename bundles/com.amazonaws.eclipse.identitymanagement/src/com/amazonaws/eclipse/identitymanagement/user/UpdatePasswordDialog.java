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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.UpdateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.User;

public class UpdatePasswordDialog extends TitleAreaDialog {

    private FormToolkit toolkit;
    private Text passwordText;
    private Text confirmPasswordText;
    private User user;
    private boolean hasPassword;
    private AmazonIdentityManagement iam;
    private UserSummary userSummary;
    protected Button okButton;

    public UpdatePasswordDialog(AmazonIdentityManagement iam, UserSummary userSummary, Shell parentShell, FormToolkit toolkit, User user,  boolean hasPassword) {
        super(parentShell);
        this.toolkit = toolkit;
        this.user = user;
        this.hasPassword = hasPassword;
        this.iam = iam;
        this.userSummary = userSummary;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        if (hasPassword) {
            setTitle("Update User Password");
        } else {
            setTitle("Create User Password");
        }
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        okButton = getButton(IDialogConstants.OK_ID);
        validate();
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (hasPassword) {
            shell.setText("Update User Password");
        } else {
            shell.setText("Create User Password");
        }
        shell.setMinimumSize(600, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control control;
        control = createContentPasswordLayout(parent);
        return control;

    }

    private Composite createContentPasswordLayout(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.setLayout(new GridLayout(1, false));
        composite.setBackground(toolkit.getColors().getBackground());

        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).span(2, SWT.DEFAULT).hint(200, SWT.DEFAULT);

        toolkit.createLabel(composite, "Password:");
        passwordText = toolkit.createText(composite, "", SWT.BORDER);
        gridDataFactory.copy().span(2, SWT.DEFAULT).applyTo(passwordText);
        toolkit.createLabel(composite, "Confirm Password:");
        passwordText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        confirmPasswordText = toolkit.createText(composite, "", SWT.BORDER);
        gridDataFactory.copy().span(2, SWT.DEFAULT).applyTo(confirmPasswordText);
        confirmPasswordText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });
        return composite;
    }

    private void createPassword(String password) {
        iam.createLoginProfile(new CreateLoginProfileRequest().withPassword(password).withUserName(user.getUserName()));
    }

    private void updatePassword(String password) {
        iam.updateLoginProfile(new UpdateLoginProfileRequest().withPassword(password).withUserName(user.getUserName()));
    }

    @Override
    protected void okPressed() {

        final String password = passwordText.getText();
        if (hasPassword) {
            new Job("Update password") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        updatePassword(password);
                        userSummary.refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to create the password: " + e.getMessage(), e);
                    }
                }
            }.schedule();
        } else {
            new Job("Create password") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        createPassword(password);
                        userSummary.refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to update the password: " + e.getMessage(), e);
                    }

                }
            }.schedule();
        }
        super.okPressed();
    }

    private void validate() {
        if (passwordText.getText() == null || passwordText.getText().length() <= 0) {
            setErrorMessage("Please input a valid password.");
            okButton.setEnabled(false);
            return;
        }
        if (confirmPasswordText.getText() == null || confirmPasswordText.getText().length() <= 0 || (!confirmPasswordText.getText().equals(passwordText.getText()))) {
            setErrorMessage("Password fields do not match.");
            okButton.setEnabled(false);
            return;
        }
        okButton.setEnabled(true);
        setErrorMessage(null);
    }
}
