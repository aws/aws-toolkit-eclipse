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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerlessTemplate {
    @JsonProperty("AWSTemplateFormatVersion")
    private String AWSTemplateFormatVersion;

    @JsonProperty("Transform")
    private String transform;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Resources")
    private Map<String, TypeProperties> resources;
    
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("AWSTemplateFormatVersion")
    public String getAWSTemplateFormatVersion() {
        return AWSTemplateFormatVersion;
    }
    @JsonProperty("AWSTemplateFormatVersion")
    public void setAWSTemplateFormatVersion(String aWSTemplateFormatVersion) {
        AWSTemplateFormatVersion = aWSTemplateFormatVersion;
    }
    public String getTransform() {
        return transform;
    }
    public void setTransform(String transform) {
        this.transform = transform;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Map<String, TypeProperties> getResources() {
        if (resources == null) {
            resources = new HashMap<String, TypeProperties>();
        }
        return resources;
    }
    public void setResources(Map<String, TypeProperties> resources) {
        this.resources = resources;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
