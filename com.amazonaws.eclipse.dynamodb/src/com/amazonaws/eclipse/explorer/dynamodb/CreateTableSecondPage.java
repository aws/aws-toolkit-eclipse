/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.dynamodb;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;

public class CreateTableSecondPage extends WizardPage {

    private List<LocalSecondaryIndex> localSecondaryIndices;
    private IndexTable indexTable;
    private final int MAX_NUM_LSI = 5;
    private CreateTableWizard wizard;
    private final String OK_MESSAGE = "Add one or more Local Secondary Indexes(optional)";

    CreateTableSecondPage(CreateTableWizard wizard) {
        super("Configure table");
        setMessage(OK_MESSAGE);
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        wizard.getDataModel().setLocalSecondaryIndices(new LinkedList<LocalSecondaryIndex>());
        localSecondaryIndices = wizard.getDataModel().getLocalSecondaryIndices();
        this.wizard = wizard;
    }

    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        comp.setLayout(new GridLayout(1, false));
        createLinkSection(comp);
        Button addIndexButton = new Button(comp, SWT.PUSH);
        addIndexButton.setText("Add Index");
        addIndexButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addIndexButton.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                if (localSecondaryIndices.size() >= MAX_NUM_LSI) {
                    new MessageDialog(getShell(), "Validation Error", null, "A table cannot have more than five indexes", MessageDialog.ERROR, new String[] { "OK", "CANCEL" }, 0).open();
                    return;

                }
                AddLSIDialog addLSIDialog = new AddLSIDialog(Display.getCurrent().getActiveShell(), wizard.getDataModel());
                if (addLSIDialog.open() == 0) {
                    localSecondaryIndices.add(addLSIDialog.getLocalSecondaryIndex());
                    AttributeDefinition indexRangeKeyttributeDefinition = addLSIDialog.getIndexRangeKeyAttributeDefinition();
                    // Only add to attribute definition to the data model if the
                    // index is not on the primary range key.
                    if (indexRangeKeyttributeDefinition != null) {
                        wizard.getDataModel().getAttributeDefinitions().add(indexRangeKeyttributeDefinition);
                    }
                    indexTable.refresh();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        indexTable = new IndexTable(comp);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(indexTable);
        setControl(comp);
    }

    public void createLinkSection(Composite parent) {
        String ConceptUrl1 = "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LSI.html";
        String ConceptUrl2 = "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LSI.html#LSI.ItemCollections";
        Link link = new Link(parent, SWT.NONE | SWT.WRAP);
        link.setText("You can only define local secondary indexes at table creation time, and cannot remove or modify them later. "
                + "Local secondary indexes are not appropriate for every application; see " + "<a href=\"" + ConceptUrl1 + "\">Secondary Indexes</a> and <a href=\"" + ConceptUrl2
                + "\">Item Collections</a>.\n");

        link.addListener(SWT.Selection, new WebLinkListener());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
    }

    private String findAttributeType(String attributeName) {
        for (AttributeDefinition attribute : wizard.getDataModel().getAttributeDefinitions()) {
            if (attribute.getAttributeName().equals(attributeName)) {
                return attribute.getAttributeType();
            }
        }
        // Primary range key is not added to the attribute definition
        // before finalizing the CreateTable request.
        if (attributeName.equals(wizard.getDataModel().getRangeKeyName())) {
            return wizard.getDataModel().getRangeKeyType();
        }
        return "";
    }

    // The table to show the local secondary index info
    private class IndexTable extends Composite {

        private TableViewer viewer;
        private IndexTableContentProvider contentProvider;
        private IndexTableLabelProvider labelProvider;

        IndexTable(Composite parent) {
            super(parent, SWT.NONE);

            TableColumnLayout tableColumnLayout = new TableColumnLayout();
            this.setLayout(tableColumnLayout);

            contentProvider = new IndexTableContentProvider();
            labelProvider = new IndexTableLabelProvider();

            viewer = new TableViewer(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            viewer.getTable().setLinesVisible(true);
            viewer.getTable().setHeaderVisible(true);
            viewer.setLabelProvider(labelProvider);
            viewer.setContentProvider(contentProvider);
            createColumns(tableColumnLayout, viewer.getTable());

            MenuManager menuManager = new MenuManager("#PopupMenu");
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener() {

                public void menuAboutToShow(IMenuManager manager) {
                    if (viewer.getTable().getSelectionCount() > 0) {

                        manager.add(new Action() {

                            @Override
                            public ImageDescriptor getImageDescriptor() {
                                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                            }

                            @Override
                            public void run() {
                                localSecondaryIndices.remove(viewer.getTable().getSelectionIndex());
                                refresh();
                            }

                            @Override
                            public String getText() {
                                return "Delete Index";
                            }

                        });
                    }
                }
            });
            viewer.getTable().setMenu(menuManager.createContextMenu(viewer.getTable()));
        }

        // Enforce call getElement method in contentProvider
        public void refresh() {
            viewer.setInput(new Object());
        }

        protected final class IndexTableContentProvider extends ArrayContentProvider {

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return localSecondaryIndices.toArray();
            }
        }

        protected final class IndexTableLabelProvider implements ITableLabelProvider {
            public void addListener(ILabelProviderListener listener) {
            }

            public void removeListener(ILabelProviderListener listener) {
            }

            public void dispose() {
            }

            public boolean isLabelProperty(Object element, String property) {
                return false;
            }

            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }

            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof LocalSecondaryIndex == false)
                    return "";
                LocalSecondaryIndex index = (LocalSecondaryIndex) element;
                switch (columnIndex) {
                case 0:
                    return index.getIndexName();
                case 1:
                    String returnString = "";
                    returnString += index.getKeySchema().get(1).getAttributeName() + " (";
                    returnString += findAttributeType(index.getKeySchema().get(1).getAttributeName()) + ")";
                    return returnString;

                case 2:
                    return getProjectionAttributes(index);
                }
                return element.toString();
            }
        }

        // Generate a String has the detail info about projection in this LSI
        private String getProjectionAttributes(LocalSecondaryIndex index) {
            String returnString = "";
            if (index.getProjection().getProjectionType().equals("All Attributes")) {
                return index.getProjection().getProjectionType();
            } else if (index.getProjection().getProjectionType().equals("Specify Attributes")) {
                for (String attribute : index.getProjection().getNonKeyAttributes()) {
                    returnString += attribute + ", ";
                }
                returnString = returnString.substring(0, returnString.length() - 2);
                return returnString;
            } else {
                returnString += wizard.getDataModel().getHashKeyName() + ", ";
                if (wizard.getDataModel().getEnableRangeKey()) {
                    returnString += wizard.getDataModel().getRangeKeyName() + ", ";
                }
                returnString += index.getKeySchema().get(1).getAttributeName();
                return returnString;

            }
        }

        private void createColumns(TableColumnLayout columnLayout, Table table) {
            createColumn(table, columnLayout, "Index Name");
            createColumn(table, columnLayout, "Attribute To Index");
            createColumn(table, columnLayout, "Projected Attributes");
        }

        private TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(text);
            column.setMoveable(true);
            columnLayout.setColumnData(column, new ColumnPixelData(150));
            return column;
        }
    }

}
