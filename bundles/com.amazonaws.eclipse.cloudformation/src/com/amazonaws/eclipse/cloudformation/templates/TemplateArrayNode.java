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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonLocation;

/**
 * Represents a JSON array structure in a Template document.
 */
public class TemplateArrayNode extends TemplateNode {
    private JsonLocation startLocation;
    private JsonLocation endLocation;
    private List<TemplateNode> members = new ArrayList<TemplateNode>();

    public TemplateArrayNode(JsonLocation startLocation) {
        this.startLocation = startLocation;
    }

    public boolean isArray() {
        return true;
    }

    public List<TemplateNode> getMembers() {
        return members;
    }

    public void add(TemplateNode node) {
        node.setParent(this);

        members.add(node);
    }

    public JsonLocation getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(JsonLocation startLocation) {
        this.startLocation = startLocation;
    }

    public JsonLocation getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(JsonLocation endLocation) {
        this.endLocation = endLocation;
    }
}