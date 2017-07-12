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
import com.amazonaws.eclipse.elasticbeanstalk.resources.BeanstalkResourceProvider;
import com.amazonaws.eclipse.elasticbeanstalk.solutionstacks.SolutionStacks;
import com.amazonaws.eclipse.elasticbeanstalk.util.BeanstalkConstants;
import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
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
import com.amazonaws.util.StringUtils;

public class ElasticBeanstalkPublishingUtils {

    /** Duration (in milliseconds) before we give up polling for deployment status */
    private static final int POLLING_TIMEOUT = 1000 * 60 * 20;

    /** Period (in milliseconds) between attempts to poll */
    private static final int PAUSE = 1000 * 15;

    private final BeanstalkResourceProvider resourceProvider = new BeanstalkResourceProvider();
    private final AWSElasticBeanstalk beanstalkClient;
    private final ElasticBeanstalkClientExtensions beanstalkClientExtensions;
    private final AmazonS3 s3;
    private final Environment environment;
    private final AmazonIdentityManagement iam;

    public ElasticBeanstalkPublishingUtils(Environment environment) {
        this.environment = environment;

        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(environment.getAccountId());

        this.beanstalkClient = clientFactory.getElasticBeanstalkClientByRegion(environment.getRegionId());
        this.beanstalkClientExtensions = new ElasticBeanstalkClientExtensions(beanstalkClient);
        this.iam = clientFactory.getIAMClientByRegion(environment.getRegionId());
        this.s3 = clientFactory.getS3ClientByRegion(environment.getRegionId());
    }

