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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.sqltools.data.internal.core.editor.RowDataImpl;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataCell;
import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.editor.Messages;

public class SDBTableDataWizardPage extends WizardPage {

    private static final int INDENT = 6;
    private final TableDataEditor editor;
    private int col;
    private RowDataImpl rowData;
    private Table table;
    private Button edit;
    private Button add;
    private Button remove;
    private TableColumn tColumn;

    public SDBTableDataWizardPage(final TableDataEditor editor) {
        super(Messages.pageTitle);
        setTitle(Messages.pageTitle);
        setMessage(Messages.mainMessage);
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
        this.table = new Table(composite, SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER | SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.MULTI);
        this.add = new Button(composite, SWT.None);
        this.remove = new Button(composite, SWT.None);
        this.edit = new Button(composite, SWT.None);
        this.table.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent se) {
                SDBTableDataWizardPage.this.edit.setEnabled(SDBTableDataWizardPage.this.table.getSelection().length == 1);
                SDBTableDataWizardPage.this.remove.setEnabled(SDBTableDataWizardPage.this.table.getSelection().length > 0);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {

            }
        });
        this.table.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(final MouseEvent arg0) {

            }

            @Override
            public void mouseDown(final MouseEvent arg0) {

            }

            @Override
            public void mouseDoubleClick(final MouseEvent arg0) {
                if (SDBTableDataWizardPage.this.table.getSelectionCount() > 0) {
                    String newValue = getNewValue(SDBTableDataWizardPage.this.table.getSelection()[0].getText());
                    if (newValue != null) {
                        SDBTableDataWizardPage.this.table.getSelection()[0].setText(newValue);
                        SDBTableDataWizardPage.this.tColumn.pack();
                    }
                }
            }
        });
        this.tColumn = new TableColumn(this.table, SWT.NONE);
        createContent();
        this.tColumn.pack();

        this.edit.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                String newValue = getNewValue(SDBTableDataWizardPage.this.table.getSelection()[0].getText());
                if (newValue != null) {
                    SDBTableDataWizardPage.this.table.getSelection()[0].setText(newValue);
                    SDBTableDataWizardPage.this.tColumn.pack();

                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
            }
        });

        this.add.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                String newValue = getNewValue(Messages.newValue);
                if (newValue != null) {
                    TableItem item = createItem(newValue);
                    SDBTableDataWizardPage.this.table.setSelection(item);
                    SDBTableDataWizardPage.this.remove.setEnabled(true);
                    SDBTableDataWizardPage.this.edit.setEnabled(true);
                    SDBTableDataWizardPage.this.tColumn.pack();
                }

            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
            }
        });

        this.remove.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                int[] selectionIndices = SDBTableDataWizardPage.this.table.getSelectionIndices();
                SDBTableDataWizardPage.this.table.remove(selectionIndices);

                if (selectionIndices != null && SDBTableDataWizardPage.this.table.getItemCount() > 0) {
                    int minSelected = Integer.MAX_VALUE;
                    for (int i = 0; i < selectionIndices.length; i++) {
                        if (selectionIndices[i] < minSelected) {
                            minSelected = selectionIndices[i];
                        }
                    }
                    --minSelected;
                    if (minSelected < 0 || minSelected == Integer.MAX_VALUE) {
                        minSelected = 0;
                    }
                    try {
                        SDBTableDataWizardPage.this.table.setSelection(minSelected);
                    } catch (Exception e) { // just to be on the safe side
                        e.printStackTrace();
                    }
                }
                SDBTableDataWizardPage.this.edit.setEnabled(SDBTableDataWizardPage.this.table.getSelection().length == 1);
                SDBTableDataWizardPage.this.remove.setEnabled(SDBTableDataWizardPage.this.table.getSelection().length > 0);
                SDBTableDataWizardPage.this.tColumn.pack();

            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {

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
        d.right = new FormAttachment(this.remove, -INDENT);
        d.bottom = new FormAttachment(100, 0);
        this.table.setLayoutData(d);

        this.add.setText(Messages.add);
        d = new FormData();
        d.right = new FormAttachment(100, 0);
        d.top = new FormAttachment(l, INDENT);
        d.width = 70;
        this.add.setLayoutData(d);

        this.remove.setText(Messages.remove);
        d = new FormData();
        d.top = new FormAttachment(this.add, 0);
        d.right = new FormAttachment(100, 0);
        d.width = 70;
        this.remove.setLayoutData(d);

        this.edit.setText(Messages.edit);
        d = new FormData();
        d.right = new FormAttachment(100, 0);
        d.top = new FormAttachment(this.remove, INDENT);
        d.width = 70;
        this.edit.setLayoutData(d);

        setControl(composite);
    }

    private String getNewValue(final String value) {
        InputDialog id = new InputDialog(this.editor.getEditorSite().getShell(), Messages.dialogTitle,
                Messages.dialogDescription, value, new IInputValidator() {

            @Override
            public String isValid(final String s) {
                if (s.getBytes().length > 1024) {
                    return Messages.valueToLong;
                }
                return null;
            }
        });
        if (id.open() == Window.OK) {
            return id.getValue();
        } else {
            return null;
        }
    }

    protected void saveData() {

        TableItem[] items = this.table.getItems();
        if (this.rowData != null) {
            if (items.length == 0) {
                this.rowData.updateValue(this.col, null);
            } else if (items.length == 1) {
                this.rowData.updateValue(this.col, items[0].getText());
            } else {
                String[] s = createValue(items);
                this.rowData.updateValue(this.col, s);
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
    }

    private String[] createValue(final TableItem[] items) {
        String[] s = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            TableItem it = items[i];
            s[i] = it.getText();
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private void createContent() {
        this.col = this.editor.getCursor().getColumn();
        StructuredSelection selection = (StructuredSelection) this.editor.getSelectionProvider().getSelection();
        TableDataCell firstElement = (TableDataCell) selection.getFirstElement();
        Object value = null;

        if (firstElement.getRow() instanceof RowDataImpl) {
            this.rowData = (RowDataImpl) firstElement.getRow();
            value = this.rowData.getValue(this.col);
        } else {
            //There is no rowData yet, we need to create it ourselves
            //      ITableData tableData = this.editor.getTableData();
            this.rowData = (RowDataImpl) this.editor.getOrCreateRow();
            value = firstElement.getRow();
        }

        if (value instanceof String) {
            createItem((String) value);
        } else if (value instanceof String[]) {
            String[] strings = (String[]) value;
            for (String s : strings) {
                createItem(s);
            }
        } else if (value instanceof ArrayList) {
            List<String> strings = (List<String>) value;
            for (String s : strings) {
                createItem(s);
            }
        }

        if (this.table.getItems().length > 0) {
            this.table.setSelection(0);
            this.remove.setEnabled(true);
            this.edit.setEnabled(true);
        } else {
            this.remove.setEnabled(false);
            this.edit.setEnabled(false);
        }
    }

    private TableItem createItem(final String value) {
        TableItem item = new TableItem(this.table, SWT.NONE);
        item.setText(new String[] { value });
        return item;
    }
}
