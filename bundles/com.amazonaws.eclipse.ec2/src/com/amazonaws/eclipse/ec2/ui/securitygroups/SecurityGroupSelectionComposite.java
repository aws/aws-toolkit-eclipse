/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.ui.securitygroups;

import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * Selection table containing a list of security groups.
 */
public class SecurityGroupSelectionComposite extends SelectionTable {

    private Action createSecurityGroupAction;
    private Action deleteSecurityGroupAction;
    private Action refreshSecurityGroupsAction;
    private SecurityGroupTableProvider securityGroupTableProvider = new SecurityGroupTableProvider();
    private PermissionsComposite permissionsComposite;

    /**
     * Creates a new security group selection table parented by the specified
     * composite.
     *
     * @param parent
     *            The parent of this new selection table.
     */
    public SecurityGroupSelectionComposite(Composite parent) {
        super(parent);

        viewer.setContentProvider(securityGroupTableProvider);
        viewer.setLabelProvider(securityGroupTableProvider);
        viewer.setComparator(new SecurityGroupComparator());

        refreshSecurityGroups();
    }

    /**
     * Sets the optional security group permissions selection table that can be
     * linked with this security group selection table.
     *
     * @param permissionsComposite
     *            The security group permissions selection table that is linked
     *            with this security group selection table.
     */
    public void setPermissionsComposite(PermissionsComposite permissionsComposite) {
        this.permissionsComposite = permissionsComposite;
    }

    /**
     * Returns the currently selected security group.
     *
     * @return The currently selected security group.
     */
    public SecurityGroup getSelectedSecurityGroup() {
        StructuredSelection selection = (StructuredSelection)viewer.getSelection();

        return (SecurityGroup)selection.getFirstElement();
    }


    /*
     * Action Accessors
     */

    public Action getRefreshSecurityGroupsAction() {
        return refreshSecurityGroupsAction;
    }

    public Action getCreateSecurityGroupAction() {
        return createSecurityGroupAction;
    }

    public Action getDeleteSecurityGroupAction() {
        return deleteSecurityGroupAction;
    }


    /*
     * SelectionTable Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Name", 20);
        newColumn("Description", 80);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        deleteSecurityGroupAction.setEnabled(getSelectedSecurityGroup() != null);

        manager.add(refreshSecurityGroupsAction);
        manager.add(new Separator());
        manager.add(createSecurityGroupAction);
        manager.add(deleteSecurityGroupAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        createSecurityGroupAction = new AwsAction(AwsToolkitMetricType.EXPLORER_EC2_NEW_SECURITY_GROUP) {
            @Override
            public void doRun() {
                final CreateSecurityGroupDialog dialog = new CreateSecurityGroupDialog(Display.getCurrent().getActiveShell());
                if (dialog.open() != Dialog.OK) {
                    actionCanceled();
                } else {
                    new CreateSecurityGroupThread(dialog.getSecurityGroupName(), dialog.getSecurityGroupDescription()).start();
                    actionSucceeded();
                }
                actionFinished();
            }
        };
        createSecurityGroupAction.setText("New Group...");
        createSecurityGroupAction.setToolTipText("Create a new security group");
        createSecurityGroupAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("add"));

        deleteSecurityGroupAction = new AwsAction(AwsToolkitMetricType.EXPLORER_EC2_DELETE_SECURITY_GROUP) {
            @Override
            public void doRun() {
                new DeleteSecurityGroupThread(getSelectedSecurityGroup()).start();
                actionFinished();
            }
        };
        deleteSecurityGroupAction.setText("Delete Group");
        deleteSecurityGroupAction.setToolTipText("Delete security group");
        deleteSecurityGroupAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));

        refreshSecurityGroupsAction = new AwsAction(AwsToolkitMetricType.EXPLORER_EC2_REFRESH_SECURITY_GROUP) {
            @Override
            public void doRun() {
                refreshSecurityGroups();
                actionFinished();
            }
        };
        refreshSecurityGroupsAction.setText("Refresh");
        refreshSecurityGroupsAction.setToolTipText("Refresh security groups");
        refreshSecurityGroupsAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));
    }

    /*
     * Private Interface
     */

