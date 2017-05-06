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
package com.amazonaws.eclipse.lambda.project.template.data;

/**
 * Data model of Freemarker template for pom.xml
 */
public class PomFileTemplateData {

    private String groupId;
    private String artifactId;
    private String version;
    private String awsJavaSdkVersion;

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
    public String getAwsJavaSdkVersion() {
        return awsJavaSdkVersion;
    }
    public void setAwsJavaSdkVersion(String awsJavaSdkVersion) {
        this.awsJavaSdkVersion = awsJavaSdkVersion;
    }
}
