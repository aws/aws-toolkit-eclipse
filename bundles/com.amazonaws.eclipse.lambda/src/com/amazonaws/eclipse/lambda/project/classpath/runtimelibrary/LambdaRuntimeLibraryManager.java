/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class LambdaRuntimeLibraryManager {

    private static final String LAMBDA_RUNTIME_LIBRARY_LATEST_VERSION = "1.1";

    private static final String LAMBDA_RUNTIME_JAR_BASEDIR = "lambda-runtime-jar";
    private static final String LAMBDA_RUNTIME_JAR_CORE = "aws-lambda-java-core-1.0.0.jar";
    private static final String LAMBDA_RUNTIME_JAR_EVENTS = "aws-lambda-java-events-1.1.0.jar";
    private static final String LAMBDA_RUNTIME_JAR_CORE_JAVADOC = "aws-lambda-java-core-1.0.0-javadoc.jar";
    private static final String LAMBDA_RUNTIME_JAR_EVENTS_JAVADOC = "aws-lambda-java-events-1.1.0-javadoc.jar";

    private static final LambdaRuntimeLibraryManager INSTANCE = new LambdaRuntimeLibraryManager();

    public static LambdaRuntimeLibraryManager getInstance() {
        return INSTANCE;
    }

    public LambdaRuntimeLibraryVersion getLatestVersion() {

        try {
            Bundle bundle = LambdaPlugin.getDefault().getBundle();

            URL coreJarFileUrl = FileLocator.resolve(bundle.getEntry(
                    String.format("/%s/%s",
                            LAMBDA_RUNTIME_JAR_BASEDIR,
                            LAMBDA_RUNTIME_JAR_CORE)));
            File coreJarFile = new File(coreJarFileUrl.getFile());

            URL coreJavadocJarFileUrl = FileLocator.resolve(bundle.getEntry(
                    String.format("/%s/%s",
                            LAMBDA_RUNTIME_JAR_BASEDIR,
                            LAMBDA_RUNTIME_JAR_CORE_JAVADOC)));
            File coreJavadocJarFile = new File(coreJavadocJarFileUrl.getFile());

            URL eventsJarFileUrl = FileLocator.resolve(bundle.getEntry(
                    String.format("/%s/%s",
                            LAMBDA_RUNTIME_JAR_BASEDIR,
                            LAMBDA_RUNTIME_JAR_EVENTS)));
            File eventsJarFile = new File(eventsJarFileUrl.getFile());

            URL eventsJavadocJarFileUrl = FileLocator.resolve(bundle.getEntry(
                    String.format("/%s/%s",
                            LAMBDA_RUNTIME_JAR_BASEDIR,
                            LAMBDA_RUNTIME_JAR_EVENTS_JAVADOC)));
            File eventsJavadocJarFile = new File(eventsJavadocJarFileUrl.getFile());

            return new LambdaRuntimeLibraryVersion(
                    LAMBDA_RUNTIME_LIBRARY_LATEST_VERSION,
                    Arrays.asList(
                            new LambdaRuntimeLibraryComponent(coreJarFile, coreJavadocJarFile, true),
                            new LambdaRuntimeLibraryComponent(eventsJarFile, eventsJavadocJarFile, false))
                    );

        } catch (IOException e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to load the Lambda function runtime jar", e);
            return null;
        }
    }

    private LambdaRuntimeLibraryManager() {
    }
}
