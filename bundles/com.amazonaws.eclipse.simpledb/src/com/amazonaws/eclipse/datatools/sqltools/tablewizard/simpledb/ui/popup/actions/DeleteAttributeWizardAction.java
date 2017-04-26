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
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.connectivity.sqm.core.connection.DatabaseConnectionRegistry;
import org.eclipse.datatools.connectivity.sqm.core.containment.ContainmentService;
import org.eclipse.datatools.connectivity.sqm.core.rte.ICatalogObject;
import org.eclipse.datatools.connectivity.sqm.internal.core.RDBCorePlugin;
import org.eclipse.datatools.connectivity.sqm.internal.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.internal.core.containment.GroupID;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.navigator.CommonViewer;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver.JdbcStatement;
import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

public class DeleteAttributeWizardAction extends AbstractEditorClosingAction {

    private static final String TEXT = Messages.attributeDeleteMenu;

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
            Iterator<?> iter = ((IStructuredSelection) this.event.getSelection()).iterator();
            Object selectedObj = iter.next();

            if (selectedObj instanceof Column) {
                Column column = (Column) selectedObj;

                boolean confirmed = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
                        Messages.ConfirmAttributeDeletion, MessageFormat.format(Messages.ConfirmAttributeDeletionMessage, column
                                .getTable().getName()
                                + "." + column.getName())); //$NON-NLS-1$
                if (!confirmed) {
                    return;
                }

                if (!closeEditors(column.getTable())) {
                    return;
                }

                Database db = getDatabase(column.getTable().getSchema());
                ConnectionInfo conInfo = (ConnectionInfo) DatabaseConnectionRegistry.getConnectionForDatabase(db);
                try {
                    deleteColumn(conInfo, column);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Database getDatabase(final Schema schema) {
        return schema.getCatalog() == null ? schema.getDatabase() : schema.getCatalog().getDatabase();
    }

    /**
     * @param info
     * @param column
     */
    private void deleteColumn(final ConnectionInfo info, final Column column) throws Exception {
        final Table table = column.getTable();

        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            @Override
            protected void execute(final IProgressMonitor monitor) {
                try {
                    Connection conn = info.getSharedConnection();
                    conn.prepareStatement("alter table " + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE + table.getName() //$NON-NLS-1$
                            + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE + " drop " + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE //$NON-NLS-1$
                            + column.getName() + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE).executeUpdate();

                    CommonViewer cViewer = (CommonViewer) DeleteAttributeWizardAction.this.event.getSource();
                    TreePath[] expanded = cViewer.getExpandedTreePaths();

                    ((ICatalogObject) table).refresh();

                    for (TreePath path : expanded) {
                        cViewer.expandToLevel(path, 0);
                    }
                    cViewer.setSelection(new StructuredSelection(table), true);

                } catch (Exception e) {
                    Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
                } finally {
                    monitor.done();
                }
            }
        };

        Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().run(false, true, operation);
    }
}
