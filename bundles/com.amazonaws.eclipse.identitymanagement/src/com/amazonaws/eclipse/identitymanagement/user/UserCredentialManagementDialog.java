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
package com.amazonaws.eclipse.identitymanagement.user;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.StatusType;
import com.amazonaws.services.identitymanagement.model.UpdateAccessKeyRequest;

public class UserCredentialManagementDialog extends TitleAreaDialog {

    private AmazonIdentityManagement iam;
    private String userName;
    private AccessKeyTable accessKeyTable;
    private Button createAccessKeyButton;

    public UserCredentialManagementDialog(AmazonIdentityManagement iam, Shell parentShell, String userName) {
        super(parentShell);
        this.iam = iam;
        this.userName = userName;
        setShellStyle(SWT.RESIZE);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("Use access keys to make secure requests to any AWS services.");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Manage Access Keys");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout());
        composite = new Composite(composite, SWT.NULL);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout(1, false));

        accessKeyTable = new AccessKeyTable(composite);

        createAccessKeyButton = new Button(composite, SWT.PUSH);
        createAccessKeyButton.setText("Create Access Key");
        createAccessKeyButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        createAccessKeyButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                new NewAccessKeyDialog(iam, userName, accessKeyTable).open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        createLinkSection(composite);
        accessKeyTable.refresh();

        return composite;
    }

    // Create the help link
    public void createLinkSection(Composite parent) {
        String ConceptUrl = "http://docs.aws.amazon.com/IAM/latest/UserGuide/ManagingCredentials.html";

        Link link = new Link(parent, SWT.NONE | SWT.WRAP);
        link.setText("For your protection, you should never share your secret access keys with anyone.  " + "In addition, industry best practice recommends frequent key rotation. " + "<a href=\""
                + ConceptUrl + "\">Learn more about Access Keys</a>.");

        link.addListener(SWT.Selection, new WebLinkListener());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
    }

    // Table for the access keys
    public class AccessKeyTable extends Composite {
        private TableViewer viewer;
        private ContentProvider contentProvider;
        private LabelProvider labelProvider;

        public AccessKeyTable(Composite parent) {
            super(parent, SWT.NONE);

            this.setLayoutData(new GridData(GridData.FILL_BOTH));
            TableColumnLayout tableColumnLayout = new TableColumnLayout();
            this.setLayout(tableColumnLayout);

            contentProvider = new ContentProvider();
            labelProvider = new LabelProvider();

            viewer = new TableViewer(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            viewer.getTable().setLinesVisible(true);
            viewer.getTable().setHeaderVisible(true);
            viewer.setLabelProvider(labelProvider);
            viewer.setContentProvider(contentProvider);
            createColumns(tableColumnLayout, viewer.getTable());

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
                                deleteAccessKey(viewer.getTable().getSelectionIndex());
                                refresh();
                            }

                            @Override
                            public String getText() {
                                return "Delete Access Key";
                            }

                        });

                        manager.add(new Action() {

                            @Override
                            public ImageDescriptor getImageDescriptor() {
                                return null;
                            }

                            @Override
                            public void run() {
                                deactivateAccessKey(viewer.getTable().getSelectionIndex());
                                refresh();
                            }

                            @Override
                            public String getText() {
                                return "Make Inactive";
                            }

                        });
                    }
                }
            });
            viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));
        }

        public void refresh() {
            new LoadAccessKeyTableThread().start();
        }

        private void deleteAccessKey(int index) {
            final String accessKeyId = contentProvider.getItemByIndex(index).getAccessKeyId();
            new Job("Delete access key") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        iam.deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId(accessKeyId).withUserName(userName));
                        refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete the access key: " + e.getMessage(), e);
                    }
                }
            }.schedule();
        }

        private void deactivateAccessKey(int index) {
            final String accessKeyId = contentProvider.getItemByIndex(index).getAccessKeyId();
            new Job("Deactivate cccess key") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        iam.updateAccessKey(new UpdateAccessKeyRequest().withAccessKeyId(accessKeyId).withUserName(userName).withStatus(StatusType.Inactive));
                        refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to make the access key inactive: " + e.getMessage(), e);
                    }
                }
            }.schedule();
        }

        // Load the content for access key table.
        private class LoadAccessKeyTableThread extends Thread {
            @Override
            public void run() {
                try {
                    final List<AccessKeyMetadata> metadatas = listAcesskeyMetadata();
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (metadatas == null) {
                                viewer.setInput(null);
                            } else {
                                viewer.setInput(metadatas.toArray(new AccessKeyMetadata[metadatas.size()]));
                            }
                        }
                    });
                } catch (Exception e) {
                    Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to list access keys: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }

        private class ContentProvider extends ArrayContentProvider {

            private AccessKeyMetadata[] metadatas;

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                if (newInput instanceof AccessKeyMetadata[]) {
                    metadatas = (AccessKeyMetadata[]) newInput;
                } else {
                    metadatas = new AccessKeyMetadata[0];
                }
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return metadatas;
            }

            public AccessKeyMetadata getItemByIndex(int index) {
                return metadatas[index];
            }
        }

        private class LabelProvider implements ITableLabelProvider {
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
                if (element instanceof AccessKeyMetadata == false)
                    return "";
                AccessKeyMetadata metadata = (AccessKeyMetadata) element;
                switch (columnIndex) {
                case 0:
                    return metadata.getCreateDate().toString();
                case 1:
                    return metadata.getAccessKeyId();
                case 2:
                    return metadata.getStatus();
                }
                return element.toString();
            }

        }
    }

    private void createColumns(TableColumnLayout columnLayout, Table table) {
        createColumn(table, columnLayout, "Created");
        createColumn(table, columnLayout, "Access Key ID");
        createColumn(table, columnLayout, "Status");
    }

    private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    private List<AccessKeyMetadata> listAcesskeyMetadata() {
        return iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName)).getAccessKeyMetadata();
    }
}
