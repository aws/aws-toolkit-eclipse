package com.amazonaws.eclipse.cloudformation.templates;

import org.codehaus.jackson.JsonLocation;

public class TemplateNamedObjectNode extends TemplateObjectNode {

    private String name;

    public TemplateNamedObjectNode(JsonLocation startLocation) {
        super(startLocation);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
