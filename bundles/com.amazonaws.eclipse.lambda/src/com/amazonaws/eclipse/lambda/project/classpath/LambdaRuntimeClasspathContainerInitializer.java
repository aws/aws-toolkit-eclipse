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
package com.amazonaws.eclipse.lambda.project.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryManager;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryVersion;

/**
 * The Classpath container containing the AWS Lambda Java runtime JAR file.
 */
public class LambdaRuntimeClasspathContainerInitializer extends ClasspathContainerInitializer {

    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        try {
            LambdaRuntimeLibraryVersion lastestRuntimeJar =
                    LambdaRuntimeLibraryManager.getInstance().getLatestVersion();

            LambdaRuntimeClasspathContainer classpathContainer =
                    new LambdaRuntimeClasspathContainer(lastestRuntimeJar);
            JavaCore.setClasspathContainer(
                    containerPath,
                    new IJavaProject[] {javaProject},
                    new IClasspathContainer[] {classpathContainer},
                    null);

        } catch (Exception e) {
            String message = "Unable to initialize Lambda Java Runtime classpath.";
            LambdaPlugin.getDefault().reportException(message, e);
        }
    }
}
