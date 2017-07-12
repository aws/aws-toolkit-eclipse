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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.Instance;

@SuppressWarnings("restriction")
public class Environment extends ServerDelegate {

    private static final String PROPERTY_REGION_ID                = "regionId";
    private static final String PROPERTY_REGION_ENDPOINT          = "regionEndpoint";
    private static final String PROPERTY_APPLICATION_NAME         = "applicationName";
    private static final String PROPERTY_APPLICATION_DESCRIPTION  = "applicationDescription";
    private static final String PROPERTY_ENVIRONMENT_NAME         = "environmentName";
    private static final String PROPERTY_ENVIRONMENT_TIER         = "environmentTier";
    private static final String PROPERTY_ENVIRONMENT_TYPE         = "environmentType";
    private static final String PROPERTY_ENVIRONMENT_DESCRIPTION  = "environmentDescription";
    private static final String PROPERTY_KEY_PAIR_NAME            = "keyPairName";
    private static final String PROPERTY_CNAME                    = "cname";
    private static final String PROPERTY_HEALTHCHECK_URL          = "healthcheckUrl";
    private static final String PROPERTY_SSL_CERT_ID              = "sslCertId";
    private static final String PROPERTY_ACCOUNT_ID               = "accountId";
    private static final String PROPERTY_SNS_ENDPOINT             = "snsEndpoint";
    private static final String PROPERTY_SOLUTION_STACK           = "solutionStack";
    private static final String PROPERTY_INCREMENTAL_DEPLOYMENT   = "incrementalDeployment";
    private static final String PROPERTY_INSTANCE_ROLE_NAME       = "instanceRoleName";
    private static final String PROPERTY_SERVICE_ROLE_NAME        = "serviceRoleName";
    private static final String PROPERTY_SKIP_IAM_ROLE_AND_INSTANCE_PROFILE_CREATION
                                                                  = "skipIamRoleAndInstanceProfileCreation";
    private static final String PROPERTY_WORKER_QUEUE_URL         = "workerQueueUrl";
    private static final String PROPERTY_VPC_ID                   = "vpcId";
    private static final String PROPERTY_SUBNETS                  = "subnets";
    private static final String PROPERTY_ELB_SUBNETS              = "elbSubnets";
    private static final String PROPERTY_ELB_SCHEME               = "elbScheme";
    private static final String PROPERTY_SECURITY_GROUP           = "securityGroup";
    private static final String PROPERTY_ASSOCIATE_PUBLIC_IP_ADDRESS = "associatePublicIpAddress";

    private static Map<String, EnvironmentDescription> map = new HashMap<>();

    @Override
    public void setDefaults(IProgressMonitor monitor) {
        // Disable auto publishing
        setAttribute("auto-publish-setting", 1);
    }

    public String getAccountId() {
        return getAttribute(PROPERTY_ACCOUNT_ID, (String)null);
    }

    public void setAccountId(String accountId) {
        setAttribute(PROPERTY_ACCOUNT_ID, accountId);
    }

