/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.Service;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.AWSCodeCommitClient;
import com.amazonaws.services.codecommit.AWSCodeCommitClientBuilder;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClientBuilder;
import com.amazonaws.services.codestar.AWSCodeStar;
import com.amazonaws.services.codestar.AWSCodeStarClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.AWSOpsWorksClient;
import com.amazonaws.services.opsworks.AWSOpsWorksClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.AmazonSimpleDBClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * Factory for creating AWS clients.
 */
public class AWSClientFactory {

    /**
     * This constant is intended only for testing so that unit tests can
     * override the Eclipse preference store implementation of AccountInfo.
     */
    public static final String ACCOUNT_INFO_OVERRIDE_PROPERTY = "com.amazonaws.eclipse.test.AccountInfoOverride";

    /** Manages the cached client objects by endpoint. */
    private CachedClients cachedClientsByEndpoint = new CachedClients();

    /** Manages the cached client objects by region. **/
    private CachedClients cachedClients = new CachedClients();

    private final String accountId;

    /**
     * The account info for accessing the user's credentials.
     */
    private AccountInfo accountInfo;

    private AWSCredentialsProvider credentialsProviderOverride;

    /**
     * Constructs a client factory that uses the given account identifier to
     * retrieve its credentials.
     */
    public AWSClientFactory(String accountId) {

        AwsToolkitCore plugin = AwsToolkitCore.getDefault();

        this.accountId = accountId;
        accountInfo = plugin.getAccountManager().getAccountInfo(accountId);

        plugin.getProxyService().addProxyChangeListener(this::onProxyChange);
        plugin.getAccountManager().addAccountInfoChangeListener(this::onAccountInfoChange);
    }

    /**
     * Decoupling AWS Client Factory from AwsToolkitCore for testing purpose only.
     * @TestOnly
     */
    public AWSClientFactory(AWSCredentialsProvider credentialsProvider) {
        this.accountId = null;
        this.credentialsProviderOverride = credentialsProvider;
    }

    private void onProxyChange(IProxyChangeEvent e) {
        onAccountInfoChange();
    }

    private void onAccountInfoChange() {
        // When the system AWS accounts refresh, we need to refresh the member variable accountInfo as well in case it is still referencing the previous credentials.
        accountInfo = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(accountId);
        cachedClientsByEndpoint.invalidateClients();
        cachedClients.invalidateClients();
    }

