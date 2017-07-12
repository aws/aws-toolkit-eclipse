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
    private List<TemplateNode> members = new ArrayList<>();

    public TemplateArrayNode(JsonLocation startLocation) {
        setStartLocation(startLocation);
    }

    public List<TemplateNode> getMembers() {
        return members;
    }

    public void add(TemplateNode node) {
        TemplateIndexNode indexNode = new TemplateIndexNode(members.size());
        indexNode.setParent(this);
        node.setParent(indexNode);
        members.add(node);
    }

    public TemplateNode get(int index) {
        return members.get(index);
    }
}
