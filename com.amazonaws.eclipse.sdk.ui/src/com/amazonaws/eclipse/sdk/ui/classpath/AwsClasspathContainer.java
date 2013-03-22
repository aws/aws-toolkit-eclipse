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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.sdk.ui.JavaSdkInstall;

/**
 * The Classpath container containing the AWS SDK for Java JAR files, and those of
 * its third-party dependencies.
 */
public class AwsClasspathContainer implements IClasspathContainer {

    /** The SDK containing the libraries to expose on the Java Project classpath */
    private JavaSdkInstall sdkInstall;

    /**
     * Creates a new AWS SDK for Java classpath container exposing the libraries
     * contained in the specified SDK.
     *
     * @param sdkInstall
     *            The SDK containing the libraries to expose on the Java project
     *            classpath.
     */
    public AwsClasspathContainer(JavaSdkInstall sdkInstall) {
        if (sdkInstall == null)
            throw new IllegalArgumentException("No SDK version specified");

        this.sdkInstall = sdkInstall;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        return loadSdkClasspathEntriesAsArray(sdkInstall);
    }

    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    public String getDescription() {
        return "AWS SDK for Java";
    }

    /**
     * Returns the version number of this SDK install.
     * @return the version number of this SDK install.
     */
    public String getVersion() {
        return sdkInstall.getVersion();
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
        return new Path("com.amazonaws.eclipse.sdk.AWS_JAVA_SDK");
    }


    /*
     * Private Interface
     */

    /**
     * Loads the JDT classpath entries for the AWS SDK for Java from the specified
     * SDK install base and returns them as an array.
     *
     * @param sdkInstall
     *            The SDK install from which to load the classpath entries.
     *
     * @return An array of the JDT classpath entries for the AWS SDK for Java at the
     *         specified install base.
     */
    private IClasspathEntry[] loadSdkClasspathEntriesAsArray(JavaSdkInstall sdkInstall) {
        List<IClasspathEntry> classpathEntries = loadSdkClasspathEntries(sdkInstall);
        return classpathEntries.toArray(new IClasspathEntry[classpathEntries.size()]);
    }

    /**
     * Loads the JDT classpath entries for the AWS SDK for Java from the specified
     * SDK install and returns them as a list.
     *
     * @param sdkInstall
     *            The SDK install from which to load the classpath entries.
     *
     * @return A list of the JDT classpath entries for the AWS SDK for Java at the
     *         specified install base.
     */
    private List<IClasspathEntry> loadSdkClasspathEntries(JavaSdkInstall sdkInstall) {
        List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
        IPath sdkJarPath, sdkSourceJarPath;
        
        try {
            sdkJarPath = new Path(sdkInstall.getSdkJar().getAbsolutePath());
            sdkSourceJarPath = new Path(sdkInstall.getSdkSourceJar().getAbsolutePath());
        } catch (FileNotFoundException e) {
            // The SDK has been deleted, there's no classpath entries anymore.
            return new ArrayList<IClasspathEntry>();
        }
        IClasspathAttribute externalJavadocPath = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, sdkInstall.getJavadocURL());
        if (externalJavadocPath.getValue() != null) {
            classpathEntries.add(JavaCore.newLibraryEntry(
                    sdkJarPath, sdkSourceJarPath, null, new IAccessRule[0],
                    new IClasspathAttribute[] {externalJavadocPath}, true));
        } else {
            classpathEntries.add(JavaCore.newLibraryEntry(
                    sdkJarPath, sdkSourceJarPath, null, new IAccessRule[0],
                    new IClasspathAttribute[0], true));
        }

        for (File jarFile : sdkInstall.getThirdPartyJars()) {
            IPath thirdPartyJarPath = new Path(jarFile.getAbsolutePath());
            classpathEntries.add(JavaCore.newLibraryEntry(
                    thirdPartyJarPath, thirdPartyJarPath, null, true));
        }

        return classpathEntries;
    }

}
