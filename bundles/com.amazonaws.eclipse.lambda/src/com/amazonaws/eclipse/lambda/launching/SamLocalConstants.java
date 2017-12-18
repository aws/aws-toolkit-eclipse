/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.launching;

/**
 * Constants used in SAM Local feature, mostly are attribute constants of SAM Local Launch configurations.
 */
public class SamLocalConstants {

    // Preference name for AWS SAM Local executable
    public static String P_SAM_LOCAL_EXECUTABLE = "AwsSamLocalExecutable";

    // SAM Local process type. This is configured in plugin.xml
    public static String PROCESS_TYPE = "sam_local";

    // Attribute names for executing a SAM Local command.
    public static String A_SAM = "sam";
    public static String A_MAVEN_GOALS = "maven-goals";
    public static String A_ACTION = "action";
    public static String A_PROJECT = "project";

    // These attribute names must be identical to those of the command line parameter names
    public static String A_PROFILE = "profile";
    public static String A_TEMPLATE = "template";
    public static String A_ENV_VARS = "env-vars";
    public static String A_DEBUG_PORT = "debug-port";
    public static String A_REGION = "region";
    public static String A_CODE_URI = "code-uri";
    public static String A_TIME_OUT = "timeout";

    // Attributes for `sam local invoke` only
    public static String A_LAMBDA_IDENTIFIER = "lambda-id";
    public static String A_EVENT = "event";

    // Attributes for `sam local start-api` only
    public static String A_PORT = "port";
    public static String A_HOST = "host";

    // Doc links
    public static String LINKS_INSTALL_SAM_LOCAL = "https://github.com/awslabs/aws-sam-local#installation";
    public static String LINKS_SAM_LOCAL_ANNOUNCEMENT = "https://aws.amazon.com/about-aws/whats-new/2017/08/introducing-aws-sam-local-a-cli-tool-to-test-aws-lambda-functions-locally/";
}
