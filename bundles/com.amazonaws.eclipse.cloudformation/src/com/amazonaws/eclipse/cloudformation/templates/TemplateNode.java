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

import com.fasterxml.jackson.core.JsonLocation;


/**
 * Abstract base class representing a generic JSON node in a Template document.
 */
public abstract class TemplateNode {
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
        String path = "";
        TemplateNode node = parent;
        while (node != null) {
            if (node.isField()) {
                path = ((TemplateFieldNode)node).getText() + "/" + path;
            }
            node = node.getParent();
        }

        return "ROOT/" + path;
    }

    public boolean isField() {
        return false;
    }

    public boolean isObject() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isValue() {
        return false;
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