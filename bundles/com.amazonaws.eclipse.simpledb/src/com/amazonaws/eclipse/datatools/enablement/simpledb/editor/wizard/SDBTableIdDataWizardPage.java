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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor.wizard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.sqltools.data.internal.core.editor.RowDataImpl;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataCell;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.editor.Messages;

public class SDBTableIdDataWizardPage extends WizardPage {

    private static final int INDENT = 6;
    private final TableDataEditor editor;
    private int col;
    private RowDataImpl rowData;
    private Text t;

    public SDBTableIdDataWizardPage(final TableDataEditor editor) {
        super(Messages.idPageTitle);
        setTitle(Messages.idPageTitle);
        setMessage(Messages.idMainMessage);
        this.editor = editor;
    }

    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        FormLayout layout = new FormLayout();
        layout.marginWidth = INDENT;
        layout.marginHeight = INDENT;
        composite.setLayout(layout);

        final Label l = new Label(composite, SWT.None);
        l.setText(Messages.labelEditAttributeValues);

        this.t = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        this.t.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent a) {
                if (SDBTableIdDataWizardPage.this.t.getText().getBytes().length > 1024) {
                    setErrorMessage(Messages.valueToLong);
                    setPageComplete(false);
                } else {
                    setErrorMessage(null);
                    setPageComplete(true);
                }
            }
        });
        FormData d = new FormData();
        d.top = new FormAttachment(0, 0);
        d.left = new FormAttachment(0, 0);
        d.right = new FormAttachment(100, 0);
        l.setLayoutData(d);

        d = new FormData();
        d.top = new FormAttachment(l, INDENT);
        d.left = new FormAttachment(0, 0);
        d.right = new FormAttachment(100, 0);
        d.bottom = new FormAttachment(100, 0);
        this.t.setLayoutData(d);

        createContent();

        setControl(composite);
    }

    protected void saveData() {

        Object oldVal = this.rowData.getValue(this.col);
        if (oldVal instanceof SimpleDBItemName) {
            ((SimpleDBItemName) oldVal).setItemName(this.t.getText());
        } else {
            this.rowData.updateValue(this.col, this.t.getText());
        }

        try {
            Class<?> c = Class.forName(TableDataEditor.class.getCanonicalName());
            Field f1 = c.getDeclaredField("tableViewer");//$NON-NLS-1$
            f1.setAccessible(true);

            TableViewer tv = (TableViewer) f1.get(this.editor);
            tv.refresh(this.rowData);
            this.editor.getCursor().redraw();

            Method m = c.getDeclaredMethod("setDirty", new Class[] { java.lang.Boolean.TYPE });//$NON-NLS-1$
            m.setAccessible(true);
            m.invoke(this.editor, new Object[] { Boolean.TRUE });
            this.editor.setDirtyBackground(this.col, this.editor.getCursor().getRow());

        } catch (SecurityException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (ClassNotFoundException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (NoSuchFieldException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (IllegalArgumentException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (IllegalAccessException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (NoSuchMethodException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        } catch (InvocationTargetException e) {
            Activator.logMessage(e.getMessage(), e, IStatus.ERROR);
        }
    }

    private void createContent() {
        Object value;

        this.col = this.editor.getCursor().getColumn();
        StructuredSelection selection = (StructuredSelection) this.editor.getSelectionProvider().getSelection();
        TableDataCell firstElement = (TableDataCell) selection.getFirstElement();
        Object row = firstElement.getRow();
        if (row instanceof RowDataImpl) {
            this.rowData = (RowDataImpl) row;
            value = this.rowData.getValue(this.col);
        } else {
            //This usually means that the row was just created and no RowDataImpl have been created yet.
            this.rowData = (RowDataImpl) this.editor.getOrCreateRow();
            value = ""; //$NON-NLS-1$
        }
        if (value == null) {
            this.t.setText(""); //$NON-NLS-1$
            setPageComplete(true);
        } else if (value instanceof String) {
            this.t.setText((String) value);
            setPageComplete(true);
        } else if (value instanceof SimpleDBItemName) {
            this.t.setText(((SimpleDBItemName) value).getItemName());
            setPageComplete(true);
        } else {
            setErrorMessage((MessageFormat.format(Messages.incorrectDataType, value.getClass().getCanonicalName())));
            this.t.setEnabled(false);
            setPageComplete(false);
        }

    }

}
