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

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.TableColumnLayout;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.auth.policy.Principal;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;

public class TrustedEntityTable extends Composite {


    private TableViewer viewer;
    private Collection<Principal> principals;

    public TrustedEntityTable(Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        this.setLayout(tableColumnLayout);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        final RoleTableContentProvider contentProvider = new RoleTableContentProvider();
        RoleTableLabelProvider labelProvider = new RoleTableLabelProvider();

        viewer = new TableViewer(this, SWT.MULTI);
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(false);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);
        createColumns(tableColumnLayout, viewer.getTable());
        refresh();
    }

    private void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "Trust Entities");
    }

    private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    private class RoleTableContentProvider extends ArrayContentProvider {
        private Principal[] principals;

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Principal[])
                principals = (Principal[]) newInput;
            else
                principals = new Principal[0];
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return principals;
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
            String result = null;
            if (element instanceof Principal == false)
                return "";

            Principal principal = (Principal) element;

            if (principal.getProvider().equalsIgnoreCase("AWS")) {
                result = "AWS account " + principal.getId();
            } else if (principal.getProvider().equalsIgnoreCase("Service")) {
                result = "The service " + principal.getId();
            }
        return result;
    }
    }


    public void setPrincipals(Collection<Principal> principals) {
        this.principals = principals;
        refresh();
    }

    public void refresh() {
        new LoadRoleTableThread().start();
    }

    private class LoadRoleTableThread extends Thread {
        @Override
        public void run() {
            try {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (principals == null) {
                            viewer.setInput(null);
                        } else {
                        viewer.setInput(principals.toArray(new Principal[principals.size()]));
                        }
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe roles", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }
}
