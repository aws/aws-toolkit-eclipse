/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.amazonaws.eclipse.sdk.ui.SdkInstall;

/**
 * A utility class  
 */
public class AwsSdkClasspathUtils {
    
    /**
     * Modifies the classpath of the specified Java project to contain the classpath container
     * for the AWS SDK for Java.
     * 
     * @param javaProject The Java project to modify.
     * @param sdkInstall The AWS SDK for Java installation to add.
     */
    public static void addAwsSdkToProjectClasspath(IJavaProject javaProject, SdkInstall sdkInstall) {
        try {
            AwsClasspathContainer classpathContainer = new AwsClasspathContainer(sdkInstall);

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
            e.printStackTrace();
        }
    }
    
    /**
     * Modifies the classpath of the specified Java project to remove the classpath container
     * for the AWS SDK for Java.
     * 
     * If the specified SDK installation is not already present on the project's classpath,
     * nothing is done, and no error is returned.
     * 
     * @param javaProject The Java project to modify.
     * @param sdkInstall The AWS SDK for Java installation to remove.
     */
    public static void removeAwsSdkFromProjectClasspath(IJavaProject javaProject, SdkInstall sdkInstall) {
        try{
            AwsClasspathContainer classpathContainer = new AwsClasspathContainer(sdkInstall);

            IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
            List<IClasspathEntry> newList = new ArrayList<IClasspathEntry>();
            for (IClasspathEntry entry : rawClasspath) {
                if (entry.getPath().equals(classpathContainer.getPath()) == false) {
                    newList.add(entry);
                }
            }
            
            javaProject.setRawClasspath(newList.toArray(new IClasspathEntry[newList.size()]), null);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }
}
