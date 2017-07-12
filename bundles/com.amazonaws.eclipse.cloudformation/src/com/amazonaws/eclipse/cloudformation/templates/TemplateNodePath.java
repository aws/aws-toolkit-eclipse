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
package com.amazonaws.eclipse.cloudformation.templates;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A path used to denote the target template node from ROOT.
 */
public class TemplateNodePath {

    public static class PathNode {
        private static final String PATH_NODE_SEPARATOR = ",";
        private final String fieldName;
        private final Integer index;
        private final List<String> parameters;

        public PathNode(Integer index) {
            this(null, index);
        }

        public PathNode(String fieldName, String... parameters) {
            this(fieldName, null, parameters);
        }

        private PathNode(String fieldName, Integer index, String... parameters) {
            this.fieldName = fieldName;
            this.index = index;
            this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
        }

        public String getFieldName() {
            return fieldName;
        }

        public Integer getIndex() {
            return index;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public String getReadiblePath() {
            if (index != null) {
                return String.valueOf(index);
            }
            StringBuilder builder = new StringBuilder(fieldName);
            for (String parameter : parameters) {
                builder.append(PATH_NODE_SEPARATOR + parameter);
            }
            return builder.toString();
        }

        @Override
        public String toString() {
            return getReadiblePath();
        }
    }
}
