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
package com.amazonaws.eclipse.sdk.ui.classpath;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.sdk.ui.JavaSdkInstall;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkProjectMetadata;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager.JavaSdkInstallFactory;

/**
 * An initializer for the Classpath container for the AWS SDK for Java.
 */
public class ClasspathContainerInitializer extends
        org.eclipse.jdt.core.ClasspathContainerInitializer {

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        try {
            SdkProjectMetadata sdkProjectMetadataFile =
                new SdkProjectMetadata(javaProject.getProject());
            File sdkInstallRoot = sdkProjectMetadataFile.getSdkInstallRootForProject();

            if (sdkInstallRoot == null)
                throw new Exception("No SDK install directory specified");

            JavaSdkInstall sdkInstall = new JavaSdkInstallFactory().createSdkInstallFromDisk(sdkInstallRoot);
            if (sdkInstall.isValidSdkInstall() == false)
                throw new Exception("Invalid SDK install directory specified: " + sdkInstall.getRootDirectory());

            AwsClasspathContainer classpathContainer = new AwsClasspathContainer(sdkInstall);
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {javaProject}, new IClasspathContainer[] {classpathContainer}, null);
        } catch (Exception e) {
            JavaSdkInstall defaultSdkInstall = JavaSdkManager.getInstance().getDefaultSdkInstall();
            if ( defaultSdkInstall == null )
                throw new CoreException(new Status(IStatus.ERROR, JavaSdkPlugin.PLUGIN_ID, "No SDKs available"));

            AwsClasspathContainer classpathContainer = new AwsClasspathContainer(defaultSdkInstall);
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {javaProject}, new IClasspathContainer[] {classpathContainer}, null);
            try {
                defaultSdkInstall.writeMetadataToProject(javaProject);
            } catch (IOException ioe) {
                StatusManager.getManager().handle(new Status(Status.WARNING, JavaSdkPlugin.PLUGIN_ID, ioe.getMessage(), ioe), StatusManager.LOG);
            }

            String message = "Unable to initialize previous AWS SDK for Java classpath entries - defaulting to latest version";
            Status status = new Status(Status.WARNING, JavaSdkPlugin.PLUGIN_ID, message, e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }
}
