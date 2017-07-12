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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.sqltools.data.internal.core.editor.RowDataImpl;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.IExternalTableDataEditor;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.ITableDataEditor;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataCell;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.amazonaws.eclipse.datatools.enablement.simpledb.editor.wizard.SDBTableDataWizard;
import com.amazonaws.eclipse.datatools.enablement.simpledb.editor.wizard.SDBTableIdDataWizard;

public class SDBTextEditor implements IExternalTableDataEditor {

    public SDBTextEditor() {}

    @Override
    public void externalEdit(final ITableDataEditor editor) {
        Object obj = editor;
        if (obj instanceof TableDataEditor) {
            externalEdit((TableDataEditor)obj);
        }
    }


    /* (non-Javadoc)
     * @see org.eclipse.datatools.sqltools.data.internal.ui.editor.IExternalTableDataEditor#externalEdit(org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor)
     */
    public void externalEdit(final TableDataEditor editor) {
        if (editor.getCursor().getColumn() == 0) {
            Object value = getCellValue(editor);
            if ((value instanceof SimpleDBItemName && !((SimpleDBItemName) value).isPersisted()) || value instanceof String
                    || value == null) {
                SDBTableIdDataWizard wizard = new SDBTableIdDataWizard(editor);
                WizardDialog dialog = new WizardDialog(editor.getSite().getShell(), wizard);
                dialog.setPageSize(400, 250);
                dialog.open();
            } else {
                ErrorDialog ed = new ErrorDialog(editor.getEditorSite().getShell(), Messages.idErrorDialogTitle,
                        Messages.idErrorDialogMessage,
                        new Status(IStatus.INFO, Activator.PLUGIN_ID, Messages.idErrorStatusMessage), SWT.ERROR);
                ed.open();
            }
        } else {
            SDBTableDataWizard wizard = new SDBTableDataWizard(editor);
            WizardDialog dialog = new WizardDialog(editor.getSite().getShell(), wizard);
            dialog.setPageSize(400, 250);
            dialog.open();
        }

    }

    private Object getCellValue(final TableDataEditor editor) {
        int col = editor.getCursor().getColumn();
        StructuredSelection selection = (StructuredSelection) editor.getSelectionProvider().getSelection();
        TableDataCell firstElement = (TableDataCell) selection.getFirstElement();
        Object row = firstElement.getRow();
        if (row instanceof RowDataImpl) {
            RowDataImpl rowData = (RowDataImpl) row;
            Object value = rowData.getValue(col);
            return value;
        } else {
            //This usually means that the row was just created and no RowDataImpl have been created yet.
            return ""; //$NON-NLS-1$
        }
    }

}
