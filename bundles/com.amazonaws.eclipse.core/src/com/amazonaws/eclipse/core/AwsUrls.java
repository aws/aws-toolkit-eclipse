/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core;

/**
 * Commonly used AWS URL constants.
 */
public class AwsUrls {
    public static final String TRACKING_PARAMS = "utm_source=eclipse&utm_medium=ide&utm_campaign=awstoolkitforeclipse";

    public static final String SIGN_UP_URL = "http://aws.amazon.com/register" + "?" + TRACKING_PARAMS;
    public static final String SECURITY_CREDENTIALS_URL = "https://console.aws.amazon.com/iam/home?#security_credential" + "?" + TRACKING_PARAMS;

    public static final String JAVA_DEVELOPMENT_FORUM_URL = "http://developer.amazonwebservices.com/connect/forum.jspa?forumID=70" + "&" + TRACKING_PARAMS;

    public static final String AWS_TOOLKIT_FOR_ECLIPSE_HOMEPAGE_URL = "http://aws.amazon.com/eclipse" + "?" + TRACKING_PARAMS;
    public static final String AWS_TOOLKIT_FOR_ECLIPSE_FAQ_URL = "http://aws.amazon.com/eclipse/faqs/" + "?" + TRACKING_PARAMS;
    public static final String AWS_TOOLKIT_FOR_ECLIPSE_GITHUB_URL = "https://github.com/amazonwebservices/aws-toolkit-for-eclipse/";

    public static final String AWS_MANAGEMENT_CONSOLE_URL = "http://aws.amazon.com/console" + "?" + TRACKING_PARAMS;
}
