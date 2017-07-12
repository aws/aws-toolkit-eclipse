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
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNodePath.PathNode;
import com.fasterxml.jackson.core.JsonLocation;

/**
 * Abstract base class representing a generic JSON node in a Template document.
 */
public abstract class TemplateNode {

    public static final String ROOT_PATH = "ROOT";
    public static final String PATH_SEPARATOR = "/";

    private TemplateNode parent;
    private JsonLocation startLocation;
    private JsonLocation endLocation;

    public TemplateNode getParent() {
        return parent;
    }

    public void setParent(TemplateNode parent) {
        this.parent = parent;
    }

    public String getPath() {
        List<PathNode> subPaths = getSubPaths();
        StringBuilder builder = new StringBuilder();
        for (PathNode subPath : subPaths) {
            builder.append(subPath.getReadiblePath() + TemplateNode.PATH_SEPARATOR);
        }
        return builder.toString();
    }

    public List<PathNode> getSubPaths() {
        Stack<PathNode> stack = new Stack<>();
        TemplateNode node = parent;
        while (node != null) {
            if (node instanceof TemplateFieldNode) {
                String fieldName = ((TemplateFieldNode) node).getText();
                stack.push(new PathNode(fieldName));
            } else if (node instanceof TemplateIndexNode) {
                int index = ((TemplateIndexNode) node).getIndex();
                stack.push(new PathNode(index));
            } else if (node instanceof TemplateObjectNode) {
                TemplateObjectNode objectNode = (TemplateObjectNode) node;
                TemplateNode typeNode = objectNode.get("Type");
                if (typeNode != null && typeNode instanceof TemplateValueNode) {
                    String type = ((TemplateValueNode) typeNode).getText();
                    node = node.getParent();
                    if (node == null) {
                        break;
                    }
                    if (!(node instanceof TemplateFieldNode)) {
                        continue;
                    }
                    String fieldName = ((TemplateFieldNode) node).getText();
                    stack.push(new PathNode(fieldName, type));
                }
            }
            node = node.getParent();
        }
        stack.push(new PathNode(TemplateNode.ROOT_PATH));
        List<PathNode> stackList = new ArrayList<>(stack);
        Collections.reverse(stackList);
        return Collections.unmodifiableList(stackList);
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