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
package com.amazonaws.eclipse.lambda.launching;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;

import com.amazonaws.eclipse.core.util.PluginUtils;

public class SamLocalPathFinder {
    private static final String[] TEMPLATE_FILES = {
        "template.yaml", "template.yml", "serverless.template", "serverless.json", "template.json", "sam.template", "sam.json"
    };
    private static final String[] ENV_VAR_FILES = {"envvars.json", "envvar.json", "env-vars.json"};
    private static final String[] EVENT_FILES = {"s3-event.json", "sns-event.json", "kinesis-event.json", "dynamodb-event.json", "api-event.json", "schedule-event.json"};

    public static String findTemplateFile(String projectName) {
        return find(projectName, TEMPLATE_FILES);
    }

    public static String findEnvVarFile(String projectName) {
        return find(projectName, ENV_VAR_FILES);
    }

    public static String findEventFile(String projectName) {
        return find(projectName, EVENT_FILES);
    }

    /**
     * Parse the project pom.xml file to generate the location for the build artifact.
     */
    public static String findCodeUri(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return "";
        }

        IFile pomFile = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFile("pom.xml");
        if (pomFile.exists()) {
            try {
                Model mavenModel = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);

                String artifactId = mavenModel.getArtifactId();
                String version = mavenModel.getVersion();
                return String.format("./target/%s-%s.jar", artifactId, version);
            } catch (CoreException e) {
                return "";
            }
        }

        return "";
    }

    private static String variablePluginReplace(String path) {
        try {
            return PluginUtils.variablePluginReplace(path);
        } catch (CoreException e) {
            return path;
        }
    }

    private static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    private static String find(String projectName, String[] candidatePaths) {
        if (projectName == null || projectName.isEmpty()) {
            return "";
        }
        return Arrays.stream(candidatePaths)
                .map(subpath -> PluginUtils.variablePluginGenerateWorkspacePath(projectName + "/" + subpath))
                .filter(path -> fileExists(variablePluginReplace(path)))
                .findFirst().orElse("");
    }
}
