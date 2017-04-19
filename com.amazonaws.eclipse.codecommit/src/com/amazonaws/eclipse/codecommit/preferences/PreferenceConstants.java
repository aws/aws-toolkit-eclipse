/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codecommit.preferences;

import java.io.File;

/**
 * Preference constant values for CodeCommit plugin.
 */
public class PreferenceConstants {
    public static final String GIT_CREDENTIALS_FILE_PREFERENCE_NAME = "com.amazonaws.eclipse.codecommit.preference.GitCredentialsFile";
    public static final String DEFAULT_GIT_CREDENTIALS_FILE;
    static {
        DEFAULT_GIT_CREDENTIALS_FILE = new File(
                System.getProperty("user.home") + File.separator
                + ".aws" + File.separator + "gitCredentials")
        .getAbsolutePath();
    }
}
