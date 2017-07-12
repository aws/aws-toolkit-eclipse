/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.schema.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A union type for all kinds of Template element
 */
public class TemplateElement {

    // A fake template element to be returned when the underlying schema is not known
    public static final TemplateElement GENERIC_RESOURCE_ELEMENT = new TemplateElement();

    static {
        GENERIC_RESOURCE_ELEMENT.setType(ElementType.OBJECT.getTypeValue());
        TemplateElement type = new TemplateElement();
        type.setType(ElementType.STRING.getTypeValue());
        type.setRequired("true");
        Map<String, TemplateElement> properties = new HashMap<>();
        properties.put("Type", type);
        GENERIC_RESOURCE_ELEMENT.setProperties(properties);
    }
    // Allowed values: Array, Boolean, ConditionDeclaration, ConditionDefinitions, DestinationCidrBlock,
    //                 Json, Named-Array, Number, Object, Policy, Reference, Resource, String
    private String type;
    private String required;  // Allowed values: true, false, Json
    private List<AllowedValue> allowedValues;
    private Boolean disableRefs;
    private Boolean disableFunctions;
    private String description;
    private String arrayType;
    // The attribute name to look for the child schema, must be an existing key of the childSchemas attribute
    private String schemaLookupProperty;
    private List<String> resourceRefType;
    private List<ReturnValue> returnValues;

    // All the child schemas for this Json node.
    private Map<String, TemplateElement> childSchemas;
    private TemplateElement defaultChildSchema;
    private Map<String, TemplateElement> properties;

    /** Irregular fields in the schema file, might need to remove these. */
    private TemplateElement S3DestinationConfiguration;
    private TemplateElement TextTransformation;

    public TemplateElement getS3DestinationConfiguration() {
        return S3DestinationConfiguration;
    }
    public void setS3DestinationConfiguration(TemplateElement s3DestinationConfiguration) {
        S3DestinationConfiguration = s3DestinationConfiguration;
    }
    public TemplateElement getTextTransformation() {
        return TextTransformation;
    }
    public void setTextTransformation(TemplateElement textTransformation) {
        TextTransformation = textTransformation;
    }
    /** End irregular fields in the schema file. */

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getRequired() {
        return required;
    }
    public void setRequired(String required) {
        this.required = required;
    }
    public List<AllowedValue> getAllowedValues() {
        return allowedValues;
    }

    @JsonProperty("allowed-values")
    public void setAllowedValues(List<AllowedValue> allowedValues) {
        this.allowedValues = allowedValues;
    }
    public Boolean getDisableRefs() {
        return disableRefs;
    }

    @JsonProperty("disable-refs")
    public void setDisableRefs(Boolean disableRefs) {
        this.disableRefs = disableRefs;
    }
    public Boolean getDisableFunctions() {
        return disableFunctions;
    }

    @JsonProperty("disable-functions")
    public void setDisableFunctions(Boolean disableFunctions) {
        this.disableFunctions = disableFunctions;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getArrayType() {
        return arrayType;
    }

    @JsonProperty("array-type")
    public void setArrayType(String arrayType) {
        this.arrayType = arrayType;
    }
    public String getSchemaLookupProperty() {
        return schemaLookupProperty;
    }

    @JsonProperty("schema-lookup-property")
    public void setSchemaLookupProperty(String schemaLookupProperty) {
        this.schemaLookupProperty = schemaLookupProperty;
    }

    public List<String> getResourceRefType() {
        return resourceRefType;
    }

    @JsonProperty("resource-ref-type")
    public void setResourceRefType(List<String> resourceRefType) {
        this.resourceRefType = resourceRefType;
    }

    public List<ReturnValue> getReturnValues() {
        return returnValues;
    }

    @JsonProperty("return-values")
    public void setReturnValues(List<ReturnValue> returnValues) {
        this.returnValues = returnValues;
    }

    public Map<String, TemplateElement> getChildSchemas() {
        return childSchemas;
    }

    @JsonProperty("child-schemas")
    public void setChildSchemas(Map<String, TemplateElement> childSchemas) {
        this.childSchemas = childSchemas;
    }
    public TemplateElement getDefaultChildSchema() {
        return defaultChildSchema;
    }
    @JsonProperty("default-child-schema")
    public void setDefaultChildSchema(TemplateElement defaultChildSchema) {
        this.defaultChildSchema = defaultChildSchema;
    }
    public Map<String, TemplateElement> getProperties() {
        return properties;
    }
    public void setProperties(Map<String, TemplateElement> properties) {
        this.properties = properties;
    }
}
