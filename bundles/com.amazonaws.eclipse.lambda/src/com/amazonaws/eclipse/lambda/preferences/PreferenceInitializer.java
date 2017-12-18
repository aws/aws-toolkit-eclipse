/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.util.OsPlatformUtils;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.launching.SamLocalConstants;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
    private static final String SAM_LOCAL_WINDOWS_DEFAULT_LOCATION =
            String.format("C:\\Users\\%s\\AppData\\Roaming\\npm\\sam.exe", OsPlatformUtils.currentUser());
    private static final String SAM_LOCAL_LINUX_DEFAULT_LOCATION = "/usr/local/bin/sam";

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = LambdaPlugin.getDefault().getPreferenceStore();
        store.setDefault(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE, getDefaultSamLocalLocation());
    }

    private String getDefaultSamLocalLocation() {
        if (OsPlatformUtils.isWindows()) {
            return SAM_LOCAL_WINDOWS_DEFAULT_LOCATION;
        } else {
            return SAM_LOCAL_LINUX_DEFAULT_LOCATION;
        }
    }
}
