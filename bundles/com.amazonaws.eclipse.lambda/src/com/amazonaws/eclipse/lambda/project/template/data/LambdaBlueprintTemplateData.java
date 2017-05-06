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
package com.amazonaws.eclipse.lambda.project.template.data;

//TODO make it immutable
/*
 * Data model of Freemarker template for handler.java, handler-test.java, test-utils.java, and test-context.java
 */
public class LambdaBlueprintTemplateData {
    private String packageName;
    private String handlerClassName;
    private String handlerTestClassName;
    private String inputJsonFileName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public void setHandlerClassName(String handlerClassName) {
        this.handlerClassName = handlerClassName;
    }

    public String getHandlerTestClassName() {
        return handlerTestClassName;
    }

    public void setHandlerTestClassName(String handlerTestClassName) {
        this.handlerTestClassName = handlerTestClassName;
    }

    public String getInputJsonFileName() {
        return inputJsonFileName;
    }

    public void setInputJsonFileName(String inputJsonFileName) {
        this.inputJsonFileName = inputJsonFileName;
    }
}
