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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.UserIdGroupPair;

/**
 * Selection table for users to select individual permissions in a security
 * group.
 */
public class PermissionsComposite extends SelectionTable {

    private PermissionsTableProvider permissionsTableProvider = new PermissionsTableProvider();
    private Action addPermissionAction;
    private Action removePermissionAction;
    private SecurityGroupSelectionComposite securityGroupSelectionComposite;


    /**
     * Comparator to sort permissions.
     *
     * @author Jason Fulghum <fulghum@amazon.com>
     */
    class IpPermissionComparator extends ViewerComparator {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof IpPermission && e2 instanceof IpPermission)) {
                return 0;
            }

            IpPermission ipPermission1 = (IpPermission)e1;
            IpPermission ipPermission2 = (IpPermission)e2;

            int comparision = ipPermission1.getIpProtocol().compareTo(ipPermission2.getIpProtocol());

            if (comparision == 0) {
                comparision = ipPermission1.getFromPort() - ipPermission2.getFromPort();
            }

            return comparision;
        }
    }

    /**
     * Content and label provider for security group permission details.
     *
     * @author Jason Fulghum <fulghum@amazon.com>
     */
    private class PermissionsTableProvider extends LabelProvider implements ITreeContentProvider, ITableLabelProvider {

        private List<IpPermission> ipPermissions;

        /*
         * IStructuredContentProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            if (ipPermissions == null) {
                return null;
            }

            return ipPermissions.toArray();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof List) {
                ipPermissions = (List<IpPermission>)newInput;
            }
        }


        /*
         * ITableLableProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
         */
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof IpPermission)) {
                return "???";
            }

            IpPermission ipPermission = (IpPermission)element;

            switch (columnIndex) {
            case 0:
                return ipPermission.getIpProtocol();
            case 1:
                return formatPortRange(ipPermission);
            case 2:
                return formatUidGroupPairs(ipPermission);
            case 3:
                return formatIpRanges(ipPermission);
            }

            return "?";
        }

        private String formatIpRanges(IpPermission ipPermission) {
            StringBuilder builder = new StringBuilder();
            for (String s : ipPermission.getIpRanges()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(s);
            }
            return builder.toString();
        }

        private String formatUidGroupPairs(IpPermission ipPermission) {
            StringBuilder builder = new StringBuilder();
            for (UserIdGroupPair s : ipPermission.getUserIdGroupPairs()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(s.getUserId() + ":" + (s.getGroupName() == null ? s.getGroupId() : s.getGroupName()));
            }
            return builder.toString();
        }

        private String formatPortRange(IpPermission ipPermission) {
            Integer fromPort = ipPermission.getFromPort();
            Integer toPort = ipPermission.getToPort();

            if (fromPort == null || toPort == null) {
                return "";
            } else if (fromPort.intValue() == toPort.intValue()) {
                return Integer.toString(fromPort);
            } else {
                return fromPort + " - " + toPort;
            }
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return false;
        }
    }

    /**
     * Creates a new PermissionsComposite with the specified parent.
     *
     * @param parent the parent of this new Composite.
     */
    public PermissionsComposite(Composite parent) {
        super(parent);

        viewer.setContentProvider(permissionsTableProvider);
        viewer.setLabelProvider(permissionsTableProvider);
        viewer.setComparator(new IpPermissionComparator());
    }

    /**
     * Refreshes the data in the permissions table
     */
    public void refreshPermissions() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                SecurityGroup selectedSecurityGroup = securityGroupSelectionComposite.getSelectedSecurityGroup();

                if (selectedSecurityGroup == null) {
                    setInput(new ArrayList<IpPermission>());
                    return;
                }

                String groupId = selectedSecurityGroup.getGroupId();
                new RefreshPermissionsThread(groupId).start();
            }
        });
    }

    /**
     * Sets the security group selection table that controls which security
     * group's permissions are shown in this selection table.
     *
     * @param securityGroupSelectionComposite
     *            The security group selection table that controls which
     *            security group's permissions are shown in this selection
     *            table.
     */
    public void setSecurityGroupComposite(
            SecurityGroupSelectionComposite securityGroupSelectionComposite) {

        this.securityGroupSelectionComposite = securityGroupSelectionComposite;
    }

    /**
     * Returns the currently selected permission.
     *
     * @return The currently selected permission.
     */
    public IpPermission getSelectedIpPermission() {
        Object obj = this.getSelection();

        return (IpPermission)obj;
    }


    /*
     * SelectionTable Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        if (securityGroupSelectionComposite.getSelectedSecurityGroup() == null) {
            addPermissionAction.setEnabled(false);
        } else {
            addPermissionAction.setEnabled(true);
        }

        if (this.getSelection() == null) {
            removePermissionAction.setEnabled(false);
        } else {
            removePermissionAction.setEnabled(true);
        }

        manager.add(addPermissionAction);
        manager.add(removePermissionAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        addPermissionAction = new AwsAction(AwsToolkitMetricType.EXPLORER_EC2_ADD_PERMISSIONS_TO_SECURITY_GROUP) {
            @Override
            public void doRun() {
                String securityGroup = securityGroupSelectionComposite.getSelectedSecurityGroup().getGroupName();

                EditSecurityGroupPermissionEntryDialog dialog = new EditSecurityGroupPermissionEntryDialog(Display.getCurrent().getActiveShell(), securityGroup);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    actionCanceled();
                } else {
                    new AuthorizePermissionsThread(securityGroup, dialog).start();
                    actionSucceeded();
                }
                actionFinished();
            }
        };
        addPermissionAction.setText("Add Permissions...");
        addPermissionAction.setToolTipText("Add new permissions to the selected security group");
        addPermissionAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("add"));

        removePermissionAction = new AwsAction(AwsToolkitMetricType.EXPLORER_EC2_REMOVE_PERMISSIONS_FROM_SECURITY_GROUP) {
            @Override
            public void doRun() {
                String securityGroup = securityGroupSelectionComposite.getSelectedSecurityGroup().getGroupName();
                new RevokePermissionsThread(securityGroup, getSelectedIpPermission()).start();
                actionFinished();
            }
        };
        removePermissionAction.setText("Remove Permissions");
        removePermissionAction.setToolTipText("Remove the selected permission from the selected security group");
        removePermissionAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Protocol", 10);
        newColumn("Port", 10);
        newColumn("User:Group", 10);
        newColumn("Source CIDR", 70);
    }

    /**
     * Sets the input for this permissions composite, ensuring that the widgets
     * are updated in the UI thread.
     *
     * @param ipPermissions
     *            The list of permissions to display in this permissions
     *            composite.
     */
    private void setInput(final List<IpPermission> ipPermissions) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                viewer.setInput(ipPermissions);
                packColumns();
            }
        });
    }


    /*
     * Private Threads for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to list all permissions in an
     * EC2 security group.
     */
    private class RefreshPermissionsThread extends Thread {
        private final String groupId;

        /**
         * Creates a new RefreshPermissionsThread ready to be started to query
         * the permissions in the specified EC2 security group and update the
         * permissions list widget.
         *
         * @param groupId
         *            The EC2 security group whose permissions are to be
         *            refreshed.
         */
        public RefreshPermissionsThread(String groupId) {
            this.groupId = groupId;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {

                DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
                        .withGroupIds(groupId);
                DescribeSecurityGroupsResult response = getAwsEc2Client().describeSecurityGroups(request);
                List<SecurityGroup> securityGroups = response.getSecurityGroups();

                if (securityGroups.isEmpty()) return;

                setInput(securityGroups.get(0).getIpPermissions());
            } catch (Exception e) {
                // Only log an error if the account info is valid and we
                // actually expected this call to work
                if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to refresh security group permissions: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }
    }

    /**
     * Thread for making an EC2 service call to authorize new permissions in an
     * EC2 security group.
     */
    private class AuthorizePermissionsThread extends Thread {

        private final EditSecurityGroupPermissionEntryDialog dialog;
        private final String securityGroup;

        /**
         * Creates a new AuthorizePermissionsThread ready to be run to add the
         * permissions detailed in the specified permission entry dialog to the
         * specified security group.
         *
         * @param securityGroup
         *            The security group in which to authorize the new
         *            permissions.
         * @param dialog
         *            The permission entry dialog containing the user input on
         *            what permissions to authorize.
         */
        public AuthorizePermissionsThread(String securityGroup, EditSecurityGroupPermissionEntryDialog dialog) {
            this.securityGroup = securityGroup;
            this.dialog = dialog;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                if (dialog.isUserGroupPermission()) {
                    String groupName = dialog.getUserGroupPermissionComposite().getSecurityGroup();
                    String userId = dialog.getUserGroupPermissionComposite().getUserId();

                    AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
                    request.setGroupName(securityGroup);
                    request.setSourceSecurityGroupName(groupName);
                    request.setSourceSecurityGroupOwnerId(userId);

                    getAwsEc2Client().authorizeSecurityGroupIngress(request);
                } else {
                    String protocol = dialog.getPortRangePermissionComposite().getProtocol();
                    int fromPort = dialog.getPortRangePermissionComposite().getFromPort();
                    int toPort = dialog.getPortRangePermissionComposite().getToPort();
                    String networkMask = dialog.getPortRangePermissionComposite().getNetwork();

                    AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
                    request.setGroupName(securityGroup);
                    request.setIpProtocol(protocol);
                    request.setFromPort(fromPort);
                    request.setToPort(toPort);
                    request.setCidrIp(networkMask);

                    getAwsEc2Client().authorizeSecurityGroupIngress(request);
                }
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to add permission to group: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
            }

            refreshPermissions();
        }

    }

    /**
     * Thread for making an EC2 service call to revoke specified permissions in
     * an EC2 security group.
     */
    private class RevokePermissionsThread extends Thread {

        private final IpPermission ipPermission;
        private final String securityGroup;

        /**
         * Creates a new RevokePermissionsThread ready to be run to remove the
         * specified permissions from the specified EC2 security group.
         *
         * @param securityGroup
         *            The group from which to revoke permissions.
         * @param permission
         *            The permissions to revoke.
         */
        public RevokePermissionsThread(String securityGroup, IpPermission permission) {
            this.securityGroup = securityGroup;
            this.ipPermission = permission;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 client = getAwsEc2Client();

                if (ipPermission.getUserIdGroupPairs().isEmpty()) {
                    for (String ipRange : ipPermission.getIpRanges()) {
                        RevokeSecurityGroupIngressRequest request = new RevokeSecurityGroupIngressRequest();
                        request.setGroupName(securityGroup);
                        request.setIpProtocol(ipPermission.getIpProtocol());
                        request.setFromPort(ipPermission.getFromPort());
                        request.setToPort(ipPermission.getToPort());
                        request.setCidrIp(ipRange);

                        client.revokeSecurityGroupIngress(request);
                    }
                } else {
                    for (UserIdGroupPair pair : ipPermission.getUserIdGroupPairs()) {
                        RevokeSecurityGroupIngressRequest request = new RevokeSecurityGroupIngressRequest();
                        request.setGroupName(securityGroup);
                        request.setSourceSecurityGroupName(pair.getGroupName());
                        request.setSourceSecurityGroupOwnerId(pair.getUserId());
                        getAwsEc2Client().revokeSecurityGroupIngress(request);
                    }
                }
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to remove security group permissions: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
            }

            refreshPermissions();
        }
    }

}
