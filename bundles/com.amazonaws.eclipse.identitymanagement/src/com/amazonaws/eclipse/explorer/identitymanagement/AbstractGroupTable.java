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
import com.amazonaws.services.identitymanagement.model.Group;

public abstract class AbstractGroupTable extends Composite {
    protected TableViewer viewer;
    protected List<Group> groups;
    protected GroupTableContentProvider contentProvider;
    protected AmazonIdentityManagement iam;

    public AbstractGroupTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        this.setLayout(tableColumnLayout);

        contentProvider = new GroupTableContentProvider();
        GroupTableLabelProvider labelProvider = new GroupTableLabelProvider();

        viewer = new TableViewer(this, SWT.BORDER | SWT.MULTI);
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);
        createColumns(tableColumnLayout, viewer.getTable());
        this.iam = iam;
        refresh();
    }

    private void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "Group Name");
        createColumn(table, columnLayout, "Creation Time");
    }

    private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    protected class GroupTableContentProvider extends ArrayContentProvider {
        private Group[] groups;

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Group[]) {
                groups = (Group[]) newInput;
            } else {
                groups = new Group[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return groups;
        }

        public Group getItemByIndex(int index) {
            return groups[index];
        }
    }

    private class GroupTableLabelProvider implements ITableLabelProvider {

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
            if (element instanceof Group == false)
                return "";

            Group group = (Group) element;
            switch (columnIndex) {
            case 0:
                return group.getGroupName();
            case 1:
                return group.getCreateDate().toString();
            default:
                return "";
            }
        }
    }

    abstract public void refresh();

    abstract protected void listGroups();

    protected class LoadGroupTableThread extends Thread {
        public LoadGroupTableThread() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void run() {
            try {
                listGroups();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (groups != null) {
                        viewer.setInput(groups.toArray(new Group[groups.size()]));
                        } else {
                            viewer.setInput(null);
                        }
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe groups", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

}
