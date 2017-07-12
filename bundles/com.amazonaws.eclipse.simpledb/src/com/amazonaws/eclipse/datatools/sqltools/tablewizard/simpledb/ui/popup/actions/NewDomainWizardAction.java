/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.popup.actions;

import java.sql.Connection;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.core.containment.ContainmentService;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.popup.AbstractAction;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.virtual.ITableNode;
import org.eclipse.datatools.connectivity.sqm.core.rte.ICatalogObject;
import org.eclipse.datatools.connectivity.sqm.internal.core.RDBCorePlugin;
import org.eclipse.datatools.connectivity.sqm.internal.core.containment.GroupID;
import org.eclipse.datatools.connectivity.sqm.internal.core.util.ConnectionUtil;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver.JdbcStatement;
import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

public class NewDomainWizardAction extends AbstractAction {
    private static final String TEXT = Messages.domainNewMenu;

    @Override
    public void selectionChanged(final SelectionChangedEvent event) {
        super.selectionChanged(event);

        if (event.getSelection() instanceof IStructuredSelection
                && ((IStructuredSelection) event.getSelection()).getFirstElement() instanceof EObject) {
            EObject o = (EObject) ((IStructuredSelection) event.getSelection()).getFirstElement();
            ContainmentService containmentService = RDBCorePlugin.getDefault().getContainmentService();
            String groupID = containmentService.getGroupId(o);
            setEnabled((groupID != null) && (groupID.startsWith(GroupID.CORE_PREFIX)));
        }
    }

    @Override
    public void initialize() {
        initializeAction(null, null, TEXT, TEXT);
    }

    @Override
    public void run() {
        if (!this.event.getSelection().isEmpty()) {
            try {
                Display display = Display.getCurrent();
                Object o = ((IStructuredSelection) this.event.getSelection()).getFirstElement();
                Database db = findDatabase(o);
                if (db != null) {
                    ConnectionInfo info = ConnectionUtil.getConnectionForEObject(db);
                    if (info != null) {
                        InputDialog dlg = new InputDialog(display.getActiveShell(), Messages.CreateNewDomain,
                                Messages.NewDomainName, "", new IInputValidator() { //$NON-NLS-1$

                            @Override
                            public String isValid(final String newText) {
                                return newText != null && newText.trim().length() > 0 ? null : Messages.EmptyDomainName;
                            }

                        });
                        if (dlg.open() == Window.OK) {
                            String domainName = dlg.getValue();

                            if (!Pattern.matches("^[\\w\\.-]{3,255}+$", domainName)) { //$NON-NLS-1$
                                MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.InvalidDomainName,
                                        Messages.InvalidDomainNameDescription);
                                return;
                            }

                            performFinish(info, domainName, db);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Database findDatabase(final Object o) {
        if (o instanceof ITableNode) {
            ITableNode node = (ITableNode) o;
            Object parent = node.getParent();
            if (parent instanceof Database) {
                return (Database) parent;
            }
            if (parent instanceof Schema) {
                return ((Schema) parent).getDatabase() != null ? ((Schema) parent).getDatabase() : ((Schema) parent)
                        .getCatalog().getDatabase();
            }
        }
        return null;
    }

    public boolean performFinish(final ConnectionInfo info, final String domainName, final Database db) {
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            @Override
            protected void execute(final IProgressMonitor monitor) {
                try {
                    //          SQLDevToolsConfiguration f = SQLToolsFacade.getConfigurationByProfileName(info.getConnectionProfile().getName());
                    //          ConnectionService conService = f.getConnectionService();
                    //          DatabaseVendorDefinitionId dbVendorId = ProfileUtil.getDatabaseVendorDefinitionId(profileName);
                    //          DatabaseIdentifier databaseIdentifier = new DatabaseIdentifier(info.getConnectionProfile().getName(), info.getDatabaseName());
                    //          Connection conn = conService.createConnection(databaseIdentifier, true);
                    Connection conn = info.getSharedConnection();
                    conn.prepareStatement("create domain " + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE + domainName //$NON-NLS-1$
                            + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE).executeUpdate();

                    CommonViewer cViewer = (CommonViewer) NewDomainWizardAction.this.event.getSource();

                    TreePath[] expanded = cViewer.getExpandedTreePaths();
                    //          Object selected = ((IStructuredSelection) NewDomainWizardAction.this.event.getSelection()).getFirstElement();

                    ((ICatalogObject) db).refresh();

                    for (TreePath path : expanded) {
                        cViewer.expandToLevel(path, 0);
                    }
                    cViewer.setSelection(new StructuredSelection(db), true);

                } catch (Exception e) {
                    Status status = new Status(IStatus.ERROR,
                            "com.amazonaws.eclipse.datatools.enablement.simpledb",
                            "Unable to create domain: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.SHOW);
                    e.printStackTrace();
                } finally {
                    if (monitor != null) {
                        monitor.done();
                    }
                }
            }
        };

        try {
            Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().run(false, true, operation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
