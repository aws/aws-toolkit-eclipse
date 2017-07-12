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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;

public class CreateTableSecondPage extends WizardPage {

    private final List<LocalSecondaryIndex> localSecondaryIndexes;
    private final List<AttributeDefinition> localSecondaryIndexKeyAttributes;
    private IndexTable<LocalSecondaryIndex> localSecondaryIndexesTable;
    private final int MAX_NUM_LSI = 5;
    
    private final List<GlobalSecondaryIndex> globalSecondaryIndexes;
    private final List<List<AttributeDefinition>> globalSecondaryIndexKeyAttributes;
    private IndexTable<GlobalSecondaryIndex> globalSecondaryIndexesTable;
    private final int MAX_NUM_GSI = 5;
    
    private CreateTableWizard wizard;
    private final String OK_MESSAGE = "Add one or more Local/Global Secondary Indexes(optional)";

    CreateTableSecondPage(CreateTableWizard wizard) {
        super("Configure table");
        setMessage(OK_MESSAGE);
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        
        localSecondaryIndexes = new LinkedList<>();
        wizard.getDataModel().setLocalSecondaryIndexes(localSecondaryIndexes);
        
        localSecondaryIndexKeyAttributes = new LinkedList<>();
        
        globalSecondaryIndexes = new LinkedList<>();
        wizard.getDataModel().setGlobalSecondaryIndexes(globalSecondaryIndexes);
        
        globalSecondaryIndexKeyAttributes = new LinkedList<>();
        
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(comp);
        createLinkSection(comp);
        
        // LSI
        Group lsiGroup = CreateTablePageUtil.newGroup(comp, "Local Secondary Index", 1);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(lsiGroup);

        Button addLSIButton = new Button(lsiGroup, SWT.PUSH);
        addLSIButton.setText("Add Local Secondary Index");
        addLSIButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addLSIButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (localSecondaryIndexes.size() >= MAX_NUM_LSI) {
                    new MessageDialog(getShell(), "Validation Error", null, "A table cannot have more than five local secondary indexes.", MessageDialog.ERROR, new String[] { "OK", "CANCEL" }, 0).open();
                    return;

                }
                AddLSIDialog addLSIDialog = new AddLSIDialog(Display.getCurrent().getActiveShell(), wizard.getDataModel());
                if (addLSIDialog.open() == 0) {
                    // Add the LSI schema object
                    localSecondaryIndexes.add(addLSIDialog.getLocalSecondaryIndex());
                    // Add the key attribute definitions associated with this LSI
                    localSecondaryIndexKeyAttributes.add(addLSIDialog.getIndexRangeKeyAttributeDefinition());
                    wizard.collectAllAttribtueDefinitions();

                    localSecondaryIndexesTable.refresh();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        // LSI label provider
        IndexTable.IndexTableLabelProvider localSecondaryIndexTableLabelProvider = new IndexTable.IndexTableLabelProvider() {

            @Override
            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof LocalSecondaryIndex == false)
                    return "";
                LocalSecondaryIndex index = (LocalSecondaryIndex) element;
                switch (columnIndex) {
                case 0: // index name
                    return index.getIndexName();
                case 1: // index range key
                    String returnString = "";
                    returnString += index.getKeySchema().get(1).getAttributeName() + " (";
                    returnString += findAttributeType(index.getKeySchema().get(1).getAttributeName()) + ")";
                    return returnString;
                case 2: // index projection
                    return IndexTable.getProjectionAttributes(
                            index.getProjection(),
                            index.getKeySchema(),
                            wizard.getDataModel());
                }
                return element.toString();
            }
            
        };
        localSecondaryIndexesTable = new IndexTable<LocalSecondaryIndex>(
                lsiGroup,
                localSecondaryIndexes,
                localSecondaryIndexTableLabelProvider) {
            
            @Override
            protected void createColumns(TableColumnLayout columnLayout, Table table) {
                createColumn(table, columnLayout, "Index Name");
                createColumn(table, columnLayout, "Attribute To Index");
                createColumn(table, columnLayout, "Projected Attributes");
            }

            @Override
            protected void onRemoveItem(int removed) {
                localSecondaryIndexKeyAttributes.remove(removed);
                wizard.collectAllAttribtueDefinitions();
            }
        };
        GridDataFactory.fillDefaults().grab(true, true).applyTo(localSecondaryIndexesTable);
        
