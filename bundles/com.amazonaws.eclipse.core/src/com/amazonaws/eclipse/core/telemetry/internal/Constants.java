/*
 * Copyright 2015 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core.telemetry.internal;

import com.amazonaws.regions.Regions;

public class Constants {
    /**
     * API Gateway endpoint for ErrorReport service.
     */
    public static final String ERROR_REPORT_SERVICE_ENDPOINT = "https://hshkx4p74l.execute-api.us-east-1.amazonaws.com/";
    public static final String AWS_TOOLKIT_FOR_ECLIPSE_PRODUCT_NAME = "aws-toolkit-for-eclipse";
    public static final String AWS_TOOLKIT_FOR_ECLIPSE_PRODUCT_NAME_TEST = "aws-toolkit-for-eclipse-test";

    /*
     * Debug setup
     */
    public static final String MOBILE_ANALYTICS_APP_TITLE_TEST = "eclipse-toolkit-test";
    public static final String MOBILE_ANALYTICS_APP_ID_TEST = "7acd90a2b0ea495aa08b83afad9d37ba";
    public static final String COGNITO_IDENTITY_POOL_ID_TEST = "us-east-1:f63ac181-d01d-4a8f-91b2-c6aac01f899b";

    /*
     * Error reporting prod setup
     */
    public static final String MOBILE_ANALYTICS_APP_TITLE_PROD = "eclipse-toolkit-prod";
    public static final String MOBILE_ANALYTICS_APP_ID_PROD = "f46ac3eb2a67407caf8352a3210bf8b0";
    public static final String COGNITO_IDENTITY_POOL_ID_PROD = "us-east-1:d4fdc07f-94fd-4cc6-94fb-f903657f05c1";
    
    /*
     * V2 setup
     */
    public static final String COGNITO_IDENTITY_POOL_ID_PROD_V2 = "us-east-1:820fd6d1-95c0-4ca4-bffb-3f01d32da842"; 
    
    /*
     * Java preferences system data key
     */
    public static final String JAVA_PREFERENCE_NODE_FOR_AWS_TOOLKIT_FOR_ECLIPSE = "aws-toolkit-for-eclipse";

    /*
     * Preference store location for persisting user data
     */
    public static final String MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY = "mobileAnalyticsClientId";
    public static final String COGNITO_IDENTITY_ID_PREF_STORE_KEY = "cognitoIdentityId";
    public static final String COGNITO_IDENTITY_POOL_ID_PREF_STORE_KEY = "cognitoIdentityPoolId";

    /*
     * For constructing client context header
     */
    public static final String CLIENT_CONTEXT_MAP_KEY_CLIENT_ID = "client_id";
    public static final String CLIENT_CONTEXT_MAP_KEY_APP_TITLE = "app_title";
    public static final String CLIENT_CONTEXT_MAP_KEY_PLATFORM_NAME = "platform";
    public static final String CLIENT_CONTEXT_MAP_KEY_PLATFORM_VERSION = "platform_version";
    public static final String CLIENT_CONTEXT_MAP_KEY_LOCALE = "locale";
    public static final String CLIENT_CONTEXT_MAP_KEY_SERVICE_NAME = "mobile_analytics";
    public static final String CLIENT_CONTEXT_MAP_KEY_APP_ID = "app_id";

    /*
     * See valid values for platform name:
     * http://docs.aws.amazon.com/mobileanalytics/latest/ug/PutEvents.html#putEvents-request-client-context-header
     */
    public static final String CLIENT_CONTEXT_ENV_PLATFORM_WINDOWS = "windows";
    public static final String CLIENT_CONTEXT_ENV_PLATFORM_MACOS = "macos";
    public static final String CLIENT_CONTEXT_ENV_PLATFORM_LINUX = "linux";

    public static final String SESSION_START_EVENT_TYPE = "session_start";
    public static final String SESSION_STOP_EVENT_TYPE = "session_stop";

    /*
     * Mobile Analytics service limits
     */
    public static final int MAX_ATTRIBUTES_AND_METRICS_PER_EVENT = 40;
    public static final int MAX_ATTRIBUTE_OR_METRIC_NAME_LENGTH = 50;
    public static final int MAX_ATTRIBUTE_VALUE_LENGTH = 200;

    /*
     * Default region for Cognito Identity service
     */
    public static final Regions COGNITO_IDENTITY_SERVICE_REGION = Regions.US_EAST_1;

    /*
     * Mobile Analytics service version
     * http://docs.aws.amazon.com/mobileanalytics/latest/ug/PutEvents.html#putEvents-requests
     */
    public static final String MOBILE_ANALYTICS_SERVICE_VERSION = "v2.0";
}
