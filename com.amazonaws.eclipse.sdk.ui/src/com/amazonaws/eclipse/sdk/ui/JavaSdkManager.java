/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.sdk.ui;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.sdk.ui.preferences.JavaSDKPreferencePage;
import com.amazonaws.eclipse.sdk.ui.preferences.PreferenceConstants;

/**
 * A manager for all the different copies of the AWS SDK for Java that are
 * installed.
 */
public class JavaSdkManager extends AbstractSdkManager<JavaSdkInstall> {

    private static JavaSdkManager instance;

    /**
     * Returns the singleton instance of SdkManager.
     *
     * @return The singleton instance of SdkManager.
     */
    public static JavaSdkManager getInstance() {
        if ( instance == null ) instance = new JavaSdkManager();
        return instance;
    }

    /* Hide the default public constructor */
    private JavaSdkManager() {
        super("AWS SDK for Java", "aws-java-sdk", "aws-java-sdk", new JavaSdkInstallFactory());
    }

    public static class JavaSdkInstallFactory implements SdkInstallFactory<JavaSdkInstall> {
        public JavaSdkInstall createSdkInstallFromDisk(File sdkRootDirectory) {
            return new JavaSdkInstall(sdkRootDirectory);
        }
    }
    
    /**
     * Returns the directory where SDKs are installed, configured by a
     * preference.
     */
    protected File getSDKInstallDir() {
        String path = JavaSdkPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.DOWNLOAD_DIRECTORY);
        return new File(path);
    }
    
    @Override
    protected Action getHyperlinkAction() {        
        
        return new Action("Configure SDK Download Behavior") {
            public void run() {
                PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
                        JavaSDKPreferencePage.ID, new String[] { JavaSDKPreferencePage.ID }, null).open();
            }                        
        };
        
    }

}
