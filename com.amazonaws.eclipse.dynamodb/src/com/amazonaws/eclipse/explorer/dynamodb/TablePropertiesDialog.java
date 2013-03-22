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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;

/**
 * Wizard to create a new DynamoDB table.
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
        tableDescription = AwsToolkitCore.getClientFactory().getDynamoDBClient()
                .describeTable(new DescribeTableRequest().withTableName(tableName)).getTable();
        readCapacity = tableDescription.getProvisionedThroughput().getReadCapacityUnits();
        writeCapacity = tableDescription.getProvisionedThroughput().getWriteCapacityUnits();
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

        new Label(comp, SWT.READ_ONLY).setText("Created on:");
        newReadOnlyTextField(comp).setText(tableDescription.getCreationDateTime().toString());

        new Label(comp, SWT.READ_ONLY).setText("Status:");
        newReadOnlyTextField(comp).setText(tableDescription.getTableStatus());

        new Label(comp, SWT.READ_ONLY).setText("Item count:");
        newReadOnlyTextField(comp).setText(tableDescription.getItemCount().toString());

        new Label(comp, SWT.READ_ONLY).setText("Size (bytes):");
        newReadOnlyTextField(comp).setText(tableDescription.getTableSizeBytes().toString());

        new Label(comp, SWT.READ_ONLY).setText("Hash key attribute:");
        newReadOnlyTextField(comp).setText(tableDescription.getKeySchema().getHashKeyElement().getAttributeName());

        new Label(comp, SWT.READ_ONLY).setText("Hash key type:");
        newReadOnlyTextField(comp).setText(tableDescription.getKeySchema().getHashKeyElement().getAttributeType());

        if ( tableDescription.getKeySchema().getRangeKeyElement() != null ) {
            new Label(comp, SWT.READ_ONLY).setText("Range key attribute:");
            newReadOnlyTextField(comp).setText(tableDescription.getKeySchema().getRangeKeyElement().getAttributeName());

            new Label(comp, SWT.READ_ONLY).setText("Range key type:");
            newReadOnlyTextField(comp).setText(tableDescription.getKeySchema().getRangeKeyElement().getAttributeType());
        }

        new Label(comp, SWT.READ_ONLY).setText("Read capacity units:");
        readCapacityText = newTextField(comp);
        readCapacityText.setText(readCapacity.toString());
        readCapacityText.addModifyListener(new ModifyListener() {
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

}
