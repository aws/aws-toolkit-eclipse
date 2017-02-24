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
package com.amazonaws.eclipse.android.sdk.classpath;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.android.sdk.AndroidSdkInstall;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager.AndroidSdkInstallFactory;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkProjectMetadata;

public class AndroidSdkClasspathContainerInitializer extends ClasspathContainerInitializer {


    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        try {
            SdkProjectMetadata sdkProjectMetadataFile = new SdkProjectMetadata(javaProject.getProject());
            File sdkInstallRoot = sdkProjectMetadataFile.getSdkInstallRootForProject();

            if (sdkInstallRoot == null)
                throw new Exception("No SDK install directory specified");

            AndroidSdkInstall sdkInstall = new AndroidSdkInstallFactory().createSdkInstallFromDisk(sdkInstallRoot);

            if (sdkInstall.isValidSdkInstall() == false)
                throw new Exception("Invalid SDK install directory specified: " + sdkInstall.getRootDirectory());

            copySdkJarToProject(javaProject.getProject(), sdkInstall);

            AndroidSdkClasspathContainer classpathContainer = new AndroidSdkClasspathContainer(sdkInstall, javaProject.getProject());
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {javaProject}, new IClasspathContainer[] {classpathContainer}, null);
        } catch (Exception e) {
            AndroidSdkInstall defaultSdkInstall = AndroidSdkManager.getInstance().getDefaultSdkInstall();
            if ( defaultSdkInstall == null )
                throw new CoreException(new Status(IStatus.ERROR, JavaSdkPlugin.getDefault().getPluginId(), "No SDKs available"));

            AndroidSdkClasspathContainer classpathContainer = new AndroidSdkClasspathContainer(defaultSdkInstall, javaProject.getProject());
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {javaProject}, new IClasspathContainer[] {classpathContainer}, null);
            try {
                defaultSdkInstall.writeMetadataToProject(javaProject);
            } catch (IOException ioe) {
                JavaSdkPlugin.getDefault().logWarning(ioe.getMessage(), ioe);
            }

            String message = "Unable to initialize previous AWS SDK for Android classpath entries - defaulting to latest version";
            JavaSdkPlugin.getDefault().logWarning(message, e);
        }
    }

    private void copySdkJarToProject(IProject project, AndroidSdkInstall sdkInstall) {
        try {
            File sdkJar = sdkInstall.getSdkJar();

            File projectRoot = project.getLocation().toFile();
            File libsDirectory = new File(projectRoot, "libs");
            if (libsDirectory.exists() == false) {
                if (!libsDirectory.mkdir()) throw new Exception("Unable to create project libs directory");
            }

            if (libsDirectory.isDirectory() == false) {
                throw new Exception("Project contains a non-directory file named 'libs' already");
            }

            File destinationFile = new File(libsDirectory, sdkJar.getName());
            if (!destinationFile.exists()) {
                FileUtils.copyFile(sdkJar, destinationFile);
                project.refreshLocal(IResource.DEPTH_ONE, null);
            }
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Unable to copy AWS SDK for Android jar to project's lib directory", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
}
