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
package com.amazonaws.eclipse.lambda.serverless.model.transform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.lambda.serverless.model.AdditionalProperties;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerlessModel extends AdditionalProperties {
    private static final String DEFAULT_AWS_TEMPLATE_FORMAT_VERSION = "2010-09-09";
    private static final List<String> DEFAULT_TRANSFORM = Arrays.asList("AWS::Serverless-2016-10-31");

    @JsonProperty("AWSTemplateFormatVersion")
    private String awsTemplateFormatVersion;
    private List<String> transform;
    private String description;

    private final Map<String, ServerlessFunction> serverlessFunctions = new HashMap<>();
    // A map of Lambda handler name to Lambda function physical ID
    private final Map<String, String> serverlessFunctionPhysicalIds = new HashMap<>();
    // Unrecognized resources
    private final Map<String, TypeProperties> additionalResources = new HashMap<>();

    /**
     * @return non-null
     */
    public Map<String, ServerlessFunction> getServerlessFunctions() {
        return serverlessFunctions;
    }

    @JsonProperty("AWSTemplateFormatVersion")
    public String getAWSTemplateFormatVersion() {
        if (awsTemplateFormatVersion == null) {
            awsTemplateFormatVersion = DEFAULT_AWS_TEMPLATE_FORMAT_VERSION;
        }
        return awsTemplateFormatVersion;
    }

    public Map<String, String> getServerlessFunctionPhysicalIds() {
        return serverlessFunctionPhysicalIds;
    }

    @JsonProperty("AWSTemplateFormatVersion")
    public void setAWSTemplateFormatVersion(String awsTemplateFormatVersion) {
        this.awsTemplateFormatVersion = awsTemplateFormatVersion;
    }

    public List<String> getTransform() {
        if (transform == null) {
            transform = DEFAULT_TRANSFORM;
        }
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

    public void addServerlessFunction(String key, ServerlessFunction serverlessFunction) {
        this.serverlessFunctions.put(key, serverlessFunction);
        this.serverlessFunctionPhysicalIds.put(serverlessFunction.getHandler(), key);
    }

    /**
     * @return non-null
     */
    public Map<String, TypeProperties> getAdditionalResources() {
        return additionalResources;
    }

    public void addAdditionalResource(String key, TypeProperties additionalResource) {
        this.additionalResources.put(key, additionalResource);
    }
}
