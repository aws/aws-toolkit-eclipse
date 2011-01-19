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
package com.amazonaws.eclipse.sdk.ui;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Collection of filename filters to pull out various files from the AWS SDK for
 * Java.
 */
public class FilenameFilters {
    private static final String AWS_JAVA_SDK_PREFIX = "aws-java-sdk-";

    /**
     * Filename filter accepting jar files.
     */
    public static class JarFilenameFilter implements FilenameFilter {
        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".jar"));
        }
    }

    /**
     * Filename filter accepting only the library jar from the AWS SDK for Java.
     */
    public static class SdkLibraryJarFilenameFilter extends JarFilenameFilter {

        /**
         * @see com.amazonaws.eclipse.sdk.ui.AwsClasspathContainer.JarFilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(File dir, String name) {
            if (!super.accept(dir, name)) return false;
            if (!name.startsWith(AWS_JAVA_SDK_PREFIX)) return false;

            if (name.contains("source")) return false;
            if (name.contains("javadoc")) return false;

            return true;
        }
    }

    /**
     * Filename filter accepting only the source jar from the AWS SDK for Java.
     */
    public static class SdkSourceJarFilenameFilter extends JarFilenameFilter {
        /**
         * @see com.amazonaws.eclipse.sdk.ui.AwsClasspathContainer.JarFilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(File dir, String name) {
            if (!super.accept(dir, name)) return false;

            if (!name.startsWith(AWS_JAVA_SDK_PREFIX )) return false;
            if (!name.contains("sources")) return false;

            return true;
        }
    }
    
    /**
     * Filename filter accepting only .java source files.
     */
    public static class JavaSourceFilenameFilter implements FilenameFilter {
        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".java");
        }
    }

}
