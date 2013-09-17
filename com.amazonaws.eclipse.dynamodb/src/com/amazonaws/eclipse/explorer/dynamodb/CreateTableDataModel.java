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

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;

public class CreateTableDataModel {

    private String tableName;
    private String hashKeyName;
    private String hashKeyType;
    // Whether there is a range key
    private boolean enableRangeKey;
    private String rangeKeyName;
    private String rangeKeyType;
    private Long readCapacity;
    private Long writeCapacity;
    private List<LocalSecondaryIndex> localSecondaryIndices;
    // It is a must in create table request
    private List<AttributeDefinition> attributeDefinitions = new LinkedList<AttributeDefinition>();

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setHashKeyName(String hashKeyName) {
        this.hashKeyName = hashKeyName;
    }

    public String getHashKeyName() {
        return hashKeyName;
    }

    public void setHashKeyType(String hashKeyType) {
        this.hashKeyType = hashKeyType;
    }

    public String getHashKeyType() {
        return hashKeyType;
    }

    public void setReadCapacity(Long readCapacity) {
        this.readCapacity = readCapacity;
    }

    public Long getReadCapacity() {
        return readCapacity;
    }

    public Long getWriteCapacity() {
        return writeCapacity;
    }

    public void setWriteCapacity(Long writeCapacity) {
        this.writeCapacity = writeCapacity;
    }

    public void setEnableRangeKey(boolean enableRangeKey) {
        this.enableRangeKey = enableRangeKey;
    }

    public boolean getEnableRangeKey() {
        return enableRangeKey;
    }

    public void setRangeKeyName(String rangeKeyName) {
        this.rangeKeyName = rangeKeyName;
    }

    public String getRangeKeyName() {
        return rangeKeyName;
    }

    public void setRangeKeyType(String rangeKeyType) {
        this.rangeKeyType = rangeKeyType;
    }

    public String getRangeKeyType() {
        return rangeKeyType;
    }

    public List<LocalSecondaryIndex> getLocalSecondaryIndices() {
        return localSecondaryIndices;
    }

    public void setLocalSecondaryIndices(List<LocalSecondaryIndex> localSecondaryIndices) {
        this.localSecondaryIndices = localSecondaryIndices;
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
        this.attributeDefinitions = attributeDefinitions;
    }

}
