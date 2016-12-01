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

public class Response {
    private String statusCode;
    private Map<String, String> responseTemplates;
    private Map<String, String> responseParameters;

    public String getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    public Map<String, String> getResponseTemplates() {
        return responseTemplates;
    }
    public void setResponseTemplates(Map<String, String> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }
    public Map<String, String> getResponseParameters() {
        return responseParameters;
    }
    public void setResponseParameters(Map<String, String> responseParameters) {
        this.responseParameters = responseParameters;
    }
}
