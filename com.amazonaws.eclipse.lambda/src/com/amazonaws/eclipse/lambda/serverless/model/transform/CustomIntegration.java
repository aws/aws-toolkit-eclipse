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

import java.util.List;
import java.util.Map;

public class CustomIntegration {
    private Map<String, String> requestTemplates;
    private String cacheNamespace;
    private List<String> cacheKeyParameters;
    private Map<String, Response> responses;

    public Map<String, String> getRequestTemplates() {
        return requestTemplates;
    }
    public void setRequestTemplates(Map<String, String> requestTemplates) {
        this.requestTemplates = requestTemplates;
    }
    public String getCacheNamespace() {
        return cacheNamespace;
    }
    public void setCacheNamespace(String cacheNamespace) {
        this.cacheNamespace = cacheNamespace;
    }
    public List<String> getCacheKeyParameters() {
        return cacheKeyParameters;
    }
    public void setCacheKeyParameters(List<String> cacheKeyParameters) {
        this.cacheKeyParameters = cacheKeyParameters;
    }
    public Map<String, Response> getResponses() {
        return responses;
    }
    public void setResponses(Map<String, Response> responses) {
        this.responses = responses;
    }

}
