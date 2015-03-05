/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.android.sdk;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.android.sdk.preferences.AndroidSDKPreferencePage;
import com.amazonaws.eclipse.android.sdk.preferences.PreferenceConstants;
import com.amazonaws.eclipse.sdk.ui.AbstractSdkManager;
import com.amazonaws.eclipse.sdk.ui.SdkInstallFactory;

public class AndroidSdkManager extends AbstractSdkManager<AndroidSdkInstall> {
    private static AndroidSdkManager instance;

    private AndroidSdkManager() {
        super("AWS SDK for Android", "aws-android-sdk", "aws-android-sdk",
                null, new AndroidSdkInstallFactory());
    }

    public static AndroidSdkManager getInstance() {
        if ( instance == null ) instance = new AndroidSdkManager();
        return instance;
    }


    public static class AndroidSdkInstallFactory implements SdkInstallFactory<AndroidSdkInstall> {
        public AndroidSdkInstall createSdkInstallFromDisk(File sdkRootDirectory) {
            return new AndroidSdkInstall(sdkRootDirectory);
        }
    }

    /**
     * Returns the directory where SDKs are installed, configured by a
     * preference.
     */
    protected File getSDKInstallDir() {
        String path = AndroidSDKPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.DOWNLOAD_DIRECTORY);
        return new File(path);
    }

    protected Action getHyperlinkAction() {

        return new Action("Configure SDK Download Behavior") {
            public void run() {
                PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
                        AndroidSDKPreferencePage.ID, new String[] { AndroidSDKPreferencePage.ID }, null).open();
            }
        };

    }

}