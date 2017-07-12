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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * Collection type for edited items in the dynamo table editor.
 */
public class EditedItems {

    /*
     * Linked hash map preserves order of insertion so that iterations are made
     * in the same order as insertions. This is so that updates are done in the
     * same order as edits.
     */
    private final Map<Map<String, AttributeValue>, EditedItem> editedItems = new LinkedHashMap<>();
        
    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public EditedItem get(Map<String, AttributeValue> key) {
        return editedItems.get(key);
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public void add(Map<String, AttributeValue> key, EditedItem value) {
        editedItems.put(key, value);
    }
    
    /**
     * Updates the item with the key given to contain the attributes given.
     */
    public void update(Map<String, AttributeValue> key, Map<String, AttributeValue> dynamoItem) {
        editedItems.get(key).setAttributes(dynamoItem);
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return editedItems.containsKey(key);
    }
    
    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public EditedItem remove(Map<String, AttributeValue> key) {
        return editedItems.remove(key);
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        editedItems.clear();        
    }

    /**
     * Returns an iterator over all the entries in the collection.
     */
    public Iterator<Entry<Map<String, AttributeValue>, EditedItem>> iterator() {
        return editedItems.entrySet().iterator();
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return editedItems.isEmpty();
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        return editedItems.size();
    }

}
