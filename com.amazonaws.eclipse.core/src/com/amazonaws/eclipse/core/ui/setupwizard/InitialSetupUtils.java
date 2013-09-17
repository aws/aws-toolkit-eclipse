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
package com.amazonaws.eclipse.core.ui.setupwizard;

import java.io.File;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Utilities for running the initial setup wizard to help users get the toolkit configured.
 */
public class InitialSetupUtils {
    private static final String INITIALIZATION_FLAG_FILE = ".toolkitInitialized";

    public static void runInitialSetupWizard() {
        // Launch the setup wizard only if there are no credentials configured
        // and the setup wizard hasn't been run before.
        
        String accessKey = AwsToolkitCore.getDefault().getAccountInfo().getAccessKey();
        boolean credentialsConfigured = (accessKey != null) && (accessKey.length() > 0);
        boolean runSetupWizard =
                credentialsConfigured == false
                && doesFlagFileExist(INITIALIZATION_FLAG_FILE) == false;

        if (runSetupWizard) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    Shell shell = new Shell(Display.getDefault(), SWT.DIALOG_TRIM);
                    WizardDialog wizardDialog = new WizardDialog(shell, new InitialSetupWizard());
                    wizardDialog.open();

                    writeFlagFile(INITIALIZATION_FLAG_FILE);
                }
            });
        }
    }

    private static boolean doesFlagFileExist(String path) {
        String userHome = System.getProperty("user.home");
        if (userHome == null) return false;

        File awsDirectory = new File(userHome, ".aws");
        return new File(awsDirectory, path).exists();
    }

    private static void writeFlagFile(String path) {
        try {
            String userHome = System.getProperty("user.home");
            if (userHome == null) return;

            File awsDirectory = new File(userHome, ".aws");

            if (!awsDirectory.exists() && awsDirectory.mkdir() == false) {
                AwsToolkitCore.getDefault().getLog().log(new Status(Status.WARNING, AwsToolkitCore.PLUGIN_ID,
                		"Unable to create ~/.aws directory to save toolkit initialization file"));
            } else {
            	File flagFile = new File(awsDirectory, path);
            	flagFile.createNewFile();
            }
        } catch (Exception e) {
            AwsToolkitCore.getDefault().getLog().log(
                new Status(Status.WARNING, AwsToolkitCore.PLUGIN_ID, "Unable to save toolkit initialization file", e));
        }
    }
}