    public void publishApplicationToElasticBeanstalk(IPath war, String versionLabel, IProgressMonitor monitor)
            throws CoreException {
        trace("Publishing application to AWS Elastic Beanstalk");

        monitor.beginTask("Deploying application with AWS Elastic Beanstalk", 100);

        String bucketName;
        String applicationName = environment.getApplicationName();
        String environmentName = environment.getEnvironmentName();
        String key = formVersionKey(applicationName, versionLabel);

        checkForCancellation(monitor);

        long deployStartTime = System.currentTimeMillis();

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

            long startTime = System.currentTimeMillis();
            s3.putObject(bucketName, key, war.toFile());
            long endTime = System.currentTimeMillis();

            ElasticBeanstalkAnalytics.trackUploadMetrics(endTime - startTime, war.toFile().length());

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
                    .withApplicationName(applicationName).withAutoCreateApplication(true)
                    .withDescription(environment.getApplicationDescription()).withVersionLabel(versionLabel)
                    .withSourceBundle(new S3Location().withS3Bucket(bucketName).withS3Key(key)));
            checkForCancellation(monitor);
            monitor.worked(40);
        } catch (AmazonClientException ace) {
            throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to register application version with AWS Elastic Beanstalk: " + ace.getMessage(), ace));
        }

        trace("Updating environment");
        monitor.setTaskName("Updating environment with latest version");
        if (beanstalkClientExtensions.doesEnvironmentExist(environmentName)) {
            try {
                beanstalkClient.updateEnvironment(new UpdateEnvironmentRequest().withEnvironmentName(environmentName)
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

        ElasticBeanstalkAnalytics.trackDeployTotalTime(System.currentTimeMillis() - deployStartTime);

        trace("Done publishing to AWS Elastic Beanstalk, waiting for env to become available...");
        checkForCancellation(monitor);
        monitor.worked(20);
        monitor.done();
    }

    /**
     * Creates a new Elastic Beanstalk application, after checking to make sure it doesn't already
     * exist.
     *
     * @param applicationName
     *            The name of the application to create.
     * @param description
     *            An optional description for the new environment.
     */
    public void createNewApplication(String applicationName, String description) {
        if (beanstalkClientExtensions.doesApplicationExist(applicationName)) {
            beanstalkClient.createApplication(new CreateApplicationRequest(applicationName)
                    .withDescription(description));
        }
    }

    /**
     * Returns the version label for the latest version registered for the specified application.
     *
     * @param applicationName
     *            The name of the application whose latest version should be returned.
     * @return The label of the latest version registered for the specified application, or null if
     *         no versions are registered.
     * @throws AmazonServiceException
     *             If the specified application doesn't exist.
     */
    public String getLatestApplicationVersion(String applicationName) {
        ApplicationVersionDescription applicationVersion = beanstalkClientExtensions
                .getLatestApplicationVersionDescription(applicationName);
        if (applicationVersion == null) {
            return null;
        }
        return applicationVersion.getVersionLabel();
    }

    private static final Map<String, String> ENV_TIER_MAP;
    static {
        Map<String, String> map = new HashMap<>();

        map.put(ConfigurationOptionConstants.WEB_SERVER, "Standard");
        map.put(ConfigurationOptionConstants.WORKER, "SQS/HTTP");

        ENV_TIER_MAP = Collections.unmodifiableMap(map);
    }

    private static final Map<String, String> ENV_TYPE_MAP;
    static {
        Map<String, String> map = new HashMap<>();

        map.put(ConfigurationOptionConstants.SINGLE_INSTANCE_ENV, "SingleInstance");
        map.put(ConfigurationOptionConstants.LOAD_BALANCED_ENV, "LoadBalanced");

        ENV_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Creates a new environment, using the specified version as the initial version to deploy.
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
                .withApplicationName(environment.getApplicationName()).withSolutionStackName(solutionStackName)
                .withDescription(environment.getEnvironmentDescription())
                .withEnvironmentName(environment.getEnvironmentName()).withVersionLabel(versionLabel);

        List<ConfigurationOptionSetting> optionSettings = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(environment.getKeyPairName())) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.LAUNCHCONFIGURATION)
                    .withOptionName("EC2KeyName").withValue(environment.getKeyPairName()));
        }
        if (!StringUtils.isNullOrEmpty(environment.getHealthCheckUrl())) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.APPLICATION)
                    .withOptionName("Application Healthcheck URL").withValue(environment.getHealthCheckUrl()));
        }
        if (!StringUtils.isNullOrEmpty(environment.getSslCertificateId())) {
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.LOADBALANCER).withOptionName("SSLCertificateId")
                    .withValue(environment.getSslCertificateId()));
        }
        if (!StringUtils.isNullOrEmpty(environment.getSnsEndpoint())) {
            optionSettings.add(new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.SNS_TOPICS)
                    .withOptionName("Notification Endpoint").withValue(environment.getSnsEndpoint()));
        }
        if (!StringUtils.isNullOrEmpty(environment.getEnvironmentTier())) {
            request.setTier(new EnvironmentTier().withName(environment.getEnvironmentTier())
                    .withType(ENV_TIER_MAP.get(environment.getEnvironmentTier())).withVersion("1.0"));
        }
        if (!StringUtils.isNullOrEmpty(environment.getEnvironmentType())) {
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.ENVIRONMENT_TYPE).withOptionName("EnvironmentType")
                    .withValue(ENV_TYPE_MAP.get(environment.getEnvironmentType())));
        }
        if (!StringUtils.isNullOrEmpty(environment.getInstanceRoleName())) {

            if (!environment.isSkipIamRoleAndInstanceProfileCreation()) {
                // Create the role/instance-profile if necessary
                tryConfigureRoleAndInstanceProfile();
            }

            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.LAUNCHCONFIGURATION)
                    .withOptionName("IamInstanceProfile").withValue(environment.getInstanceRoleName()));
        }
        if (!StringUtils.isNullOrEmpty(environment.getServiceRoleName())) {
            if (!environment.isSkipIamRoleAndInstanceProfileCreation()
                    && environment.getServiceRoleName().equals(BeanstalkConstants.DEFAULT_SERVICE_ROLE_NAME)) {
                tryCreateDefaultServiceRoleIfNotExists();
            }
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.ENVIRONMENT_TYPE).withOptionName("ServiceRole")
                    .withValue(environment.getServiceRoleName()));
            if (doesSolutionStackSupportEnhancedHealth(solutionStackName)) {
                optionSettings.add(new ConfigurationOptionSetting()
                        .withNamespace(ConfigurationOptionConstants.HEALTH_REPORTING_SYSTEM)
                        .withOptionName("SystemType").withValue("enhanced"));
            }
        }
        if (!StringUtils.isNullOrEmpty(environment.getVpcId())) {
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.VPC)
                    .withOptionName("VPCId")
                    .withValue(environment.getVpcId()));
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.VPC)
                    .withOptionName("AssociatePublicIpAddress")
                    .withValue(String.valueOf(environment.getAssociatePublicIpAddress())));
            if (!StringUtils.isNullOrEmpty(environment.getSubnets())) {
                optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.VPC)
                    .withOptionName("Subnets")
                    .withValue(environment.getSubnets()));
            }
            if (!StringUtils.isNullOrEmpty(environment.getElbSubnets())) {
                optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.VPC)
                    .withOptionName("ELBSubnets")
                    .withValue(environment.getElbSubnets()));
            }
            if (!StringUtils.isNullOrEmpty(environment.getElbScheme())
                    && BeanstalkConstants.ELB_SCHEME_INTERNAL.equals(environment.getElbScheme())) {
                optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.VPC)
                    .withOptionName("ELBScheme")
                    .withValue(environment.getElbScheme()));
            }
            if (!StringUtils.isNullOrEmpty(environment.getSecurityGroup())) {
                optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.LAUNCHCONFIGURATION)
                    .withOptionName("SecurityGroups")
                    .withValue(environment.getSecurityGroup()));
            }
        }
        if (StringUtils.isNullOrEmpty(environment.getWorkerQueueUrl())) {
            optionSettings.add(new ConfigurationOptionSetting()
                    .withNamespace(ConfigurationOptionConstants.SQSD)
                    .withOptionName("WorkerQueueURL")
                    .withValue(environment.getWorkerQueueUrl()));
        }

        if (optionSettings.size() > 0) {
            request.setOptionSettings(optionSettings);
        }

        if (!StringUtils.isNullOrEmpty(environment.getCname())) {
            request.setCNAMEPrefix(environment.getCname());
        }

        beanstalkClient.createEnvironment(request);
    }

    private boolean doesSolutionStackSupportEnhancedHealth(String solutionStackName) {
        return !solutionStackName.contains("Tomcat 6");
    }

    private void tryCreateDefaultServiceRoleIfNotExists() {
        try {
            String permissionsPolicy = resourceProvider.getServiceRolePermissionsPolicy().asString();
            String trustPolicy = resourceProvider.getServiceRoleTrustPolicy().asString();
            createRoleIfNotExists(BeanstalkConstants.DEFAULT_SERVICE_ROLE_NAME, permissionsPolicy, trustPolicy);
        } catch (Exception e) {
            Status status = new Status(Status.WARNING, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to create default service role. "
                            + "Proceeding with deployment under the assumption it already exists", e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    private void createRoleIfNotExists(String roleName, String permissionsPolicy, String trustPolicy) {
        if (!doesRoleExist(roleName)) {
            iam.createRole(new CreateRoleRequest().withRoleName(roleName).withPath("/")
                    .withAssumeRolePolicyDocument(trustPolicy));
            iam.putRolePolicy(new PutRolePolicyRequest().withRoleName(roleName)
                    .withPolicyName("DefaultAccessPolicy-" + roleName).withPolicyDocument(permissionsPolicy));
        }
    }

    private boolean doesRoleExist(String roleName) {
        return getRole(BeanstalkConstants.DEFAULT_SERVICE_ROLE_NAME) != null;
    }

    private void tryConfigureRoleAndInstanceProfile() {
        try {
            configureRoleAndInstanceProfile();
        } catch (Exception e) {
            Status status = new Status(Status.WARNING, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to create default instance role or instance profile. "
                            + "Proceeding with deployment under the assumption they already exist.", e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    private void configureRoleAndInstanceProfile() {
        String roleName = environment.getInstanceRoleName();

        if (BeanstalkConstants.DEFAULT_INSTANCE_ROLE_NAME.equals(roleName)) {
            String permissionsPolicy = resourceProvider.getInstanceProfilePermissionsPolicy().asString();
            String trustPolicy = resourceProvider.getInstanceProfileTrustPolicy().asString();
            createRoleIfNotExists(BeanstalkConstants.DEFAULT_INSTANCE_ROLE_NAME, permissionsPolicy, trustPolicy);
        }

        String instanceProfileName = roleName;

        // The console automatically creates an instance profile with the same name when you create
        // an EC2 IAM role. If no instance profile exists then the user most likely created the role
        // through the API so we create the instance profile on their behalf
        InstanceProfile instanceProfile = createInstanceProfileIfNotExists(instanceProfileName);

        // If an instance profile that has the same name as the role exists but doesn't have that
        // role attached then add it here. This can really only happen if the user is creating the
        // role and instance profile separately through the
        // API
        if (!isRoleAddedToInstanceProfile(roleName, instanceProfile)) {
            iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest().withInstanceProfileName(
                    instanceProfileName).withRoleName(roleName));
        }
    }

    /**
     * Create the instance profile with the specified name or return it if it already exists.
     *
     * @param instanceProfileName
     * @return InstanceProfile object of either the existing instance profile or the newly created
     *         one
     */
    private InstanceProfile createInstanceProfileIfNotExists(String instanceProfileName) {
        InstanceProfile instanceProfile = getInstanceProfile(instanceProfileName);
        if (instanceProfile == null) {
            instanceProfile = iam.createInstanceProfile(
                    new CreateInstanceProfileRequest().withInstanceProfileName(instanceProfileName).withPath("/"))
                    .getInstanceProfile();
        }
        return instanceProfile;
    }

    private InstanceProfile getInstanceProfile(String profileName) throws AmazonClientException {
        try {
            return iam.getInstanceProfile(new GetInstanceProfileRequest().withInstanceProfileName(profileName))
                    .getInstanceProfile();
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

    private boolean isRoleAddedToInstanceProfile(String roleName, InstanceProfile instanceProfile) {
        for (Role role : instanceProfile.getRoles()) {
            if (role.getRoleName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public void waitForEnvironmentToBecomeAvailable(IModule moduleToPublish, IProgressMonitor monitor, Runnable runnable)
            throws CoreException {
        int errorCount = 0;
        long startTime = System.currentTimeMillis();
        Date eventStartTime = new Date(startTime - (1000 * 60 * 30));

        String applicationName = environment.getApplicationName();
        String environmentName = environment.getEnvironmentName();
        boolean launchingNewEnvironment = isLaunchingNewEnvironment(environmentName);

        monitor.beginTask("Waiting for environment to become available", POLLING_TIMEOUT);
        monitor.setTaskName("Waiting for environment to become available");

        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT) {
            List<EventDescription> events = beanstalkClient.describeEvents(
                    new DescribeEventsRequest().withEnvironmentName(environmentName).withStartTime(eventStartTime))
                    .getEvents();
            if (events.size() > 0) {
                String status = "Latest Event: " + events.get(0).getMessage();
                if (launchingNewEnvironment) {
                    status += "  (Note: Launching a new environment may take several minutes)";
                } else {
                    status += "  (Note: Updating an environment may take several minutes)";
                }
                monitor.setTaskName(status);
            }

            try {
                Thread.sleep(PAUSE);
            } catch (Exception e) {
            }
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
                EnvironmentDescription environmentDesc = beanstalkClientExtensions
                        .getEnvironmentDescription(environmentName);
                if (environmentDesc != null) {
                    trace(" - " + environmentDesc.getStatus());

                    EnvironmentStatus environmentStatus = null;
                    try {
                        environmentStatus = EnvironmentStatus.fromValue(environmentDesc.getStatus());
                    } catch (IllegalArgumentException e) {
                        Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                                "Unknown environment status: " + environmentDesc.getStatus());
                        StatusManager.getManager().handle(status, StatusManager.LOG);
                        continue;
                    }

                    switch (environmentStatus) {
                    case Ready:
                        trace("   - Health: " + environmentDesc.getHealth());
                        if (EnvironmentHealth.Green.toString().equalsIgnoreCase(environmentDesc.getHealth())) {
                            trace("**Server started**");
                            Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                                    "Deployed application '" + applicationName + "' " + "to environment '"
                                            + environmentName + "' " + "\nApplication available at: "
                                            + environmentDesc.getCNAME());
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
        EnvironmentDescription environmentDesc = beanstalkClientExtensions.getEnvironmentDescription(environmentName);
        if (environmentDesc == null) {
            return true;
        }
        return environmentDesc.getStatus().equals(EnvironmentStatus.Launching.toString());
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
