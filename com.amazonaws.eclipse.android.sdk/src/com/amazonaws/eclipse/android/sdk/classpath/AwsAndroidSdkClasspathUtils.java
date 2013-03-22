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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.sdk.ui.AbstractSdkInstall;

public class AwsAndroidSdkClasspathUtils {
    public static void addAwsAndroidSdkToProjectClasspath(IJavaProject javaProject, AbstractSdkInstall sdkInstall) {
        try {
            AndroidSdkClasspathContainer classpathContainer = new AndroidSdkClasspathContainer(sdkInstall, javaProject.getProject());

            IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
            List<IClasspathEntry> newList = new ArrayList<IClasspathEntry>();
            for (IClasspathEntry entry : rawClasspath) {
                if (entry.getPath().equals(classpathContainer.getPath()) == false) {
                    newList.add(entry);
                }
            }

            newList.add(JavaCore.newContainerEntry(classpathContainer.getPath()));

            javaProject.setRawClasspath(newList.toArray(new IClasspathEntry[newList.size()]), null);
        } catch (JavaModelException e) {
            String message = "Unable to add AWS SDK for Android to the project's classpath";
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, message, e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }
}