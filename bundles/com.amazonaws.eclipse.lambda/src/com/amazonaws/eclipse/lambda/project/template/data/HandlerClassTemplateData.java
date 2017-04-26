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

import java.util.LinkedList;
import java.util.List;

public class HandlerClassTemplateData extends AbstractHandlerClassTemplateData {

    private List<String> additionalImports;
    private String inputType;
    private String outputType;

    public List<String> getAdditionalImports() {
        return additionalImports;
    }

    public void addAdditionalImport(String additionalImport) {
        if (additionalImports == null) {
            additionalImports = new LinkedList<String>();
        }
        additionalImports.add(additionalImport);
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

}
