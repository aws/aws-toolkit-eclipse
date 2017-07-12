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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleResult;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.Role;

public class RoleTable extends Composite {
    private TableViewer viewer;
    private RoleSummary roleSummary;
    private RolePermissions rolePermissions;
    private RoleTrustRelationships roleTrustRelationships;
    private RoleTableContentProvider contentProvider;
    private AmazonIdentityManagement iam;
    private final String DELTE_ROLE_CONFIRMATION = "All selected roles and their associated permissions will be deleted. This will affect applications using these roles. Do you want to continue?";

    public RoleTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.iam = iam;

        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        this.setLayout(tableColumnLayout);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        contentProvider = new RoleTableContentProvider();
        RoleTableLabelProvider labelProvider = new RoleTableLabelProvider();

        viewer = new TableViewer(this, SWT.BORDER | SWT.MULTI);
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if (viewer.getTable().getSelectionCount() > 0) {

                    manager.add(new Action() {
                        @Override
                        public ImageDescriptor getImageDescriptor() {
                            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                        }

                        @Override
                        public void run() {
                            boolean confirmation = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Delete Role", DELTE_ROLE_CONFIRMATION);
                            if (confirmation) {
                                roleSummary.setRole(null);
                                rolePermissions.setRole(null);
                                roleTrustRelationships.setRole(null);
                                deleteMultipleGroups(viewer.getTable().getSelectionIndices());
                            }
                        }

                        @Override
                        public String getText() {
                            if (viewer.getTable().getSelectionIndices().length > 1) {
                                return "Delete Roles";
                            }
                            return "Delete Role";
                        }

                    });
                }
            }
        });

        viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));

        viewer.getTable().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int index = viewer.getTable().getSelectionIndex();
                if (index >= 0) {
                    Role role = contentProvider.getItemByIndex(index);
                    roleSummary.setRole(role);
                    rolePermissions.setRole(role);
                    roleTrustRelationships.setRole(role);
                } else {
                    roleSummary.setRole(null);
                    rolePermissions.setRole(null);
                    roleTrustRelationships.setRole(null);
                }

            }
        });

        createColumns(tableColumnLayout, viewer.getTable());
        refresh();
    }

    private void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "Role Name");
        createColumn(table, columnLayout, "Creation Time");
    }

    private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    private class RoleTableContentProvider extends ArrayContentProvider {
        private Role[] roles;

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Role[])
                roles = (Role[]) newInput;
            else
                roles = new Role[0];
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return roles;
        }

        public Role getItemByIndex(int index) {
            return roles[index];
        }
    }

    private class RoleTableLabelProvider implements ITableLabelProvider {
        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof Role == false)
                return "";

            Role role = (Role) element;
            switch (columnIndex) {
            case 0:
                return role.getRoleName();
            case 1:
                return role.getCreateDate().toString();
            default:
                return "";
            }
        }
    }

    public void refresh() {
        new LoadRoleTableThread().start();
    }

    public void setRoleSummary(RoleSummary roleSummary) {
        this.roleSummary = roleSummary;
    }

    public void setRoleTrustRelationships(RoleTrustRelationships roleTruestRelationships) {
        this.roleTrustRelationships = roleTruestRelationships;
    }

    public void setRolePermissions(RolePermissions rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    private void deleteRole(String roleName) {
        ListRolePoliciesResult listRolePolicyResult = iam.listRolePolicies(new ListRolePoliciesRequest().withRoleName(roleName));
        for (String policyName : listRolePolicyResult.getPolicyNames()) {
            iam.deleteRolePolicy(new DeleteRolePolicyRequest().withPolicyName(policyName).withRoleName(roleName));
        }

        ListInstanceProfilesForRoleResult listInstanceProfilesForRoleResult = iam.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName(roleName));
        for (InstanceProfile instanceProfile : listInstanceProfilesForRoleResult.getInstanceProfiles()) {
            iam.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest().withInstanceProfileName(instanceProfile.getInstanceProfileName()).withRoleName(roleName));
        }
        iam.deleteRole(new DeleteRoleRequest().withRoleName(roleName));
    }

    private void deleteMultipleGroups(final int[] indices) {
        new Job("Delete roles") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (int index : indices) {
                    String roleName = contentProvider.getItemByIndex(index).getRoleName();
                    try {
                        deleteRole(roleName);
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete roles: " + e.getMessage(), e);
                    }
                }
                refresh();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private class LoadRoleTableThread extends Thread {
        @Override
        public void run() {
            try {
                final List<Role> roles;
                roles = iam.listRoles().getRoles();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(roles.toArray(new Role[roles.size()]));
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe roles", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

}
