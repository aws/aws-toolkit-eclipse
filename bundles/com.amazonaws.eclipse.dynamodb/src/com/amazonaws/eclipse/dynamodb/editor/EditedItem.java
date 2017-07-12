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
package com.amazonaws.eclipse.dynamodb.editor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.TableItem;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * An edited DynamoDB item in the table editor.
 */
class EditedItem {

    private final Set<String> editedAttributes = new HashSet<>();
    private Map<String, AttributeValue> attributes;
    private final TableItem tableItem;

    public EditedItem(Map<String, AttributeValue> attributes, TableItem tableItem) {
        this.attributes = attributes;
        this.tableItem = tableItem;
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, AttributeValue> attributes) {
        this.attributes = attributes;
    }

    public Set<String> getEditedAttributes() {
        return editedAttributes;
    }
    
    public void markAttributeEdited(String attributeName) {
        editedAttributes.add(attributeName);
    }

    public TableItem getTableItem() {
        return tableItem;
    }

}
