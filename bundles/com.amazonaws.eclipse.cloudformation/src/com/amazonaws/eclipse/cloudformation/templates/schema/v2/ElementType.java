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
package com.amazonaws.eclipse.cloudformation.templates.schema.v2;

public enum ElementType {
    STRING("String", "\"\"", -1),
    ARRAY("Array", "[]", -1),
    NAMED_ARRAY("Named-Array", "{}", -1),
    NUMBER("Number", "\"\"", -1),
    BOOLEAN("Boolean", "\"\"", -1),
    OBJECT("Object", "{}", -1),
    RESOURCE("Resource", "{}", -1),
    JSON("Json", "{}", -1),
    ;

    private final String typeValue;
    private final String insertionText;
    private final int cursorOffset;

    private ElementType(String typeValue, String insertionText, int cursorOffset) {
        this.typeValue = typeValue;
        this.insertionText = insertionText;
        this.cursorOffset = cursorOffset;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public String getInsertionText() {
        return insertionText;
    }

    public int getCursorOffset() {
        return cursorOffset;
    }

    public static ElementType fromValue(String typeValue) {
        for (ElementType elementType : ElementType.values()) {
            if (elementType.getTypeValue().equalsIgnoreCase(typeValue)) {
                return elementType;
            }
        }
        return STRING;
    }
}
