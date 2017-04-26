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
package com.amazonaws.eclipse.opsworks.deploy.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;

/**
 * Utilities for exporting Web Tools Platform Java web application projects to
 * WAR files.
 */
public class WTPWarUtils {

    public static IPath exportProjectToWar(IProject project, IPath directory) {
        File tempFile;
        try {
            tempFile = File.createTempFile("aws-eclipse-", ".war", directory.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to create web application archive: " + e.getMessage(), e);
        }
        return exportProjectToWar(project, directory, tempFile.getName());
    }

    public static IPath exportProjectToWar(IProject project, IPath directory, String fileName) {
        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());

        if (directory.toFile().exists() == false) {
            if (directory.toFile().mkdirs() == false) {
                throw new RuntimeException("Unable to create temp directory for web application archive.");
            }
        }

        String filename = new File(directory.toFile(), fileName).getAbsolutePath();

        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, filename);
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, project.getName());

        try {
            IDataModelOperation operation = dataModel.getDefaultOperation();
            operation.execute(new NullProgressMonitor(), null);
        } catch (ExecutionException e) {
            // TODO: better error handling
            e.printStackTrace();
            throw new RuntimeException("Unable to create web application archive: " + e.getMessage(), e);
        }

        return new Path(filename);
    }
}
