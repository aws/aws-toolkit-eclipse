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
package com.amazonaws.eclipse.elasticbeanstalk.solutionstacks;

import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;

public class SolutionStacks {

    /**
     * The String constant for Tomcat 6.
     */
    private static final String TOMCAT_6_64BIT_AMAZON_LINUX = "64bit Amazon Linux running Tomcat 6";

    public static String lookupSolutionStackByServerTypeId(String serverTypeId) {
        if (serverTypeId.equalsIgnoreCase(ElasticBeanstalkPlugin.TOMCAT_6_SERVER_TYPE_ID)) {
            return getSolutionStackNameByTomcatVersion(TomcatVersion.TOMCAT_6);
        } else if (serverTypeId.equalsIgnoreCase(ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID)) {
            return getSolutionStackNameByTomcatVersion(TomcatVersion.TOMCAT_7);
        } else if (serverTypeId.equalsIgnoreCase(ElasticBeanstalkPlugin.TOMCAT_8_SERVER_TYPE_ID)) {
            return getSolutionStackNameByTomcatVersion(TomcatVersion.TOMCAT_8);
        }

        throw new RuntimeException("Unknown server type: " + serverTypeId);
    }

    public static String lookupServerTypeIdBySolutionStack(String solutionStack) {
        if (solutionStack.contains(" Tomcat 6")) {
            return ElasticBeanstalkPlugin.TOMCAT_6_SERVER_TYPE_ID;
        } else if (solutionStack.contains(" Tomcat 7")) {
            return ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID;
        } else if (solutionStack.contains(" Tomcat 8")) {
            return ElasticBeanstalkPlugin.TOMCAT_8_SERVER_TYPE_ID;
        }


        throw new RuntimeException("Unsupported solution stack: " + solutionStack);
    }

    public static String getDefaultSolutionStackName() {
        return getSolutionStackNameByTomcatVersion(TomcatVersion.TOMCAT_8);
    }

    /**
     * Returns the appropriate solution stack name String to use to create an
     * Elastic Beanstalk environment running the specified Tomcat version.
     *
     * @param version
     *            Enumeration of the Tomcat software version.
     */
    private static String getSolutionStackNameByTomcatVersion(TomcatVersion version) {
        switch (version) {
            case TOMCAT_6:
                return TOMCAT_6_64BIT_AMAZON_LINUX;
            case TOMCAT_7:
                return Tomcat7SolutionStacks.lookupLatestSolutionStackName();
            case TOMCAT_8:
                return Tomcat8SolutionStacks.lookupLatestSolutionStackName();
            default:
                throw new RuntimeException("Unknown Tomcat version: " + version);
        }
    }

}
