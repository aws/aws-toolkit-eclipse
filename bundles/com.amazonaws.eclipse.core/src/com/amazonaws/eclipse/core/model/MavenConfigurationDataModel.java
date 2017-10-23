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
package com.amazonaws.eclipse.core.model;

import com.amazonaws.eclipse.core.maven.MavenFactory;

/**
 * Data model for Maven project configuration composite.
 */
public class MavenConfigurationDataModel extends AbstractAwsToolkitDataModel {

    public static final String P_GROUP_ID = "groupId";
    public static final String P_ARTIFACT_ID = "artifactId";
    public static final String P_PACKAGE_NAME = "packageName";
    public static final String P_VERSION = "version";

    private String groupId = "com.amazonaws";
    private String artifactId = "samples";
    private String version = "1.0.0";
    private String packageName = MavenFactory.assumePackageName(groupId, artifactId);

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackageName(String packageName) {
        this.setProperty(P_PACKAGE_NAME, packageName, this::getPackageName, (newValue) -> this.packageName = newValue);
    }

    public String getPackageName() {
        return packageName;
    }
}