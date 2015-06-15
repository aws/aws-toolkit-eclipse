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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryComponent;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryVersion;

/**
 * The Classpath container containing the AWS Lambda Java runtime JAR file.
 */
public class LambdaRuntimeClasspathContainer implements IClasspathContainer {

    public final static String DESCRIPTION = "AWS Lambda Java Function Runtime";

    public final static Path ID = new Path("com.amazonaws.eclipse.lambda.AWS_LAMBDA_JAVA_CLASSPATH_CONTAINER");

    private final LambdaRuntimeLibraryVersion runtimeLibrary;

    /**
     * @param runtimeLibrary
     *            the lambda java runtime library version to be included in the
     *            classpath
     * @param javaSdkInstall
     *            (optional) the AWS Java SDK install.
     */
    public LambdaRuntimeClasspathContainer(LambdaRuntimeLibraryVersion runtimeLibrary) {
        if (runtimeLibrary == null) {
            throw new IllegalArgumentException("No runtime library version specified");
        }
        this.runtimeLibrary = runtimeLibrary;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        List<IClasspathEntry> entries = new LinkedList<IClasspathEntry>();

        for (LambdaRuntimeLibraryComponent component : runtimeLibrary.getLibraryComponents()) {
            entries.add(loadRuntimeClasspathEntry(component));
        }
        return entries.toArray(new IClasspathEntry[entries.size()]);
    }

    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * @return the version number of the runtime jar.
     */
    public String getVersion() {
        return runtimeLibrary.getVersionString();
    }

    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
     */
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
     */
    public IPath getPath() {
        return ID;
    }

    private IClasspathEntry loadRuntimeClasspathEntry(LambdaRuntimeLibraryComponent component) {
        IPath classJarPath = new Path(component.getClassJarFile().getAbsolutePath());

        IClasspathAttribute attrs[] = new IClasspathAttribute[0];
        if (component.getJavadocJarFile() != null) {
            attrs = new IClasspathAttribute[] {
                            JavaCore.newClasspathAttribute(
                                    IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                                    constructJavadocLocationAttributeValue(component.getJavadocJarFile()))
                    };
        }

        return JavaCore.newLibraryEntry(classJarPath, null, null,
                new IAccessRule[0], attrs, true);
    }

    private String constructJavadocLocationAttributeValue(File javadocFile) {
        return "jar:" + javadocFile.toURI().toString() + "!/";
    }

}
