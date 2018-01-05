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
package com.amazonaws.eclipse.core.maven;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;

import com.amazonaws.util.StringUtils;

/**
 * A helper class used to perform common Maven related operations.
 */
public class MavenFactory {

    private static final String MAVEN_SOURCE_FOLDER = "src/main/java";
    private static final String MAVEN_TEST_FOLDER = "src/test/java";
    private static final String MAVEN_SOURCE_RESOURCES_FOLDER = "src/main/resources";
    private static final String MAVEN_TEST_RESOURCES_FOLDER = "src/test/resources";

    private static String MAVEN_MODEL_VERSION = "4.0.0";
    private static String AWS_JAVA_SDK_GROUP_NAME = "com.amazonaws";
    private static String AWS_JAVA_SDK_ARTIFACT_NAME = "aws-java-sdk";
    private static String AWS_JAVA_SDK_ARTIFACT_TYPE = "jar";
    private static String DEFAULT_AWS_JAVA_SDK_VERSION = "1.11.256";

    private static String AWS_JAVA_SDK_BOM_GROUP_NAME = "com.amazonaws";
    private static String AWS_JAVA_SDK_BOM_ARTIFACT_NAME = "aws-java-sdk-bom";
    private static String AWS_JAVA_SDK_BOM_ARTIFACT_TYPE = "pom";
    private static String DEFAULT_AWS_JAVA_SDK_BOM_VERSION = "1.11.256";

    private static String AMAZON_KINESIS_CLIENT_GROUP_NAME = "com.amazonaws";
    private static String AMAZON_KINESIS_CLIENT_ARTIFACT_NAME = "amazon-kinesis-client";
    private static String AMAZON_KINESIS_CLIENT_ARTIFACT_TYPE = "jar";

    private static String JUNIT_GROUP_NAME = "junit";
    private static String JUNIT_ARTIFACT_NAME = "junit";
    private static String JUNIT_ARTIFACT_TYPE = "jar";
    private static String DEFAULT_JUNIT_VERSION = "4.11";

    private static String AWS_LAMBDA_JAVA_CORE_GROUP_NAME = "com.amazonaws";
    private static String AWS_LAMBDA_JAVA_CORE_ARTIFACT_NAME = "aws-lambda-java-core";
    private static String AWS_LAMBDA_JAVA_CORE_ARTIFACT_TYPE = "jar";
    private static String DEFAULT_AWS_LAMBDA_JAVA_CORE_VERSION = "1.1.0";

    private static String AWS_LAMBDA_JAVA_EVENTS_GROUP_NAME = "com.amazonaws";
    private static String AWS_LAMBDA_JAVA_EVENTS_ARTIFACT_NAME = "aws-lambda-java-events";
    private static String AWS_LAMBDA_JAVA_EVENTS_ARTIFACT_TYPE = "jar";
    private static String DEFAULT_AWS_LAMBDA_JAVA_EVENTS_VERSION = "1.3.0";

    private static final String[] MAVEN_FOLDERS = {MAVEN_SOURCE_FOLDER, MAVEN_TEST_FOLDER, MAVEN_SOURCE_RESOURCES_FOLDER, MAVEN_TEST_RESOURCES_FOLDER};

    public static void createMavenProject(final IProject project, final Model model, IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().createSimpleProject(
                project, null, model, MavenFactory.MAVEN_FOLDERS,
                new ProjectImportConfiguration(), monitor);
    }

    public static List<IProject> createArchetypeProject(String archetypeGroupId, String archetypeArtifactId, String archetypeVersion,
            String groupId, String artifactId, String version, String packageName, IProgressMonitor monitor) throws CoreException {
        Archetype archetype = new Archetype();
        archetype.setGroupId(archetypeGroupId);
        archetype.setArtifactId(archetypeArtifactId);
        archetype.setVersion(archetypeVersion);
        return MavenPlugin.getProjectConfigurationManager().createArchetypeProjects(null, archetype,
                groupId, artifactId, version, packageName,
                new Properties(), new ProjectImportConfiguration(), monitor);
    }

    public static String getMavenSourceFolder() {
        return MAVEN_SOURCE_FOLDER;
    }

    public static String getMavenTestFolder() {
        return MAVEN_TEST_FOLDER;
    }

    public static String getMavenResourceFolder() {
        return MAVEN_SOURCE_RESOURCES_FOLDER;
    }

    public static String getMavenTestResourceFolder() {
        return MAVEN_TEST_RESOURCES_FOLDER;
    }

    public static String getMavenModelVersion() {
        return MAVEN_MODEL_VERSION;
    }

