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

import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeSolutionStacksRequest;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackComparisonOperator;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackDetail;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackFilter;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackFilterName;

class Tomcat7SolutionStacks {

    /**
     * The String constant for Tomcat 7. This is used as the fall-back value for
     * Tomcat 7, if we fail to retrieve the latest solution-stack name from the
     * DescribeSolutionStacks API.
     */
    private static final String TOMCAT_7_64BIT_AMAZON_LINUX_v1_0_6 = "64bit Amazon Linux 2014.03 v1.0.6 running Tomcat 7 Java 7";

    /**
     * Used for sanity check on the tomcat 7 solution stack name returned by
     * DescribeSolutionStacks
     */
    private static final String TOMCAT_7 = "Tomcat 7";

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
     * Look up the latest solution stack name for Tomcat 7, by using the
     * DescribeSolutionStacks API.
     */
    static String lookupLatestSolutionStackName(AWSElasticBeanstalk client) {
        try {
            List<SolutionStackDetail> solutionStacks = client
                    .describeSolutionStacks(new DescribeSolutionStacksRequest()
                        .withFilters(
                                new SolutionStackFilter()
                                    .withName(SolutionStackFilterName.Architecture)
                                    .withComparisonOperator(SolutionStackComparisonOperator.EQ)
                                    .withValueList("64bit"),
                                new SolutionStackFilter()
                                    .withName(SolutionStackFilterName.ProgrammingLanguageName)
                                    .withComparisonOperator(SolutionStackComparisonOperator.EQ)
                                    .withValueList("Java"),
                                new SolutionStackFilter()
                                    .withName(SolutionStackFilterName.ProgrammingLanguageVersion)
                                    .withComparisonOperator(SolutionStackComparisonOperator.EQ)
                                    .withValueList("7.0")))
                    .getSolutionStackDetails();

            if ( solutionStacks != null ) {
                for (SolutionStackDetail ss : solutionStacks) {

                    String ssName = ss.getSolutionStackName();

                    // Sanity check on the returned solution-stack name
                    if (ssName != null && ssName.contains(TOMCAT_7)) {
                        return ss.getSolutionStackName();
                    }
                }
            }

            AwsToolkitCore.getDefault().logInfo(
                    "Unabled to look up the latest solution stack name for Tomcat 7.");

        } catch (Exception e) {
            AwsToolkitCore.getDefault().logInfo(
                    "Unabled to look up the latest solution stack name for Tomcat 7."
                            + e.getMessage());
        }

        // returns the hard-coded string constant as fall-back
        return TOMCAT_7_64BIT_AMAZON_LINUX_v1_0_6;
    }

}
