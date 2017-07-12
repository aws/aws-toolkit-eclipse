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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.Group;

public class GroupSummary extends Composite {

    private Group group;
    private final Text groupARNLable;
    private final Text usersInGroupLabel;
    private final Text pathLabel;
    private final Text creationTimeLabel;
    private AmazonIdentityManagement iam;

    public GroupSummary(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.iam = iam;

        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults()
                .align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);

        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.setLayout(new GridLayout(4, false));
        this.setBackground(toolkit.getColors().getBackground());

        toolkit.createLabel(this, "Group ARN:");
        groupARNLable = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(groupARNLable);

        toolkit.createLabel(this, "Users:");
        usersInGroupLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(usersInGroupLabel);

        toolkit.createLabel(this, "Path:");
        pathLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(pathLabel);

        toolkit.createLabel(this, "Creation Time:");
        creationTimeLabel = toolkit.createText(this, "", SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(creationTimeLabel);


    }

    public void setGroup(Group group) {
        this.group = group;
        refresh();
    }

    public void refresh() {
        new LoadGroupSummaryThread().start();
    }

    private class LoadGroupSummaryThread extends Thread {
        @Override
        public void run() {
            try {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (group != null) {
                        groupARNLable.setText(group.getArn());
                        pathLabel.setText(group.getPath());
                        creationTimeLabel.setText(group.getCreateDate().toString());
                        int usersInGroup = iam.getGroup(new GetGroupRequest().withGroupName(group.getGroupName())).getUsers().size();
                        usersInGroupLabel.setText(Integer.toString(usersInGroup));
                        } else {
                            groupARNLable.setText("");
                            pathLabel.setText("");
                            creationTimeLabel.setText("");
                            usersInGroupLabel.setText("");
                        }
                    }
                });

            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe group summary", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }

    }

}
