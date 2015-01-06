/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.elasticbeanstalk.solutionstacks.SolutionStacks;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentHealth;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.s3.AmazonS3;

public class ElasticBeanstalkPublishingUtils {

    /** Duration (in milliseconds) before we give up polling for deployment status */
    private static final int POLLING_TIMEOUT = 1000 * 60 * 20;

    /** Period (in milliseconds) between attempts to poll */
    private static final int PAUSE = 1000 * 15;

    public static final String DEFAULT_ROLE_NAME = "aws-elasticbeanstalk-ec2-role";

    private final AWSElasticBeanstalk beanstalkClient;
    private final AmazonS3 s3;
    private final Environment environment;
    private final AmazonIdentityManagement iam;

    public ElasticBeanstalkPublishingUtils(Environment environment) {
        this.environment = environment;

        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(environment.getAccountId());
        Region environmentRegion = RegionUtils.getRegionByEndpoint(environment.getRegionEndpoint());

        this.beanstalkClient = clientFactory.getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        this.iam = clientFactory.getIAMClientByEndpoint(environmentRegion.getServiceEndpoint(ServiceAbbreviations.IAM));
        this.s3 = clientFactory.getS3ClientByEndpoint(environmentRegion.getServiceEndpoint(ServiceAbbreviations.S3));
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
     * Creates a new Elastic Beanstalk application, after checking to make sure
     * it doesn't already exist.
     *
     * @param applicationName
     *            The name of the application to create.
     * @param description
     *            An optional description for the new environment.
     */
    public void createNewApplication(String applicationName, String description) {
        List<ApplicationDescription> applications = beanstalkClient.describeApplications(
                new DescribeApplicationsRequest().withApplicationNames(applicationName)).getApplications();

        if (applications.isEmpty()) {
            beanstalkClient.createApplication(new CreateApplicationRequest(applicationName).withDescription(description));
        }
    }

    /**
     * Returns the version label for the latest version registered for the
     * specified application.
     *
     * @param applicationName
     *            The name of the application whose latest version should be
     *            returned.
     *
     * @return The label of the latest version registered for the specified
     *         application, or null if no versions are registered.
     *
     * @throws AmazonServiceException
     *             If the specified application doesn't exist.
     */
    public String getLatestApplicationVersion(String applicationName) {
        List<ApplicationVersionDescription> applicationVersions = beanstalkClient.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(applicationName)).getApplicationVersions();

        if (applicationVersions.isEmpty()) {
            return null;
        }

        return applicationVersions.get(0).getVersionLabel();
    }

    private static final Map<String, String> ENV_TIER_MAP;
    static {
        Map<String, String> map = new HashMap<String, String>();

        map.put(ConfigurationOptionConstants.WEB_SERVER, "Standard");
        map.put(ConfigurationOptionConstants.WORKER, "SQS/HTTP");

        ENV_TIER_MAP = Collections.unmodifiableMap(map);
    }

