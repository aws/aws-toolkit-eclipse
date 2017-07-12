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
package com.amazonaws.eclipse.cloudformation.preferences;

/**
 * The preference names for the given template token.
 */
public enum TemplateTokenPreferenceNames {

    /* The id value for these tokens must be identical to the default preferences configuration file*/
    KEY("keyToken", "Key Token"),
    VALUE("valueToken", "Value Token"),
    INTRINSIC_FUNCTION("intrinsicFunctionToken", "Intrinsic Function Name"),
    PSEUDO_PARAMETER("pseudoParameterToken", "Pseudo Parameter"),
    RESOURCE_TYPE("resourceTypeToken", "AWS Resource Type"),
    ;

    // Preference name prefix
    private static final String PREFIX = "com.amazonaws.cloudformation.editor";

    private final String id;
    private final String displayLabel;
    private final String colorRed;
    private final String colorGreen;
    private final String colorBlue;
    private final String bold;
    private final String italic;

    private TemplateTokenPreferenceNames(String id, String displayLabel) {
        this.id = id;
        this.displayLabel = displayLabel;
        this.colorRed = PREFIX + "." + id + ".red";
        this.colorGreen = PREFIX + "." + id + ".green";
        this.colorBlue = PREFIX + "." + id + ".blue";
        this.bold = PREFIX + "." + id + ".bold";
        this.italic = PREFIX + "." + id + ".italic";
    }

    public String getId() {
        return id;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getColorRed() {
        return colorRed;
    }

    public String getColorGreen() {
        return colorGreen;
    }

    public String getColorBlue() {
        return colorBlue;
    }

    public String getBold() {
        return bold;
    }

    public String getItalic() {
        return italic;
    }

    public static boolean isCloudFormationEditorProperty(String propertyName) {
        return propertyName.startsWith(PREFIX);
    }

    public static TemplateTokenPreferenceNames fromValue(String id) {
        for (TemplateTokenPreferenceNames names : TemplateTokenPreferenceNames.values()) {
            if (names.getId().equalsIgnoreCase(id)) {
                return names;
            }
        }
        assert(false);
        return null;
    }
}
