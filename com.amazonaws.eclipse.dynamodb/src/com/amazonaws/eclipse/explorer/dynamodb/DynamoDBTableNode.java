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
package com.amazonaws.eclipse.explorer.dynamodb;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.editor.OpenTableEditorAction;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

public class DynamoDBTableNode extends ExplorerNode {

    private final String tableName;
    private TableStatus tableStatus;

    public String getTableName() {
        return tableName;
    }
    
    public TableStatus getTableStatus() {
        return tableStatus;
    }
    
    /**
     * Sets the status of the table that this node represents, and changes to
     * the corresponding open action.
     */
    public void setTableStatus(final TableStatus tableStatus) {
        this.tableStatus = tableStatus;
        if ( tableStatus == null ) {
            setOpenAction(new Action() {
                
                @Override
                public void run() {
                    /*
                     * Update the table status immediately when the node is
                     * being opened, but has not been set with table status.
                     */
                    AmazonDynamoDB dynamoDBClient = AwsToolkitCore.getClientFactory().getDynamoDBV2Client();
                    boolean describeTableError = false;
                    TableStatus updatedStatus = null;
                    try {
                        updatedStatus = TableStatus.valueOf(dynamoDBClient
                                .describeTable(
                                        new DescribeTableRequest()
                                                .withTableName(tableName)).getTable()
                                .getTableStatus());
                    } catch ( AmazonServiceException ase ) {
                        if (ase.getErrorCode().equalsIgnoreCase(
                                "ResourceNotFoundException") == true) {
                            /* Show warning that the table has already been deleted */
                            MessageDialog dialog = new MessageDialog(
                                    Display.getCurrent().getActiveShell(),
                                    "Cannot open this table",
                                    AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                                    "Table has been deleted.", 
                                    MessageDialog.ERROR,
                                    new String[] { "OK" }, 0);
                            dialog.open();
                            
                            /*
                             * We need to explicitly refresh the tree view if a
                             * table node has already been deleted in DynamoDB
                             */
                            DynamoDBContentProvider.getInstance().refresh();
                            return;
                        } else {
                            describeTableError = true;
                        }
                    } catch ( IllegalArgumentException iae ) {
                        /* Unrecognized table status */
                        describeTableError = true;
                    }
                    
                    if ( describeTableError ) {
                        /*
                         * Still allow the user to open the table editor if we
                         * cannot get the table status now. (But the background
                         * job will still keep trying to update the table
                         * status).
                         */
                        setOpenAction(new OpenTableEditorAction(tableName));
                        return;
                    }
                    
                    /* assert: updatedStatus != null */
                    setTableStatus(updatedStatus);
                    DynamoDBTableNode.this.getOpenAction().run();
                }
            });
        } else if ( tableStatus == TableStatus.ACTIVE ) {
            /*
             * Open the table editor only when the node is in ACTIVE status.
             */
            setOpenAction(new OpenTableEditorAction(tableName));
        } else {
            /*
             * For CREATING/DELETING/UPDATING, suppress opening the table editor.
             * Show a warning on the table status instead.
             */
            setOpenAction(new Action() {
                
                @Override
                public void run() {
                    /* Show the warning that the table is CREATING/DELETING/UPDATING */
                    MessageDialog dialog = new MessageDialog(
                            Display.getCurrent().getActiveShell(),
                            "Cannot open this table",
                            AwsToolkitCore.getDefault()
                                    .getImageRegistry()
                                    .get(AwsToolkitCore.IMAGE_AWS_ICON),
                            "Cannot open this table(" + tableName + "), since it is in the status of " + tableStatus + ".",
                            MessageDialog.ERROR,
                            new String[] { "OK" }, 0);
                    dialog.open();
                }
            });
        }
    }
    
    public DynamoDBTableNode(String tableName) {
        this(tableName, null);
    }
            
    public DynamoDBTableNode(String tableName, TableStatus tableStatus) {
        super(tableName, 0, DynamoDBPlugin.getDefault().getImageRegistry()
                .get(DynamoDBPlugin.IMAGE_TABLE), null);
        this.tableName = tableName;
        setTableStatus(tableStatus);
    }

}
