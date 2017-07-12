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

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.cloudformation.preferences.EditorPreferences.TokenPreference;

public class EditorPreferencesLoader {

    /**
     * Load default editor preferences to the specified preference store.
     */
    static void loadDefaultPreferences(IPreferenceStore store, EditorPreferences preferences) {
        for (TokenPreference tokenPreference : preferences.getHighlight().values()) {
            loadDefaultTokenPreferences(store, tokenPreference);
        }
    }

    private static void loadDefaultTokenPreferences(IPreferenceStore store, TokenPreference tokenPreference) {
        store.setDefault(tokenPreference.getColorRedPropertyName(), tokenPreference.getColor().getRed());
        store.setDefault(tokenPreference.getColorGreenPropertyName(), tokenPreference.getColor().getGreen());
        store.setDefault(tokenPreference.getColorBluePropertyName(), tokenPreference.getColor().getBlue());
        store.setDefault(tokenPreference.getBoldPropertyName(), tokenPreference.getBold());
        store.setDefault(tokenPreference.getItalicPropertyName(), tokenPreference.getItalic());
    }

    /**
     * Load editor preferences to the specified preference store.
     * @see {@link TokenPreference#buildTemplateToken(IPreferenceStore, TemplateTokenPreferenceNames)}
     */
    public static void loadPreferences(IPreferenceStore store, EditorPreferences preferences) {
        for (TokenPreference tokenPreference : preferences.getHighlight().values()) {
            loadTokenPreferences(store, tokenPreference);
        }
    }

    public static void loadTokenPreferences(IPreferenceStore store, TokenPreference tokenPreference) {
        store.setValue(tokenPreference.getColorRedPropertyName(), tokenPreference.getColor().getRed());
        store.setValue(tokenPreference.getColorGreenPropertyName(), tokenPreference.getColor().getGreen());
        store.setValue(tokenPreference.getColorBluePropertyName(), tokenPreference.getColor().getBlue());
        store.setValue(tokenPreference.getBoldPropertyName(), tokenPreference.getBold());
        store.setValue(tokenPreference.getItalicPropertyName(), tokenPreference.getItalic());
    }

    private static void loadTokenPreferences(IPreferenceStore from, TemplateTokenPreferenceNames tokenPreferenceNames, IPreferenceStore to) {
        to.setValue(tokenPreferenceNames.getColorRed(), from.getInt(tokenPreferenceNames.getColorRed()));
        to.setValue(tokenPreferenceNames.getColorGreen(), from.getInt(tokenPreferenceNames.getColorGreen()));
        to.setValue(tokenPreferenceNames.getColorBlue(), from.getInt(tokenPreferenceNames.getColorBlue()));
        to.setValue(tokenPreferenceNames.getBold(), from.getBoolean(tokenPreferenceNames.getBold()));
        to.setValue(tokenPreferenceNames.getItalic(), from.getBoolean(tokenPreferenceNames.getItalic()));
    }

    public static void loadPreferences(IPreferenceStore from, IPreferenceStore to) {
        for (TemplateTokenPreferenceNames names : TemplateTokenPreferenceNames.values()) {
            loadTokenPreferences(from, names, to);
        }
    }
}