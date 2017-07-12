/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Utility class for managing Java project.
 */
public class JavaProjectUtils {

    /**
     * Replace the JRE version for the specified Java project to the default one and set the
     * compliance level to the default JRE version
     *
     * @param javaProject - The specified Java project
     * @param monitor - The progress monitor
     *
     * @throws JavaModelException
     */
    public static void setDefaultJreToProjectClasspath(IJavaProject javaProject, IProgressMonitor monitor)
            throws JavaModelException {

        List<IClasspathEntry> classpathEntry = new ArrayList<>();

        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
            if (!entry.getPath().toString().startsWith(JavaRuntime.JRE_CONTAINER)) {
                classpathEntry.add(entry);
            }
        }
        classpathEntry.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
        javaProject.setRawClasspath(
                classpathEntry.toArray(new IClasspathEntry[classpathEntry.size()]), monitor);
        Map<String, String> options = JavaCore.getOptions();
        javaProject.setOption(JavaCore.COMPILER_SOURCE, options.get(JavaCore.COMPILER_SOURCE));
        javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, options.get(JavaCore.COMPILER_COMPLIANCE));
        javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
    }
}
