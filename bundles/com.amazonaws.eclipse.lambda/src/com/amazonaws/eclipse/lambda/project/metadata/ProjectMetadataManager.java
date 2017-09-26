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
package com.amazonaws.eclipse.lambda.project.metadata;

import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.getProjectDirectory;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that manages loading and saving Serverless and Lambda projects metadata caches.
 * These cached data are saved as hidden files in the target project root.
 */
public class ProjectMetadataManager {

    private static final String PROJECT_SETTING_FOLDER_NAME = ".settings";
    private static final String LAMBDA_PROJECT_METADATA_FILE = "com.amazonaws.eclipse.lambda.project.json";
    private static final String SERVERLESS_PROJECT_METADATA_FILE = "com.amazonaws.eclipse.serverless.project.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * This function overrides all the existing metadata for the Lambda project.
     */
    public static void saveLambdaProjectMetadata(IProject project, LambdaFunctionProjectMetadata metadata)
            throws JsonGenerationException, JsonMappingException, IOException {
        File metadataFile = getMetadataFile(project, LAMBDA_PROJECT_METADATA_FILE);
        MAPPER.writeValue(metadataFile, metadata);
    }

    public static LambdaFunctionProjectMetadata loadLambdaProjectMetadata(IProject project)
            throws JsonParseException, JsonMappingException, IOException {

        File metadataFile = getMetadataFile(project, LAMBDA_PROJECT_METADATA_FILE);
        if (!metadataFile.exists()) {
            return null;
        }
        return MAPPER.readValue(metadataFile, LambdaFunctionProjectMetadata.class);
    }

    /**
     * This function overrides all the existing metadata for the Serverless project.
     */
    public static void saveServerlessProjectMetadata(IProject project, ServerlessProjectMetadata metadata)
            throws JsonGenerationException, JsonMappingException, IOException {
        File metadataFile = getMetadataFile(project, SERVERLESS_PROJECT_METADATA_FILE);
        MAPPER.writeValue(metadataFile, metadata);
    }

    /**
     * Return the cached metadata under the Serverless project. Return null if the file is not found.
     */
    public static ServerlessProjectMetadata loadServerlessProjectMetadata(IProject project)
            throws JsonParseException, JsonMappingException, IOException {
        File metadataFile = getMetadataFile(project, SERVERLESS_PROJECT_METADATA_FILE);
        if (!metadataFile.exists()) {
            return null;
        }

        return MAPPER.readValue(metadataFile, ServerlessProjectMetadata.class);
    }

    private static File getMetadataFile(IProject project, String metadataFileName) {
        IPath settingsDir = getProjectDirectory(project, PROJECT_SETTING_FOLDER_NAME);
        settingsDir.toFile().mkdirs();
        return settingsDir.append(metadataFileName).toFile();
    }
}