    private static final Map<String, String> ENV_TYPE_MAP;
    static {
        Map<String, String> map = new HashMap<String, String>();

        map.put(ConfigurationOptionConstants.SINGLE_INSTANCE_ENV, "SingleInstance");
        map.put(ConfigurationOptionConstants.LOAD_BALANCED_ENV, "LoadBalanced");

        ENV_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Creates a new environment, using the specified version as the initial
     * version to deploy.
     *
     * @param versionLabel
     *            The initial version to deploy to the new environment.
     */
    public void createNewEnvironment(String versionLabel) {

        String solutionStackName = environment.getSolutionStack();
        if (solutionStackName == null) {
            solutionStackName = SolutionStacks.getDefaultSolutionStackName();
        }

        CreateEnvironmentRequest request = new CreateEnvironmentRequest()
            .withApplicationName(environment.getApplicationName())
            .withSolutionStackName(solutionStackName)
            .withDescription(environment.getEnvironmentDescription())
            .withEnvironmentName(environment.getEnvironmentName())
            .withVersionLabel(versionLabel);

        List<ConfigurationOptionSetting> optionSettings = new ArrayList<ConfigurationOptionSetting>();
        if ( environment.getKeyPairName() != null && environment.getKeyPairName().length() > 0 ) {
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
        if ( environment.getEnvironmentTier() != null && environment.getEnvironmentTier().length() > 0 ) {
            request.setTier(new EnvironmentTier()
                .withName(environment.getEnvironmentTier())
                .withType(ENV_TIER_MAP.get(environment.getEnvironmentTier()))
                .withVersion("1.0"));
        }
        if ( environment.getEnvironmentType() != null && environment.getEnvironmentType().length() > 0 ) {
            optionSettings.add(new ConfigurationOptionSetting()
                .withNamespace(ConfigurationOptionConstants.ENVIRONMENT_TYPE)
                .withOptionName("EnvironmentType")
                .withValue(ENV_TYPE_MAP.get(environment.getEnvironmentType())));
        }
        if ( environment.getIamRoleName() != null && environment.getIamRoleName().length() > 0 ) {
            String iamInstanceProfileOpValue = null;

            if (environment.isSkipIamRoleAndInstanceProfileCreation()) {
                // Use name of the instance profile directly provided by the user
                iamInstanceProfileOpValue = environment.getIamRoleName();

            } else {
                // Create the role/instance-profile if necessary
                InstanceProfile instanceProfile = configureInstanceProfile();
                if (instanceProfile != null) {
                    iamInstanceProfileOpValue = instanceProfile.getArn();
                }

            }

            if (iamInstanceProfileOpValue != null) {
                optionSettings.add(new ConfigurationOptionSetting()
                        .withNamespace(ConfigurationOptionConstants.LAUNCHCONFIGURATION)
                        .withOptionName("IamInstanceProfile")
                        .withValue(iamInstanceProfileOpValue));
            }
        }
        if ( environment.getWorkerQueueUrl() != null && environment.getWorkerQueueUrl().length() > 0 ) {
            optionSettings.add(new ConfigurationOptionSetting()
                .withNamespace(ConfigurationOptionConstants.SQSD)
                .withOptionName("WorkerQueueURL")
                .withValue(environment.getWorkerQueueUrl()));
        }

        if (optionSettings.size() > 0) {
            request.setOptionSettings(optionSettings);
        }

        if (environment.getCname() != null && environment.getCname().length() > 0) {
            request.setCNAMEPrefix(environment.getCname());
        }

        beanstalkClient.createEnvironment(request);
    }

    private InstanceProfile configureInstanceProfile() {
        String roleName = environment.getIamRoleName();

        String ASSUME_ROLE_POLICY = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";
        String DEFAULT_ACCESS_POLICY = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"sqs:*\"],\"Resource\":\"*\"},{\"Effect\":\"Allow\",\"Action\":[\"cloudwatch:*\"],\"Resource\":\"*\"}]}";

        Role role = getRole(roleName);
        if (role == null) {
            if (roleName.equals(DEFAULT_ROLE_NAME)) {
                role = iam.createRole(new CreateRoleRequest().withRoleName(roleName).withPath("/").withAssumeRolePolicyDocument(ASSUME_ROLE_POLICY)).getRole();
                iam.putRolePolicy(new PutRolePolicyRequest()
                    .withRoleName(roleName)
                    .withPolicyName("DefaultAccessPolicy-" + roleName)
                    .withPolicyDocument(DEFAULT_ACCESS_POLICY));
            } else {
                Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Selected IAM role doesn't exit: " + roleName);
                ElasticBeanstalkPlugin.getDefault().getLog().log(status);
                return null;
            }
        }

        InstanceProfile instanceProfile = getInstanceProfile(roleName);
        if (instanceProfile == null) {
            instanceProfile = iam.createInstanceProfile(new CreateInstanceProfileRequest().withInstanceProfileName(roleName).withPath("/")).getInstanceProfile();
            iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest().withInstanceProfileName(roleName).withRoleName(roleName));
        }
        return instanceProfile;
    }

    private InstanceProfile getInstanceProfile(String profileName) throws AmazonClientException {
        try {
            return iam.getInstanceProfile(new GetInstanceProfileRequest().withInstanceProfileName(profileName)).getInstanceProfile();
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("NoSuchEntity")) {
                return null;
            }
            throw ase;
        }
    }

    private Role getRole(String roleName) throws AmazonClientException {
        try {
            return iam.getRole(new GetRoleRequest().withRoleName(roleName)).getRole();
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("NoSuchEntity")) {
                return null;
            }
            throw ase;
        }
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
            if (monitor.isCanceled()) {
                return;
            }

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
        if (monitor.isCanceled()) {
            throw new CoreException(new Status(Status.CANCEL, ElasticBeanstalkPlugin.PLUGIN_ID, "Canceled"));
        }
    }

    private boolean isLaunchingNewEnvironment(String environmentName) {
        List<EnvironmentDescription> environments = beanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName)).getEnvironments();
        if (environments.isEmpty()) {
            return true;
        }
        return environments.get(0).getStatus().equals(EnvironmentStatus.Launching.toString());
    }

    public boolean doesEnvironmentExist(AWSElasticBeanstalk beanstalkClient, String environmentName) {
        List<EnvironmentDescription> environments = beanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest()
            .withEnvironmentNames(environmentName)).getEnvironments();

        if (environments.isEmpty()) {
            return false;
        }

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