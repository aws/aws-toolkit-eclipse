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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserResult;
import com.amazonaws.services.identitymanagement.model.User;

public class UserSummary extends Composite {

    private User user;
    private final Text userARNLable;
    private final Text havePasswordLabel;
    private final Text groupsLabel;
    private final Text pathLabel;
    private final Text creationTimeLabel;
    private final Button updatePassowrdButton;
    private Button removePasswordButton;
    private Button manageAccessKeysButton;
    private boolean hasPassword;
    private AmazonIdentityManagement iam;

    public UserSummary(final AmazonIdentityManagement iam, Composite parent, final FormToolkit toolkit) {
        super(parent, SWT.NONE);
        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults()
                .align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);


        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.setLayout(new GridLayout(4, false));
        this.setBackground(toolkit.getColors().getBackground());

        toolkit.createLabel(this, "User ARN:");
        userARNLable = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(userARNLable);

        toolkit.createLabel(this, "Groups:");
        groupsLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(groupsLabel);

        toolkit.createLabel(this, "Path:");
        pathLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(pathLabel);

        toolkit.createLabel(this, "Creation Time:");
        creationTimeLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(creationTimeLabel);

        toolkit.createLabel(this, "Has Password:");
        havePasswordLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(havePasswordLabel);

        removePasswordButton = toolkit.createButton(this, "Remove Password", SWT.PUSH);
        removePasswordButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                 new Job("Remove password") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            removePassword();
                            refresh();
                            return Status.OK_STATUS;
                        } catch(Exception e) {
                            return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete the password: " + e.getMessage(), e);
                        }
                    }

                 }.schedule();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        removePasswordButton.setEnabled(false);
        updatePassowrdButton = toolkit.createButton(this, "Update Password", SWT.PUSH);
        updatePassowrdButton.setEnabled(false);
        updatePassowrdButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                new UpdatePasswordDialog(iam, UserSummary.this, Display.getCurrent().getActiveShell(), toolkit, user, hasPassword).open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        manageAccessKeysButton = toolkit.createButton(this, "Manage Access Keys", SWT.PUSH);
        manageAccessKeysButton.addSelectionListener((new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new UserCredentialManagementDialog(iam, Display.getCurrent().getActiveShell(), user.getUserName()).open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        }));

        manageAccessKeysButton.setEnabled(false);

        this.iam = iam;

    }

    public void setUser(User user) {
        this.user = user;
        refresh();
        if (user != null) {
            updatePassowrdButton.setEnabled(true);
            removePasswordButton.setEnabled(true);
            manageAccessKeysButton.setEnabled(true);
        } else {
            updatePassowrdButton.setEnabled(false);
            removePasswordButton.setEnabled(false);
            manageAccessKeysButton.setEnabled(false);
        }
    }

    public void refresh() {
        new LoadUserSummaryThread().start();
    }

    private class LoadUserSummaryThread extends Thread {
        @Override
        public void run() {
            try {

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {

                        if (user != null) {
                        userARNLable.setText(user.getArn());
                        pathLabel.setText(user.getPath());
                        creationTimeLabel.setText(user.getCreateDate().toString());
                        try {
                            iam.getLoginProfile(new GetLoginProfileRequest().withUserName(user.getUserName()));
                            havePasswordLabel.setText("yes");
                            removePasswordButton.setEnabled(true);
                            hasPassword = true;

                        } catch (Exception e) {
                            havePasswordLabel.setText("no");
                            removePasswordButton.setEnabled(false);
                            hasPassword = false;
                        }

                        try {
                            ListGroupsForUserResult listGroupsForUserResult = iam.listGroupsForUser(new ListGroupsForUserRequest().withUserName(user.getUserName()));
                            groupsLabel.setText(Integer.toString(listGroupsForUserResult.getGroups().size()));
                        } catch (Exception e) {
                            groupsLabel.setText("0");
                        }
                        } else {
                            userARNLable.setText("");
                            pathLabel.setText("");
                            creationTimeLabel.setText("");
                            havePasswordLabel.setText("");
                            groupsLabel.setText("");
                        }
                    }});

            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe user summary", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }

    }

    private void removePassword() {
        iam.deleteLoginProfile(new DeleteLoginProfileRequest().withUserName(user.getUserName()));
    }

}
