/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.util;

/**
 * Static validation utility methods
 */
public class ValidationUtils {

    /**
     * Validates that the given object is non-null
     *
     * @param object
     *            Object to validate
     * @param fieldName
     *            Field name to display in exception message if object is null
     * @return The object if valid
     * @throws IllegalArgumentException
     *             If object is null
     */
    public static <T> T validateNonNull(T object, String fieldName) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return object;
    }
}
