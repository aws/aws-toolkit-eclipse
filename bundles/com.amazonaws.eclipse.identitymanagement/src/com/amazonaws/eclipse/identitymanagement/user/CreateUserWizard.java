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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;

/**
 * Wizard to create a new user
 */
public class CreateUserWizard extends Wizard {

    private CreateUserWizardFirstPage page;
    private AmazonIdentityManagement iam;
    private IRefreshable refreshable;

    public CreateUserWizard(AmazonIdentityManagement iam, IRefreshable refreshable) {
        setNeedsProgressMonitor(false);
        setWindowTitle("Create New Users");
        setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        this.iam = iam;
        if (iam == null) {
            this.iam = AwsToolkitCore.getClientFactory().getIAMClient();
        }
        this.refreshable = refreshable;
    }

    public CreateUserWizard() {
        this(AwsToolkitCore.getClientFactory().getIAMClient(), null);
    }

    @Override
    public boolean performFinish() {
        final List<CreateUserRequest> createUserRequests = new ArrayList<>();
        for (Text userName : page.userNameTexts) {
            String name = userName.getText();
            if (name != null) {
                // Delete the leading space.
                name = name.trim();
                if (name.length() > 0) {
                    createUserRequests.add(new CreateUserRequest().withUserName(userName.getText()));
                }
            }
        }

        new Job("Creating users") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    for (CreateUserRequest request : createUserRequests)
                        iam.createUser(request);
                    if (refreshable != null) {
                        refreshable.refreshData();
                    }
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to create users: " + e.getMessage(), e);
                }

            }
        }.schedule();

        return true;
    }

    @Override
    public void addPages() {
        page = new CreateUserWizard.CreateUserWizardFirstPage(this);
        addPage(page);
    }

    private static class CreateUserWizardFirstPage extends WizardPage {

        private final List<Text> userNameTexts = new ArrayList<>();
        private final int MAX_NUMBER_USERS = 5;

        public CreateUserWizardFirstPage(CreateUserWizard createUserWizard) {
            super("");
        }

        @Override
        public void createControl(Composite parent) {
            final Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayoutData(new GridData(GridData.FILL_BOTH));
            comp.setLayout(new GridLayout(2,false));


            FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
            int fieldDecorationWidth = fieldDecoration.getImage().getBounds().width;

            createStackNameControl(comp, fieldDecorationWidth);

            setControl(comp);
            validate();
        }

        private void createStackNameControl(final Composite comp, int fieldDecorationWidth) {

            for (int i = 0; i < MAX_NUMBER_USERS; i++) {
                new Label(comp, SWT.READ_ONLY).setText("User Name: ");
                Text userNameText = new Text(comp, SWT.BORDER);
                userNameTexts.add(i, userNameText);
                GridDataFactory.fillDefaults().grab(true, false).applyTo(userNameText);
                userNameText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        validate();
                    }
                });
            }
        }

        private void validate() {
            for (Text userName : userNameTexts) {
                if (userName.getText() != null && userName.getText().length() != 0) {
                    setErrorMessage(null);
                    setPageComplete(true);
                    return;
                }
            }
            setErrorMessage("Enter user names");
            setPageComplete(false);
        }

    }
}
