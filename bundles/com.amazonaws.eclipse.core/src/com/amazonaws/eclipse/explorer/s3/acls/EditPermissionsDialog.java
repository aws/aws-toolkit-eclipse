/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.s3.acls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.EmailAddressGrantee;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;

/**
 * Abstract base class providing the framework for object and bucket ACL editing UIs.
 */
public abstract class EditPermissionsDialog extends Dialog {
    private Table table;
    private AccessControlList acl;

    protected EditPermissionsDialog() {
        super(Display.getDefault().getActiveShell());
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    /**
     * Returns the title to set for the dialog shell.
     *
     * @return the title to set for the dialog shell.
     */
    protected abstract String getShellTitle();

    /**
     * Returns a list of PermissionOption objects that describe which S3
     * Permissions are applicable for the specific UI provided by a subclass.
     *
     * @return a list of PermissionOption objects that describe which S3
     *         Permissions are applicable for the specific UI provided by a
     *         subclass.
     */
    protected abstract List<PermissionOption> getPermissionOptions();

    /**
     * Returns the S3 ACL to use when initializing the dialog's UI.
     *
     * @return the S3 ACL to use when initializing the dialog's UI.
     */
    protected abstract AccessControlList getAcl();



    @Override
    public void create() {
        super.create();
        getShell().setText(getShellTitle());
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout());

        createPermissionsTable(composite);

        Composite buttonComposite = new Composite(composite, SWT.NONE);
        GridLayout buttonCompositeLayout = new GridLayout(2, true);
        buttonCompositeLayout.horizontalSpacing = 0;
        buttonCompositeLayout.marginLeft = 0;
        buttonCompositeLayout.marginWidth = 0;
        buttonCompositeLayout.marginHeight = 0;
        buttonComposite.setLayout(buttonCompositeLayout);
        buttonComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        Button addButton = new Button(buttonComposite, SWT.PUSH);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        addButton.setLayoutData(gridData);
        addButton.setText("Add Grant");
        addButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createTableItem(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });


