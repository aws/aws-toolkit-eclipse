/*
 * Copyright 2011-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk;

public class SolutionStacks {
    private static final String TOMCAT_6_64BIT_AMAZON_LINUX = "64bit Amazon Linux running Tomcat 6";
    private static final String TOMCAT_7_64BIT_AMAZON_LINUX = "64bit Amazon Linux 2013.09 running Tomcat 7 Java 7";
    public static final String DEFAULT_SOLUTION_STACK = TOMCAT_7_64BIT_AMAZON_LINUX;

    public static String lookupSolutionStackByServerTypeId(String serverTypeId) {
        if (serverTypeId.equalsIgnoreCase("com.amazonaws.eclipse.elasticbeanstalk.servers.environment")) {
            return TOMCAT_6_64BIT_AMAZON_LINUX;
        } else if (serverTypeId.equalsIgnoreCase("com.amazonaws.eclipse.elasticbeanstalk.servers.tomcat7")) {
            return TOMCAT_7_64BIT_AMAZON_LINUX;
        }

        throw new RuntimeException("Unknown server type: " + serverTypeId);
    }

    public static String lookupServerTypeIdBySolutionStack(String solutionStack) {
        if (solutionStack.contains(" Tomcat 6")) {
            return ElasticBeanstalkPlugin.TOMCAT_6_SERVER_TYPE_ID;
        } else if (solutionStack.contains(" Tomcat 7")) {
            return ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID;
        }

        throw new RuntimeException("Unsupported solution stack: " + solutionStack);
    }

}
