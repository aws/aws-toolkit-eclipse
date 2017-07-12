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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;

/** 
 * A POJO model containing all the data for a CreateTable request.
 * This class is used as the model side of a data-binding.
 */
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
    
    private List<LocalSecondaryIndex> localSecondaryIndexes;
    private List<GlobalSecondaryIndex> globalSecondaryIndexes;
    
    // We use a set instead of a list to store all the attribute definitions,
    // in order to avoid duplicate from multiple secondary indexes.
    private Set<AttributeDefinition> attributeDefinitions = new HashSet<>();

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

    public List<LocalSecondaryIndex> getLocalSecondaryIndexes() {
        return localSecondaryIndexes;
    }

    public void setLocalSecondaryIndexes(List<LocalSecondaryIndex> localSecondaryIndexes) {
        this.localSecondaryIndexes = localSecondaryIndexes;
    }

    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        return globalSecondaryIndexes;
    }

    public void setGlobalSecondaryIndexes(
            List<GlobalSecondaryIndex> globalSecondaryIndexes) {
        this.globalSecondaryIndexes = globalSecondaryIndexes;
    }

    public Set<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(Set<AttributeDefinition> attributeDefinitions) {
        this.attributeDefinitions = attributeDefinitions;
    }
    
    public List<AttributeDefinition> getAttributeDefinitionsAsUnmodifiableList() {
        List<AttributeDefinition> defList = Collections.unmodifiableList(new LinkedList<AttributeDefinition>());
        defList.addAll(attributeDefinitions);
        return defList;
    }

}
