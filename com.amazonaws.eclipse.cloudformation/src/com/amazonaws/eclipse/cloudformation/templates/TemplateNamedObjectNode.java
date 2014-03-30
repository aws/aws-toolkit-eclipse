package com.amazonaws.eclipse.cloudformation.templates;

import org.codehaus.jackson.JsonLocation;

/**
 * A CloudFormation JSON script consists of a set of top-level objects which each contain one or more immediate
 * children, and each of these children has a name so that it can be referred to elsewhere in the script. These
 * named children are represented by instances of TemplateNamedObjectNode.
 *
 */
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
