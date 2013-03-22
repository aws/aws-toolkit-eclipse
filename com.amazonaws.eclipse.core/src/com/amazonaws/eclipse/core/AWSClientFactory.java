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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Status;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudfront_2012_03_15.AmazonCloudFront;
import com.amazonaws.services.cloudfront_2012_03_15.AmazonCloudFrontClient;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Factory for creating AWS clients.
 */
public class AWSClientFactory {

    /**
     * This constant is intended only for testing so that unit tests can
     * override the Eclipse preference store implementation of AccountInfo.
     */
    public static final String ACCOUNT_INFO_OVERRIDE_PROPERTY = "com.amazonaws.eclipse.test.AccountInfoOverride";

    /** Manages the cached client objects. */
    private CachedClients cachedClients = new CachedClients();

    /**
     * A shared account info object for accessing the user's credentials.
     */
    private final AccountInfo accountInfo;

    /**
     * Constructs a client factory that uses the given AWS account
     * for its credentials.
     */
    public AWSClientFactory(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;

        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if ( plugin != null ) {
            plugin.getProxyService().addProxyChangeListener(new IProxyChangeListener() {
                public void proxyInfoChanged(IProxyChangeEvent event) {
                    cachedClients.invalidateClients();
                }
            });

            plugin.addAccountInfoChangeListener(new AccountInfoChangeListener() {
                public void currentAccountChanged() {
                    cachedClients.invalidateClients();
                }
            });
        }
    }

    /*
     * Simple getters return a client configured with the default endpoint for
     * the currently selected region.
     */

