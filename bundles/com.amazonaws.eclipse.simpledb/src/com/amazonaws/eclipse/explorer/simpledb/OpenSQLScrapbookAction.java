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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.sqltools.editor.core.connection.ISQLEditorConnectionInfo;
import org.eclipse.datatools.sqltools.internal.sqlscrapbook.editor.SQLScrapbookEditor;
import org.eclipse.datatools.sqltools.internal.sqlscrapbook.util.SQLFileUtil;
import org.eclipse.datatools.sqltools.sqlbuilder.model.SQLBuilderConnectionInfo;
import org.eclipse.datatools.sqltools.sqleditor.SQLEditorStorageEditorInput;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;


public class OpenSQLScrapbookAction extends Action {
    public OpenSQLScrapbookAction() {
        setText("Open DTP SQL Scrapbook");
        setToolTipText("Opens the DTP SQL Scrapbook to run queries on your data");
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


        ISQLEditorConnectionInfo editorConnectionInfo = new SQLBuilderConnectionInfo(connectionProfile);
        SQLEditorStorageEditorInput editorStorageEditorInput = new SQLEditorStorageEditorInput("", "");
        editorStorageEditorInput.setConnectionInfo(SQLFileUtil.getConnectionInfo4Scrapbook(editorConnectionInfo));

        // the name will show as the title of the editor
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorReference[] editors = window.getActivePage().getEditorReferences();
        int suffix = 0;
        List<String> editorNameList = new ArrayList<>();
        for (int i = 0; i < editors.length; i++) {
            editorNameList.add(editors[i].getName());
        }

        while (true) {
            String name = "SQL Scrapbook - " + connectionProfile.getName();
            if (suffix > 0) {
                name = name + " " + suffix;
            }

            if (!editorNameList.contains(name)) {
                editorStorageEditorInput.setName(name);
                try {
                    window.getActivePage().openEditor(editorStorageEditorInput, SQLScrapbookEditor.EDITOR_ID);
                } catch (PartInitException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                break;
            }
            suffix++;
        }
    }
}
