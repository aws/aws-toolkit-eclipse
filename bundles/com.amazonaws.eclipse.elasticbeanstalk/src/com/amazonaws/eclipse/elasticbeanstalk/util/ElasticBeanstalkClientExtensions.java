/*
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.util;

import java.util.List;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;

/**
 * Wrapper around Beanstalk client to add several convenience extension methods
 */
public class ElasticBeanstalkClientExtensions {

    private final AWSElasticBeanstalk client;

    public ElasticBeanstalkClientExtensions(Environment environment) {
        this(environment.getClient());
    }

    public ElasticBeanstalkClientExtensions(AWSElasticBeanstalk client) {
        this.client = client;
    }

    /**
     * @param environmentName
     *            Name of environment to get CNAME for
     * @return Environment's CNAME
     */
    public String getEnvironmentCname(String environmentName) {
        EnvironmentDescription environmentDesc = getEnvironmentDescription(environmentName);
        if (environmentDesc == null) {
            return null;
        } else {
            return environmentDesc.getCNAME();
        }
    }

    /**
     * @param environmentName
     * @return {@link EnvironmentDescription} for a given environment
     */
    public EnvironmentDescription getEnvironmentDescription(String environmentName) {
        List<EnvironmentDescription> environments = client.describeEnvironments(
                new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName)).getEnvironments();
        return getFirstOrNull(environments);
    }

    /**
     * @param applicationName
     * @param environmentName
     * @return {@link EnvironmentDescription} for a given application/environment
     */
    public EnvironmentDescription getEnvironmentDescription(String applicationName, String environmentName) {
        List<EnvironmentDescription> environments = client.describeEnvironments(
                new DescribeEnvironmentsRequest().withApplicationName(applicationName).withEnvironmentNames(
                        environmentName)).getEnvironments();
        return getFirstOrNull(environments);
    }

    /**
     * @param applicationName
     * @return {@link ApplicationDescription} of the specified application
     */
    public ApplicationDescription getApplicationDescription(String applicationName) {
        List<ApplicationDescription> applications = client.describeApplications(
                new DescribeApplicationsRequest().withApplicationNames(applicationName)).getApplications();
        return getFirstOrNull(applications);
    }

    /**
     * @param applicationName
     * @return Latest {@link ApplicationVersionDescription} for the specified application
     */
    public ApplicationVersionDescription getLatestApplicationVersionDescription(String applicationName) {
        List<ApplicationVersionDescription> applicationVersions = client.describeApplicationVersions(
                new DescribeApplicationVersionsRequest().withApplicationName(applicationName)).getApplicationVersions();
        return getFirstOrNull(applicationVersions);
    }

    /**
     * @param applicationName
     * @return True if application exists in Beanstalk
     */
    public boolean doesApplicationExist(String applicationName) {
        return getApplicationDescription(applicationName) != null;
    }

    /**
     * @param environmentName
     * @return True if environment exists in any application. False otherwise
     */
    public boolean doesEnvironmentExist(String environmentName) {
        EnvironmentDescription environment = getEnvironmentDescription(environmentName);
        if (environment == null) {
            return false;
        }
        return !isStatusTerminatedOrTerminating(environment.getStatus());
    }

    /**
     * @param applicationName
     * @param environmentName
     * @return True if environment exists in the specified application. False otherwise.
     */
    public boolean doesEnvironmentExist(String applicationName, String environmentName) {
        EnvironmentDescription environment = getEnvironmentDescription(applicationName, environmentName);
        if (environment == null) {
            return false;
        }
        return !isStatusTerminatedOrTerminating(environment.getStatus());
    }

    /**
     * @return The Beanstalk client this class is wrapping
     */
    public AWSElasticBeanstalk getClient() {
        return client;
    }

    /**
     * @param list
     * @return The first element of the list or null if the list provided is null or empty
     */
    private static <T> T getFirstOrNull(List<T> list) {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    private static boolean isStatusTerminatedOrTerminating(String status) {
        if (status.equals(EnvironmentStatus.Terminated.toString())
                || status.equals(EnvironmentStatus.Terminating.toString())) {
            return true;
        }
        return false;
    }
}
