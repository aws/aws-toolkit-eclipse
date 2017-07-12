/*
 * Copyright 2012 Amazon Technologies, Inc.
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;

/**
 * Dialog to show the table properties.
 */
public class TablePropertiesDialog extends MessageDialog {

    private final String tableName;
    private final TableDescription tableDescription;

    private Text writeCapacityText;
    private Text readCapacityText;
    private Long readCapacity;
    private Long writeCapacity;

    protected TablePropertiesDialog(String tableName) {
        super(Display.getCurrent().getActiveShell(), "Table properties for " + tableName, AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), null,
                MessageDialog.NONE, new String[] { "Update", "Cancel" }, 1);
        this.tableName = tableName;
        tableDescription = AwsToolkitCore.getClientFactory().getDynamoDBV2Client()
                .describeTable(new DescribeTableRequest().withTableName(tableName)).getTable();
        readCapacity = tableDescription.getProvisionedThroughput().getReadCapacityUnits();
        writeCapacity = tableDescription.getProvisionedThroughput().getWriteCapacityUnits();
        setShellStyle(getShellStyle() | SWT.RESIZE);

    }

    public UpdateTableRequest getUpdateRequest() {
        return new UpdateTableRequest().withTableName(tableName).withProvisionedThroughput(
                new ProvisionedThroughput().withReadCapacityUnits(readCapacity).withWriteCapacityUnits(writeCapacity));
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);

        newLabel(comp).setText("Created on:");
        newReadOnlyTextField(comp).setText(tableDescription.getCreationDateTime().toString());

        newLabel(comp).setText("Status:");
        newReadOnlyTextField(comp).setText(tableDescription.getTableStatus());

        newLabel(comp).setText("Item count:");
        newReadOnlyTextField(comp).setText(tableDescription.getItemCount().toString());

        newLabel(comp).setText("Size (bytes):");
        newReadOnlyTextField(comp).setText(tableDescription.getTableSizeBytes().toString());

        newLabel(comp).setText("Hash key attribute:");
        newReadOnlyTextField(comp).setText(getHashKeyName());

        newLabel(comp).setText("Hash key type:");
        newReadOnlyTextField(comp).setText(getAttributeType(getHashKeyName()));

        if ( getRangeKeyName() != null ) {
            new Label(comp, SWT.READ_ONLY).setText("Range key attribute:");
            newReadOnlyTextField(comp).setText(getRangeKeyName());

            new Label(comp, SWT.READ_ONLY).setText("Range key type:");
            newReadOnlyTextField(comp).setText(getAttributeType(getRangeKeyName()));
        }

        new Label(comp, SWT.READ_ONLY).setText("Read capacity units:");
        readCapacityText = newTextField(comp);
        readCapacityText.setText(readCapacity.toString());
        readCapacityText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                try {
                    readCapacity = Long.parseLong(readCapacityText.getText());
                } catch ( NumberFormatException e1 ) {
                    readCapacity = null;
                }
                validate();
            }
        });

        new Label(comp, SWT.READ_ONLY).setText("Write capacity units:");
        writeCapacityText = newTextField(comp);
        writeCapacityText.setText(writeCapacity.toString());
        writeCapacityText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                try {
                    writeCapacity = Long.parseLong(writeCapacityText.getText());
                } catch ( NumberFormatException e1 ) {
                    writeCapacity = null;
                }
                validate();
            }
        });

        if ( tableDescription.getProvisionedThroughput().getLastIncreaseDateTime() != null ) {
            new Label(comp, SWT.READ_ONLY).setText("Provisioned throughput last increased:");
            newReadOnlyTextField(comp).setText(
                    tableDescription.getProvisionedThroughput().getLastIncreaseDateTime().toString());
        }

        if ( tableDescription.getProvisionedThroughput().getLastDecreaseDateTime() != null ) {
            new Label(comp, SWT.READ_ONLY).setText("Provisioned throughput last decreased:");
            newReadOnlyTextField(comp).setText(
                    tableDescription.getProvisionedThroughput().getLastDecreaseDateTime().toString());
        }

        // Local secondary index
        Group group = new Group(comp, SWT.NONE);
        group.setText("Local Secondary Index");
        group.setLayout(new GridLayout(1, false));
        GridDataFactory.fillDefaults().grab(true, true).span(2, SWT.DEFAULT).applyTo(group);
        IndexTable table = new IndexTable(group);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
        table.refresh();

        return comp;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        validate();
    }

    private Text newTextField(Composite comp) {
        Text text = new Text(comp, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
        return text;
    }

    private Text newReadOnlyTextField(Composite comp) {
        Text text = new Text(comp, SWT.READ_ONLY);
        text.setBackground(comp.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
        return text;
    }

    private Label newLabel(Composite comp) {
         Label label = new Label(comp, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
         return label;
    }

    private void validate() {
        if ( readCapacity == null || readCapacity < 5 ) {
            setErrorMessage("Please enter a read capacity of 5 or more.");
            return;
        }

        if ( writeCapacity == null || writeCapacity < 5 ) {
            setErrorMessage("Please enter a write capacity of 5 or more.");
            return;
        }

        setErrorMessage(null);
    }

    private void setErrorMessage(String message) {
        getButton(0).setEnabled(message == null);
    }

    private String getHashKeyName() {
        for (KeySchemaElement element : tableDescription.getKeySchema()) {
            if (element.getKeyType().equals(KeyType.HASH.toString())) {
                return element.getAttributeName();
            }
        }
        return null;
    }

    private String getRangeKeyName() {
        for (KeySchemaElement element : tableDescription.getKeySchema()) {
            if (element.getKeyType().equals(KeyType.RANGE.toString())) {
                return element.getAttributeName();
            }
        }
        return null;
    }

    private String getAttributeType(String attributeName) {
        for (AttributeDefinition definition : tableDescription.getAttributeDefinitions()) {
            if (definition.getAttributeName().equals(attributeName)) {
                return definition.getAttributeType();
            }
        }
        return null;
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
                if (tableDescription == null || tableDescription.getLocalSecondaryIndexes() == null) {
                    return new LocalSecondaryIndexDescription[0];
                }
                return tableDescription.getLocalSecondaryIndexes().toArray();
            }
        }

        protected final class IndexTableLabelProvider implements ITableLabelProvider {
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
            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof LocalSecondaryIndexDescription == false)
                    return "";
                LocalSecondaryIndexDescription index = (LocalSecondaryIndexDescription) element;
                switch (columnIndex) {
                case 0:
                    return index.getIndexName();
                case 1:
                    String returnString = "";
                    returnString += index.getKeySchema().get(1).getAttributeName() + " (";
                    returnString += getAttributeType(index.getKeySchema().get(1).getAttributeName()) + ")";
                    return returnString;
                case 2:
                    return index.getIndexSizeBytes().toString();
                case 3:
                    return index.getItemCount().toString();
                case 4:
                    return getProjectionAttributes(index);
                }
                return element.toString();
            }
        }

        // Generate a String has the detail info about projection in this LSI
        private String getProjectionAttributes(LocalSecondaryIndexDescription index) {
            String returnString = "";
            if (index.getProjection().getProjectionType().equals(ProjectionType.ALL.toString())) {
                return index.getProjection().getProjectionType();
            } else if (index.getProjection().getProjectionType().equals(ProjectionType.INCLUDE.toString())) {
                for (String attribute : index.getProjection().getNonKeyAttributes()) {
                    returnString += attribute + ", ";
                }
                returnString = returnString.substring(0, returnString.length() - 2);
                return returnString;
            } else {
                returnString += getHashKeyName() + ", ";
                if (getRangeKeyName() != null) {
                    returnString += getRangeKeyName() + ", ";
                }
                returnString += index.getKeySchema().get(1).getAttributeName();
                return returnString;

            }
        }

        private void createColumns(TableColumnLayout columnLayout, Table table) {
            createColumn(table, columnLayout, "Index Name");
            createColumn(table, columnLayout, "Attribute To Index");
            createColumn(table, columnLayout, "Index Size (Bytes)");
            createColumn(table, columnLayout, "Item Count");
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
