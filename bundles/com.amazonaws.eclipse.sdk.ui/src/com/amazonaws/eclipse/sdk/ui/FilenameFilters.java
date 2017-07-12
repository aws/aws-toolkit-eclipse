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
package com.amazonaws.eclipse.sdk.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Collection of filename filters to pull out various files from the AWS SDK for
 * Java.
 */
public class FilenameFilters {

    private static final Pattern AWS_JAVA_SDK_PATTERN = Pattern.compile("aws-java-sdk-(\\d+|\\.)+\\.jar");
    private static final Pattern AWS_JAVA_SDK_SOURCE_PATTERN = Pattern.compile("aws-java-sdk-(\\d+|\\.)+\\-sources\\.jar");

    /**
     * Filename filter accepting jar files.
     */
    public static class JarFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".jar"));
        }
    }

    /**
     * Filename filter accepting only the library jar from the AWS SDK for Java.
     */
    public static class SdkLibraryJarFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return AWS_JAVA_SDK_PATTERN.matcher(name).matches();
        }
    }

    /**
     * Filename filter accepting only the source jar from the AWS SDK for Java.
     */
    public static class SdkSourceJarFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return AWS_JAVA_SDK_SOURCE_PATTERN.matcher(name).matches();
        }
    }

    /**
     * Filename filter accepting only .java source files.
     */
    public static class JavaSourceFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".java");
        }
    }

    /**
     * Filename filter accepting only the credentials file.
     */
    public static class CredentialsFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.equals("credentials");
        }
    }

}
