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
package com.amazonaws.eclipse.cloudformation.templates.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Schema {
    private final Map<String, SchemaProperty> properties = new HashMap<>();
    private String description;
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addProperty(String name, SchemaProperty property) {
        properties.put(name, property);
    }

    public SchemaProperty getProperty(String property) {
        return properties.get(property); 
    }

    public Set<String> getProperties() {
        return properties.keySet();
    }
    
    @Override
    public String toString() {
        String buffer = "";
        for (Entry<String, SchemaProperty> entry : properties.entrySet()) {
            buffer += " - " + entry.getKey() + ": " + entry.getValue() + "\n"; 
        }
        
        return buffer;
    }
}