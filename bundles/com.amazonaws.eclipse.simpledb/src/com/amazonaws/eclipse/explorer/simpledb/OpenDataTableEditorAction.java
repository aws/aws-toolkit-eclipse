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
package com.amazonaws.eclipse.explorer.simpledb;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo;
import org.eclipse.datatools.connectivity.sqm.core.rte.jdbc.JDBCSchema;
import org.eclipse.datatools.connectivity.sqm.core.rte.jdbc.JDBCTable;
import org.eclipse.datatools.modelbase.sql.schema.Catalog;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditorInput;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;

public class OpenDataTableEditorAction extends Action {
    private static final String TABLE_DATA_EDITOR_ID = "org.eclipse.datatools.sqltools.data.internal.ui.editor.tableDataEditor";

    private final String domainName;

    public OpenDataTableEditorAction(final String domainName) {
        this.domainName = domainName;
        setText("Open DTP Data Editor");
        setToolTipText("Opens the DTP data editor for the contents of this domain");
    }

    private JDBCTable findTableByName(final List<JDBCTable> tables, final String name) {
        for (JDBCTable table : tables) {
            if (table.getName().equals(this.domainName)) {
                return table;
            }
        }

        return null;
    }

    @Override
    public void run() {
        Region currentRegion = RegionUtils.getCurrentRegion();
        String endpoint = currentRegion.getServiceEndpoints().get(ServiceAbbreviations.SIMPLEDB);

        if (endpoint.contains("://")) {
            endpoint = endpoint.substring(endpoint.indexOf("://") + 3);
        }

        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.lastIndexOf("/"));
        }

        SimpleDBConnectionProfileManager sdbConnectionProfileManager = new SimpleDBConnectionProfileManager();
        IConnectionProfile connectionProfile = sdbConnectionProfileManager.findOrCreateConnectionProfile(endpoint);
        IStatus status = connectionProfile.connect();
        if (!status.isOK()) {
            throw new RuntimeException("Unable to connect to Amazon SimpleDB:  " + status.getMessage());
        }

        IManagedConnection managedConnection = (connectionProfile).getManagedConnection("org.eclipse.datatools.connectivity.sqm.core.connection.ConnectionInfo");
        if (managedConnection != null) {
            try {
                ConnectionInfo connectionInfo = (ConnectionInfo) managedConnection.getConnection().getRawConnection();
                if (connectionInfo != null) {
                    Database database = connectionInfo.getSharedDatabase();

                    System.out.println("Schemas:");
                    int i = 1;
                    for (Object obj : database.getSchemas()) {
                        System.out.println(i++ + " - " + obj);
                    }

                    // TODO: which catalog?
                    for (Object obj : database.getCatalogs()) {
                        Catalog catalog = (Catalog)obj;
                        EList<JDBCSchema> schemas = catalog.getSchemas();
                        // TODO: Is this always the right schema?
                        JDBCSchema schema = schemas.get(0);
                        EList<JDBCTable> tables = schema.getTables();

                        JDBCTable table = findTableByName(tables, this.domainName);
                        if (table != null) {
                            IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                            workbenchPage.openEditor(new TableDataEditorInput(table), TABLE_DATA_EDITOR_ID);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
