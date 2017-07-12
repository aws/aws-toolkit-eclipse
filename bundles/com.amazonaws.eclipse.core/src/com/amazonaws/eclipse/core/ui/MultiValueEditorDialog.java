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
package com.amazonaws.eclipse.core.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;


/**
 * Simple table dialog to allow use user to enter multiple values for an
 * attribute.
 */
public class MultiValueEditorDialog extends MessageDialog {

    private static final String NEW_VALUE = "<new value>";
    private boolean editLocked = false;
    private int lockedRowIndex = -1;
    protected final List<String> values = new ArrayList<>();
    protected String columnText = "Attributes";

    public List<String> getValues() {
        return this.values;
    }

    protected TableViewer tableViewer;

    /**
     * Default constructor provides "OK" and "Cancel" buttons with an AWS logo.
     */
    public MultiValueEditorDialog(final Shell parentShell) {
        super(parentShell, "Edit values", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "", MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0);
    }

    /**
     * Full featured constructor from {@link MessageDialog}
     */
    public MultiValueEditorDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
            int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
        super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {

        Composite composite = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(composite);

        TableColumnLayout layout = new TableColumnLayout();
        composite.setLayout(layout);

        this.tableViewer = new TableViewer(composite);
        this.tableViewer.getTable().setHeaderVisible(true);
        TableColumn tableColumn = new TableColumn(this.tableViewer.getTable(), SWT.NONE);
        tableColumn.setText(columnText);
        layout.setColumnData(tableColumn, new ColumnWeightData(100));

        this.tableViewer.setContentProvider(new AbstractTableContentProvider() {

            @Override
            public Object[] getElements(final Object inputElement) {
                Object[] rowsPlusNew = new Object[MultiValueEditorDialog.this.values.size() + 1];
                MultiValueEditorDialog.this.values.toArray(rowsPlusNew);
                rowsPlusNew[rowsPlusNew.length - 1] = NEW_VALUE;
                return rowsPlusNew;
            }
        });

        this.tableViewer.setLabelProvider(new AbstractTableLabelProvider() {

            @Override
            public String getColumnText(final Object element, final int columnIndex) {
                return (String) element;
            }
        });

        final Table table = this.tableViewer.getTable();
        final TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        table.addListener(SWT.MouseUp, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                Rectangle clientArea = table.getClientArea();
                Point pt = new Point(event.x, event.y);
                int index = table.getTopIndex();
                while ( index < table.getItemCount() ) {
                    boolean visible = false;
                    final TableItem item = table.getItem(index);

                    // Only one column, but loop is here for completeness
                    for ( int i = 0; i < table.getColumnCount(); i++ ) {
                        Rectangle rect = item.getBounds(i);
                        if ( rect.contains(pt) ) {
                            final int column = i;
                            final Text text = new Text(table, SWT.NONE);
                            final int idx = index;
                            if ( isRowUneditable(idx) ) {
                                return;
                            }
                            Listener textListener = new Listener() {

                                @Override
                                public void handleEvent(final Event e) {
                                    if ( e.type == SWT.Traverse && e.detail == SWT.TRAVERSE_ESCAPE ) {
                                        /* Skip data validation and dispose the text editor */
                                        text.dispose();
                                        e.doit = false;
                                        return;
                                    } else if ( e.type == SWT.Traverse && e.detail != SWT.TRAVERSE_RETURN ) {
                                        /* No-op for keys other than escape or return. */
                                        return;
                                    } else {
                                        /* For all other events, we first validate the data */
                                        if ( !validateAttributeValue(text.getText()) ) {
                                            lockTableEditor(idx);
                                            return;
                                        }
                                        /* First unlock everything */
                                        unlockTableEditor();
                                        /* Then we handle different events */
                                        if ( e.type == SWT.FocusOut  ) {
                                            modifyValue(item, column, idx, text);
                                            text.dispose();
                                        } else if ( e.type == SWT.Traverse && e.detail ==  SWT.TRAVERSE_RETURN ) {
                                            modifyValue(item, column, idx, text);
                                        } else if ( e.type == SWT.Modify ) {
                                            /* No-op */
                                        }
                                    }
                                }
                            };

                            text.addListener(SWT.FocusOut, textListener);
                            text.addListener(SWT.Traverse, textListener);
                            text.addListener(SWT.Modify, textListener);
                            editor.setEditor(text, item, i);
                            text.setText(item.getText(i));

                            text.selectAll();
                            text.setFocus();
                            return;
                        }
                        if ( !visible && rect.intersects(clientArea) ) {
                            visible = true;
                        }
                    }
                    if ( !visible ) {
                        return;
                    }
                    index++;
                }
            }
        });
        
        /* Suppress changing to other table rows when the editor is locked. */
        this.tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            private boolean update = true;
            private ISelection lastSelection;
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if ( update && isLocked() ) {
                    update = false; // avoid infinite loop
                    tableViewer.setSelection(lastSelection);
                    update = true;
                } else if ( !isLocked() ) {
                    lastSelection = event.getSelection();
                }
            }

        });

        this.tableViewer.setInput(values.size());
        this.tableViewer.getTable().getItem(this.values.size())
                .setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));

        return composite;
    }

    /**
     * Called when a value in the list is modified.
     */
    protected void modifyValue(final TableItem item, final int column, final int index, final Text text) {
        String newValue = text.getText();
        if ( newValue.length() == 0 ) {
            if ( index < this.values.size() ) {
                this.values.remove(index);
            }
            this.tableViewer.refresh();
        } else {
            item.setText(column, newValue);
            if ( index == item.getParent().getItemCount() - 1 ) {
                this.values.add(newValue);
                this.tableViewer.refresh();
                item.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

                this.tableViewer.getTable().getItem(this.values.size())
                        .setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
            } else {
                this.values.set(index, newValue);
            }
        }
    }
    
    /**
     * Base class always returns true when validating the data.
     */
    protected boolean validateAttributeValue(String attributeValue) {
        return true;
    }
    /**
     * Add a customized suffix for the column text.
     */
    protected void addColumnTextDescription(String columnTextSuffix) {
        this.columnText = this.columnText + " " + columnTextSuffix;
    }
    
    /**
     * @param index
     *            The index of the locked row.
     */
    protected void lockTableEditor(int index) {
        editLocked = true;
        lockedRowIndex = index;
        this.getButton(0).setEnabled(false);
        this.getButtonBar().update(); 
    }
    
    protected void unlockTableEditor() {
        editLocked = false;
        lockedRowIndex = -1;
        this.getButton(0).setEnabled(true);
        this.getButtonBar().update(); 
    }
    
    private boolean isLocked() {
        return editLocked;
    }
    
    private boolean isRowUneditable(int index) {
        return editLocked && index != lockedRowIndex;
    }
}
