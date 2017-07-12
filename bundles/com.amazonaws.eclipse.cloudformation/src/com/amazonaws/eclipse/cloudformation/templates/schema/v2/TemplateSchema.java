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

import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNodePath.PathNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root POJO of CloudFormation Template schema.
 */
public class TemplateSchema {
    private Map<String, IntrinsicFunction> intrinsicFunctions;
    private Map<String, PseudoParameter> pseudoParameters;
    private TemplateElement rootSchemaObject;

    public Map<String, IntrinsicFunction> getIntrinsicFunctions() {
        return intrinsicFunctions;
    }

    @JsonProperty("intrinsic-functions")
    public void setIntrinsicFunctions(Map<String, IntrinsicFunction> intrinsicFunctions) {
        this.intrinsicFunctions = intrinsicFunctions;
    }

    public Map<String, PseudoParameter> getPseudoParameters() {
        return pseudoParameters;
    }

    @JsonProperty("pseudo-parameters")
    public void setPseudoParameters(Map<String, PseudoParameter> pseudoParameters) {
        this.pseudoParameters = pseudoParameters;
    }

    public TemplateElement getRootSchemaObject() {
        return rootSchemaObject;
    }

    @JsonProperty("root-schema-object")
    public void setRootSchemaObject(TemplateElement rootSchemaObject) {
        this.rootSchemaObject = rootSchemaObject;
    }

    @JsonIgnore
    public TemplateElement getTemplateElement(List<PathNode> path) {
        assert(path != null && !path.isEmpty() && path.get(0).equals(TemplateNode.ROOT_PATH));

        TemplateElement currentElement = rootSchemaObject;
        for (int i = 1; i < path.size(); ++i) {
            PathNode currentPathNode = path.get(i);
            // Current element is an Array, skip the path node
            if (ElementType.fromValue(currentElement.getType()) == ElementType.ARRAY) {
                String fieldName = currentPathNode.getFieldName();
                if (fieldName != null && currentElement.getProperties() != null) {
                    currentElement = currentElement.getProperties().get(fieldName);
                }
            // Current element is an object with fixed attributes
            } else if (currentElement.getProperties() != null) {
                Map<String, TemplateElement> properties = currentElement.getProperties();
                if (properties.containsKey(currentPathNode.getFieldName())) {
                    currentElement = properties.get(currentPathNode.getFieldName());
                } else {
                    throw new RuntimeException("Cannot find TemplateElement: " + path);
                }
            // Current element is a map with fixed value schema
            } else if (currentElement.getDefaultChildSchema() != null) {
                currentElement = currentElement.getDefaultChildSchema();
            // Current element is a map with dynamic value schema
            } else if (currentElement.getChildSchemas() != null) {
                Map<String, TemplateElement> childSchemas = currentElement.getChildSchemas();
                List<String> parameters = currentPathNode.getParameters();
                if (!parameters.isEmpty()) {
                    currentElement = childSchemas.get(parameters.get(0));
                    if (currentElement == null) {
                        throw new RuntimeException("Cannot find TemplateElement: " + path);
                    }
                } else {
                    return TemplateElement.GENERIC_RESOURCE_ELEMENT;
                }
            } else {
                throw new RuntimeException("Cannot find TemplateElement: " + path);
            }
        }
        return currentElement;
    }
}
