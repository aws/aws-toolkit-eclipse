/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.eclipse.lambda.serverless.model.transform;

import java.util.List;

public class DynamoDBTable {

    private String tableName;
    private List<AttributeDefinition> attributeDefinitions;
    private List<KeySchema> keySchemas;
    private ProvisionedThroughput provisionedThroughput;

    public String getTableName() {
        return tableName;
    }
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }
    public void setAttributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
        this.attributeDefinitions = attributeDefinitions;
    }
    public List<KeySchema> getKeySchemas() {
        return keySchemas;
    }
    public void setKeySchemas(List<KeySchema> keySchemas) {
        this.keySchemas = keySchemas;
    }
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }
    public void setProvisionedThroughput(ProvisionedThroughput provisionedThroughput) {
        this.provisionedThroughput = provisionedThroughput;
    }

}
