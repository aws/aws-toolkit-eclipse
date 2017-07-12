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
import java.util.List;
import java.util.Map;

public class SchemaProperty {
    private String description;
    private String type;
    private boolean required;
    private List<String> allowedValues;
    private Schema schema;
    private boolean disableRefs = false;
    private Map<String, Schema> childSchemas;
    private String schemaLookupProperty;
    private Schema defaultChildSchema;


    public String getSchemaLookupProperty() {
        return schemaLookupProperty;
    }

    public SchemaProperty(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean b) {
        this.required = b;
    }
    
    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }
    
    public Map<String, Schema> getChildSchemas() {
        return childSchemas;
    }
    
    public Schema getChildSchema(String id) {
        return childSchemas.get(id);
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

    public Schema getSchema() {
        return schema;
    }
    
    public void setSchema(Schema schema) {
        this.schema = schema;
    }
    
    @Override
    public String toString() {
        return "(" + type + "): " + description;
    }

    public void setDefaultChildSchema(Schema defaultChildSchema) {
        this.defaultChildSchema = defaultChildSchema;
    }
    
    public Schema getDefaultChildSchema() {
        return defaultChildSchema;
    }
}