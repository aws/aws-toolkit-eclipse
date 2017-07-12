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
package com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;

/**
 * Simple container class to collate multi-value fields into a single
 * collection.
 */
public class SimpleDBItem {

    final String itemName;
    final Map<String, Collection<String>> attributes;
    final List<String> columns;

    public SimpleDBItem(final Item item) {
        this.itemName = item.getName();
        this.attributes = new TreeMap<>();
        this.columns = new ArrayList<>();

        String currCol = null;
        for ( Attribute att : item.getAttributes() ) {
            if ( currCol == null || !currCol.equals(att.getName()) ) {
                this.columns.add(att.getName());
            }
            if ( !this.attributes.containsKey(att.getName()) ) {
                this.attributes.put(att.getName(), new LinkedList<String>());
            }
            this.attributes.get(att.getName()).add(att.getValue());
        }
    }

}
