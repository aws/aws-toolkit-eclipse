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
package com.amazonaws.eclipse.lambda.upload.wizard.util;

import java.io.File;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarBuilder;
import org.eclipse.jdt.ui.jarpackager.IManifestProvider;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class LambdaFunctionJarBuilder extends FatJarBuilder {

    private static final String LAMBDA_FUNCTINO_JAR_BUILDER_ID = LambdaFunctionJarBuilder.class.getName();

    public LambdaFunctionJarBuilder() {
    }

    @Override
    public String getId() {
        return LAMBDA_FUNCTINO_JAR_BUILDER_ID;
    }

    @Override
    public IManifestProvider getManifestProvider() {
        // we don't need to bundle manifest file for the function zip file
        return null;
    }

    @Override
    public void writeArchive(ZipFile zip, IProgressMonitor monitor) {

        String zipPath = zip.getName();
        File zipFile = new File(zipPath);
        String zipName = zipFile.getName();

        try {
            getJarWriter().write(zipFile, new Path("lib/" + zipName));
        } catch (CoreException e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to bundle dependency into the function jar file. " + zipPath, e);
        }
    }

    @Override
    public String getManifestClasspath() {
        return null;
    }

    @Override
    public boolean isMergeManifests() {
        return false;
    }

    @Override
    public boolean isRemoveSigners() {
        return true;
    }
}