    // Returns an anonymous S3 client in us-east-1 region for fetching public-read files.
    public static AmazonS3 getAnonymousS3Client() {
        final String serviceEndpoint = RegionUtils.S3_US_EAST_1_REGIONAL_ENDPOINT;
        final String serviceRegion = Regions.US_EAST_1.getName();
        ClientConfiguration clientConfiguration = createClientConfiguration(serviceEndpoint);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, serviceRegion))
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    /**
     * Returns a client for the region where the given bucket resides. No
     * caching is performed on the region lookup.
     */
    public AmazonS3 getS3ClientForBucket(String bucketName) {
        return getS3ClientByRegion(getS3BucketRegion(bucketName));
    }

    /**
     * Returns the endpoint appropriate to the given bucket.
     */
    public String getS3BucketEndpoint(String bucketName) {
        return RegionUtils.getRegion(getS3BucketRegion(bucketName))
                .getServiceEndpoint(ServiceAbbreviations.S3);
    }

    /**
     * Returns the standard region the bucket is located.
     */
    private String getS3BucketRegion(String bucketName) {
        AmazonS3 s3Client = getS3Client();
        String region = s3Client.getBucketLocation(bucketName);
        if (region == null || region.equals("US") ) {
            region = Regions.US_EAST_1.getName();
        }
        return region;
    }

    /*
     * Simple getters return a client configured with the currently selected region.
     */

    public AmazonS3 getS3Client() {
        return getS3ClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonCloudFront getCloudFrontClient() {
        return getCloudFrontClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonSimpleDB getSimpleDBClient() {
        return getSimpleDBClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonRDS getRDSClient() {
        return getRDSClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonSQS getSQSClient() {
        return getSQSClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonSNS getSNSClient() {
        return getSNSClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonDynamoDB getDynamoDBV2Client() {
        return getDynamoDBClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSSecurityTokenService getSecurityTokenServiceClient() {
        return getSecurityTokenServiceByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSElasticBeanstalk getElasticBeanstalkClient() {
        return getElasticBeanstalkClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonIdentityManagement getIAMClient() {
        return getIAMClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonCloudFormation getCloudFormationClient() {
        return getCloudFormationClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonEC2 getEC2Client() {
        return getEC2ClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AmazonCodeDeploy getCodeDeployClient() {
        return getCodeDeployClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSOpsWorks getOpsWorksClient() {
        return getOpsWorksClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSLambda getLambdaClient() {
        return getLambdaClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSCodeCommit getCodeCommitClient() {
        return getCodeCommitClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSCodeStar getCodeStarClient() {
        return getCodeStarClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSLogs getLogsClient() {
        return getLogsClientByRegion(RegionUtils.getCurrentRegion().getId());
    }

    public AWSKMS getKmsClient() {
        return getKmsClientByRegion(RegionUtils.getCurrentRegion().getId());
    }
    
    public AWSSecurityTokenService getSTSClient() {
        return getSecurityTokenServiceByRegion(RegionUtils.getCurrentRegion().getId());
    }

    /*
     * Endpoint-specific getters return clients that use the endpoint given.
     */

    @Deprecated
    public AmazonIdentityManagement getIAMClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonIdentityManagementClient.class);
    }

    @Deprecated
    public AmazonCloudFront getCloudFrontClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonCloudFrontClient.class);
    }

    @Deprecated
    public AmazonEC2 getEC2ClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonEC2Client.class);
    }

    @Deprecated
    public AmazonSimpleDB getSimpleDBClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSimpleDBClient.class);
    }

    @Deprecated
    public AmazonRDS getRDSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonRDSClient.class);
    }

    @Deprecated
    public AmazonSQS getSQSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSQSClient.class);
    }

    @Deprecated
    public AmazonSNS getSNSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSNSClient.class);
    }

    @Deprecated
    public AWSElasticBeanstalk getElasticBeanstalkClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSElasticBeanstalkClient.class);
    }

    @Deprecated
    public AmazonElasticLoadBalancing getElasticLoadBalancingClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonElasticLoadBalancingClient.class);
    }

    @Deprecated
    public AmazonAutoScaling getAutoScalingClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonAutoScalingClient.class);
    }

    @Deprecated
    public AmazonDynamoDB getDynamoDBClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonDynamoDBClient.class);
    }

    @Deprecated
    public com.amazonaws.services.dynamodbv2.AmazonDynamoDB getDynamoDBV2ClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient.class);
    }

    @Deprecated
    public AmazonCloudFormation getCloudFormationClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonCloudFormationClient.class);
    }

    @Deprecated
    public AmazonCodeDeploy getCodeDeployClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonCodeDeployClient.class);
    }

    @Deprecated
    public AWSOpsWorks getOpsWorksClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSOpsWorksClient.class);
    }

    @Deprecated
    public AWSLambda getLambdaClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSLambdaClient.class);
    }

    @Deprecated
    public AWSCodeCommit getCodeCommitClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSCodeCommitClient.class);
    }

    /**
     * Return an AWS client of the specified region by using the ClientBuilder.
     *
     * @param regionId - region id for the client, ex. us-esat-1
     * @return The cached client if already created, otherwise, create a new one.
     *         Return null if the specified regionId is invalid.
     */

    public AmazonIdentityManagement getIAMClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.IAM, regionId,
                AmazonIdentityManagementClientBuilder.standard(), AmazonIdentityManagement.class, true);
    }

    public AmazonCloudFront getCloudFrontClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.CLOUDFRONT, regionId,
                AmazonCloudFrontClientBuilder.standard(), AmazonCloudFront.class, true);
    }

    /**
     * Return regional S3 client other than the global one which is built by the default S3 client builder.
     */
    public AmazonS3 getS3ClientByRegion(String regionId) {
        if (Regions.US_EAST_1.getName().equals(regionId)) {
            synchronized (AmazonS3.class) {
                if ( cachedClients.getClient(regionId, AmazonS3.class) == null ) {
                    cachedClients.cacheClient(regionId, AmazonS3.class, createS3UsEast1RegionalClient());
                }
            }

            return cachedClients.getClient(regionId, AmazonS3.class);
        } else {
            return getOrCreateClientByRegion(ServiceAbbreviations.S3, regionId, AmazonS3ClientBuilder.standard(),
                    AmazonS3.class);
        }
    }

    public AmazonEC2 getEC2ClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.EC2, regionId,
                AmazonEC2ClientBuilder.standard(), AmazonEC2.class);
    }

    public AmazonSimpleDB getSimpleDBClientByRegion(final String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.SIMPLEDB, regionId,
                AmazonSimpleDBClientBuilder.standard(), AmazonSimpleDB.class);
    }

    public AmazonRDS getRDSClientByRegion(final String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.RDS, regionId,
                AmazonRDSClientBuilder.standard(), AmazonRDS.class);
    }

    public AmazonSQS getSQSClientByRegion(final String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.SQS, regionId,
                AmazonSQSClientBuilder.standard(), AmazonSQS.class);
    }

    public AmazonSNS getSNSClientByRegion(final String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.SNS, regionId,
                AmazonSNSClientBuilder.standard(), AmazonSNS.class);
    }

    public AWSElasticBeanstalk getElasticBeanstalkClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.BEANSTALK, regionId,
                AWSElasticBeanstalkClientBuilder.standard(), AWSElasticBeanstalk.class);
    }

    public AmazonElasticLoadBalancing getElasticLoadBalancingClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.ELB, regionId,
                AmazonElasticLoadBalancingClientBuilder.standard(), AmazonElasticLoadBalancing.class);
    }

    public AmazonAutoScaling getAutoScalingClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.AUTOSCALING, regionId,
                AmazonAutoScalingClientBuilder.standard(), AmazonAutoScaling.class);
    }

    public AmazonDynamoDB getDynamoDBClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.DYNAMODB, regionId,
                AmazonDynamoDBClientBuilder.standard(), AmazonDynamoDB.class);
    }

    public AWSSecurityTokenService getSecurityTokenServiceByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.STS, regionId,
                AWSSecurityTokenServiceClientBuilder.standard(), AWSSecurityTokenService.class, true);
    }

    public AmazonCloudFormation getCloudFormationClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.CLOUD_FORMATION, regionId,
                AmazonCloudFormationClientBuilder.standard(), AmazonCloudFormation.class);
    }

    public AmazonCodeDeploy getCodeDeployClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.CODE_DEPLOY, regionId,
                AmazonCodeDeployClientBuilder.standard(), AmazonCodeDeploy.class);
    }

    public AWSOpsWorks getOpsWorksClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.OPSWORKS, regionId,
                AWSOpsWorksClientBuilder.standard(), AWSOpsWorks.class);
    }

    public AWSLambda getLambdaClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.LAMBDA, regionId,
                AWSLambdaClientBuilder.standard(), AWSLambda.class);
    }

    public AWSCodeCommit getCodeCommitClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.CODECOMMIT, regionId,
                AWSCodeCommitClientBuilder.standard(), AWSCodeCommit.class);
    }

    public AWSCodeStar getCodeStarClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.CODESTAR, regionId,
                AWSCodeStarClientBuilder.standard(), AWSCodeStar.class);
    }

    public AWSLogs getLogsClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.LOGS, regionId,
                AWSLogsClientBuilder.standard(), AWSLogs.class);
    }

    public AWSKMS getKmsClientByRegion(String regionId) {
        return getOrCreateClientByRegion(ServiceAbbreviations.KMS, regionId,
                AWSKMSClientBuilder.standard(), AWSKMS.class);
    }

    @Deprecated
    private <T extends AmazonWebServiceClient> T getOrCreateClient(String endpoint, Class<T> clientClass) {
        synchronized (clientClass) {
            if ( cachedClientsByEndpoint.getClient(endpoint, clientClass) == null ) {
                cachedClientsByEndpoint.cacheClient(endpoint, createClient(endpoint, clientClass));
            }
        }

        return cachedClientsByEndpoint.getClient(endpoint, clientClass);
    }

    private <T> T getOrCreateClientByRegion(String serviceName, String regionId,
            AwsSyncClientBuilder<? extends AwsSyncClientBuilder, T> builder, Class<T> clientClass, boolean isGlobalClient) {
        Region region = RegionUtils.getRegion(regionId);
        if (region == null) {
            return null;
        }

        synchronized (clientClass) {
            if ( cachedClients.getClient(regionId, clientClass) == null ) {
                cachedClients.cacheClient(regionId, clientClass, createClientByRegion(builder, serviceName, region, isGlobalClient));
            }
        }

        return cachedClients.getClient(regionId, clientClass);
    }

    private <T> T getOrCreateClientByRegion(String serviceName, String regionId,
            AwsSyncClientBuilder<? extends AwsSyncClientBuilder, T> builder, Class<T> clientClass) {
        return getOrCreateClientByRegion(serviceName, regionId, builder, clientClass, false);
    }

    /**
     * @deprecated for {@link #createClientByRegion(AwsSyncClientBuilder, String, String)}
     */
    @Deprecated
    private <T extends AmazonWebServiceClient> T createClient(String endpoint, Class<T> clientClass) {
        try {
            Constructor<T> constructor = clientClass.getConstructor(AWSCredentials.class, ClientConfiguration.class);
            ClientConfiguration config = createClientConfiguration(endpoint);

            Service service = RegionUtils.getServiceByEndpoint(endpoint);
            config.setSignerOverride(service.getSignerOverride());

            AWSCredentials credentials = null;
            if (accountInfo.isUseSessionToken()) {
                credentials = new BasicSessionCredentials(
                        accountInfo.getAccessKey(), accountInfo.getSecretKey(),
                        accountInfo.getSessionToken());
            } else {
                credentials = new BasicAWSCredentials(
                        accountInfo.getAccessKey(), accountInfo.getSecretKey());
            }

            T client = constructor.newInstance(credentials, config);

            /*
             * If a serviceId is explicitly specified with the region metadata,
             * and this client has a 3-argument form of setEndpoint (for sigv4
             * overrides), then explicitly pass in the service Id and region Id
             * in case it can't be parsed from the endpoint URL by the default
             * setEndpoint method.
             */

            Method sigv4SetEndpointMethod = lookupSigV4SetEndpointMethod(clientClass);
            if (service.getServiceId() != null && sigv4SetEndpointMethod != null) {
                Region region = RegionUtils.getRegionByEndpoint(endpoint);
                sigv4SetEndpointMethod.invoke(client, endpoint, service.getServiceId(), region.getId());
            } else {
                client.setEndpoint(endpoint);
            }

            return client;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create client: " + e.getMessage(), e);
        }
    }

    // Low layer method for building a service client by using the client builder.
    private <T> T createClientByRegion(AwsSyncClientBuilder<? extends AwsSyncClientBuilder, T> builder, String serviceName, Region region,
            boolean isGlobalClient) {
        String endpoint = region.getServiceEndpoint(serviceName);
        String signingRegion = isGlobalClient ? region.getGlobalRegionSigningRegion() : region.getId();
        if (endpoint != null && signingRegion != null) {
            builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));
        }
        builder.withCredentials(new AWSStaticCredentialsProvider(getAwsCredentials())).withClientConfiguration(createClientConfiguration(endpoint));

        return (T) builder.build();
    }

    /**
     * Return the regional us-east-1 S3 client.
     */
    private AmazonS3 createS3UsEast1RegionalClient() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(getAwsCredentials()))
                .withClientConfiguration(createClientConfiguration(RegionUtils.S3_US_EAST_1_REGIONAL_ENDPOINT))
                .withEndpointConfiguration(new EndpointConfiguration(RegionUtils.S3_US_EAST_1_REGIONAL_ENDPOINT, Regions.US_EAST_1.getName()))
                .build();
    }

    /**
     * Returns the 3-argument form of setEndpoint that is used to override
     * values for sigv4 signing, or null if the specified class does not support
     * that version of setEndpoint.
     *
     * @param clientClass
     *            The class to introspect.
     *
     * @return The 3-argument method form of setEndpoint, or null if it doesn't
     *         exist in the specified class.
     */
    private <T extends AmazonWebServiceClient> Method lookupSigV4SetEndpointMethod(Class<T> clientClass) {
        try {
            return clientClass.getMethod("setEndpoint", String.class, String.class, String.class);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to lookup class methods via reflection", e);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private AWSCredentials getAwsCredentials() {
        AWSCredentials credentials = null;

        if (credentialsProviderOverride != null) {
            credentials = credentialsProviderOverride.getCredentials();
        } else if (accountInfo.isUseSessionToken()) {
            credentials = new BasicSessionCredentials(
                    accountInfo.getAccessKey(), accountInfo.getSecretKey(),
                    accountInfo.getSessionToken());
        } else {
            credentials = new BasicAWSCredentials(
                    accountInfo.getAccessKey(), accountInfo.getSecretKey());
        }

        return credentials;
    }

    private static ClientConfiguration createClientConfiguration(String secureEndpoint) {
        ClientConfiguration config = new ClientConfiguration();

        IPreferenceStore preferences =
            AwsToolkitCore.getDefault().getPreferenceStore();

        int connectionTimeout =
            preferences.getInt(PreferenceConstants.P_CONNECTION_TIMEOUT);
        int socketTimeout =
            preferences.getInt(PreferenceConstants.P_SOCKET_TIMEOUT);

        config.setConnectionTimeout(connectionTimeout);
        config.setSocketTimeout(socketTimeout);

        config.setUserAgentPrefix(AwsClientUtils.formatUserAgentString("AWS-Toolkit-For-Eclipse", AwsToolkitCore.getDefault()));

        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if ( plugin != null ) {
            IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
            if ( proxyService.isProxiesEnabled() ) {
                try {
                    IProxyData[] proxyData;
                    proxyData = proxyService.select(new URI(secureEndpoint));
                    if ( proxyData.length > 0 ) {
                        config.setProxyHost(proxyData[0].getHost());
                        config.setProxyPort(proxyData[0].getPort());

                        if ( proxyData[0].isRequiresAuthentication() ) {
                            config.setProxyUsername(proxyData[0].getUserId());
                            config.setProxyPassword(proxyData[0].getPassword());
                        }
                    }
                } catch ( URISyntaxException e ) {
                    plugin.logError(e.getMessage(), e);
                }
            }
        }
        return config;
    }

    /**
     * Responsible for managing the various AWS client objects needed for each service/region combination.
     * This class is thread safe.
     */
    private static class CachedClients {

        private final Map<Class<?>, Map<String, Object>> cachedClients = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public synchronized <T> T getClient(String region, Class<T> clientClass) {
            if (cachedClients.get(clientClass) == null) {
                return null;
            }
            return (T)cachedClients.get(clientClass).get(region);
        }

        public synchronized <T> void cacheClient(String region, T client) {
            cacheClient(region, null, client);
        }

        public <T> void cacheClient(String region, Class<T> clazz, T client) {
            Class<?> key = clazz == null ? client.getClass() : clazz;
            if (cachedClients.get(key) == null) {
                cachedClients.put(key, new ConcurrentHashMap<String, Object>());
            }

            Map<String, Object> map = cachedClients.get(key);
            map.put(region, client);
        }

        public synchronized void invalidateClients() {
            cachedClients.clear();
        }
    }
}