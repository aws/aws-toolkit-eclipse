/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core.validator;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Java package name model.
 */
public class JavaPackageName {

    private static final String DOT = ".";
    private static final String DOT_REGEX = "\\.";
    private static final String VALID_JAVA_PACKAGE_NAME = "([a-z][a-z_0-9]*\\.)*[a-z][a-z_0-9]*";

    private final List<String> components;

    private JavaPackageName(List<String> components) {
        this.components = new LinkedList<>(components);
    }

    public JavaPackageName append(String component) {
        this.components.add(component);
        return this;
    }

    public List<String> getComponents() {
        return new LinkedList<>(this.components);
    }

    public boolean isEmpty() {
        return this.components.isEmpty();
    }

    public String asDotDelimitedString() {
        return join(components, DOT);
    }

    /**
     * @param path
     *            dot-delimited representation of the classpath
     */
    public static JavaPackageName parse(String path) {
        if (path.isEmpty()) {
            return new JavaPackageName(new LinkedList<String>());
        }

        if (!path.matches(VALID_JAVA_PACKAGE_NAME)) {
            throw new IllegalArgumentException("Invalid classpath string " + path);
        }

        String[] components = path.split(DOT_REGEX);
        return new JavaPackageName(Arrays.asList(components));
    }

    private static String join(Collection<String> components, String conjuction) {
        if (components == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String component : components) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(conjuction);
            }
            sb.append(component);
        }
        return sb.toString();
    }
}
