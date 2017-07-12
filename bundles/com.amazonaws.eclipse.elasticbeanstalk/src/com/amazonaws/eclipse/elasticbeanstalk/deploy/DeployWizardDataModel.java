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
package com.amazonaws.eclipse.elasticbeanstalk.deploy;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.trace;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;
import com.amazonaws.services.ec2.model.KeyPairInfo;

public class DeployWizardDataModel {

    // Bean property name constants
    public static final String EXISTING_APPLICATION_NAME = "existingApplicationName";
    public static final String CREATING_NEW_APPLICATION = "creatingNewApplication";
    public static final String NEW_APPLICATION_NAME = "newApplicationName";
    public static final String NEW_APPLICATION_DESCRIPTION = "newApplicationDescription";

    public static final String VERSION_RELEASE_LABEL = "versionReleaseLabel";
    public static final String VERSION_DESCRIPTION = "versionDescription";

    public static final String EXISTING_S3_BUCKET_NAME = "existingS3BucketName";
    public static final String CREATING_NEW_S3_BUCKET = "creatingNewS3Bucket";
    public static final String NEW_S3_BUCKET_NAME = "newS3BucketName";

    public static final String NEW_ENVIRONMENT_NAME = "newEnvironmentName";
    public static final String NEW_ENVIRONMENT_DESCRIPTION = "newEnvironmentDescription";
    public static final String ENVIRONMENT_TYPE = "environmentType";

    public static final String USING_KEY_PAIR = "usingKeyPair";
    public static final String KEY_PAIR = "keyPair";

    public static final String USING_CNAME = "usingCname";
    public static final String CNAME = "cname";

    public static final String INCREMENTAL_DEPLOYMENT = "incrementalDeployment";

    public static final String SNS_ENDPOINT = "snsEndpoint";
    public static final String SSL_CERTIFICATE_ID = "sslCertificateId";
    public static final String HEALTH_CHECK_URL = "healthCheckUrl";

    public static final String USE_NON_DEFAULT_VPC = "useNonDefaultVpc";
    public static final String VPC_ID = "vpcId";
    public static final String ELB_SCHEME = "elbScheme";
    public static final String SECURITY_GROUP = "securityGroup";
    public static final String ASSOCIATE_PUBLIC_IP_ADDRESS = "associatePublicIpAddress";

    public static final String INSTANCE_ROLE_NAME = "instanceRoleName";
    public static final String SERVICE_ROLE_NAME = "serviceRoleName";

    public static final String REGION_ENDPOINT = "regionEndpoint";

    public static final String WORKER_QUEUE_URL = "workerQueueUrl";

    private Region region;

    private String existingApplicationName;
    private boolean isCreatingNewApplication;
    private String newApplicationName;
    private String newApplicationDescription;

    private String newEnvironmentName;
    private String newEnvironmentDescription;
    private String environmentType;

    private boolean usingCname = false;
    private String cname;

    private boolean usingKeyPair = false;
    private KeyPairInfo keyPair;

    private boolean incrementalDeployment = false;

    private String snsEndpoint;
    private String healthCheckUrl;
    private String sslCertificateId;

    private boolean useNonDefaultVpc;
    private String vpcId;
    private final Set<String> ec2Subnets = new HashSet<>();
    private final Set<String> elbSubnets = new HashSet<>();
    private String elbScheme;
    private String securityGroup;
    private boolean associatePublicIpAddress;

    private String instanceRoleName;
    private String serviceRoleName;

    private boolean skipIamRoleAndInstanceProfileCreation;

    private String workerQueueUrl;

    // Share reference to make is easy to update the composite when region
    // changed.
    private KeyPairComposite keyPairComposite;

    public boolean isIncrementalDeployment() {
        return incrementalDeployment;
    }

    public void setIncrementalDeployment(boolean b) {
        this.incrementalDeployment = b;
    }

    public boolean isUsingKeyPair() {
        return usingKeyPair;
    }

    public void setUsingKeyPair(boolean usingKeyPair) {
        trace("Setting using key pair = " + usingKeyPair);
        this.usingKeyPair = usingKeyPair;
    }

    public boolean isUsingCname() {
        return usingCname;
    }

    public void setUsingCname(boolean usingCname) {
        trace("Setting using cname = " + usingCname);
        this.usingCname = usingCname;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        trace("Setting cname = " + cname);
        this.cname = cname;
    }

