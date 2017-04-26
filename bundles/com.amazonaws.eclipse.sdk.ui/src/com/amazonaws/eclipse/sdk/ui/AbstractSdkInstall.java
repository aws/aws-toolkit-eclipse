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
package com.amazonaws.eclipse.sdk.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

public abstract class AbstractSdkInstall {

    /** The root directory of this SDK install */
    protected final File sdkRootDirectory;

    public AbstractSdkInstall(File sdkRootDirectory) {
        this.sdkRootDirectory = sdkRootDirectory;
    }

    /**
     * Returns the root directory where this SDK version is installed.
     *
     * @return The root directory where this SDK version is installed.
     */
    public File getRootDirectory() {
        return sdkRootDirectory;
    }

    public boolean isValidSdkInstall() {
        return true;
    }

    public File getSdkJar() throws FileNotFoundException {
        return null;
    }

    public String getJavadocURL() {
        return null;
    }

    public File getSdkSourceJar() throws FileNotFoundException {
        return null;
    }

    public List<File> getThirdPartyJars() {
        return null;
    }

    public String getVersion() {
        return null;
    }

    public List<SdkSample> getSamples() {
        return null;
    }

    /**
     * Writes a metadata file to the SDK Plugin root directory specifying which version
     * of the AWS SDK for Java the specified project is using.
     * @param javaProject The project using this SdkInstall.
     * @throws IOException if the plugin root directory could not be written to.
     */
    public void writeMetadataToProject(IJavaProject javaProject) throws IOException {
        SdkProjectMetadata sdkProjectMetadataFile =
            new SdkProjectMetadata(javaProject.getProject());
        sdkProjectMetadataFile.setSdkInstallRootForProject(this.getRootDirectory());
    }

}