        Button removeButton = new Button(buttonComposite, SWT.PUSH);
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        removeButton.setText("Remove Grants");
        removeButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_REMOVE));
        removeButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<Integer> selectionIndices = new ArrayList<>();
                for (int selectionIndex : table.getSelectionIndices()) {
                    // Skip the first two rows with group permissions
                    if (selectionIndex < 2) continue;

                    selectionIndices.add(selectionIndex);
                    TableItem item = table.getItem(selectionIndex);
                    TableItemData data = (TableItemData)item.getData();

                    for (Button button : data.checkboxes) button.dispose();
                    if (data.granteeTextbox != null) data.granteeTextbox.dispose();
                }

                int[] indiciesToRemove = new int[selectionIndices.size()];
                for (int i = 0; i < selectionIndices.size(); i++) {
                    indiciesToRemove[i] = selectionIndices.get(i);
                }
                table.remove(indiciesToRemove);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        displayPermissions();

        return composite;
    }

    public AccessControlList getAccessControlList() {
        return acl;
    }

    @Override
    public boolean close() {
        acl = new AccessControlList();
        acl.setOwner((Owner)table.getData("owner"));
        Set<Grant> grants = acl.getGrants();

        for (TableItem item : table.getItems()) {
            TableItemData data = (TableItemData)item.getData();
            Grantee grantee = data.grantee;

            for (Button checkbox : data.checkboxes) {
                if (checkbox.getSelection()) {
                    Permission permission = (Permission)checkbox.getData();
                    grants.add(new Grant(grantee, permission));
                }
            }
        }

        /*
         * TODO: We might want to run the S3 setAcl call here in case it
         *       fails with some correctable error, like a non-unique email
         *       address grantee, uknown id, etc.
         */

        return super.close();
    }

    private void createPermissionsTable(Composite composite) {
        table = new Table(composite, SWT.BORDER | SWT.MULTI);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        layoutData.minimumHeight = 125;
        table.setLayoutData(layoutData);

        TableColumn granteeColumn = new TableColumn(table, SWT.NONE);
        granteeColumn.setWidth(250);
        granteeColumn.setText("Grantee");

        for (PermissionOption permissionOption : getPermissionOptions()) {
            TableColumn readDataPermissionColumn = new TableColumn(table, SWT.CENTER);
            readDataPermissionColumn.setWidth(75);
            readDataPermissionColumn.setText(permissionOption.header);
        }

        table.addListener(SWT.MeasureItem, new Listener() {
            private Point preferredSize;

            @Override
            public void handleEvent(Event event) {
                if (preferredSize == null) {
                    Text text = new Text(table, SWT.BORDER);
                    preferredSize = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                    text.dispose();
                }

                event.height = preferredSize.y;
            }
        });

        table.layout();
    }

    static class PermissionOption {
        Permission permission;
        String header;

        PermissionOption(Permission permission, String header) {
            this.permission = permission;
            this.header = header;
        }
    }


    private TableItem createTableItem(boolean withTextEditor) {
        final TableItem item = new TableItem(table, SWT.NONE);
        final TableItemData data = new TableItemData();
        item.setData(data);

        if (withTextEditor) {
            TableEditor editor = new TableEditor(table);
            final Text granteeText = new Text(table, SWT.BORDER);
            data.granteeTextbox = granteeText;
            granteeText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    String grantee = granteeText.getText();
                    if (grantee.contains("@")) {
                        data.grantee = new EmailAddressGrantee(grantee);
                    } else {
                        data.grantee = new CanonicalGrantee(grantee);
                    }
                }
            });
            granteeText.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {}

                @Override
                public void focusGained(FocusEvent e) {
                    table.setSelection(item);
                }
            });
            granteeText.setFocus();
            editor.grabHorizontal = true;
            editor.setEditor(granteeText, item, 0);
        }

        int column = 1;
        for (PermissionOption permissionOption : getPermissionOptions()) {
            TableEditor editor = new TableEditor(table);
            final Button checkbox = new Button(table, SWT.CHECK);
            checkbox.setData(permissionOption.permission);
            Point checkboxSize = checkbox.computeSize(SWT.DEFAULT, table.getItemHeight());
            editor.minimumWidth = checkboxSize.x;
            editor.minimumHeight = checkboxSize.y;
            editor.setEditor(checkbox, item, column++);

            data.checkboxes.add(checkbox);
        }

        return item;
    }


    private void displayPermissions() {
        AccessControlList startingAcl = getAcl();

        table.clearAll();
        table.setData("owner", startingAcl.getOwner());

        Map<Grantee, Set<Permission>> permissionsByGrantee = new TreeMap<>(new GranteeComparator());
        permissionsByGrantee.put(GroupGrantee.AllUsers, new HashSet<Permission>());
        permissionsByGrantee.put(GroupGrantee.AuthenticatedUsers, new HashSet<Permission>());
        for (Grant grant : startingAcl.getGrants()) {
            if (permissionsByGrantee.get(grant.getGrantee()) == null) {
                permissionsByGrantee.put(grant.getGrantee(), new HashSet<Permission>());
            }
            Set<Permission> permissions = permissionsByGrantee.get(grant.getGrantee());
            permissions.add(grant.getPermission());
        }


        for (Grantee grantee : permissionsByGrantee.keySet()) {
            Set<Permission> permissions = permissionsByGrantee.get(grantee);

            TableItem item = createTableItem(false);
            TableItemData data = (TableItemData)item.getData();
            data.grantee = grantee;

            item.setText(new String[] {getGranteeDisplayName(grantee), "", ""});

            if (permissions.contains(Permission.FullControl)) {
                for (Button checkbox : data.checkboxes) {
                    checkbox.setSelection(true);
                }
            }

            for (Button checkbox : data.checkboxes) {
                Permission permission = (Permission)checkbox.getData();
                if (permissions.contains(permission)) {
                    checkbox.setSelection(true);
                }
            }
        }
    }

    private String getGranteeDisplayName(Grantee grantee) {
        if (grantee instanceof CanonicalGrantee) {
            CanonicalGrantee canonicalGrantee = (CanonicalGrantee)grantee;
            if (canonicalGrantee.getDisplayName() != null) {
                return canonicalGrantee.getDisplayName();
            }
        }

        if (grantee instanceof GroupGrantee) {
            switch ((GroupGrantee)grantee) {
            case AllUsers:
                return "All Users";
            case AuthenticatedUsers:
                return "Authenticated Users";
            case LogDelivery:
                return "Log Delivery";
            }
        }

        return grantee.getIdentifier();
    }

    static class TableItemData {
        Grantee grantee;
        List<Button> checkboxes = new ArrayList<>();
        Text granteeTextbox;
    }

}
