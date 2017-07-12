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
package com.amazonaws.eclipse.cloudformation.templates;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.JsonLocation;

/**
 * Represents a JSON object structure in a Template document.
 */
public class TemplateObjectNode extends TemplateNode {
    private Map<String, TemplateNode> map = new LinkedHashMap<>();

    public TemplateObjectNode(JsonLocation startLocation) {
        setStartLocation(startLocation);
    }

    public void put(String field, TemplateNode value) {
        TemplateFieldNode fieldNode = new TemplateFieldNode(field);
        fieldNode.setParent(this);
        value.setParent(fieldNode);

        map.put(field, value);
    }

    public TemplateNode get(String field) {
        return map.get(field);
    }

    public Set<Entry<String,TemplateNode>> getFields() {
        return map.entrySet();
    }
}