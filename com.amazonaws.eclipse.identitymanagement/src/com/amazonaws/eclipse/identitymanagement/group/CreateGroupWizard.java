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
package com.amazonaws.eclipse.identitymanagement.group;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateGroupRequest;
import com.amazonaws.services.identitymanagement.model.PutGroupPolicyRequest;

public class CreateGroupWizard extends Wizard {
    private CreateGroupFirstPage firstPage;
    private CreateGroupSecondPage secondPage;
    private CreateGroupWizardDataModel dataModel;
    private AmazonIdentityManagement iam;
    private IRefreshable refreshable;

    public CreateGroupWizard(AmazonIdentityManagement iam, IRefreshable refreshable) {
        setNeedsProgressMonitor(false);
        setWindowTitle("Create New Group");
        setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        dataModel = new CreateGroupWizardDataModel();
        this.iam = iam;
        if (iam == null) {
            this.iam = AwsToolkitCore.getClientFactory().getIAMClient();
        }
        this.refreshable = refreshable;
    }

    public CreateGroupWizard() {
        this(AwsToolkitCore.getClientFactory().getIAMClient(), null);
    }

    @Override
    public boolean performFinish() {
        final PutGroupPolicyRequest putGroupPolicyRequest = generatePutGroupPolicyRequest();

        new Job("Creating groups") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    iam.createGroup(new CreateGroupRequest().withGroupName(dataModel.getGroupName()));

                    if (putGroupPolicyRequest != null) {
                        iam.putGroupPolicy(putGroupPolicyRequest);
                    }

                    if (refreshable != null) {
                        refreshable.refreshData();
                    }

                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to create the group: " + e.getMessage(), e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        return true;
    }

    @Override
    public void addPages() {
        firstPage = new CreateGroupFirstPage(this);
        secondPage = new CreateGroupSecondPage(this);
        addPage(firstPage);
        addPage(secondPage);
    }

    private PutGroupPolicyRequest generatePutGroupPolicyRequest() {
        if ( !dataModel.getGrantPermission() ) {
            return null;
        } else {
            return new PutGroupPolicyRequest().withGroupName(dataModel.getGroupName()).withPolicyName(dataModel.getPolicyName()).withPolicyDocument(dataModel.getPolicyDoc());
        }
    }

    public CreateGroupWizardDataModel getDataModel() {
        return dataModel;
    }

}
