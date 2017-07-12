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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.core.containment.ContainmentService;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.providers.content.virtual.ColumnNode;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.virtual.IColumnNode;
import org.eclipse.datatools.connectivity.sqm.core.rte.ICatalogObject;
import org.eclipse.datatools.connectivity.sqm.core.ui.explorer.virtual.IVirtualNode;
import org.eclipse.datatools.connectivity.sqm.internal.core.RDBCorePlugin;
import org.eclipse.datatools.connectivity.sqm.internal.core.containment.GroupID;
import org.eclipse.datatools.connectivity.sqm.internal.core.util.ConnectionUtil;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.navigator.CommonViewer;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;
import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

public class NewAttributeWizardAction extends AbstractEditorClosingAction {
    private static final String TEXT = Messages.attributeNewMenu;

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
                Table table = findTable(o);
                Database db = findDatabase(o);
                if (table != null && db != null) {

                    if (!closeEditors(table)) {
                        return;
                    }

                    ConnectionInfo info = ConnectionUtil.getConnectionForEObject(db);
                    if (info != null) {
                        InputDialog dlg = new InputDialog(display.getActiveShell(), Messages.CreateNewAttribute,
                                Messages.NewAttributeName, "", new IInputValidator() { //$NON-NLS-1$

                            @Override
                            public String isValid(final String newText) {
                                return newText != null && newText.trim().length() > 0 ? null : Messages.EmptyAttributeName;
                            }

                        });
                        if (dlg.open() == Window.OK) {
                            performFinish(info, table.getName(), dlg.getValue(), o);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Table findTable(final Object o) {
        if (o instanceof IColumnNode) {
            ColumnNode node = (ColumnNode) o;
            Object parent = node.getParent();
            if (parent instanceof Table) {
                Table table = (Table) parent;
                return table;
            }
        }
        return null;
    }

    private Database findDatabase(final Object o) {
        if (o instanceof IColumnNode) {
            ColumnNode node = (ColumnNode) o;
            Object parent = node.getParent();
            if (parent instanceof Table) {
                Table table = (Table) parent;
                Schema schema = table.getSchema();
                Database db = schema.getDatabase() == null ? schema.getCatalog().getDatabase() : schema.getDatabase();
                //        JDBCColumn o2 = new JDBCColumn();
                //        o2.setName("FooBarBaz");
                //        DatabaseDefinition definition = RDBCorePlugin.getDefault().getDatabaseDefinitionRegistry().getDefinition(db);
                //        PredefinedDataTypeDefinition pdtd = definition.getPredefinedDataTypeDefinition("TEXT");
                //        PredefinedDataType pdt = definition.getPredefinedDataType(pdtd);
                //        o2.setDataType(pdt);
                //        ((JDBCTable) table).getColumns().add(o2);
                //        node.addChildren(o2);
                //        CommonViewer cViewer = (CommonViewer) this.event.getSource();
                //        //        cViewer.getControl().setRedraw(false);
                //        cViewer.add(node, o2);
                //        //        cViewer.getControl().setRedraw(true);
                return db;
            }
        }
        return null;
    }

    public boolean performFinish(final ConnectionInfo info, final String domainName, final String attrName,
            final Object node) {
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

                    //          conn.prepareStatement("create domain " + domainName).executeUpdate();
                    JdbcConnection jdbcConn = (JdbcConnection) conn;
                    jdbcConn.addPendingColumn(domainName, attrName);

                    CommonViewer cViewer = (CommonViewer) NewAttributeWizardAction.this.event.getSource();

                    TreePath[] expanded = cViewer.getExpandedTreePaths();
                    //          Object selected = ((IStructuredSelection) NewAttributeWizardAction.this.event.getSelection())
                    //              .getFirstElement();

                    IVirtualNode vnode = (IVirtualNode) node;
                    ICatalogObject table = (ICatalogObject) vnode.getParent();
                    table.refresh();

                    //          if (NewAttributeWizardAction.this.event.getSelection() instanceof IStructuredSelection
                    //              && ((IStructuredSelection) NewAttributeWizardAction.this.event.getSelection()).getFirstElement() instanceof EObject) {
                    //            EObject o = (EObject) ((IStructuredSelection) NewAttributeWizardAction.this.event.getSelection())
                    //                .getFirstElement();
                    //            cViewer.expandToLevel(o, 1);
                    //          }
                    //          cViewer.refresh();

                    //          Object col = ((JDBCTable) table).getColumns().get(((JDBCTable) table).getColumns().size() - 1);

                    for (TreePath path : expanded) {
                        cViewer.expandToLevel(path, 0);
                    }
                    cViewer.setSelection(new StructuredSelection(table), true);

                    //          cViewer.reveal(table);
                    //          cViewer.expandToLevel(table, 2);
                    //          cViewer.setSelection(new StructuredSelection(table));

                    //          cViewer.reveal(((IStructuredSelection) NewAttributeWizardAction.this.event.getSelection()).getFirstElement());
                    //          cViewer.expandToLevel(((IStructuredSelection) NewAttributeWizardAction.this.event.getSelection())
                    //              .getFirstElement(), 1);
                    //          cViewer.setSelection(new StructuredSelection(((IStructuredSelection) NewAttributeWizardAction.this.event
                    //              .getSelection()).getFirstElement()));

                } catch (Exception e) {
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