    public String getRegionEndpoint() {
        return RegionUtils.getRegion(getRegionId()).getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);
    }

    public String getApplicationName() {
        return getAttribute(PROPERTY_APPLICATION_NAME, (String)null);
    }

    public void setApplicationName(String applicationName) {
        setAttribute(PROPERTY_APPLICATION_NAME, applicationName);
    }

    public String getApplicationDescription() {
        return getAttribute(PROPERTY_APPLICATION_NAME, (String)null);
    }

    public void setApplicationDescription(String applicationDescription) {
        setAttribute(PROPERTY_APPLICATION_DESCRIPTION, applicationDescription);
    }

    public String getEnvironmentTier() {
        return getAttribute(PROPERTY_ENVIRONMENT_TIER, (String) null);
    }

    public void setEnvironmentTier(String environmentTier) {
        setAttribute(PROPERTY_ENVIRONMENT_TIER, environmentTier);
    }

    public void setEnvironmentType(String environmentType) {
        setAttribute(PROPERTY_ENVIRONMENT_TYPE, environmentType);
    }

    public String getEnvironmentType() {
        return getAttribute(PROPERTY_ENVIRONMENT_TYPE, (String)null);
    }

    public String getEnvironmentName() {
        return getAttribute(PROPERTY_ENVIRONMENT_NAME, (String)null);
    }

    public void setEnvironmentName(String environmentName) {
        setAttribute(PROPERTY_ENVIRONMENT_NAME, environmentName);
    }

    public String getEnvironmentDescription() {
        return getAttribute(PROPERTY_ENVIRONMENT_DESCRIPTION, (String)null);
    }

    public void setEnvironmentDescription(String environmentDescription) {
        setAttribute(PROPERTY_ENVIRONMENT_DESCRIPTION, environmentDescription);
    }

    public String getEnvironmentUrl() {
        EnvironmentDescription cachedEnvironmentDescription = getCachedEnvironmentDescription();
        if (cachedEnvironmentDescription == null) {
            return null;
        }
        if (cachedEnvironmentDescription.getCNAME() == null) {
            return null;
        }
        return "http://" + cachedEnvironmentDescription.getCNAME();
    }

    public String getCname() {
        return getAttribute(PROPERTY_CNAME, (String)null);
    }

    public void setCname(String cname) {
        setAttribute(PROPERTY_CNAME, cname);
    }

    public String getKeyPairName() {
        return getAttribute(PROPERTY_KEY_PAIR_NAME, (String) null);
    }

    public void setKeyPairName(String keyPairName) {
        setAttribute(PROPERTY_KEY_PAIR_NAME, keyPairName);
    }

    public String getSslCertificateId() {
        return getAttribute(PROPERTY_SSL_CERT_ID, (String) null);
    }

    public void setSslCertificateId(String sslCertificateId) {
        setAttribute(PROPERTY_SSL_CERT_ID, sslCertificateId);
    }

    public String getHealthCheckUrl() {
        return getAttribute(PROPERTY_HEALTHCHECK_URL, (String)null);
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        setAttribute(PROPERTY_HEALTHCHECK_URL, healthCheckUrl);
    }

     public String getSnsEndpoint() {
       return getAttribute(PROPERTY_SNS_ENDPOINT, (String)null);
    }

    public void setSnsEndpoint(String snsEndpoint) {
        setAttribute(PROPERTY_SNS_ENDPOINT, snsEndpoint);
    }

    public String getSolutionStack() {
        return getAttribute(PROPERTY_SOLUTION_STACK, (String)null);
    }

    public void setSolutionStack(String solutionStackForServerType) {
        setAttribute(PROPERTY_SOLUTION_STACK, solutionStackForServerType);
    }

    public boolean getIncrementalDeployment() {
        return getAttribute(PROPERTY_INCREMENTAL_DEPLOYMENT, false);
    }

    public void setIncrementalDeployment(boolean incrementalDeployment) {
        setAttribute(PROPERTY_INCREMENTAL_DEPLOYMENT, incrementalDeployment);
    }

    public void setRegionId(String regionName) {
        setAttribute(PROPERTY_REGION_ID, regionName);
    }

    public String getRegionId() {
       return getAttribute(PROPERTY_REGION_ID, (String)null);
    }

    /**
     * Sets the name for the optional instance IAM role for this environment. If a role is
     * specified, the EC2 instances launched in this environment will have that
     * role available, including secure credentials distribution.
     *
     * @param instanceRoleName
     *            The name for a valid instance IAM role.
     */
    public void setInstanceRoleName(String instanceRoleName) {
        setAttribute(PROPERTY_INSTANCE_ROLE_NAME, instanceRoleName);
    }

    /**
     * Returns the name of the optional IAM role for this environment. If a role
     * is specified, the EC2 instances launched in this environment will have
     * that role available, including secure credentials distribution.
     *
     * @return The name of a valid IAM role.
     */
    public String getInstanceRoleName() {
        return getAttribute(PROPERTY_INSTANCE_ROLE_NAME, (String)null);
    }

    /**
     * Sets the name of the optional IAM role that the service (ElasticBeanstalk) is allowed to
     * impersonate. Currently this is only used for Enhanced Health reporting/monitoring
     *
     * @param serviceRoleName
     *            The name of the role that ElasticBeanstalk can assume
     */
    public void setServiceRoleName(String serviceRoleName) {
        setAttribute(PROPERTY_SERVICE_ROLE_NAME, serviceRoleName);
    }

    /**
     * Returns the name of the optional IAM role that the service (ElasticBeanstalk) is allowed to
     * impersonate. Currently this is only used for Enhanced Health reporting/monitoring
     *
     * @return The name of the role that ElasticBeanstalk can assume
     */
    public String getServiceRoleName() {
        return getAttribute(PROPERTY_SERVICE_ROLE_NAME, (String) null);
    }

    /**
     * Sets the id of the optional VPC that the service (ElasticBeanstalk) is to be deployed.
     *
     * @param vpcId
     *           The id of the VPC that ElasticBeanstalk will be deployed to.
     */
    public void setVpcId(String vpcId) {
        setAttribute(PROPERTY_VPC_ID, vpcId);
    }

    public String getVpcId() {
        return getAttribute(PROPERTY_VPC_ID, (String) null);
    }

    public void setSubnets(String subnets) {
        setAttribute(PROPERTY_SUBNETS, subnets);
    }

    public String getSubnets() {
        return getAttribute(PROPERTY_SUBNETS, (String) null);
    }

    public void setElbSubnets(String elbSubnets) {
        setAttribute(PROPERTY_ELB_SUBNETS, elbSubnets);
    }

    public String getElbSubnets() {
        return getAttribute(PROPERTY_ELB_SUBNETS, (String) null);
    }

    public void setElbScheme(String elbScheme) {
        setAttribute(PROPERTY_ELB_SCHEME, elbScheme);
    }

    public String getElbScheme() {
        return getAttribute(PROPERTY_ELB_SCHEME, (String) null);
    }

    public void setSecurityGroup(String securityGroup) {
        setAttribute(PROPERTY_SECURITY_GROUP, securityGroup);
    }

    public String getSecurityGroup() {
        return getAttribute(PROPERTY_SECURITY_GROUP, (String) null);
    }

    public void setAssociatePublicIpAddress(boolean associatePublicIpAddress) {
        setAttribute(PROPERTY_ASSOCIATE_PUBLIC_IP_ADDRESS, associatePublicIpAddress);
    }

    public boolean getAssociatePublicIpAddress() {
        return getAttribute(PROPERTY_ASSOCIATE_PUBLIC_IP_ADDRESS, false);
    }

    public void setSkipIamRoleAndInstanceProfileCreation(boolean skip) {
        setAttribute(PROPERTY_SKIP_IAM_ROLE_AND_INSTANCE_PROFILE_CREATION, skip);
    }

    public boolean isSkipIamRoleAndInstanceProfileCreation() {
        return getAttribute(PROPERTY_SKIP_IAM_ROLE_AND_INSTANCE_PROFILE_CREATION, false);
    }

    public String getWorkerQueueUrl() {
        return getAttribute(PROPERTY_WORKER_QUEUE_URL, (String) null);
    }

    public void setWorkerQueueUrl(String url) {
        setAttribute(PROPERTY_WORKER_QUEUE_URL, url);
    }

    /*
     * TODO: We can't quite turn this on yet because WTPWarUtils runs an operation that tries to lock
     *       the whole workspace when it exports the WAR for a project.  If we can figure out how to
     *       get that to not lock the whole workspace, then we can turn this back on.
     */
