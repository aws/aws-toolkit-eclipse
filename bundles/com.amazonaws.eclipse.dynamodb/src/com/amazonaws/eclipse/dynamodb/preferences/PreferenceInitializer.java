/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_DOWNLOAD_DIRECTORY;
    static {
        File userHome = new File(System.getProperty("user.home"));
        DEFAULT_DOWNLOAD_DIRECTORY =
            new File(userHome, "dynamodb-local").getAbsolutePath();
    }

    public static final int DEFAULT_PORT = 8000;

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = DynamoDBPlugin.getDefault().getPreferenceStore();

        store.setDefault(
            TestToolPreferencePage.DOWNLOAD_DIRECTORY_PREFERENCE_NAME,
            DEFAULT_DOWNLOAD_DIRECTORY
        );
        store.setDefault(
            TestToolPreferencePage.DEFAULT_PORT_PREFERENCE_NAME,
            DEFAULT_PORT
        );
    }
}
