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
package com.amazonaws.eclipse.lambda.serverless.template;

/**
 * Freemarker template data for serverless-handler.ftl
 */
public class ServerlessHandlerTemplateData {

    private String packageName;
    private String inputFqcn;
    private String outputFqcn;
    private String className;

    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public String getInputFqcn() {
        return inputFqcn;
    }
    public void setInputFqcn(String inputFqcn) {
        this.inputFqcn = inputFqcn;
    }
    public String getOutputFqcn() {
        return outputFqcn;
    }
    public void setOutputFqcn(String outputFqcn) {
        this.outputFqcn = outputFqcn;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
}