        // GSI
        Group gsiGroup = CreateTablePageUtil.newGroup(comp, "Global Secondary Index", 1);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(gsiGroup);

        Button addGSIButton = new Button(gsiGroup, SWT.PUSH);
        addGSIButton.setText("Add Global Secondary Index");
        addGSIButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addGSIButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (globalSecondaryIndexes.size() >= MAX_NUM_GSI) {
                    new MessageDialog(getShell(), "Validation Error", null, "A table cannot have more than five global secondary indexes.", MessageDialog.ERROR, new String[] { "OK", "CANCEL" }, 0).open();
                    return;

                }
                AddGSIDialog addGSIDialog = new AddGSIDialog(Display.getCurrent().getActiveShell(), wizard.getDataModel());
                if (addGSIDialog.open() == 0) {
                    globalSecondaryIndexes.add(addGSIDialog.getGlobalSecondaryIndex());
                    globalSecondaryIndexKeyAttributes.add(addGSIDialog.getIndexKeyAttributeDefinitions());
                    wizard.collectAllAttribtueDefinitions();

                    globalSecondaryIndexesTable.refresh();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        // GSI label provider
        IndexTable.IndexTableLabelProvider globalSecondaryIndexTableLabelProvider = new IndexTable.IndexTableLabelProvider() {

            @Override
            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof GlobalSecondaryIndex == false)
                    return "";
                GlobalSecondaryIndex index = (GlobalSecondaryIndex) element;
                switch (columnIndex) {
                case 0: // index name
                    return index.getIndexName();
                case 1: // index hash
                    String returnString = "";
                    returnString += index.getKeySchema().get(0).getAttributeName() + " (";
                    returnString += findAttributeType(index.getKeySchema().get(0).getAttributeName()) + ")";
                    return returnString;
                case 2: // index range
                    returnString = "";
                    if (index.getKeySchema().size() > 1) {
                        returnString += index.getKeySchema().get(1).getAttributeName() + " (";
                        returnString += findAttributeType(index.getKeySchema().get(1).getAttributeName()) + ")";
                    }
                    return returnString;
                case 3: // index projection
                    return IndexTable.getProjectionAttributes(
                            index.getProjection(),
                            index.getKeySchema(),
                            wizard.getDataModel());
                case 4: // index throughput
                    return "Read : " + index.getProvisionedThroughput().getReadCapacityUnits() 
                            + ", Write : " + index.getProvisionedThroughput().getWriteCapacityUnits();
                default:
                    return "";
                }
            }
            
        };
        globalSecondaryIndexesTable = new IndexTable<GlobalSecondaryIndex>(
                gsiGroup,
                globalSecondaryIndexes,
                globalSecondaryIndexTableLabelProvider) {
            
            @Override
            protected void createColumns(TableColumnLayout columnLayout, Table table) {
                createColumn(table, columnLayout, "Index Name");
                createColumn(table, columnLayout, "Index Hash Key");
                createColumn(table, columnLayout, "Index Range Key");
                createColumn(table, columnLayout, "Projected Attributes");
                createColumn(table, columnLayout, "Provisioned Throughput");
            }

            @Override
            protected void onRemoveItem(int removed) {
                globalSecondaryIndexKeyAttributes.remove(removed);
                wizard.collectAllAttribtueDefinitions();
            }
        };
        GridDataFactory.fillDefaults().grab(true, true).applyTo(globalSecondaryIndexesTable);
        
        
        setControl(comp);
    }

    public void createLinkSection(Composite parent) {
        String ConceptUrl1 = "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LSI.html";
        String ConceptUrl2 = "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html";
        Link link = new Link(parent, SWT.NONE | SWT.WRAP);
        link.setText("You can only define local/global secondary indexes at table creation time, and cannot remove them or modify the index key schema later. "
                + "Local/global secondary indexes are not appropriate for every application; see " + "<a href=\"" + ConceptUrl1 + "\">Local Secondary Indexes</a> and <a href=\"" + ConceptUrl2
                + "\">Global Secondary Indexes</a>.\n");

        link.addListener(SWT.Selection, new WebLinkListener());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
    }

    public Set<AttributeDefinition> getAllIndexKeyAttributeDefinitions() {
        Set<AttributeDefinition> allAttrs = new HashSet<>();
        
        for (AttributeDefinition lsiKey : localSecondaryIndexKeyAttributes) {
            allAttrs.add(lsiKey);
        }
        for (List<AttributeDefinition> gsiKey : globalSecondaryIndexKeyAttributes) {
            allAttrs.addAll(gsiKey);
        }
        
        return allAttrs;
    }

    private String findAttributeType(String attributeName) {
        for (AttributeDefinition attribute : wizard.getDataModel().getAttributeDefinitions()) {
            if (attribute.getAttributeName().equals(attributeName)) {
                return attribute.getAttributeType();
            }
        }
        return "";
    }
    
    /** Abstract base class that could be customized for LSI/GSI */
    private static abstract class IndexTable <T> extends Composite {

        private final List<T> indexes;
        private final TableViewer viewer;

        IndexTable(Composite parent, 
                   final List<T> indexes,
                   final IndexTableLabelProvider labelProvider) {
            super(parent, SWT.NONE);
            this.indexes = indexes;
            
            TableColumnLayout tableColumnLayout = new TableColumnLayout();
            this.setLayout(tableColumnLayout);

            viewer = new TableViewer(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            viewer.getTable().setLinesVisible(true);
            viewer.getTable().setHeaderVisible(true);
            viewer.setLabelProvider(labelProvider);
            viewer.setContentProvider(new IndexTableContentProvider());
            createColumns(tableColumnLayout, viewer.getTable());

            MenuManager menuManager = new MenuManager("#PopupMenu");
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener() {

                @Override
                public void menuAboutToShow(IMenuManager manager) {
                    if (viewer.getTable().getSelectionCount() > 0) {

                        manager.add(new Action() {

                            @Override
                            public ImageDescriptor getImageDescriptor() {
                                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                            }

                            @Override
                            public void run() {
                                int selected = viewer.getTable().getSelectionIndex();
                                IndexTable.this.indexes.remove(selected);
                                IndexTable.this.onRemoveItem(selected);
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

        protected abstract void createColumns(TableColumnLayout columnLayout, Table table);

        /**
         * Callback for any additional operations to be executed after an item
         * in the table is removed.
         */
        protected abstract void onRemoveItem(int removed);

        protected TableColumn createColumn(Table table, TableColumnLayout columnLayout, String text) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(text);
            column.setMoveable(true);
            columnLayout.setColumnData(column, new ColumnPixelData(150));
            return column;
        }

        protected static abstract class IndexTableLabelProvider implements ITableLabelProvider {
            @Override
            public void addListener(ILabelProviderListener listener) {
            }

            @Override
            public void removeListener(ILabelProviderListener listener) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public boolean isLabelProperty(Object element, String property) {
                return false;
            }

            @Override
            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }

            @Override
            public abstract String getColumnText(Object element, int columnIndex);
        }
        
        /** Shared by LSI and GSI table */
        private final class IndexTableContentProvider extends ArrayContentProvider {

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return indexes.toArray();
            }
        }

        /** Enforce call getElement method in contentProvider */
        private void refresh() {
            viewer.setInput(new Object());
        }
        
        /** Generate a String has the detail info about the project attribute. */
        private static String getProjectionAttributes(Projection indexProjection, List<KeySchemaElement> indexKeys, CreateTableDataModel dataModel) {
            String returnString = "";
            String DELIM = ", ";
            if (indexProjection.getProjectionType().equals("All Attributes")) {
                return indexProjection.getProjectionType();
            } else if (indexProjection.getProjectionType().equals("Specify Attributes")) {
                return CreateTablePageUtil.stringJoin(indexProjection.getNonKeyAttributes(), DELIM);
            } else {
                // Primary keys
                Set<String> projectedKeys = new HashSet<>();
                returnString += dataModel.getHashKeyName() + DELIM;
                projectedKeys.add(dataModel.getHashKeyName());
                if (dataModel.getEnableRangeKey()) {
                    returnString += dataModel.getRangeKeyName() + DELIM;
                    projectedKeys.add(dataModel.getRangeKeyName());
                }
                
                // Index keys (we should not include index keys that are already part of the primary key)
                for (KeySchemaElement indexKey : indexKeys) {
                    String indexKeyName = indexKey.getAttributeName();
                    if ( !projectedKeys.contains(indexKeyName) ) {
                        returnString += indexKeyName + DELIM;
                        projectedKeys.add(indexKeyName);
                    }
                }
                
                return returnString.substring(0, returnString.length() - DELIM.length());
            }
        }
    }

}