    public static Dependency getLatestAwsBomDependency() {
        return getLatestArtifactDependency(AWS_JAVA_SDK_BOM_GROUP_NAME, AWS_JAVA_SDK_BOM_ARTIFACT_NAME, "import", AWS_JAVA_SDK_BOM_ARTIFACT_TYPE, DEFAULT_AWS_JAVA_SDK_BOM_VERSION);
    }

    public static Dependency getLatestAwsSdkDependency(String scope) {
        return getLatestArtifactDependency(AWS_JAVA_SDK_GROUP_NAME, AWS_JAVA_SDK_ARTIFACT_NAME, scope, AWS_JAVA_SDK_ARTIFACT_TYPE, DEFAULT_AWS_JAVA_SDK_VERSION);
    }

    public static Dependency getAwsJavaSdkDependency(String version, String scope) {
        return createArtifactDependency(AWS_JAVA_SDK_GROUP_NAME, AWS_JAVA_SDK_ARTIFACT_NAME, version, scope, AWS_JAVA_SDK_ARTIFACT_TYPE);
    }

    public static Dependency getAmazonKinesisClientDependency(String version, String scope) {
        return createArtifactDependency(AMAZON_KINESIS_CLIENT_GROUP_NAME, AMAZON_KINESIS_CLIENT_ARTIFACT_NAME,
                version, scope, AMAZON_KINESIS_CLIENT_ARTIFACT_TYPE);
    }

    public static Dependency getAwsLambdaJavaEventsDependency(String version, String scope) {
        return createArtifactDependency(AWS_LAMBDA_JAVA_EVENTS_GROUP_NAME, AWS_LAMBDA_JAVA_EVENTS_ARTIFACT_NAME,
                version, scope, AWS_LAMBDA_JAVA_EVENTS_ARTIFACT_TYPE);
    }

    public static Dependency getAwsLambdaJavaEventsDependency() {
        return getAwsLambdaJavaEventsDependency(DEFAULT_AWS_LAMBDA_JAVA_EVENTS_VERSION, "compile");
    }

    public static Dependency getAwsLambdaJavaCoreDependency(String version, String scope) {
        return createArtifactDependency(AWS_LAMBDA_JAVA_CORE_GROUP_NAME, AWS_LAMBDA_JAVA_CORE_ARTIFACT_NAME,
                version, scope, AWS_LAMBDA_JAVA_CORE_ARTIFACT_TYPE);
    }

    public static Dependency getAwsLambdaJavaCoreDependency() {
        return getAwsLambdaJavaCoreDependency(DEFAULT_AWS_LAMBDA_JAVA_CORE_VERSION, "compile");
    }

    public static Dependency getJunitDependency(String version, String scope) {
        return createArtifactDependency(JUNIT_GROUP_NAME, JUNIT_ARTIFACT_NAME, version, scope, JUNIT_ARTIFACT_TYPE);
    }

    public static Dependency getJunitDependency() {
        return getJunitDependency(DEFAULT_JUNIT_VERSION, "test");
    }

    @NonNull
    public static String getLatestJavaSdkVersion() {
        return Optional.ofNullable(getLatestArtifactVersion(AWS_JAVA_SDK_GROUP_NAME, AWS_JAVA_SDK_ARTIFACT_NAME)).orElse(DEFAULT_AWS_JAVA_SDK_VERSION);
    }

    private static Dependency getLatestArtifactDependency(String groupId, String artifactId, String scope, String type, String defaultVersion) {
        String version = getLatestArtifactVersion(groupId, artifactId);
        if (version == null) {
            if (defaultVersion == null) {
                throw new RuntimeException(
                        String.format("The latest version of %s cannot be fetched either from remote Maven repository or the local", artifactId));
            }
            version = defaultVersion;
        }
        return createArtifactDependency(groupId, artifactId, version, scope, type);
    }

    private static Dependency createArtifactDependency(
            String groupId, String artifactId, String version, String scope, String type) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        dependency.setType(type);
        return dependency;
    }

    /**
     * Access the default remote Maven repository to fetch the latest version for the specified artifact.
     * If the repository is not available or no such an artifact, access the local repository instead.
     */
    public static String getLatestArtifactVersion(String groupId, String artifactId) {

        String remoteLatestVersion = MavenRepositories.getRemoteMavenRepository().getLatestVersion(groupId, artifactId);
        return remoteLatestVersion == null ? MavenRepositories.getDefaultLocalMavenRepository().getLatestVersion(groupId, artifactId)
                : remoteLatestVersion;
    }

    /**
     * Assume package name from the group id and artifact id: concatenating them with a dot '.'.
     * Return empty string if the parameter is not valid.
     */
    public static String assumePackageName(String groupId, String artifactId) {
        return StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(artifactId)
                ? "" : groupId + "." + artifactId;
    }
}
