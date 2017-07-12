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

public class IntrinsicFunction {
    private final String name;
    private final String parameter;
    private final String description;

    public IntrinsicFunction(String name, String parameter, String description) {
        this.name = name;
        this.parameter = parameter;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }

    public String getParameter() {
        return parameter;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " (" + parameter + "): " + description; 
    }
}