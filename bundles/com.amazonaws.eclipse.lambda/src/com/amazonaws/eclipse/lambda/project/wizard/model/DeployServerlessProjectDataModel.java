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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.lambda.project.metadata.ServerlessProjectMetadata;

public class DeployServerlessProjectDataModel {

    public static final String P_BUCKET_NAME = "bucketName";
    public static final String P_STACK_NAME = "stackName";

    private Region region;
    private String bucketName;
    private String stackName;
    private final String projectName;
    private final ServerlessProjectMetadata metadata;

    public DeployServerlessProjectDataModel(String projectName, ServerlessProjectMetadata metadata) {
        this.projectName = projectName;
        this.metadata = metadata == null ? new ServerlessProjectMetadata() : metadata;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * The returned metadata is nonnull
     */
    public ServerlessProjectMetadata getMetadata() {
        return metadata;
    }
}
