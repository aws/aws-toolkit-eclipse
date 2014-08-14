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

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.popup.AbstractAction;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

/**
 * Abstract action which is able to close editors of the given table when asked to.
 */
public abstract class AbstractEditorClosingAction extends AbstractAction {

    /**
     * @param table
     *          table to be modified
     * @return <code>true</code> if no open editors left; i.e. there was no open editors or user confirmed to close
     */
    public boolean closeEditors(final Table table) {
        IEditorReference ep = getEditor(table);

        if (ep != null) {
            boolean confirm = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.EditorsToBeClosed,
                    Messages.EditorsToBeClosedMessage);
            if (!confirm) {
                return false;
            }
            return ep.getEditor(false).getSite().getPage().closeEditors(new IEditorReference[] { ep }, true);
        }

        return true;
    }

    /**
     * Get the editor that corresponds to the table and return the reference to the editor.
     *
     * @param table
     * @return
     */
    private IEditorReference getEditor(final Table table) {
        IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : workbenchWindows) {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] editorReferences = page.getEditorReferences();
                for (IEditorReference reference : editorReferences) {
                    IEditorPart editor = reference.getEditor(false);
                    if (editor instanceof TableDataEditor && table.equals(((TableDataEditor) editor).getSqlTable())) {
                        return reference;
                    }
                }
            }
        }
        return null;
    }
}
