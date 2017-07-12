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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = DeserializerFactory.AllowedValuesDeserializer.class)
public class AllowedValue {
    private static final AllowedValue TRUE = new AllowedValue("true", "true");
    private static final AllowedValue FALSE = new AllowedValue("false", "false");

    public static final List<AllowedValue> BOOLEAN_ALLOWED_VALUES =
            Collections.unmodifiableList(Arrays.asList(TRUE, FALSE));

    public static final String P_DISPLAY_LABEL = "display-label";
    public static final String P_VALUE = "value";

    private String displayLabel;
    private String value;

    public AllowedValue(@JsonProperty(P_DISPLAY_LABEL) String displayLabel,
            @JsonProperty(P_VALUE) String value) {
        this.displayLabel = displayLabel;
        this.value = value;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    @JsonProperty(P_DISPLAY_LABEL)
    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
