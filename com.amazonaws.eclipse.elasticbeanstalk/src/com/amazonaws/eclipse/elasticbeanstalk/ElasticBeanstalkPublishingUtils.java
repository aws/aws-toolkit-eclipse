/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.trace;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentHealth;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3;

public class ElasticBeanstalkPublishingUtils {

    /** Duration (in milliseconds) before we give up polling for deployment status */
    private static final int POLLING_TIMEOUT = 1000 * 60 * 20;

    /** Period (in milliseconds) between attempts to poll */
    private static final int PAUSE = 1000 * 15;

    private final AWSElasticBeanstalk beanstalkClient;
    private final AmazonS3 s3;
    private final Environment environment;

    public ElasticBeanstalkPublishingUtils(AWSElasticBeanstalk beanstalkClient, AmazonS3 s3, Environment environment) {
        this.beanstalkClient = beanstalkClient;
        this.s3 = s3;
        this.environment = environment;
    }

    public void publishApplicationToElasticBeanstalk(IPath war, String versionLabel, IProgressMonitor monitor) throws CoreException {
        trace("Publishing application to AWS Elastic Beanstalk");

        monitor.beginTask("Deploying application with AWS Elastic Beanstalk", 100);

        String bucketName;
        String applicationName = environment.getApplicationName();
        String environmentName = environment.getEnvironmentName();
        String key             = formVersionKey(applicationName, versionLabel);

        checkForCancellation(monitor);

        try {
            bucketName = beanstalkClient.createStorageLocation().getS3Bucket();

            if (s3.doesBucketExist(bucketName) == false) {
                trace("Creating Amazon S3 bucket");
                monitor.setTaskName("Creating Amazon S3 bucket");
                s3.createBucket(bucketName);
                checkForCancellation(monitor);
            }

            trace("Uploading application to Amazon S3");
            monitor.setTaskName("Uploading application to Amazon S3");
            s3.putObject(bucketName, key, war.toFile());
            checkForCancellation(monitor);
            monitor.worked(40);
        } catch (AmazonClientException ace) {
            throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Unable to upload application to Amazon S3: " + ace.getMessage(), ace));
        }

        try {
            trace("Registering new application version from " + war.toOSString());
            monitor.setTaskName("Registering application version " + versionLabel);

            beanstalkClient.createApplicationVersion(new CreateApplicationVersionRequest()
                .withApplicationName(applicationName)
                .withAutoCreateApplication(true)
                .withDescription(environment.getApplicationDescription())
                .withVersionLabel(versionLabel)
                .withSourceBundle(new S3Location().withS3Bucket(bucketName).withS3Key(key)));
            checkForCancellation(monitor);
            monitor.worked(40);
        } catch (AmazonClientException ace) {
            throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Unable to register application version with AWS Elastic Beanstalk: " + ace.getMessage(), ace));
        }


        trace("Updating environment");
        monitor.setTaskName("Updating environment with latest version");
        if (doesEnvironmentExist(beanstalkClient, environmentName)) {
            try {
                beanstalkClient.updateEnvironment(new UpdateEnvironmentRequest()
                    .withEnvironmentName(environmentName)
                    .withVersionLabel(versionLabel));
            } catch (AmazonClientException ace) {
                throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to update environment with new application version: " + ace.getMessage(), ace));
            }
        } else {
            try {
                createNewEnvironment(versionLabel);
            } catch (AmazonClientException ace) {
                throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to create new environment: " + ace.getMessage(), ace));
            }
        }

        trace("Done publishing to AWS Elastic Beanstalk, waiting for env to become available...");
        checkForCancellation(monitor);
        monitor.worked(20);
        monitor.done();
    }

