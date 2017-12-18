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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.eclipse.lambda.serverless.model.Resource;
import com.amazonaws.eclipse.lambda.serverless.model.ResourceType;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerlessFunction extends Resource {

    private static final String DEFAULT_RUNTIME = "java8";
    private static final Integer DEFAULT_TIMEOUT = 300;
    private static final Integer DEFAULT_MEMORY_SIZE = 512;

    @JsonProperty("Handler")
    private String handler;

    @JsonProperty("Runtime")
    private String runtime;

    @JsonProperty("CodeUri")
    private String codeUri;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("MemorySize")
    private Integer memorySize;

    @JsonProperty("Timeout")
    private Integer timeout;

    @JsonProperty("Role")
    private JsonNode role;

    @JsonProperty("Policies")
    private final List<String> policies = new ArrayList<>();

    @JsonIgnore
    // These are additional properties in the Type, Properties level.
    private final Map<String, Object> additionalTopLevelProperties = new HashMap<>();

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getRuntime() {
        return runtime == null ? DEFAULT_RUNTIME : runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getCodeUri() {
        return codeUri;
    }

    public void setCodeUri(String codeUri) {
        this.codeUri = codeUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMemorySize() {
        return memorySize == null ? DEFAULT_MEMORY_SIZE : memorySize;
    }

    public void setMemorySize(Integer memorySize) {
        this.memorySize = memorySize;
    }

    public Integer getTimeout() {
        return timeout == null ? DEFAULT_TIMEOUT : timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public JsonNode getRole() {
        return role;
    }

    public void setRole(JsonNode role) {
        this.role = role;
    }

    /**
     * @return non-null
     */
    public List<String> getPolicies() {
        return policies;
    }

    public void addPolicy(String policy) {
        this.policies.add(policy);
    }

    /**
     * @return non-null
     */
    public Map<String, Object> getAdditionalTopLevelProperties() {
        return additionalTopLevelProperties;
    }

    public void addAdditionalTopLevelProperty(String key, Object value) {
        this.additionalTopLevelProperties.put(key, value);
    }

    @Override
    public TypeProperties toTypeProperties() {
        TypeProperties tp = new TypeProperties();
        tp.setType(ResourceType.AWS_SERVERLESS_FUNCTION.toString());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> properties = mapper.convertValue(this, Map.class);
        for (Entry<String, Object> entry : properties.entrySet()) {
            tp.addProperty(entry.getKey(), entry.getValue());
        }

        Map<String, Object> topLevelProperties = getAdditionalTopLevelProperties();
        for (Entry<String, Object> entry : topLevelProperties.entrySet()) {
            tp.addAdditionalProperty(entry.getKey(), entry.getValue());
        }

        return tp;
    }

    @Override
    public String toString() {
        return "ServerlessFunction [handler=" + handler + ", runtime=" + runtime + ", codeUri=" + codeUri
                + ", description=" + description + ", memorySize=" + memorySize + ", timeout=" + timeout + ", role="
                + role + ", policies=" + policies + ", getAdditionalProperties()=" + getAdditionalProperties() + "]";
    }

}
