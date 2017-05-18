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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metadata POJO for deploying Serverless project. This POJO records the last deployment information
 * for the corresponding Serverless project which could be reused for the next time of deployment.
 */
public class ServerlessProjectMetadata {
    // Lambda handler package prefix
    private String packagePrefix;
    // Region ID for last deployment
    private String lastDeploymentRegionId;
    // Configurations for last deployment to a specific region
    private Map<String, RegionConfig> regionConfig;

    public String getLastDeploymentRegionId() {
        return lastDeploymentRegionId;
    }
    public void setLastDeploymentRegionId(String lastDeploymentRegionId) {
        this.lastDeploymentRegionId = lastDeploymentRegionId;
    }
    public Map<String, RegionConfig> getRegionConfig() {
        return regionConfig;
    }
    public void setRegionConfig(Map<String, RegionConfig> regionConfig) {
        this.regionConfig = regionConfig;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    @JsonIgnore
    public String getLastDeploymentBucket() {
        RegionConfig regionConfig = getDefaultRegionConfig();
        return regionConfig == null ? null : regionConfig.getBucket();
    }

    @JsonIgnore
    public String getLastDeploymentBucket(String regionId) {
        RegionConfig regionConfig = getRegionConfig(regionId);
        return regionConfig == null ? null : regionConfig.getBucket();
    }

    @JsonIgnore
    public void setLastDeploymentBucket(String bucketName) {
        RegionConfig regionConfig = getDefaultRegionConfig();
        if (regionConfig != null) {
            regionConfig.setBucket(bucketName);
        }
    }

    @JsonIgnore
    public String getLastDeploymentStack() {
        RegionConfig regionConfig = getDefaultRegionConfig();
        return regionConfig == null ? null : regionConfig.getStack();
    }

    @JsonIgnore
    public String getLastDeploymentStack(String regionId) {
        RegionConfig regionConfig = getRegionConfig(regionId);
        return regionConfig == null ? null : regionConfig.getStack();
    }

    @JsonIgnore
    public void setLastDeploymentStack(String stackName) {
        RegionConfig regionConfig = getDefaultRegionConfig();
        if (regionConfig != null) {
            regionConfig.setStack(stackName);
        }
    }

    /**
     * Create the path to the last deployment {@link RegionConfig} if it is null and return it.
     */
    @JsonIgnore
    private RegionConfig getDefaultRegionConfig() {
        if (regionConfig == null) {
            regionConfig = new HashMap<>();
        }
        if (lastDeploymentRegionId != null) {
            RegionConfig defaultRegionConfig = regionConfig.get(lastDeploymentRegionId);
            if (defaultRegionConfig == null) {
                defaultRegionConfig = new RegionConfig();
                regionConfig.put(lastDeploymentRegionId, defaultRegionConfig);
            }
            return defaultRegionConfig;
        }
        return null;
    }

    @JsonIgnore
    private RegionConfig getRegionConfig(String regionId) {
        if (regionConfig == null) {
            regionConfig = new HashMap<>();
        }
        return regionId == null ? null : regionConfig.get(regionId);
    }

    public static class RegionConfig {
        private String bucket;
        private String stack;
        public String getBucket() {
            return bucket;
        }
        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
        public String getStack() {
            return stack;
        }
        public void setStack(String stack) {
            this.stack = stack;
        }
    }
}