    /**
     * Creates a new environment
     */
    void createNewEnvironment(String versionLabel) {
        String solutionStackName = environment.getSolutionStack();
        if (solutionStackName == null) {
        	solutionStackName = SolutionStacks.TOMCAT_6_64BIT_AMAZON_LINUX;
        }

        CreateEnvironmentRequest request = new CreateEnvironmentRequest()
            .withApplicationName(environment.getApplicationName())
            .withSolutionStackName(solutionStackName)
            .withDescription(environment.getEnvironmentDescription())
            .withEnvironmentName(environment.getEnvironmentName())
            .withVersionLabel(versionLabel);

        List<ConfigurationOptionSetting> optionSettings = new ArrayList<ConfigurationOptionSetting>();
        if ( environment.getKeyPairName() != null ) {
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace("aws:autoscaling:launchconfiguration").withOptionName("EC2KeyName")
                    .withValue(environment.getKeyPairName()));
        }
        if ( environment.getHealthCheckUrl() != null && environment.getHealthCheckUrl().length() > 0 ) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.APPLICATION)
                    .withOptionName("Application Healthcheck URL").withValue(environment.getHealthCheckUrl()));
        }
        if ( environment.getSslCertificateId() != null && environment.getSslCertificateId().length() > 0 ) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.LOADBALANCER)
                    .withOptionName("SSLCertificateId").withValue(environment.getSslCertificateId()));
        }
        if ( environment.getSnsEndpoint() != null && environment.getSnsEndpoint().length() > 0 ) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.SNS_TOPICS)
                    .withOptionName("Notification Endpoint").withValue(environment.getSnsEndpoint()));
        }

        if (optionSettings.size() > 0) request.setOptionSettings(optionSettings);

        if (environment.getCname() != null) {
            request.setCNAMEPrefix(environment.getCname());
        }

        beanstalkClient.createEnvironment(request);
    }

    public void waitForEnvironmentToBecomeAvailable(IModule moduleToPublish, IProgressMonitor monitor, Runnable runnable) throws CoreException {
        int errorCount = 0;
        long startTime = System.currentTimeMillis();
        Date eventStartTime = new Date(startTime - (1000 * 60 * 30));

        String applicationName = environment.getApplicationName();
        String environmentName = environment.getEnvironmentName();
        boolean launchingNewEnvironment = isLaunchingNewEnvironment(environmentName);

        monitor.beginTask("Waiting for environment to become available", POLLING_TIMEOUT);
        monitor.setTaskName("Waiting for environment to become available");

        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT) {
            List<EventDescription> events = beanstalkClient.describeEvents(new DescribeEventsRequest().withEnvironmentName(environmentName).withStartTime(eventStartTime)).getEvents();
            if (events.size() > 0) {
                String status = "Latest Event: " + events.get(0).getMessage();
                if (launchingNewEnvironment) {
                    status += "  (Note: Launching a new environment may take several minutes)";
                } else {
                    status += "  (Note: Updating an environment may take several minutes)";
                }
                monitor.setTaskName(status);
            }

            try {Thread.sleep(PAUSE);} catch (Exception e) {}
            if (monitor.isCanceled()) return;

            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }

            try {
                trace("Polling environment for status...");
                DescribeEnvironmentsRequest request = new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName);
                List<EnvironmentDescription> environments = beanstalkClient.describeEnvironments(request).getEnvironments();
                if (environments.size() > 0) {
                    EnvironmentDescription environment = environments.get(0);
                    trace(" - " + environment.getStatus());

                    EnvironmentStatus environmentStatus = null;
                    try {
                        environmentStatus = EnvironmentStatus.fromValue(environment.getStatus());
                    } catch (IllegalArgumentException e) {
                        Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                            "Unknown environment status: " + environment.getStatus());
                        StatusManager.getManager().handle(status, StatusManager.LOG);
                        continue;
                    }

                    switch (environmentStatus) {
                        case Ready:
                            trace("   - Health: " + environment.getHealth());
                            if (EnvironmentHealth.Green.toString().equalsIgnoreCase(environment.getHealth())) {
                                trace("**Server started**");
                                Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                                    "Deployed application '" + applicationName + "' " +
                                    "to environment '" + environmentName + "' " +
                                    "\nApplication available at: " + environment.getCNAME());
                                StatusManager.getManager().handle(status, StatusManager.LOG);
                                return;
                            }
                            break;
                        case Terminated:
                        case Terminating:
                            throw new CoreException(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                                "Environment failed to deploy.  Check environment events for more details."));
                    }
                }

                // reset error count so that we only count consecutive errors
                errorCount = 0;
            } catch (AmazonClientException ace) {
                if (errorCount++ > 4) {
                    throw new CoreException(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                        "Unable to detect application deployment: " + ace.getMessage(), ace));
                }
            }
        }

        throw new CoreException(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
            "Unable to detect application deployment"));
    }

    private void checkForCancellation(IProgressMonitor monitor) throws CoreException {
        if (monitor.isCanceled())
            throw new CoreException(new Status(Status.CANCEL, ElasticBeanstalkPlugin.PLUGIN_ID, "Canceled"));
    }

    private boolean isLaunchingNewEnvironment(String environmentName) {
        List<EnvironmentDescription> environments = beanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName)).getEnvironments();
        if (environments.isEmpty()) return true;
        return environments.get(0).getStatus().equals(EnvironmentStatus.Launching.toString());
    }

    private boolean doesEnvironmentExist(AWSElasticBeanstalk beanstalkClient, String environmentName) {
        List<EnvironmentDescription> environments = beanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest()
            .withEnvironmentNames(environmentName)).getEnvironments();

        if (environments.isEmpty()) return false;

        String status = environments.get(0).getStatus();
        return (!status.equals(EnvironmentStatus.Terminated.toString()) &&
                !status.equals(EnvironmentStatus.Terminating.toString()));
    }

    /**
     * Returns the key under which to store an uploaded application version.
     */
    private String formVersionKey(String applicationName, String versionLabel) {
        try {
            return URLEncoder.encode(applicationName + "-" + versionLabel + ".war", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return applicationName + "-" + versionLabel + ".war";
        }
    }
}