    public AmazonCloudFront getCloudFrontClient() {
        return getCloudFrontClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.CLOUDFRONT));
    }

    public AmazonS3 getAnonymousS3Client() {
        /*
         * We hardcode the S3 endpoint here, since this method is used to download the
         * initial regions.xml file, so we can't necessarily look up an endpoint yet.
         *
         * In the future, we should ship some version of the regions.xml file, so that we
         * always have something available, even the first time the user runs this code.
         */
        String serviceEndpoint = "https://s3.amazonaws.com";
        ClientConfiguration clientConfiguration = createClientConfiguration(serviceEndpoint);
        clientConfiguration.setProtocol(Protocol.HTTP);
        return new AmazonS3Client((AWSCredentials)null, clientConfiguration);
    }

    public AmazonS3 getS3Client() {
        return getS3ClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.S3));
    }

    /**
     * Returns a client for the region where the given bucket resides. No
     * caching is performed on the region lookup.
     */
    public AmazonS3 getS3ClientForBucket(String bucketName) {
        String serviceEndpoint = getS3BucketEndpoint(bucketName);
        return getS3ClientByEndpoint(serviceEndpoint);
    }

    /**
     * Returns the endpoint appropriate to the given bucket.
     */
    public String getS3BucketEndpoint(String bucketName) {
        AmazonS3 globalS3Client = getS3Client();
        String bucketLocation = globalS3Client.getBucketLocation(bucketName);
        String region = bucketLocation;
        if ( bucketLocation == null || bucketLocation.equals("US") ) {
            region = "us-east-1";
        }
        String serviceEndpoint = RegionUtils.getRegion(region).getServiceEndpoint(ServiceAbbreviations.S3);
        return serviceEndpoint;
    }
    
    public AmazonSimpleDB getSimpleDBClient() {
        return getSimpleDBClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.SIMPLEDB));
    }

    public AmazonRDS getRDSClient() {
        return getRDSClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.RDS));
    }

    public AmazonSQS getSQSClient() {
        return getSQSClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.SQS));
    }

    public AmazonSNS getSNSClient() {
        return getSNSClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.SNS));
    }

    public AmazonDynamoDB getDynamoDBClient() {
        return getDynamoDBClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.DYNAMODB));
    }
    
    public AWSSecurityTokenService getSecurityTokenServiceClient() {
        return getSecurityTokenServiceByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.STS));
    }

    public AWSElasticBeanstalk getElasticBeanstalkClient() {
        return getElasticBeanstalkClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.BEANSTALK));
    }

    public AmazonIdentityManagement getIAMClient() {
        return getIAMClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.IAM));
    }
    
    public AmazonCloudFormation getCloudFormationClient() {
        return getCloudFormationClientByEndpoint(RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.CLOUD_FORMATION));
    }

    /*
     * Endpoint-specific getters return clients that use the endpoint given.
     */

    public AmazonIdentityManagement getIAMClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonIdentityManagementClient.class);
    }

    public AmazonCloudFront getCloudFrontClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonCloudFrontClient.class);
    }

    public AmazonS3 getS3ClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonS3Client.class);
    }

    public AmazonEC2 getEC2ClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonEC2Client.class);
    }

    public AmazonSimpleDB getSimpleDBClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSimpleDBClient.class);
    }

    public AmazonRDS getRDSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonRDSClient.class);
    }

    public AmazonSQS getSQSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSQSClient.class);
    }

    public AmazonSNS getSNSClientByEndpoint(final String endpoint) {
        return getOrCreateClient(endpoint, AmazonSNSClient.class);
    }

    public AWSElasticBeanstalk getElasticBeanstalkClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSElasticBeanstalkClient.class);
    }

    public AmazonElasticLoadBalancing getElasticLoadBalancingClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonElasticLoadBalancingClient.class);
    }

    public AmazonAutoScaling getAutoScalingClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonAutoScalingClient.class);
    }

    public AmazonDynamoDB getDynamoDBClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonDynamoDBClient.class);
    }

    public AWSSecurityTokenService getSecurityTokenServiceByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AWSSecurityTokenServiceClient.class);
    }
    
    public AmazonCloudFormation getCloudFormationClientByEndpoint(String endpoint) {
        return getOrCreateClient(endpoint, AmazonCloudFormationClient.class);
    }

    private <T extends AmazonWebServiceClient> T getOrCreateClient(String endpoint, Class<T> clientClass) {
        synchronized (clientClass) {
            if ( cachedClients.getClient(endpoint, clientClass) == null ) {
                cachedClients.cacheClient(endpoint, createClient(endpoint, clientClass));
            }
        }

        return cachedClients.getClient(endpoint, clientClass);
    }

    private <T extends AmazonWebServiceClient> T createClient(String endpoint, Class<T> clientClass) {
        try {
            Constructor<T> constructor = clientClass.getConstructor(AWSCredentials.class, ClientConfiguration.class);
            ClientConfiguration config = createClientConfiguration(endpoint);

            AWSCredentials credentials = new BasicAWSCredentials(accountInfo.getAccessKey(), accountInfo.getSecretKey());

            T client = constructor.newInstance(credentials, config);
            client.setEndpoint(endpoint);

            return client;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create client: " + e.getMessage(), e);
        }
    }

    private static ClientConfiguration createClientConfiguration(String secureEndpoint) {
        ClientConfiguration config = new ClientConfiguration();
        AwsClientUtils clientUtils = new AwsClientUtils();

        config.setUserAgent(clientUtils.formUserAgentString("AWS-Toolkit-For-Eclipse", AwsToolkitCore.getDefault()));

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
                    plugin.getLog().log(new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, e.getMessage(), e));
                }
            }
        }
        return config;
    }

    /**
     * Responsible for managing the various AWS client objects needed for each service/region combination.
     */
    private static class CachedClients {

        private final Map<Class<?>, Map<String, Object>> cachedClientsByEndpoint = Collections
                .synchronizedMap(new HashMap<Class<?>, Map<String, Object>>());

        @SuppressWarnings("unchecked")
        public <T> T getClient(String endpoint, Class<T> clientClass) {
            if (cachedClientsByEndpoint.get(clientClass) == null) return null;
            return (T)cachedClientsByEndpoint.get(clientClass).get(endpoint);
        }

        public <T> void cacheClient(String endpoint, T client) {
            if (cachedClientsByEndpoint.get(client.getClass()) == null) {
                cachedClientsByEndpoint.put(client.getClass(), new HashMap<String, Object>());
            }

            Map<String, Object> map = cachedClientsByEndpoint.get(client.getClass());
            map.put(endpoint, client);
        }

        public synchronized void invalidateClients() {
            cachedClientsByEndpoint.clear();
        }
    }

}
