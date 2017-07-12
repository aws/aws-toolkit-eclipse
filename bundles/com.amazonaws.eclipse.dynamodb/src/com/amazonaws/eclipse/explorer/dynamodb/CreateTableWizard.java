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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

/**
 * Wizard to create a new DynamoDB table.
 */
class CreateTableWizard extends Wizard {

    // Map the UI text value to that for creating table request
    private Map<String, String> UINameToValueMap;

    private CreateTableFirstPage firstPage;
    private CreateTableSecondPage secondPage;
    private CreateTableDataModel dataModel;

    public CreateTableWizard() {
        init();
        setNeedsProgressMonitor(true);
        setWindowTitle("Create New DynamoDB Table");
        dataModel = new CreateTableDataModel();
    }

    @Override
    public void addPages() {
        firstPage = new CreateTableFirstPage(this);
        addPage(firstPage);
        secondPage = new CreateTableSecondPage(this);
        addPage(secondPage);

    }

    @Override
    public boolean performFinish() {

        final String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        final AmazonDynamoDB dynamoDBClient = AwsToolkitCore.getClientFactory(accountId).getDynamoDBV2Client();
        final CreateTableRequest createTableRequest = generateCreateTableRequest();

        new Job("Creating table") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    dynamoDBClient.createTable(createTableRequest);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, DynamoDBPlugin.getDefault().getPluginId(), "Unable to create the table: " + e.getMessage(), e);
                }
            }
        }.schedule();

        return true;
    }

    public CreateTableDataModel getDataModel() {
        return dataModel;
    }

    /** Clear and then collect all the AttributeDefinitions defined in the primary table and each index */
    public void collectAllAttribtueDefinitions() {
        dataModel.getAttributeDefinitions().clear();

        // Primary keys
        dataModel.getAttributeDefinitions().add(new AttributeDefinition().withAttributeName(dataModel.getHashKeyName()).withAttributeType(dataModel.getHashKeyType()));
        if (dataModel.getEnableRangeKey()) {
            dataModel.getAttributeDefinitions().add(new AttributeDefinition().withAttributeName(dataModel.getRangeKeyName()).withAttributeType(dataModel.getRangeKeyType()));
        }

        // Index keys defined in the second page
        dataModel.getAttributeDefinitions().addAll(secondPage.getAllIndexKeyAttributeDefinitions());
    }

    private CreateTableRequest generateCreateTableRequest() {
        preProcessDataModel();
        CreateTableRequest createTableRequest = new CreateTableRequest();
        createTableRequest.setTableName(dataModel.getTableName());
        ProvisionedThroughput throughput = new ProvisionedThroughput();
        throughput.setReadCapacityUnits(dataModel.getReadCapacity());
        throughput.setWriteCapacityUnits(dataModel.getWriteCapacity());
        createTableRequest.setProvisionedThroughput(throughput);
        List<KeySchemaElement> keySchema = new LinkedList<>();
        KeySchemaElement keySchemaElement = new KeySchemaElement();
        keySchemaElement.setAttributeName(dataModel.getHashKeyName());
        keySchemaElement.setKeyType(KeyType.HASH);
        keySchema.add(keySchemaElement);
        if (dataModel.getEnableRangeKey()) {
            keySchemaElement = new KeySchemaElement();
            keySchemaElement.setAttributeName(dataModel.getRangeKeyName());
            keySchemaElement.setKeyType(KeyType.RANGE);
            keySchema.add(keySchemaElement);
        }

        createTableRequest.setKeySchema(keySchema);
        createTableRequest.setAttributeDefinitions(dataModel.getAttributeDefinitions());
        if ( dataModel.getLocalSecondaryIndexes() != null
                && ( !dataModel.getLocalSecondaryIndexes().isEmpty() ) ) {
            createTableRequest.setLocalSecondaryIndexes(dataModel.getLocalSecondaryIndexes());
        }
        if ( dataModel.getGlobalSecondaryIndexes() != null
                && ( !dataModel.getGlobalSecondaryIndexes().isEmpty() ) ) {
            createTableRequest.setGlobalSecondaryIndexes(dataModel.getGlobalSecondaryIndexes());
        }
        System.out.println(createTableRequest);
        return createTableRequest;
    }

    /**
     * Collect all the attribute definitions from primary table and secondary
     * index, then convert the string value shown in UI to the that for the
     * creating table request.
     */
    private void preProcessDataModel() {
        collectAllAttribtueDefinitions();

        for (AttributeDefinition attribute : dataModel.getAttributeDefinitions()) {
            attribute.setAttributeType(UINameToValueMap.get(attribute.getAttributeType()));
        }

        if (null != dataModel.getLocalSecondaryIndexes()) {
            for (LocalSecondaryIndex index : dataModel.getLocalSecondaryIndexes()) {
                index.getProjection().setProjectionType(UINameToValueMap.get(index.getProjection().getProjectionType()));
            }
        }

        if (null != dataModel.getGlobalSecondaryIndexes()) {
            for (GlobalSecondaryIndex index : dataModel.getGlobalSecondaryIndexes()) {
                index.getProjection().setProjectionType(UINameToValueMap.get(index.getProjection().getProjectionType()));
            }
        }
    }

    private void init() {
        UINameToValueMap = new HashMap<>();
        UINameToValueMap.put("String", "S");
        UINameToValueMap.put("Number", "N");
        UINameToValueMap.put("Binary", "B");
        UINameToValueMap.put("All Attributes", "ALL");
        UINameToValueMap.put("Table and Index Keys", "KEYS_ONLY");
        UINameToValueMap.put("Specify Attributes", "INCLUDE");

    }
}