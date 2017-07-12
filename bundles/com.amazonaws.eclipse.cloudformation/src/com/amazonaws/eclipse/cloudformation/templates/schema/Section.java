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


public class Section {
    private final String description;
    private final String type;
    private final boolean required;
    final Schema schema;
    private boolean disableRefs = false;
    Map<String, Schema> childSchemas;
    private String schemaLookupProperty;
    
    
    
    public Section(String type, String description, boolean required, Schema schema) {
        this.type = type;
        this.description = description;
        this.required = required;
        this.schema = schema;
    }
    

    public Map<String, Schema> getChildSchemas() {
        return childSchemas;
    }
    
    public void setSchemaLookupProperty(String schemaLookupProperty) {
        this.schemaLookupProperty = schemaLookupProperty;
    }


    public void addChildSchema(String name, Schema schema) {
        if (childSchemas == null) childSchemas = new HashMap<>();
        
        childSchemas.put(name, schema);
    }
    
    public void setDisableRefs(boolean b) {
        this.disableRefs = b;
    }
    
    public boolean getDisableRefs() {
        return disableRefs;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Schema getSchema() {
        return schema;
    }
}