    public KeyPairInfo getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPairInfo keyPair) {
        trace("Setting key pair = " + keyPair);
        this.keyPair = keyPair;
    }

    public String getApplicationName() {
        if (isCreatingNewApplication) {
            return newApplicationName;
        }
        return existingApplicationName;
    }

    public String getExistingApplicationName() {
        return existingApplicationName;
    }

    public void setExistingApplicationName(String existingApplicationName) {
        trace("Setting existing application name = " + existingApplicationName);
        this.existingApplicationName = existingApplicationName;
    }

    public boolean isCreatingNewApplication() {
        return isCreatingNewApplication;
    }

    public void setCreatingNewApplication(boolean isCreatingNewApplication) {
        trace("Setting creating new application = " + isCreatingNewApplication);
        this.isCreatingNewApplication = isCreatingNewApplication;
    }

    public String getNewApplicationName() {
        return newApplicationName;
    }

    public void setNewApplicationName(String newApplicationName) {
        trace("Setting new application name = " + newApplicationName);
        this.newApplicationName = newApplicationName;
    }

    public String getNewApplicationDescription() {
        return newApplicationDescription;
    }

    public void setNewApplicationDescription(String newApplicationDescription) {
        trace("Setting new application description = " + newApplicationDescription);
        this.newApplicationDescription = newApplicationDescription;
    }

    // Environment Options

    public String getEnvironmentName() {
        return newEnvironmentName;
    }

    public String getNewEnvironmentName() {
        return newEnvironmentName;
    }

    public void setNewEnvironmentName(String newEnvironmentName) {
        trace("Setting new environment name = " + newEnvironmentName);
        this.newEnvironmentName = newEnvironmentName;
    }

    public String getNewEnvironmentDescription() {
        return newEnvironmentDescription;
    }

    public void setNewEnvironmentDescription(String newEnvironmentDescription) {
        trace("Setting new environment description = " + newEnvironmentDescription);
        this.newEnvironmentDescription = newEnvironmentDescription;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getRegionEndpoint() {
        return getRegion().getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);
    }

    public String getSnsEndpoint() {
        return snsEndpoint;
    }

    public String getEc2Endpoint() {
        return getRegion().getServiceEndpoints().get(ServiceAbbreviations.EC2);
    }

    public void setSnsEndpoint(String snsEndpoint) {
        trace("Setting sns endpoint = " + snsEndpoint);
        this.snsEndpoint = snsEndpoint;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return this.region;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        trace("Setting healthcheck url = " + healthCheckUrl);
        this.healthCheckUrl = healthCheckUrl;
    }

    public String getSslCertificateId() {
        return sslCertificateId;
    }

    public void setSslCertificateId(String sslCertificateId) {
        trace("Setting ssl certificate id = " + sslCertificateId);
        this.sslCertificateId = sslCertificateId;
    }

    public void setKeyPairComposite(KeyPairComposite keyPairComposite) {
        this.keyPairComposite = keyPairComposite;
    }

    public KeyPairComposite getKeyPairComposite() {
        return keyPairComposite;
    }

    public String getVpcId() {
        return vpcId;
    }

    /**
     * Sets the optional VPC id to be used when creating environment.
     *
     * @param vpcId the VPC to be used when creating environment.
     */
    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public boolean isUseNonDefaultVpc() {
        return useNonDefaultVpc;
    }

    public void setUseNonDefaultVpc(boolean useNonDefaultVpc) {
        this.useNonDefaultVpc = useNonDefaultVpc;
    }

    public Set<String> getEc2Subnets() {
        return ec2Subnets;
    }

    public Set<String> getElbSubnets() {
        return elbSubnets;
    }

    public String getElbScheme() {
        return elbScheme;
    }

    public void setElbScheme(String elbScheme) {
        this.elbScheme = elbScheme;
    }

    public boolean isAssociatePublicIpAddress() {
        return associatePublicIpAddress;
    }

    public void setAssociatePublicIpAddress(boolean associatePublicIpAddress) {
        this.associatePublicIpAddress = associatePublicIpAddress;
    }

    /**
     * Sets the optional IAM role to use when launching this environment. Using a role will cause it
     * to be available on the EC2 instances running as part of the Beanstalk environment, and allow
     * applications to securely access credentials from that role.
     *
     * @param role
     *            The role with which to launch EC2 instances in the Beanstalk environment.
     */
    public void setInstanceRoleName(String roleName) {
        this.instanceRoleName = roleName;
    }

    /**
     * Returns the optional IAM role to use when launching this environment. Using a role will cause
     * the role's security credentials to be securely distributed to the EC2 instances running as
     * part of the Beanstalk environment.
     *
     * @return The optional role name with which to launch EC2 instances in the Beanstalk
     *         environment.
     */
    public String getInstanceRoleName() {
        return instanceRoleName;
    }

    /**
     * Returns the name of the optional IAM role that the service (ElasticBeanstalk) is allowed to
     * impersonate. Currently this is only used for Enhanced Health reporting/monitoring
     *
     * @return The name of the role that ElasticBeanstalk can assume
     */
    public String getServiceRoleName() {
        return serviceRoleName;
    }

    /**
     * Sets the name of the optional IAM role that the service (ElasticBeanstalk) is allowed to
     * impersonate. Currently this is only used for Enhanced Health reporting/monitoring
     *
     * @param serviceRoleName
     *            The name of the role that ElasticBeanstalk can assume
     */
    public void setServiceRoleName(String serviceRoleName) {
        this.serviceRoleName = serviceRoleName;
    }

    /**
     * Returns true if the name of the IAM role/Instance Profile is directly provided via user input
     * and that the plugin should not attempt to re-create them.
     */
    public boolean isSkipIamRoleAndInstanceProfileCreation() {
        return skipIamRoleAndInstanceProfileCreation;
    }

    public void setSkipIamRoleAndInstanceProfileCreation(boolean skipIamRoleAndInstanceProfileCreation) {
        this.skipIamRoleAndInstanceProfileCreation = skipIamRoleAndInstanceProfileCreation;
    }

    public String getWorkerQueueUrl() {
        return workerQueueUrl;
    }

    public void setWorkerQueueUrl(String value) {
        workerQueueUrl = value;
    }
}
