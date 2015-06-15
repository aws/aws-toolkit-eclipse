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

