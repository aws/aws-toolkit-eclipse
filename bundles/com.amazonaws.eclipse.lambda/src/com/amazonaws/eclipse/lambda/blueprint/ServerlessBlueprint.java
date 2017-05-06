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
package com.amazonaws.eclipse.lambda.blueprint;

import java.util.Map;

public class ServerlessBlueprint {
    private String baseDir;
    private String displayName;
    private String description;
    private Map<String, String> handlerTemplatePaths;
    private boolean needLambdaProxyIntegrationModel;

    public String getBaseDir() {
        return baseDir;
    }
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Map<String, String> getHandlerTemplatePaths() {
        return handlerTemplatePaths;
    }
    public void setHandlerTemplatePaths(Map<String, String> handlerTemplatePaths) {
        this.handlerTemplatePaths = handlerTemplatePaths;
    }
    public boolean isNeedLambdaProxyIntegrationModel() {
        return needLambdaProxyIntegrationModel;
    }
    public void setNeedLambdaProxyIntegrationModel(
            boolean needLambdaProxyIntegrationModel) {
        this.needLambdaProxyIntegrationModel = needLambdaProxyIntegrationModel;
    }
}
