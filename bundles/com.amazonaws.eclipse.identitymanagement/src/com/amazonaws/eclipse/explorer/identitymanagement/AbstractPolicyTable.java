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
package com.amazonaws.eclipse.explorer.identitymanagement;

import java.util.List;

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

import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

/**
 * Base class for list policies in a table for user, group and role.
 */
public abstract class AbstractPolicyTable extends Composite {
    protected TableViewer viewer;
    protected List<String> policyNames;
    protected TableContentProvider contentProvider;
    protected AmazonIdentityManagement iam;

    protected AbstractPolicyTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.iam = iam;

        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        this.setLayout(tableColumnLayout);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        contentProvider = new TableContentProvider();
        TableLabelProvider labelProvider = new TableLabelProvider();

        viewer = new TableViewer(this, SWT.SINGLE | SWT.BORDER );
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);
        createColumns(tableColumnLayout, viewer.getTable());

    }

    protected void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "Policy Name");
    }

    protected TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    protected class TableContentProvider extends ArrayContentProvider {
        private String[] policyNames;

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof String[])
                policyNames = (String[]) newInput;
            else
                policyNames = new String[0];
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return policyNames;
        }

        public String getItemByIndex(int index) {
            return policyNames[index];
        }

    }

    protected class TableLabelProvider implements ITableLabelProvider {

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
            if (element instanceof String == false)
                return "";
            String policyName = (String) element;
            return policyName;
        }
    }

    abstract public void refresh();

    abstract protected void getPolicyNames();

    protected class LoadPermissionTableThread extends Thread {
        public LoadPermissionTableThread() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void run() {
            try {
                getPolicyNames();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (policyNames == null) {
                            viewer.setInput(null);
                        } else {
                        viewer.setInput(policyNames.toArray(new String[policyNames.size()]));
                        }
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to list policies", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }
}
