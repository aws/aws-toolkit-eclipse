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
import com.amazonaws.services.identitymanagement.model.User;

public abstract class AbstractUserTable extends Composite {

    protected TableViewer viewer;
    protected final EditorInput userEditorInput;
    protected final UserTableContentProvider contentProvider;
    protected List<User> users;
    protected AmazonIdentityManagement iam;

    protected final class UserTableContentProvider extends ArrayContentProvider {

        private User[] users;

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof User[]) {
                users = (User[]) newInput;
            } else {
                users = new User[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return users;
        }

        public User getItemByIndex(int index) {
            return users[index];
        }
    }

    protected final class userTableLabelProvider implements ITableLabelProvider {
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
            if (element instanceof User == false)
                return "";
            User user = (User) element;
            switch (columnIndex) {
            case 0:
                return user.getUserName();
            case 1:
                return user.getCreateDate().toString();
            }
            return element.toString();
        }
    }

    public AbstractUserTable(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit, EditorInput userEditorInput) {
        super(parent, SWT.NONE);
        this.userEditorInput = userEditorInput;
        this.iam = iam;

        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        this.setLayout(tableColumnLayout);

        contentProvider = new UserTableContentProvider();
        userTableLabelProvider labelProvider = new userTableLabelProvider();

        viewer = new TableViewer(this, SWT.BORDER | SWT.MULTI);
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTable());
        refresh();
    }

    public abstract void refresh();
    protected abstract void listUsers();

    private void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "User Name");
        createColumn(table, columnLayout, "Creation Time");
    }

    private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    protected class LoadUserTableThread extends Thread {
        public LoadUserTableThread() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void run() {
            try {
                listUsers();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (users == null) {
                            viewer.setInput(null);
                        } else {
                        viewer.setInput(users.toArray(new User[users.size()]));
                        }
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe users", e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

}
