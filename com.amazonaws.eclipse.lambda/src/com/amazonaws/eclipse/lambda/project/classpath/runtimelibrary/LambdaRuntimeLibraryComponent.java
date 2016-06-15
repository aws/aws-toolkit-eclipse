/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

public class LambdaRuntimeLibraryComponent {

    private final File classJarFile;
    private final File javadocJarFile;
    private final boolean shouldBeExcludedInFunctionCode;

    LambdaRuntimeLibraryComponent(File classJarFile, File javadocJarFile,
            boolean shouldBeExcludedInFunctionCode) {
        this.classJarFile = classJarFile;
        this.javadocJarFile = javadocJarFile;
        this.shouldBeExcludedInFunctionCode = shouldBeExcludedInFunctionCode;
    }

    public File getClassJarFile() {
        return classJarFile;
    }

    public File getJavadocJarFile() {
        return javadocJarFile;
    }

    public boolean isShouldBeExcludedInFunctionCode() {
        return shouldBeExcludedInFunctionCode;
    }

}
