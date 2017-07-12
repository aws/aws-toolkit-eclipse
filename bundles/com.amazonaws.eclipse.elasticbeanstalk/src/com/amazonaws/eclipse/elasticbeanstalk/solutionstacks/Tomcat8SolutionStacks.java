/*
 * Copyright 2011-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackDescription;

class Tomcat8SolutionStacks {

    /**
     * The String constant for Tomcat 8. This is used as the fall-back value for
     * Tomcat 8, if we fail to retrieve the latest solution-stack name from the
     * DescribeSolutionStacks API.
     */
    private static final String TOMCAT_8_64BIT_AMAZON_LINUX_v2_5_2 = "64bit Amazon Linux 2016.09 v2.5.2 running Tomcat 8 Java 8";

    private static final String SIX_FOUR_BIT_PREFIX = "64bit Amazon Linux ";
    private static final String TOMCAT_8_Java_8_SUFFIX = " running Tomcat 8 Java 8";

    /**
     * Look up the latest solution stack name for Tomcat 7, by using the
     * DescribeSolutionStacks API.
     *
     * @return The solution stack name returned by the
     *         ListAvailableSolutionStacks API, that has the latest internal
     *         version number.
     */
    public static String lookupLatestSolutionStackName() {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory()
                .getElasticBeanstalkClient();

        return lookupLatestSolutionStackName(client);
    }

    /**
     * Look up the latest solution stack name for Tomcat 8, by using the
     * DescribeSolutionStacks API.
     */
    static String lookupLatestSolutionStackName(AWSElasticBeanstalk client) {
        try {

            List<String> tomcat8Java864bitStacks = new ArrayList<>();

            for (SolutionStackDescription ss : client.listAvailableSolutionStacks().getSolutionStackDetails()) {
                String ssName = ss.getSolutionStackName();

                if (ssName != null &&
                    ssName.startsWith(SIX_FOUR_BIT_PREFIX) &&
                    ssName.endsWith(TOMCAT_8_Java_8_SUFFIX)) {
                    tomcat8Java864bitStacks.add(ssName);
                }
            }

            if ( !tomcat8Java864bitStacks.isEmpty() ) {
                Collections.sort(tomcat8Java864bitStacks);
                // The last element in lexicographically ascending order
                String latest = tomcat8Java864bitStacks.get(tomcat8Java864bitStacks.size() - 1);

                AwsToolkitCore.getDefault().logInfo(
                        "Found the latest solution stack name: " + latest);
                return latest;
            }

            AwsToolkitCore.getDefault().logInfo(
                    "Unabled to look up the latest solution stack name for Tomcat 8.");

        } catch (Exception e) {
            AwsToolkitCore.getDefault().logInfo(
                    "Unabled to look up the latest solution stack name for Tomcat 8."
                            + e.getMessage());
        }

        // returns the hard-coded string constant as fall-back
        return TOMCAT_8_64BIT_AMAZON_LINUX_v2_5_2;
    }

}
