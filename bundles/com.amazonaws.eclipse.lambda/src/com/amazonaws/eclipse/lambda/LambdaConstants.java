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
package com.amazonaws.eclipse.lambda;

import com.amazonaws.eclipse.core.ui.WebLinkListener;

public class LambdaConstants {

    public static final String LAMBDA_JAVA_HANDLER_DOC_URL = "https://docs.aws.amazon.com/lambda/latest/dg/programming-model.html";
    public static final String LAMBDA_EXECUTION_ROLE_DOC_URL = "https://docs.aws.amazon.com/lambda/latest/dg/intro-permission-model.html#lambda-intro-execution-role";
    public static final String CLOUDFORMATION_CAPABILITIES = "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-iam-template.html#capabilities";
    public static final String LAMBDA_REQUEST_HANDLER_DOC_URL = "https://docs.aws.amazon.com/lambda/latest/dg/java-programming-model-req-resp.html";
    public static final String LAMBDA_STREAM_REQUEST_HANDLER_DOC_URL = "https://docs.aws.amazon.com/lambda/latest/dg/java-handler-io-type-stream.html";
    public static final String LAMBDA_FUNCTION_VERSIONING_AND_ALIASES_URL = "http://docs.aws.amazon.com/lambda/latest/dg/versioning-aliases.html";
    public static final String LAMBDA_FUNCTION_ENCRYPTION_URL = "http://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html";

    public static final String LAMBDA_PROJECT_DECORATOR_ID = "com.amazonaws.eclipse.lambda.project.lambdaFunctionProjectDecorator";

    public static final WebLinkListener webLinkListener = new WebLinkListener();

    public static final int MAX_LAMBDA_TAGS = 50;
    public static final int MAX_LAMBDA_TAG_KEY_LENGTH = 127;
    public static final int MAX_LAMBDA_TAG_VALUE_LENGTH = 255;
}
