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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EditorPreferences {

    private static EditorPreferences DEFAULT_EDITOR_PREFERENCES;
    private Map<String, TokenPreference> highlight;

    public static EditorPreferences getDefaultPreferences() {
        if (DEFAULT_EDITOR_PREFERENCES == null) {
            try {
                InputStream inputStream = EditorPreferences.class
                        .getResourceAsStream("template-default-preferences.json");
                DEFAULT_EDITOR_PREFERENCES = new ObjectMapper().readValue(inputStream, EditorPreferences.class);
            } catch (IOException e) {
                CloudFormationPlugin.getDefault().logError(e.getMessage(), e);
            }
        }
        return DEFAULT_EDITOR_PREFERENCES;
    }

    public static Map<String, TokenPreference> buildHighlightPreferences(IPreferenceStore store) {
        Map<String, TokenPreference> highlightPreferences = new HashMap<>();
        for (TemplateTokenPreferenceNames names : TemplateTokenPreferenceNames.values()) {
            TokenPreference tokenPreference = TokenPreference.buildTemplateToken(store, names);
            highlightPreferences.put(tokenPreference.getId(), tokenPreference);
        }
        return highlightPreferences;
    }

    public Map<String, TokenPreference> getHighlight() {
        return highlight;
    }

    public void setHighlight(Map<String, TokenPreference> highlight) {
        this.highlight = highlight;
    }

    public static class TokenPreference {
        private final String id;
        @JsonIgnore
        private final TemplateTokenPreferenceNames preferenceNames;
        private Color color;
        private Boolean bold;
        private Boolean italic;

        @JsonCreator
        public TokenPreference(
                @JsonProperty("id") String id,
                @JsonProperty("color") Color color,
                @JsonProperty("bold") Boolean bold,
                @JsonProperty("italic") Boolean italic) {
            this.id = id;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.preferenceNames = TemplateTokenPreferenceNames.fromValue(id);
        }

        public TextAttribute toTextAttribute() {
            RGB rgb = new RGB(getColor().getRed(), getColor().getGreen(), getColor().getBlue());
            int style = 0x00;
            if (bold) {
                style |= SWT.BOLD;
            }
            if (italic) {
                style |= SWT.ITALIC;
            }
            return new TextAttribute(new org.eclipse.swt.graphics.Color(Display.getCurrent(), rgb), null, style);
        }

        public String getId() {
            return id;
        }

        @JsonIgnore
        public String getDisplayLabel() {
            return preferenceNames.getDisplayLabel();
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @JsonIgnore
        public String getColorRedPropertyName() {
            return preferenceNames.getColorRed();
        }

        @JsonIgnore
        public String getColorGreenPropertyName() {
            return preferenceNames.getColorGreen();
        }

        @JsonIgnore
        public String getColorBluePropertyName() {
            return preferenceNames.getColorBlue();
        }

        public Boolean getBold() {
            return bold;
        }

        public void setBold(Boolean bold) {
            this.bold = bold;
        }

        @JsonIgnore
        public String getBoldPropertyName() {
            return preferenceNames.getBold();
        }

        public Boolean getItalic() {
            return italic;
        }

        public void setItalic(Boolean italic) {
            this.italic = italic;
        }

        @JsonIgnore
        public String getItalicPropertyName() {
            return preferenceNames.getItalic();
        }

        /**
         * Build a template token from the given preference store according to the preference names of the token.
         */
        public static TokenPreference buildTemplateToken(IPreferenceStore store, TemplateTokenPreferenceNames preferenceNames) {

            Color color = new Color(
                    store.getInt(preferenceNames.getColorRed()),
                    store.getInt(preferenceNames.getColorGreen()),
                    store.getInt(preferenceNames.getColorBlue())
                    );
            Boolean bold = store.getBoolean(preferenceNames.getBold());
            Boolean italic = store.getBoolean(preferenceNames.getItalic());

            return new TokenPreference(preferenceNames.getId(), color, bold, italic);
        }
    }

    public static class Color {
        private int red;
        private int green;
        private int blue;

        @JsonCreator
        public Color(
                @JsonProperty("red") int red,
                @JsonProperty("green") int green,
                @JsonProperty("blue") int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int getRed() {
            return red;
        }

        public void setRed(int red) {
            this.red = red;
        }

        public int getGreen() {
            return green;
        }

        public void setGreen(int green) {
            this.green = green;
        }

        public int getBlue() {
            return blue;
        }

        public void setBlue(int blue) {
            this.blue = blue;
        }
    }
}
