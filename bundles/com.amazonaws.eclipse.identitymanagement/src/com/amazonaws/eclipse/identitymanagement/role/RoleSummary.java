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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleResult;
import com.amazonaws.services.identitymanagement.model.Role;

public class RoleSummary extends Composite {

    private Role role;
    private Text roleARNLabel;
    private Text instanceProfileLabel;
    private Text pathLabel;
    private Text creationTimeLabel;
    private AmazonIdentityManagement iam;
    private final String ConceptUrl = "http://docs.aws.amazon.com/IAM/latest/UserGuide/WorkingWithRoles.html";


    public RoleSummary(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
       super(parent, SWT.NONE);

       this.iam = iam;

       GridDataFactory gridDataFactory = GridDataFactory.swtDefaults()
               .align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);

           this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
           this.setLayout(new GridLayout(4, false));
           this.setBackground(toolkit.getColors().getBackground());

           toolkit.createLabel(this, "Role ARN:");
           roleARNLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
           gridDataFactory.applyTo(roleARNLabel);

           toolkit.createLabel(this, "Instance Profile ARN(s):");
           instanceProfileLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
           gridDataFactory.applyTo(instanceProfileLabel);

           toolkit.createLabel(this, "Path:");
           pathLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
           gridDataFactory.applyTo(pathLabel);

           toolkit.createLabel(this, "Creation Time:");
           creationTimeLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
           gridDataFactory.applyTo(creationTimeLabel);

           Link link = new Link(this, SWT.NONE | SWT.WRAP);
           link.setBackground(toolkit.getColors().getBackground());
           link.setText("\nFor more information about IAM roles, see " +
                   "<a href=\"" +
                   ConceptUrl + "\">Delegating API access by using roles</a> in the using IAM guide.");

           link.addListener(SWT.Selection, new WebLinkListener());

           gridDataFactory.copy().span(4, SWT.DEFAULT).applyTo(link);

    }

    public void setRole(Role role) {
        this.role = role;
        refresh();
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
                        if (role != null) {
                            roleARNLabel.setText(role.getArn());
                            pathLabel.setText(role.getPath());
                            creationTimeLabel.setText(role.getCreateDate().toString());
                            StringBuilder instanceProfiles = new StringBuilder();
                            ListInstanceProfilesForRoleResult listInstanceProfilesForRoleResult = iam.listInstanceProfilesForRole(
                                    new ListInstanceProfilesForRoleRequest().withRoleName(role.getRoleName()));
                            for (InstanceProfile instanceProfile : listInstanceProfilesForRoleResult.getInstanceProfiles()) {
                                instanceProfiles.append(instanceProfile.getArn());
                                instanceProfiles.append("");
                            }
                            instanceProfileLabel.setText(instanceProfiles.toString());
                        } else {
                            roleARNLabel.setText("");
                            pathLabel.setText("");
                            creationTimeLabel.setText("");
                            instanceProfileLabel.setText("");
                        }
                    }
                });

            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe the roles: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }

    }

}
