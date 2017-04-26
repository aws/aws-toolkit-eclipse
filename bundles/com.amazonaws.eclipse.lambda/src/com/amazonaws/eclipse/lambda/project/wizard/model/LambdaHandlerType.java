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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.model.ComboBoxItemData;
import com.amazonaws.eclipse.lambda.UrlConstants;

/**
 * This enum indicates all the Request Handler types provisioned by AWS Lambda. The Handler Type combo box
 * in the "Create a new AWS Lambda Java Project" UI will take use of these values.
 */
public class LambdaHandlerType implements ComboBoxItemData {
    public static final LambdaHandlerType REQUEST_HANDLER = new LambdaHandlerType("Request Handler", UrlConstants.LAMBDA_REQUEST_HANDLER_DOC_URL);
    public static final LambdaHandlerType STREAM_REQUEST_HANDLER = new LambdaHandlerType("Stream Request Handler", UrlConstants.LAMBDA_STREAM_REQUEST_HANDLER_DOC_URL);

    private static final List<LambdaHandlerType> LIST = new ArrayList<LambdaHandlerType>();
    static {
        LIST.add(REQUEST_HANDLER);
        LIST.add(STREAM_REQUEST_HANDLER);
    }

    // Acting as the text in the combo box.
    private final String name;
    private final String docUrl;

    private LambdaHandlerType(String name, String docUrl) {
        this.name = name;
        this.docUrl = docUrl;
    }

    public static List<LambdaHandlerType> list() {
        return LIST;
    }

    public String getDocUrl() {
        return docUrl;
    }

    public String getName() {
        return name;
    }

}
