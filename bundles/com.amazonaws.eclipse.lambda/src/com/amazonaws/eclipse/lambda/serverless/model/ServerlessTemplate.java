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
package com.amazonaws.eclipse.lambda.serverless.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerlessTemplate extends AdditionalProperties {
    @JsonProperty("AWSTemplateFormatVersion")
    private String AWSTemplateFormatVersion;

    @JsonProperty("Transform")
    private List<String> transform;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Resources")
    private final Map<String, TypeProperties> resources = new HashMap<>();

    @JsonProperty("AWSTemplateFormatVersion")
    public String getAWSTemplateFormatVersion() {
        return AWSTemplateFormatVersion;
    }

    @JsonProperty("AWSTemplateFormatVersion")
    public void setAWSTemplateFormatVersion(String aWSTemplateFormatVersion) {
        AWSTemplateFormatVersion = aWSTemplateFormatVersion;
    }

    public List<String> getTransform() {
        return transform;
    }

    public void setTransform(List<String> transform) {
        this.transform = transform;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return non-null
     */
    public Map<String, TypeProperties> getResources() {
        return resources;
    }

    public void addResource(String key, TypeProperties resource) {
        this.resources.put(key, resource);
    }
}
