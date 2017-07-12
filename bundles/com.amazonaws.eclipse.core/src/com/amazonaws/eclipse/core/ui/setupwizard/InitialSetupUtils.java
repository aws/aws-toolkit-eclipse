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

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Utilities for running the initial setup wizard to help users get the toolkit configured.
 */
public class InitialSetupUtils {

    private static final String ACCOUNT_INITIALIZATION_FLAG_FILE = ".toolkitInitialized";
    private static final String ANALYTICS_INITIALIZATION_FLAG_FILE = ".analyticsInitialized";

    private static final int INIT_SETUP_WIZARD_DIALOG_WIDTH = 550;
    private static final int INIT_SETUP_WIZARD_DIALOG_HEIGHT = 250;

    public static void runInitialSetupWizard() {

        final boolean showAccountInitPage = shouldShowAccountInitPage();
        final boolean showAnalyticsInitPage = shouldShowAnalyticsInitPage();
        final boolean runSetupWizard = showAccountInitPage || showAnalyticsInitPage;

        if (runSetupWizard) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    Shell shell = new Shell(Display.getDefault(), SWT.DIALOG_TRIM);
                    WizardDialog wizardDialog = new WizardDialog(shell,
                            new InitialSetupWizard(showAccountInitPage,
                                    showAnalyticsInitPage, AwsToolkitCore
                                            .getDefault().getPreferenceStore()));
                    wizardDialog.setPageSize(INIT_SETUP_WIZARD_DIALOG_WIDTH,
                            INIT_SETUP_WIZARD_DIALOG_HEIGHT);
                    wizardDialog.open();

                    if (showAccountInitPage) {
                        markAccountInitPageShown();
                    }
                    if (showAnalyticsInitPage) {
                        markAnalyticsInitPageShown();
                    }
                }
            });
        }
    }

    private static boolean shouldShowAccountInitPage() {
        boolean showAccountInitPage = !isCredentialsConfigured()
                && !doesFlagFileExist(ACCOUNT_INITIALIZATION_FLAG_FILE);
        return showAccountInitPage;
    }

    private static boolean shouldShowAnalyticsInitPage() {
        return !doesFlagFileExist(ANALYTICS_INITIALIZATION_FLAG_FILE);
    }

    private static void markAccountInitPageShown() {
        writeFlagFile(ACCOUNT_INITIALIZATION_FLAG_FILE);
    }

    private static void markAnalyticsInitPageShown() {
        writeFlagFile(ANALYTICS_INITIALIZATION_FLAG_FILE);
    }

    private static boolean isCredentialsConfigured() {
        String accessKey = AwsToolkitCore.getDefault().getAccountInfo().getAccessKey();
        boolean credentialsConfigured = (accessKey != null) && (accessKey.length() > 0);
        return credentialsConfigured;
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
                AwsToolkitCore.getDefault().logWarning("Unable to create ~/.aws directory to save toolkit initialization file", null);
            } else {
                File flagFile = new File(awsDirectory, path);
                flagFile.createNewFile();
            }
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logWarning("Unable to save toolkit initialization file", e);
        }
    }
}