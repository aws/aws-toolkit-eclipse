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

import java.util.Map;

import com.amazonaws.eclipse.lambda.serverless.model.Resource;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;

public class ServerlessApi extends Resource {
    private String stageName;
    private String definitionUri;
    private Boolean cacheClusterEnabled;
    private Integer cacheClusterSize;
    private Map<String, String> variables;

    public String getStageName() {
        return stageName;
    }
    public void setStageName(String stageName) {
        this.stageName = stageName;
    }
    public String getDefinitionUri() {
        return definitionUri;
    }
    public void setDefinitionUri(String definitionUri) {
        this.definitionUri = definitionUri;
    }
    public Boolean getCacheClusterEnabled() {
        return cacheClusterEnabled;
    }
    public void setCacheClusterEnabled(Boolean cacheClusterEnabled) {
        this.cacheClusterEnabled = cacheClusterEnabled;
    }
    public Integer getCacheClusterSize() {
        return cacheClusterSize;
    }
    public void setCacheClusterSize(Integer cacheClusterSize) {
        this.cacheClusterSize = cacheClusterSize;
    }
    public Map<String, String> getVariables() {
        return variables;
    }
    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
    @Override
    public TypeProperties toTypeProperties() {
        // TODO Auto-generated method stub
        return null;
    }
}
