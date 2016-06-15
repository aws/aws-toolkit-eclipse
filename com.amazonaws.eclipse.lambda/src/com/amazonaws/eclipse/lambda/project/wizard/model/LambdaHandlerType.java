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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import com.amazonaws.eclipse.lambda.UrlConstants;

/**
 * This enum indicates all the Request Handler types provisioned by AWS Lambda. The Handler Type combo box
 * in the "Create a new AWS Lambda Java Project" UI will take use of these values.
 */
public enum LambdaHandlerType {
    // The order of the handler types matters that the first handler type in this enum will be chosen
    // as the default selection for the lambda handler combo.
    REQUEST_HANDLER("Request Handler", UrlConstants.LAMBDA_REQUEST_HANDLER_DOC_URL),
    STREAM_REQUEST_HANDLER("Stream Request Handler", UrlConstants.LAMBDA_STREAM_REQUEST_HANDLER_DOC_URL)
    ;

    private final String name;
    private final String docUrl;

    LambdaHandlerType(String name, String docUrl) {
        this.name = name;
        this.docUrl = docUrl;
    }

    public String getName() {
        return name;
    }

    public String getDocUrl() {
        return docUrl;
    }

}
