/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.sdk.ui.preferences;

import org.eclipse.ui.IWorkbench;

import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;


/**
 * Preference page for the Java SDK
 */
public class JavaSDKPreferencePage extends AbstractSDKPreferencesPage {

    public static final String ID = "com.amazonaws.eclipse.sdk.ui.preferences.JavaSDKPreferencePage";
    
    public JavaSDKPreferencePage() {
        super("AWS SDK For Java Preferences");
    }

    protected String getDownloadAutomaticallyPreferenceName() {
        return PreferenceConstants.DOWNLOAD_AUTOMATICALLY;
    }

    protected String getDownloadDirectoryPreferenceName() {
        return PreferenceConstants.DOWNLOAD_DIRECTORY;
    }

    protected void checkForSDKUpdates() {
        JavaSdkManager.getInstance().initializeSDKInstalls();
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(JavaSdkPlugin.getDefault().getPreferenceStore());
    }

}