//    public boolean isUseProjectSpecificSchedulingRuleOnPublish() {
//        return true;
//    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#canModifyModules(org.eclipse.wst.server.core.IModule[], org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IStatus canModifyModules(IModule[] add, IModule[] remove) {
        // If we're not adding any modules, we know this request is fine
        if (add == null) {
            return Status.OK_STATUS;
        }

        if (add.length > 1) {
            return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Only one web application can run in each AWS Elastic Beanstalk environment");
        }

        for (IModule module : add) {
            String moduleTypeId = module.getModuleType().getId().toLowerCase();
            if (moduleTypeId.equals("jst.web") == false) {
                return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unsupported module type: " + module.getModuleType().getName());
            }

            if (module.getProject() != null) {
                IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
                if (status != null && !status.isOK()) {
                    return status;
                }
            }
        }

        return Status.OK_STATUS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#getChildModules(org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IModule[] getChildModules(IModule[] module) {
        if (module == null) {
            return null;
        }

        IModuleType moduleType = module[0].getModuleType();

        if (module.length == 1 && moduleType != null && "jst.web".equalsIgnoreCase(moduleType.getId())) {
            IWebModule webModule = (IWebModule)module[0].loadAdapter(IWebModule.class, null);
            if (webModule != null) {
                return webModule.getModules();
            }
        }

        return new IModule[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#getRootModules(org.eclipse.wst.server.core.IModule)
     */
    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        String moduleTypeId = module.getModuleType().getId().toLowerCase();
        if (moduleTypeId.equals("jst.web")) {
            IStatus status = canModifyModules(new IModule[] {module}, null);
            if (status == null || !status.isOK()) {
                throw new CoreException(status);
            }

            return new IModule[] {module};
        }

        return J2EEUtil.getWebModules(module, null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#modifyModules(org.eclipse.wst.server.core.IModule[], org.eclipse.wst.server.core.IModule[], org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
        IStatus status = canModifyModules(add, remove);
        if (status == null || !status.isOK()) {
            throw new CoreException(status);
        }

        if (add != null && add.length > 0 && getServer().getModules().length > 0) {
            ServerWorkingCopy serverWorkingCopy = (ServerWorkingCopy)getServer();
            serverWorkingCopy.modifyModules(new IModule[0], serverWorkingCopy.getModules(), monitor);
        }
    }

    public void setCachedEnvironmentDescription(EnvironmentDescription environmentDescription) {
        map.put(getServer().getId(), environmentDescription);
    }

    public EnvironmentDescription getCachedEnvironmentDescription() {
        return map.get(getServer().getId());
    }

    /*
     * Utility methods for communicating with environments
     */

    /**
     * Returns the environment's configured remote debugging port, or null if it
     * cannot be determined.
     */
    public static String getDebugPort(List<ConfigurationSettingsDescription> settings) {
        ConfigurationOptionSetting opt = Environment.getJVMOptions(settings);
        if ( opt != null ) {
            return getDebugPort(opt.getValue());
        }
        return null;
    }

    /**
     * Returns the debug port in the JVM options string given, or null if it isn't present.
     */
    public static String getDebugPort(String jvmOptions) {
        if ( jvmOptions.contains("-Xdebug") && jvmOptions.contains("-Xrunjdwp:") ) {
            Matcher matcher = Pattern.compile("-Xrunjdwp:\\S*address=(\\d+)").matcher(jvmOptions);
            if ( matcher.find() && matcher.groupCount() > 0 && matcher.group(1) != null ) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Returns the "JVM Options" configuration setting, if it exists, or null otherwise.
     */
    public static ConfigurationOptionSetting getJVMOptions(List<ConfigurationSettingsDescription> settings) {
        for (ConfigurationSettingsDescription setting : settings) {
            for (ConfigurationOptionSetting opt : setting.getOptionSettings()) {
                if (opt.getOptionName().equals("JVM Options") && opt.getNamespace().equals(ConfigurationOptionConstants.JVMOPTIONS)) {
                    return opt;
                }
            }
        }
        return null;
    }

    /**
     * Returns the security group name given in the configuration settings, or null if it cannot be determined.
     */
    public static String getSecurityGroup(List<ConfigurationSettingsDescription> settings) {
        for (ConfigurationSettingsDescription setting : settings) {
            for (ConfigurationOptionSetting opt : setting.getOptionSettings()) {
                if (opt.getOptionName().equals("SecurityGroups") && opt.getNamespace().equals(ConfigurationOptionConstants.LAUNCHCONFIGURATION)) {
                    return opt.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns whether the given port is open on the security group for the
     * environment settings given.
     */
    public boolean isIngressAllowed(int port, List<ConfigurationSettingsDescription> settings) {
        String securityGroup = Environment.getSecurityGroup(settings);

        if (securityGroup == null) {
            throw new RuntimeException("Couldn't determine security group of environent");
        }

        AmazonEC2 ec2 = getEc2Client();

        DescribeSecurityGroupsResult describeSecurityGroups = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(securityGroup));
        for (SecurityGroup group : describeSecurityGroups.getSecurityGroups()) {
            if (ingressAllowed(group, port)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see Environment#isIngressAllowed(int, List)
     */
    public boolean isIngressAllowed(String port, List<ConfigurationSettingsDescription> settings) {
        return isIngressAllowed(Integer.parseInt(port), settings);
    }

    /**
     * Returns an EC2 client configured to talk to the appropriate region for
     * this environment.
     */
    public AmazonEC2 getEc2Client() {
        return AwsToolkitCore.getClientFactory(getAccountId()).getEC2ClientByEndpoint(
                RegionUtils.getRegion(getRegionId()).getServiceEndpoint(ServiceAbbreviations.EC2));
    }

    /**
     * Returns whether the group given allows TCP ingress on the port given.
     */
    private boolean ingressAllowed(SecurityGroup group, int debugPortInt) {
        for (IpPermission permission : group.getIpPermissions()) {
            if ("tcp".equals(permission.getIpProtocol()) && permission.getIpRanges() != null && permission.getIpRanges().contains("0.0.0.0/0")) {
                if (permission.getFromPort() != null && permission.getFromPort() <= debugPortInt
                    && permission.getToPort() != null && permission.getToPort() >= debugPortInt) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the Beanstalk environment represented by this WTP server has been created
     * yet.
     */
    public boolean doesEnvironmentExistInBeanstalk() {
        return new ElasticBeanstalkClientExtensions(this).doesEnvironmentExist(getApplicationName(),
                getEnvironmentName());
    }

    /**
     * Returns the list of current settings for this environment
     */
    public List<ConfigurationSettingsDescription> getCurrentSettings() {
        if (doesEnvironmentExistInBeanstalk() == false) {
            return new ArrayList<>();
        }

        AWSElasticBeanstalk beanstalk = getClient();
        return beanstalk.describeConfigurationSettings(
            new DescribeConfigurationSettingsRequest().withEnvironmentName(getEnvironmentName())
            .withApplicationName(getApplicationName())).getConfigurationSettings();
    }

    /**
     * Returns a client for this environment.
     */
    public AWSElasticBeanstalk getClient() {
        AccountInfo account = AwsToolkitCore.getDefault()
                .getAccountManager()
                    .getAccountInfo(getAccountId());

        //TODO: better way to handle this
        if (account == null) {
            // Fall back to the current account
            account = AwsToolkitCore.getDefault().getAccountInfo();
        }

        return AwsToolkitCore.getClientFactory(account.getInternalAccountId())
                .getElasticBeanstalkClientByEndpoint(getRegionEndpoint());
    }

    /**
     * Opens up the port given on the security group for the environment
     * settings given.
     */
    public void openSecurityGroupPort(int debugPort, String securityGroup) {
        getEc2Client().authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest().withCidrIp("0.0.0.0/0")
                .withFromPort(debugPort).withToPort(debugPort).withIpProtocol("tcp")
                .withGroupName(securityGroup));
    }

    /**
     * @see Environment#openSecurityGroupPort(int, String)
     */
    public void openSecurityGroupPort(String debugPort, String securityGroup) {
        openSecurityGroupPort(Integer.parseInt(debugPort), securityGroup);
    }

    /**
     * Returns the EC2 instance IDs being used by this environment.
     */
    public List<String> getEC2InstanceIds() {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(getAccountId())
                .getElasticBeanstalkClientByEndpoint(getRegionEndpoint());
        DescribeEnvironmentResourcesResult describeEnvironmentResources = client
                .describeEnvironmentResources(new DescribeEnvironmentResourcesRequest()
                        .withEnvironmentName(getEnvironmentName()));
        List<String> instanceIds = new ArrayList<>(describeEnvironmentResources.getEnvironmentResources()
                .getInstances().size());
        for ( Instance i : describeEnvironmentResources.getEnvironmentResources().getInstances() ) {
            instanceIds.add(i.getId());
        }
        return instanceIds;
    }

    /**
     * We change the data model to save the server environment information. This
     * function is used to convert the old data format to the new one.
     */
    public void convertLegacyServer() {
        String regionEndpoint = null;
        if ((getRegionId() == null) && ((regionEndpoint = getAttribute(PROPERTY_REGION_ENDPOINT, (String)null)) != null)) {
            setAttribute(PROPERTY_REGION_ID, RegionUtils.getRegionByEndpoint(regionEndpoint).getId());
        }

    }

    public static String catSubnetList(Set<String> subnetList) {
        if (null == subnetList || subnetList.isEmpty()) {
            return "";
        }
        Iterator<String> iterator = subnetList.iterator();
        StringBuilder builder = new StringBuilder(iterator.next());
        while (iterator.hasNext()) {
            builder.append(",");
            builder.append(iterator.next());
        }

        return builder.toString();
    }

}
