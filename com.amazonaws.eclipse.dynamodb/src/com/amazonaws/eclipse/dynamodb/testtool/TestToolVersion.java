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
package com.amazonaws.eclipse.dynamodb.testtool;

public class TestToolVersion {

    public static enum InstallState {
        NOT_INSTALLED,
        INSTALLING,
        INSTALLED,
        RUNNING
    }

    private final String name;
    private final String description;
    private final String downloadKey;
    private final InstallState installState;

    public TestToolVersion(final String name,
                           final String description,
                           final String downloadKey,
                           final InstallState installState) {

        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (downloadKey == null) {
            throw new NullPointerException("downloadKey cannot be null");
        }
        if (installState == null) {
            throw new NullPointerException("installState cannot be null");
        }

        this.name = name;
        this.description = (description == null ? "" : description);
        this.downloadKey = downloadKey;
        this.installState = installState;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDownloadKey() {
        return downloadKey;
    }

    public boolean isInstalled() {
        return (installState == InstallState.INSTALLED);
    }

    public boolean isInstalling() {
        return (installState == InstallState.INSTALLING);
    }

    public boolean isRunning() {
        return (installState == InstallState.RUNNING);
    }

    public InstallState getInstallState() {
        return installState;
    }

    @Override
    public String toString() {
        return "DynamoDB_Local_" + name;
    }

}