    private void refreshSecurityGroups() {
        new RefreshSecurityGroupsThread().start();
    }

    private class SecurityGroupComparator extends ViewerComparator {
        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof SecurityGroup && e2 instanceof SecurityGroup)) {
                return 0;
            }

            SecurityGroup securityGroup1 = (SecurityGroup)e1;
            SecurityGroup securityGroup2 = (SecurityGroup)e2;

            return (securityGroup1.getGroupName().compareTo(securityGroup2.getGroupName()));
        }
    }

    /**
     * Sets the list of security groups to display in the security group table.
     *
     * @param securityGroups
     *            The security groups to display.
     */
    private void setInput(final List<SecurityGroup> securityGroups) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                final SecurityGroup previouslySelectedGroup = getSelectedSecurityGroup();

                viewer.setInput(securityGroups);

                if (previouslySelectedGroup != null) {
                    for (int index = 0; index < viewer.getTree().getItemCount(); index++) {
                        TreeItem treeItem = viewer.getTree().getItem(index);
                        SecurityGroup groupDescription = (SecurityGroup)treeItem.getData();

                        if (groupDescription.getGroupName().equals(previouslySelectedGroup.getGroupName())) {
                            viewer.getTree().select(treeItem);
                        }
                    }
                }

                viewer.getTree().getColumn(0).pack();
                viewer.getTree().layout();
                layout();
            }
        });
    }


    /*
     * Private Thread subclasses for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to create a new security group.
     */
    private class CreateSecurityGroupThread extends Thread {
        /** The requested name of the group */
        private final String name;
        /** The requested description of the group */
        private final String description;

        /**
         * Creates a new thread ready to be started to create the specified
         * security group.
         *
         * @param name
         *            The requested name for the new security group.
         * @param description
         *            The requested description for the new security group.
         */
        public CreateSecurityGroupThread(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 ec2 = getAwsEc2Client();
                CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
                request.setGroupName(name);
                request.setDescription(description);
                ec2.createSecurityGroup(request);

                refreshSecurityGroups();
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to create security group: "    + e.getMessage(), e);
                StatusManager.getManager().handle(status,
                        StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

    /**
     * Thread for making an EC2 service call to delete a security group.
     */
    private class DeleteSecurityGroupThread extends Thread {
        /** The security group to delete */
        private final SecurityGroup securityGroup;

        /**
         * Creates a new thread ready to be started and delete the specified
         * security group.
         *
         * @param securityGroup
         *            The security group to delete.
         */
        public DeleteSecurityGroupThread(final SecurityGroup securityGroup) {
            this.securityGroup = securityGroup;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 ec2 = getAwsEc2Client();
                DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest();
                request.setGroupName(securityGroup.getGroupName());
                ec2.deleteSecurityGroup(request);

                refreshSecurityGroups();
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to delete security group: "    + e.getMessage(), e);
                StatusManager.getManager().handle(status,
                        StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

    /**
     * Thread for making an EC2 serivce call to list all security groups.
     */
    private class RefreshSecurityGroupsThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                if (selectionTableListener != null) selectionTableListener.loadingData();

                AmazonEC2 ec2 = getAwsEc2Client();
                final List<SecurityGroup> securityGroups = ec2.describeSecurityGroups().getSecurityGroups();
                setInput(securityGroups);

                if (permissionsComposite != null) permissionsComposite.refreshPermissions();
            } catch (Exception e) {
                // Only log an error if the account info is valid and we
                // actually expected this call to work
                if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to refresh security groups: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            } finally {
                if (selectionTableListener != null) selectionTableListener.finishedLoadingData(-1);
            }
        }
    }

}
