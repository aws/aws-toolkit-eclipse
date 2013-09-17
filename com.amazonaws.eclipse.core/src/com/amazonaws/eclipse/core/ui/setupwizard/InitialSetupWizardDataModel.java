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

/**
 * Simple data model to hold the data collected by the initial setup wizard.
 */
public final class InitialSetupWizardDataModel {
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String OPEN_EXPLORER = "openExplorer";


    /** Hold the users AWS access key once it's entered into the UI */
    private String accessKeyId;

    /** Hold the users AWS secret key once it's entered into the UI */
    private String secretAccessKey;

    /**
     * True (the default setting) if the AWS Explorer view should be opened
     * after the wizard is completed.
     */
    private boolean openExplorer = true;


    public boolean isOpenExplorer() {
        return openExplorer;
    }

    public void setOpenExplorer(boolean openExplorer) {
        this.openExplorer = openExplorer;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}